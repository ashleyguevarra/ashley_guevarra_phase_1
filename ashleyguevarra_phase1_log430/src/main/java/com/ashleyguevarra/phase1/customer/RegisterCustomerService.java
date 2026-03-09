package com.ashleyguevarra.phase1.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class RegisterCustomerService {

    private final CustomerRepository repository;

    public RegisterCustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Customer register(String fullName, String email) {

        if (repository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        Customer customer = new Customer(fullName, email);
        return repository.save(customer);
    }

    public Customer getById(UUID id) {
    return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }  
}