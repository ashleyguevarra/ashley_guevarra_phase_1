package com.ashleyguevarra.phase1.ledger.dto;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        String direction,
        long amountCents,
        String description,
        Instant createdAt
) {}