package com.ashleyguevarra.phase1.account.dto;

import java.util.UUID;

public record BalanceResponse(UUID accountId, long balanceCents) {}