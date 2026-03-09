package com.ashleyguevarra.phase1.ledger.dto;

import java.util.List;

public record LedgerPageResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<LedgerEntryResponse> items
) {}