package com.ashleyguevarra.phase1.saga;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_steps")
public class SagaStep {

    @Id
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "step_type", nullable = false)
    private String stepType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SagaStep() {}

    public SagaStep(UUID transferId, String stepType) {
        this.id = UUID.randomUUID();
        this.transferId = transferId;
        this.stepType = stepType;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public String getStepType() {
        return stepType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

