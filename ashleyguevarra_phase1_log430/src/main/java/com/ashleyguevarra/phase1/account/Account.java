package com.ashleyguevarra.phase1.account;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String currency;

    @Column(name = "balance_cents", nullable = false)
    private long balanceCents;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Account() {}

    public Account(UUID customerId, String type, String currency) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.type = type;
        this.currency = currency;
        this.balanceCents = 0;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }

    public void setBalanceCents(long balanceCents){
        this.balanceCents = balanceCents;
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public long getBalanceCents() { return balanceCents; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getType() { return type; }
}