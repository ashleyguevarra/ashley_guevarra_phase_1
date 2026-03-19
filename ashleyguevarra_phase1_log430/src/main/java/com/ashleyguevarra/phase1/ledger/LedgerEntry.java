package com.ashleyguevarra.phase1.ledger;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private String direction; // CREDIT / DEBIT

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "transfer_id")
    private UUID transferId;

    protected LedgerEntry() {}

    public LedgerEntry(UUID accountId, String direction, long amountCents, String description, UUID transferId) {
        this.id = UUID.randomUUID();
        this.accountId = accountId;
        this.direction = direction;
        this.amountCents = amountCents;
        this.description = description;
        this.createdAt = Instant.now();
        this.transferId = transferId;
    }

    public UUID getId() { return id; }
    public UUID getAccountId() { return accountId; }
    public String getDirection() { return direction; }
    public long getAmountCents() { return amountCents; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getTransferId() { return transferId; }
}