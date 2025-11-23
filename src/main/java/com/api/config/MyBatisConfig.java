package com.api.config;

import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * MyBatis 설정
 * 데이터베이스별로 다른 SQL을 사용하기 위한 databaseIdProvider 설정
 */
@Configuration
public class MyBatisConfig {

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider provider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("PostgreSQL", "postgresql");
        properties.setProperty("MySQL", "mysql");
        properties.setProperty("MariaDB", "mysql");  // MariaDB는 MySQL과 동일한 SQL 사용
        properties.setProperty("H2", "h2");
        provider.setProperties(properties);
        return provider;
    }
}

