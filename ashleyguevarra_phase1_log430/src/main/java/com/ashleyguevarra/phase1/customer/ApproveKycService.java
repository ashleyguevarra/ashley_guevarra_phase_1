package com.ashleyguevarra.phase1.customer;

import com.ashleyguevarra.phase1.audit.AuditLog;
import com.ashleyguevarra.phase1.audit.AuditLogRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Profile("account")
public class ApproveKycService {

    private final CustomerRepository customers;
    private final AuditLogRepository audit;

    public ApproveKycService(CustomerRepository customers, AuditLogRepository audit) {
        this.customers = customers;
        this.audit = audit;
    }

    @Transactional
    public Customer approve(UUID customerId) {

        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        customer.approveKyc();
        Customer saved = customers.save(customer);

        audit.save(new AuditLog(
                "KYC_APPROVED",
                "CUSTOMER",
                saved.getId(),
                "{\"kycStatus\":\"APPROVED\"}"
        ));

        return saved;
    }
}