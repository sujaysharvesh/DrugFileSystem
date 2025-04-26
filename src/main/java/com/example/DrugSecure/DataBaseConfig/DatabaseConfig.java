package com.example.DrugSecure.DataBaseConfig;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.example.DrugSecure.userAuth.User",
        entityManagerFactoryRef = "userEntityManagerFactory",
        transactionManagerRef = "userTransactionManager"
)
public class DatabaseConfig {

    @Primary
    @Bean(name = "userDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.user")
    public DataSource userDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "drugDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.drug")
    public DataSource drugDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = {"entityManagerFactory", "userEntityManagerFactory"})
    public LocalContainerEntityManagerFactoryBean userEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("userDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.DrugSecure.userAuth.User")
                .persistenceUnit("user")
                .build();
    }

    @Bean(name = "drugEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean drugEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("drugDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.DrugSecure")
                .persistenceUnit("drug")
                .build();
    }

    @Primary
    @Bean(name = "userTransactionManager")
    public PlatformTransactionManager userTransactionManager(
            @Qualifier("userEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "drugTransactionManager")
    public PlatformTransactionManager drugTransactionManager(
            @Qualifier("drugEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}