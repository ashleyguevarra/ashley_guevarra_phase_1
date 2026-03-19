package com.ashleyguevarra.phase1.account.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class DebitSagaRequest {

    @NotNull
    private UUID transferId;

    @NotNull
    private UUID fromAccountId;

    @NotNull
    private UUID toAccountId;

    @NotNull
    private UUID customerId;

    @Min(1)
    private long amountCents;

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setTransferId(UUID transferId) { this.transferId = transferId; }
    public void setFromAccountId(UUID fromAccountId) { this.fromAccountId = fromAccountId; }
    public void setToAccountId(UUID toAccountId) { this.toAccountId = toAccountId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public void setAmountCents(long amountCents) { this.amountCents = amountCents; }

    public DebitSagaRequest() {}

    public DebitSagaRequest(UUID transferId, UUID fromAccountId, UUID toAccountId, UUID customerId, long amountCents) {
        this.transferId = transferId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.customerId = customerId;
        this.amountCents = amountCents;
    }
}

