package com.ashleyguevarra.phase1.transfer;

import com.ashleyguevarra.phase1.transfer.dto.CreateTransferRequest;
import com.ashleyguevarra.phase1.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Profile("transfer")
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    private UUID customerIdFromHeader(@RequestHeader("X-Customer-Id") String customerId) {
        return UUID.fromString(customerId);
    }

    @PostMapping
    public TransferResponse create(@RequestHeader("X-Customer-Id") String customerIdHeader,
                                  @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                  @Valid @RequestBody CreateTransferRequest body) {

        UUID customerId = customerIdFromHeader(customerIdHeader);

        Transfer t = service.createTransfer(
                customerId,
                body.getFromAccountId(),
                body.getToAccountId(),
                body.getAmountCents(),
                idempotencyKey
        );

        return new TransferResponse(
                t.getId(),
                t.getFromAccountId(),
                t.getToAccountId(),
                t.getAmountCents(),
                t.getStatus(),
                t.getCreatedAt()
        );
    }
}