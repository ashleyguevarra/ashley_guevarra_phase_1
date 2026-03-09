package com.ashleyguevarra.phase1.account.dto;

import jakarta.validation.constraints.NotBlank;

public class OpenAccountRequest {

    @NotBlank
    private String type;

    @NotBlank
    private String currency;

    public String getType() { return type; }
    public String getCurrency() { return currency; }
}