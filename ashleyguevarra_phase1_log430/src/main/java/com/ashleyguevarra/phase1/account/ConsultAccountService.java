package com.ashleyguevarra.phase1.account;

import com.ashleyguevarra.phase1.ledger.LedgerEntry;
import com.ashleyguevarra.phase1.ledger.LedgerEntryRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ConsultAccountService {

    private final AccountRepository accounts;
    private final LedgerEntryRepository ledger;

    public ConsultAccountService(AccountRepository accounts, LedgerEntryRepository ledger) {
        this.accounts = accounts;
        this.ledger = ledger;
    }

    public Account getAccountOwned(UUID accountId, UUID customerId) {
        Account acc = accounts.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("ACCOUNT_NOT_FOUND"));

        if (!acc.getCustomerId().equals(customerId)) {
            throw new SecurityException("FORBIDDEN_RESOURCE");
        }
        return acc;
    }

    @Cacheable(value = "accountBalance", key = "#accountId.toString() + ':' + #customerId.toString()")
    public long getBalance(UUID accountId, UUID customerId) {
        System.out.println("Fetching balance from DB for account " + accountId);
        return getAccountOwned(accountId, customerId).getBalanceCents();
    }

    public Page<LedgerEntry> getHistory(UUID accountId, UUID customerId, Pageable pageable) {
        getAccountOwned(accountId, customerId);
        return ledger.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
    }
}