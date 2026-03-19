package com.ashleyguevarra.phase1.account;

import com.ashleyguevarra.phase1.account.dto.CompleteLedgerSagaRequest;
import com.ashleyguevarra.phase1.api.ApiException;
import com.ashleyguevarra.phase1.account.dto.CompensateDebitSagaRequest;
import com.ashleyguevarra.phase1.account.dto.CreditSagaRequest;
import com.ashleyguevarra.phase1.account.dto.DebitSagaRequest;
import com.ashleyguevarra.phase1.account.dto.InstantCurrencyResponse;
import com.ashleyguevarra.phase1.audit.AuditLog;
import com.ashleyguevarra.phase1.audit.AuditLogRepository;
import com.ashleyguevarra.phase1.ledger.LedgerEntry;
import com.ashleyguevarra.phase1.ledger.LedgerEntryRepository;
import com.ashleyguevarra.phase1.saga.SagaStep;
import com.ashleyguevarra.phase1.saga.SagaStepRepository;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Profile("account")
@RequestMapping("/internal/saga")
public class InternalAccountSagaController {

    private static final String STEP_DEBIT = "DEBIT";
    private static final String STEP_CREDIT = "CREDIT";
    private static final String STEP_COMPENSATE_DEBIT = "COMPENSATE_DEBIT";

    private final AccountRepository accounts;
    private final SagaStepRepository sagaSteps;
    private final LedgerEntryRepository ledger;
    private final AuditLogRepository audit;

    public InternalAccountSagaController(AccountRepository accounts, SagaStepRepository sagaSteps,
                                         LedgerEntryRepository ledger, AuditLogRepository audit) {
        this.accounts = accounts;
        this.sagaSteps = sagaSteps;
        this.ledger = ledger;
        this.audit = audit;
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase();
    }

    @PostMapping("/debit")
    @Transactional
    public InstantCurrencyResponse debit(@Valid @RequestBody DebitSagaRequest req) {
        if (sagaSteps.existsByTransferIdAndStepType(req.getTransferId(), STEP_DEBIT)) {
            Account from = accounts.findById(req.getFromAccountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Source account not found"));
            return new InstantCurrencyResponse(normalizeCurrency(from.getCurrency()));
        }

        Account from = accounts.findByIdForUpdate(req.getFromAccountId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Source account not found"));
        Account to = accounts.findByIdForUpdate(req.getToAccountId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Destination account not found"));

        if (!from.getCustomerId().equals(req.getCustomerId())) {
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
        if (from.getBalanceCents() < req.getAmountCents()) {
            throw new ApiException(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "Insufficient funds");
        }

        from.setBalanceCents(from.getBalanceCents() - req.getAmountCents());
        accounts.save(from);

        try {
            sagaSteps.save(new SagaStep(req.getTransferId(), STEP_DEBIT));
        } catch (DataIntegrityViolationException e) {
            // Step was inserted concurrently (unique constraint). Balance update rolls back if it happened
            // before the insert; treat as effectively-once.
            Account currentFrom = accounts.findById(req.getFromAccountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Source account not found"));
            return new InstantCurrencyResponse(normalizeCurrency(currentFrom.getCurrency()));
        }

        return new InstantCurrencyResponse(normalizeCurrency(from.getCurrency()));
    }

    @PostMapping("/credit")
    @Transactional
    public void credit(@Valid @RequestBody CreditSagaRequest req) {
        if (sagaSteps.existsByTransferIdAndStepType(req.getTransferId(), STEP_CREDIT)) {
            return; // effectively-once
        }

        if (!sagaSteps.existsByTransferIdAndStepType(req.getTransferId(), STEP_DEBIT)) {
            throw new ApiException(HttpStatus.CONFLICT, "SAGA_STEP_ORDER_VIOLATION", "DEBIT step must be processed before CREDIT");
        }

        Account to = accounts.findByIdForUpdate(req.getToAccountId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Destination account not found"));

        if (!"ACTIVE".equals(to.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_INACTIVE", "Destination account is not ACTIVE");
        }

        String expectedCurrency = normalizeCurrency(req.getCurrency());
        if (!to.getCurrency().equals(expectedCurrency)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH", "Destination account currency mismatch");
        }

        to.setBalanceCents(to.getBalanceCents() + req.getAmountCents());
        accounts.save(to);

        try {
            sagaSteps.save(new SagaStep(req.getTransferId(), STEP_CREDIT));
        } catch (DataIntegrityViolationException e) {
            // Step inserted concurrently.
        }
    }

    @PostMapping("/compensate-debit")
    @Transactional
    public void compensateDebit(@Valid @RequestBody CompensateDebitSagaRequest req) {
        if (sagaSteps.existsByTransferIdAndStepType(req.getTransferId(), STEP_COMPENSATE_DEBIT)) {
            return;
        }

        if (!sagaSteps.existsByTransferIdAndStepType(req.getTransferId(), STEP_DEBIT)) {
            throw new ApiException(HttpStatus.CONFLICT, "SAGA_STEP_MISSING", "DEBIT step must exist before compensation");
        }

        // If CREDIT already happened, the net outcome is already correct (no compensation).
        if (sagaSteps.existsByTransferIdAndStepType(req.getTransferId(), STEP_CREDIT)) {
            return;
        }

        Account from = accounts.findByIdForUpdate(req.getFromAccountId())
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Source account not found"));

        if (!"ACTIVE".equals(from.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_INACTIVE", "Source account is not ACTIVE");
        }

        String expectedCurrency = normalizeCurrency(req.getCurrency());
        if (!from.getCurrency().equals(expectedCurrency)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH", "Source account currency mismatch");
        }

        from.setBalanceCents(from.getBalanceCents() + req.getAmountCents());
        accounts.save(from);

        try {
            sagaSteps.save(new SagaStep(req.getTransferId(), STEP_COMPENSATE_DEBIT));
        } catch (DataIntegrityViolationException e) {
            // Step inserted concurrently.
        }
    }

    @PostMapping("/complete-ledger")
    @Transactional
    public void completeLedger(@Valid @RequestBody CompleteLedgerSagaRequest req) {
        try {
            ledger.save(new LedgerEntry(
                req.fromAccountId(),
                "DEBIT",
                req.amountCents(),
                "TRANSFER " + req.transferId(),
                req.transferId()
            ));
        } catch (DataIntegrityViolationException ignored) {
            // Idempotent retry.
        }
        try {
            ledger.save(new LedgerEntry(
                req.toAccountId(),
                "CREDIT",
                req.amountCents(),
                "TRANSFER " + req.transferId(),
                req.transferId()
            ));
        } catch (DataIntegrityViolationException ignored) {
            // Idempotent retry.
        }
        try {
            audit.save(new AuditLog(
                "TRANSFER_COMPLETED",
                "TRANSFER",
                req.transferId(),
                "{\"from\":\"" + req.fromAccountId() + "\",\"to\":\"" + req.toAccountId() + "\",\"amountCents\":" + req.amountCents() + "}"
            ));
        } catch (DataIntegrityViolationException ignored) {
            // Idempotent retry.
        }
    }
}

