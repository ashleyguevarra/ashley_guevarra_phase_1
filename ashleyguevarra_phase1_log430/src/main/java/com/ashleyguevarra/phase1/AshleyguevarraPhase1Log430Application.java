package com.ashleyguevarra.phase1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class AshleyguevarraPhase1Log430Application {

    public static void main(String[] args) {
        SpringApplication.run(AshleyguevarraPhase1Log430Application.class, args);
    }
}