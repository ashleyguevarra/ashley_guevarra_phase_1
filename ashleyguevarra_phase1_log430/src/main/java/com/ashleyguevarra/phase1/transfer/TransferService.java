package com.ashleyguevarra.phase1.transfer;

import com.ashleyguevarra.phase1.account.dto.CompleteLedgerSagaRequest;
import com.ashleyguevarra.phase1.account.dto.CompensateDebitSagaRequest;
import com.ashleyguevarra.phase1.account.dto.CreditSagaRequest;
import com.ashleyguevarra.phase1.account.dto.DebitSagaRequest;
import com.ashleyguevarra.phase1.account.dto.InstantCurrencyResponse;
import com.ashleyguevarra.phase1.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("transfer")
public class TransferService {

    private final TransferRepository transfers;
    private final RestClient accountServiceClient;

    public TransferService(TransferRepository transfers,
                           @Value("${canbankx.account-service.base-url:http://account-service:8080}") String accountServiceBaseUrl) {
        this.transfers = transfers;
        this.accountServiceClient = RestClient.builder()
            .baseUrl(accountServiceBaseUrl)
            .build();
    }

    @Caching(evict = {
            @CacheEvict(value = "accountBalance", key = "#fromAccountId.toString() + ':' + #customerId.toString()"),
            @CacheEvict(value = "accountBalance", allEntries = true)
    })
    public Transfer createTransfer(UUID customerId,
                                  UUID fromAccountId,
                                  UUID toAccountId,
                                  long amountCents,
                                  String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required");
        }
        if (amountCents <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be > 0");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT", "Source and destination accounts must be different");
        }

        var existing = transfers.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Transfer transfer;
        try {
            Transfer pending = new Transfer(fromAccountId, toAccountId, amountCents, "PENDING", idempotencyKey);
            transfer = transfers.save(pending);
        } catch (DataIntegrityViolationException e) {
            // Transfer already created concurrently (unique idempotency_key).
            transfer = transfers.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "DB error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())));
        }

        // If already completed, return it (idempotency).
        if ("COMPLETED".equals(transfer.getStatus())) {
            return transfer;
        }

        String normalizedCurrency;
        try {
            InstantCurrencyResponse debitResponse = accountServiceClient.post()
                .uri("/internal/saga/debit")
                .body(new DebitSagaRequest(transfer.getId(), fromAccountId, toAccountId, customerId, amountCents))
                .retrieve()
                .body(InstantCurrencyResponse.class);
            normalizedCurrency = debitResponse.currency();
        } catch (RestClientResponseException e) {
            updateTransferStatusSafe(transfer.getId(), "FAILED");
            throw new ApiException(HttpStatus.valueOf(e.getRawStatusCode()), "SAGA_DEBIT_FAILED", "Debit step failed");
        }

        try {
            accountServiceClient.post()
                .uri("/internal/saga/credit")
                .body(new CreditSagaRequest(transfer.getId(), toAccountId, amountCents, normalizedCurrency))
                .retrieve()
                .toBodilessEntity();

            try {
                accountServiceClient.post()
                    .uri("/internal/saga/complete-ledger")
                    .body(new CompleteLedgerSagaRequest(transfer.getId(), fromAccountId, toAccountId, amountCents))
                    .retrieve()
                    .toBodilessEntity();
            } catch (RestClientResponseException ledgerEx) {
                // Ledger failed but balances are correct. Mark complete; ledger can be reconciled later.
            }

            updateTransferStatusSafe(transfer.getId(), "COMPLETED");
            return transfers.findById(transfer.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Transfer not found"));
        } catch (RestClientResponseException e) {
            // Credit failed after debit: compensate.
            compensateDebitSafe(transfer.getId(), fromAccountId, amountCents, normalizedCurrency);
            updateTransferStatusSafe(transfer.getId(), "FAILED");
            throw new ApiException(HttpStatus.valueOf(e.getRawStatusCode()), "SAGA_CREDIT_FAILED", "Credit step failed");
        }
    }

    private void updateTransferStatusSafe(UUID transferId, String status) {
        Transfer t = transfers.findById(transferId)
            .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Transfer not found"));
        t.setStatus(status);
        transfers.save(t);
    }

    private void compensateDebitSafe(UUID transferId, UUID fromAccountId, long amountCents, String currency) {
        try {
            accountServiceClient.post()
                .uri("/internal/saga/compensate-debit")
                .body(new CompensateDebitSagaRequest(transferId, fromAccountId, amountCents, currency))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception ignored) {
            // Compensation best-effort; balances should be restored eventually via idempotent saga steps.
        }
    }
}