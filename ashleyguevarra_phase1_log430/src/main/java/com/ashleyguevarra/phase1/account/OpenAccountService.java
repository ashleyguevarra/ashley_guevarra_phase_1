package com.ashleyguevarra.phase1.account;

import com.ashleyguevarra.phase1.audit.AuditLog;
import com.ashleyguevarra.phase1.audit.AuditLogRepository;
import com.ashleyguevarra.phase1.customer.Customer;
import com.ashleyguevarra.phase1.customer.CustomerRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Profile("account")
public class OpenAccountService {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("CAD", "USD");
    private static final Set<String> SUPPORTED_TYPES = Set.of("CHECKING", "SAVINGS");

    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final AuditLogRepository audit;

    public OpenAccountService(CustomerRepository customers, AccountRepository accounts, AuditLogRepository audit) {
        this.customers = customers;
        this.accounts = accounts;
        this.audit = audit;
    }

    @Transactional
    public Account open(UUID customerId, String type, String currency) {

        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("CUSTOMER_NOT_FOUND"));

        if (!"APPROVED".equals(customer.getKycStatus())) {
            throw new SecurityException("KYC_REQUIRED");
        }

        String normalizedCurrency = currency.trim().toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(normalizedCurrency)) {
            throw new IllegalArgumentException("UNSUPPORTED_CURRENCY");
        }

        String normalizedType = type.trim().toUpperCase();
        if (!SUPPORTED_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("UNSUPPORTED_ACCOUNT_TYPE");
        }

        Account account = new Account(customerId, normalizedType, normalizedCurrency);
        Account saved = accounts.save(account);

        audit.save(new AuditLog(
                "ACCOUNT_OPENED",
                "ACCOUNT",
                saved.getId(),
                "{\"customerId\":\"" + customerId + "\",\"currency\":\"" + normalizedCurrency + "\",\"type\":\"" + normalizedType + "\"}"
        ));

        return saved;
    }
}