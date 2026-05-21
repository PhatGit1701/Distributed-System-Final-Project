package com.example.distributedsystemproject.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "app.datasource.node1")
    public DataSource node1DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.datasource.node2")
    public DataSource node2DataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.datasource.node3")
    public DataSource node3DataSource() {
        return DataSourceBuilder.create().build();
    }

    private LocalContainerEntityManagerFactoryBean createEntityManagerFactory(DataSource dataSource, String unitName) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.example.distributedsystemproject.model");
        factory.setPersistenceUnitName(unitName);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        factory.setJpaPropertyMap(properties);
        return factory;
    }
    @Bean(name = "entityManagerFactoryNode1") @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryNode1() { return createEntityManagerFactory(node1DataSource(), "node1"); }
    @Bean(name = "entityManagerFactoryNode2")
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryNode2() { return createEntityManagerFactory(node2DataSource(), "node2"); }
    @Bean(name = "entityManagerFactoryNode3")
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryNode3() { return createEntityManagerFactory(node3DataSource(), "node3"); }

    @Bean(name = "transactionManagerNode1") @Primary
    public JpaTransactionManager transactionManagerNode1(@Qualifier("entityManagerFactoryNode1") EntityManagerFactory emf) { return new JpaTransactionManager(emf); }
    @Bean(name = "transactionManagerNode2")
    public JpaTransactionManager transactionManagerNode2(@Qualifier("entityManagerFactoryNode2") EntityManagerFactory emf) { return new JpaTransactionManager(emf); }
    @Bean(name = "transactionManagerNode3")
    public JpaTransactionManager transactionManagerNode3(@Qualifier("entityManagerFactoryNode3") EntityManagerFactory emf) { return new JpaTransactionManager(emf); }

    @Configuration
    @EnableJpaRepositories(basePackages = "com.example.distributedsystemproject.repository.node1", entityManagerFactoryRef = "entityManagerFactoryNode1", transactionManagerRef = "transactionManagerNode1")
    static class Node1RepoConfig {}

    @Configuration
    @EnableJpaRepositories(basePackages = "com.example.distributedsystemproject.repository.node2", entityManagerFactoryRef = "entityManagerFactoryNode2", transactionManagerRef = "transactionManagerNode2")
    static class Node2RepoConfig {}

    @Configuration
    @EnableJpaRepositories(basePackages = "com.example.distributedsystemproject.repository.node3", entityManagerFactoryRef = "entityManagerFactoryNode3", transactionManagerRef = "transactionManagerNode3")
    static class Node3RepoConfig {}
}