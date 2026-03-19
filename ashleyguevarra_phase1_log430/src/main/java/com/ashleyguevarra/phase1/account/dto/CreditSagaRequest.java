package com.ashleyguevarra.phase1.account.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreditSagaRequest {

    @NotNull
    private UUID transferId;

    @NotNull
    private UUID toAccountId;

    @Min(1)
    private long amountCents;

    @NotNull
    private String currency;

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setTransferId(UUID transferId) { this.transferId = transferId; }
    public void setToAccountId(UUID toAccountId) { this.toAccountId = toAccountId; }
    public void setAmountCents(long amountCents) { this.amountCents = amountCents; }
    public void setCurrency(String currency) { this.currency = currency; }

    public CreditSagaRequest() {}

    public CreditSagaRequest(UUID transferId, UUID toAccountId, long amountCents, String currency) {
        this.transferId = transferId;
        this.toAccountId = toAccountId;
        this.amountCents = amountCents;
        this.currency = currency;
    }
}

