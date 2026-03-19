package com.ashleyguevarra.phase1.saga;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SagaStepRepository extends JpaRepository<SagaStep, UUID> {

    boolean existsByTransferIdAndStepType(UUID transferId, String stepType);
}

