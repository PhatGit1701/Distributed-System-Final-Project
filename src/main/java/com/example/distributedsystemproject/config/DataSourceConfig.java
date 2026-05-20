package com.example.distributedsystemproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

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

    @Bean public JdbcTemplate jdbcNode1() { return new JdbcTemplate(node1DataSource()); }
    @Bean public JdbcTemplate jdbcNode2() { return new JdbcTemplate(node2DataSource()); }
    @Bean public JdbcTemplate jdbcNode3() { return new JdbcTemplate(node3DataSource()); }
}