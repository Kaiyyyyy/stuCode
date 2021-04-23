package com.kaiy.cloud.alibaba.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.kaiy.cloud.alibaba.oauth.JsonSerializationStrategy;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.metadata.HikariDataSourcePoolMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStoreSerializationStrategy;

import javax.sql.DataSource;

public class AutoConfiguration {

    @Bean
    public RedisTokenStoreSerializationStrategy redisTokenStoreSerializationStrategy(){
        return new JsonSerializationStrategy();
    }

    @Bean
    public RedisTokenStore redisTokenStore(RedisConnectionFactory connectionFactory) {
        RedisTokenStore redisTokenStore = new RedisTokenStore(connectionFactory);
//        redisTokenStore.setSerializationStrategy(new JsonSerializationStrategy());
        return redisTokenStore;
    }




}
