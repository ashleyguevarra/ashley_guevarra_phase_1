package com.ashleyguevarra.phase1.account;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Internal endpoint for seeding account balance (testing/load tests only).
 */
@RestController
@Profile("account")
@RequestMapping("/internal/seed")
public class SeedFundController {

    private final AccountRepository accounts;

    public SeedFundController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/fund")
    @Transactional
    public ResponseEntity<Map<String, Object>> fund(
            @RequestBody Map<String, Object> body) {
        UUID accountId = UUID.fromString((String) body.get("accountId"));
        long amountCents = ((Number) body.getOrDefault("amountCents", 10000L)).longValue();

        Account acc = accounts.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("ACCOUNT_NOT_FOUND"));
        acc.setBalanceCents(acc.getBalanceCents() + amountCents);
        accounts.save(acc);

        return ResponseEntity.ok(Map.of(
                "accountId", accountId.toString(),
                "balanceCents", acc.getBalanceCents()
        ));
    }
}
