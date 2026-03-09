package com.ashleyguevarra.phase1.customer;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "kyc_status", nullable = false)
    private String kycStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Customer() {}

    public Customer(String fullName, String email) {
        this.id = UUID.randomUUID();
        this.fullName = fullName;
        this.email = email;
        this.kycStatus = "PENDING";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getKycStatus() { return kycStatus; }

    public void approveKyc() {
    if (!"PENDING".equals(this.kycStatus)) {
        throw new IllegalStateException("KYC cannot be approved from status: " + this.kycStatus);
    }
    this.kycStatus = "APPROVED";
}
}