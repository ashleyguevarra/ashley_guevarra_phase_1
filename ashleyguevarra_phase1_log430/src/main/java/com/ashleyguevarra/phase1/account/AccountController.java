package com.ashleyguevarra.phase1.account;

import com.ashleyguevarra.phase1.account.dto.OpenAccountRequest;
import com.ashleyguevarra.phase1.account.dto.OpenAccountResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Profile("account")
@RequestMapping("/api/v1/customers/{customerId}/accounts")
public class AccountController {

    private final OpenAccountService openAccountService;

    public AccountController(OpenAccountService openAccountService) {
        this.openAccountService = openAccountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OpenAccountResponse open(@PathVariable UUID customerId, @Valid @RequestBody OpenAccountRequest body) {
        Account account = openAccountService.open(customerId, body.getType(), body.getCurrency());
        return new OpenAccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getType(),
                account.getCurrency(),
                account.getStatus(),
                account.getBalanceCents()
        );
    }
}