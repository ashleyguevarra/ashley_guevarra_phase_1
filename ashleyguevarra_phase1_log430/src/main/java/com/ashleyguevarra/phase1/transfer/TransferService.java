package com.ashleyguevarra.phase1.transfer;

import com.ashleyguevarra.phase1.account.Account;
import com.ashleyguevarra.phase1.account.AccountRepository;
import com.ashleyguevarra.phase1.api.ApiException;
import com.ashleyguevarra.phase1.audit.AuditLog;
import com.ashleyguevarra.phase1.audit.AuditLogRepository;
import com.ashleyguevarra.phase1.ledger.LedgerEntry;
import com.ashleyguevarra.phase1.ledger.LedgerEntryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransferService {

    private final TransferRepository transfers;
    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;
    private final AuditLogRepository audit;

    public TransferService(TransferRepository transfers,
                           AccountRepository accounts,
                           LedgerEntryRepository ledger,
                           AuditLogRepository audit) {
        this.transfers = transfers;
        this.accounts = accounts;
        this.ledger = ledger;
        this.audit = audit;
    }

    @Transactional
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

        try {
            Account from = accounts.findByIdForUpdate(fromAccountId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Source account not found"));

            Account to = accounts.findByIdForUpdate(toAccountId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Destination account not found"));

            if (!from.getCustomerId().equals(customerId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN_RESOURCE", "Account does not belong to customer");
            }

            if (!"ACTIVE".equals(from.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_INACTIVE", "Source account is not ACTIVE");
            }
            if (!"ACTIVE".equals(to.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_INACTIVE", "Destination account is not ACTIVE");
            }

            if (!from.getCurrency().equals(to.getCurrency())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH", "Accounts must share same currency");
            }

            if (from.getBalanceCents() < amountCents) {
                throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "Insufficient funds");
            }

            from.setBalanceCents(from.getBalanceCents() - amountCents);
            to.setBalanceCents(to.getBalanceCents() + amountCents);
            accounts.save(from);
            accounts.save(to);

            Transfer transfer = new Transfer(fromAccountId, toAccountId, amountCents, "COMPLETED", idempotencyKey);
            Transfer savedTransfer = transfers.save(transfer);

            ledger.save(new LedgerEntry(fromAccountId, "DEBIT", amountCents, "TRANSFER " + savedTransfer.getId()));
            ledger.save(new LedgerEntry(toAccountId, "CREDIT", amountCents, "TRANSFER " + savedTransfer.getId()));

            audit.save(new AuditLog(
                    "TRANSFER_CREATED",
                    "TRANSFER",
                    savedTransfer.getId(),
                    "{\"from\":\"" + fromAccountId + "\",\"to\":\"" + toAccountId + "\",\"amountCents\":" + amountCents + "}"
            ));

            return savedTransfer;

        } catch (DataIntegrityViolationException e) {
            return transfers.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "DB error"));
        }
    }
}