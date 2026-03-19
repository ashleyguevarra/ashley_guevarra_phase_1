package com.ashleyguevarra.phase1.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("account")
@EntityScan({
    "com.ashleyguevarra.phase1.customer",
    "com.ashleyguevarra.phase1.account",
    "com.ashleyguevarra.phase1.ledger",
    "com.ashleyguevarra.phase1.audit",
    "com.ashleyguevarra.phase1.saga"
})
@EnableJpaRepositories(basePackages = {
    "com.ashleyguevarra.phase1.customer",
    "com.ashleyguevarra.phase1.account",
    "com.ashleyguevarra.phase1.ledger",
    "com.ashleyguevarra.phase1.audit",
    "com.ashleyguevarra.phase1.saga"
})
public class AccountPersistenceConfig {
}
