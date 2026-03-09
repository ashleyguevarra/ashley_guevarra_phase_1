package com.ashleyguevarra.phase1.transfer.dto;

import java.time.Instant;
import java.util.UUID;

public class TransferResponse {
    private UUID id;
    private UUID fromAccountId;
    private UUID toAccountId;
    private long amountCents;
    private String status;
    private Instant createdAt;

    public TransferResponse(UUID id, UUID fromAccountId, UUID toAccountId, long amountCents, String status, Instant createdAt) {
        this.id = id;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amountCents = amountCents;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId() { return toAccountId; }
    public long getAmountCents() { return amountCents; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}