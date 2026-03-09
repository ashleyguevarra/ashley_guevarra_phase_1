package com.ashleyguevarra.phase1.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterCustomerRequest {

    @NotBlank
    @Size(min = 2, max = 200)
    private String fullName;

    @NotBlank
    @Email
    private String email;

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
}