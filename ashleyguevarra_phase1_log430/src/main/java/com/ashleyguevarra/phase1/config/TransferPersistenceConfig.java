package com.ashleyguevarra.phase1.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("transfer")
@EntityScan("com.ashleyguevarra.phase1.transfer")
@EnableJpaRepositories(basePackages = "com.ashleyguevarra.phase1.transfer")
public class TransferPersistenceConfig {
}
