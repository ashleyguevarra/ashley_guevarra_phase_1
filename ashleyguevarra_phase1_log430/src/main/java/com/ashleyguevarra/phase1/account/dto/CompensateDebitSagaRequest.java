package com.ashleyguevarra.phase1.account.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CompensateDebitSagaRequest {

    @NotNull
    private UUID transferId;

    @NotNull
    private UUID fromAccountId;

    @Min(1)
    private long amountCents;

    @NotNull
    private String currency;

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setTransferId(UUID transferId) { this.transferId = transferId; }
    public void setFromAccountId(UUID fromAccountId) { this.fromAccountId = fromAccountId; }
    public void setAmountCents(long amountCents) { this.amountCents = amountCents; }
    public void setCurrency(String currency) { this.currency = currency; }

    public CompensateDebitSagaRequest() {}

    public CompensateDebitSagaRequest(UUID transferId, UUID fromAccountId, long amountCents, String currency) {
        this.transferId = transferId;
        this.fromAccountId = fromAccountId;
        this.amountCents = amountCents;
        this.currency = currency;
    }
}

