package com.ashleyguevarra.phase1.account.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CompleteLedgerSagaRequest(
    @NotNull UUID transferId,
    @NotNull UUID fromAccountId,
    @NotNull UUID toAccountId,
    @Min(1) long amountCents
) {}
