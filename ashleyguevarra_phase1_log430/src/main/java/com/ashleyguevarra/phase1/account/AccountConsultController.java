package com.ashleyguevarra.phase1.account;

import com.ashleyguevarra.phase1.account.dto.BalanceResponse;
import com.ashleyguevarra.phase1.ledger.dto.LedgerEntryResponse;
import com.ashleyguevarra.phase1.ledger.dto.LedgerPageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountConsultController {

    private final ConsultAccountService service;

    public AccountConsultController(ConsultAccountService service) {
        this.service = service;
    }

    /**
     * Phase 1: on utilise Basic Auth (admin:admin) donc auth.getName() = "admin" (pas un UUID).
     * Pour simuler l'identité du client et vérifier l'ownership, on passe le customerId via un header.
     *
     * Exemple:
     *   -H "X-Customer-Id: <UUID>"
     *
     * Plus tard (UC-05), on pourra remplacer ça par JWT (sub).
     */
    private UUID customerIdFromHeader(String customerIdHeader) {
        if (customerIdHeader == null || customerIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MISSING_CUSTOMER_ID");
        }
        try {
            return UUID.fromString(customerIdHeader);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CUSTOMER_ID");
        }
    }

    @GetMapping("/{accountId}/balance")
    public BalanceResponse balance(@PathVariable UUID accountId,
                                   @RequestHeader("X-Customer-Id") String customerIdHeader) {

        UUID customerId = customerIdFromHeader(customerIdHeader);
        long balance = service.getBalance(accountId, customerId);
        return new BalanceResponse(accountId, balance);
    }

    @GetMapping("/{accountId}/ledger")
    public LedgerPageResponse ledger(@PathVariable UUID accountId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestHeader("X-Customer-Id") String customerIdHeader) {

        UUID customerId = customerIdFromHeader(customerIdHeader);

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = service.getHistory(accountId, customerId, pageable);

        var items = result.getContent().stream()
                .map(e -> new LedgerEntryResponse(
                        e.getId(),
                        e.getDirection(),
                        e.getAmountCents(),
                        e.getDescription(),
                        e.getCreatedAt()
                ))
                .toList();

        return new LedgerPageResponse(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                items
        );
    }
}