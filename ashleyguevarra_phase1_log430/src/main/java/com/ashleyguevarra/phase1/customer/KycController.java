package com.ashleyguevarra.phase1.customer;

import com.ashleyguevarra.phase1.customer.dto.RegisterCustomerRequest;
import com.ashleyguevarra.phase1.customer.dto.RegisterCustomerResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

@RestController
@Profile("account")
@RequestMapping("/api/v1/customers")
public class KycController {

    private final RegisterCustomerService registerService;
    private final ApproveKycService approveKycService;

    public KycController(RegisterCustomerService registerService, ApproveKycService approveKycService) {
        this.registerService = registerService;
        this.approveKycService = approveKycService;
    }

    @PostMapping
    public RegisterCustomerResponse register(@Valid @RequestBody RegisterCustomerRequest body) {
        Customer customer = registerService.register(body.getFullName(), body.getEmail());
        return new RegisterCustomerResponse(customer.getId(), customer.getKycStatus());
    }

    @PatchMapping("/{id}/kyc/approve")
    public RegisterCustomerResponse approve(@PathVariable UUID id) {
        Customer customer = approveKycService.approve(id);
        return new RegisterCustomerResponse(customer.getId(), customer.getKycStatus());
    }

    @GetMapping("/{id}")
    public RegisterCustomerResponse get(@PathVariable UUID id) {
    Customer customer = registerService.getById(id);
    return new RegisterCustomerResponse(customer.getId(), customer.getKycStatus());
    }
}