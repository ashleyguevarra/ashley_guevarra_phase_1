package com.ashleyguevarra.phase1.account.dto;

import java.util.UUID;

public class OpenAccountResponse {
    private final UUID id;
    private final UUID customerId;
    private final String type;
    private final String currency;
    private final String status;
    private final long balanceCents;

    public OpenAccountResponse(UUID id, UUID customerId, String type, String currency, String status, long balanceCents) {
        this.id = id;
        this.customerId = customerId;
        this.type = type;
        this.currency = currency;
        this.status = status;
        this.balanceCents = balanceCents;
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public String getType() { return type; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public long getBalanceCents() { return balanceCents; }
}