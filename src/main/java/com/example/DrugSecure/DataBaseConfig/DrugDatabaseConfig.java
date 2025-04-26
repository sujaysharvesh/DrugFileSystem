package com.example.DrugSecure.DataBaseConfig;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.DrugSecure",
        entityManagerFactoryRef = "drugEntityManagerFactory",
        transactionManagerRef = "drugTransactionManager"
)
@EntityScan("com.example.DrugSecure")
public class DrugDatabaseConfig {
}