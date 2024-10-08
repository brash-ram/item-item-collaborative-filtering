package com.brash.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbit", ignoreUnknownFields = false)
public record RabbitMQConfig(
        String queue,
        String exchange,
        String addItemClass,
        String addUserClass,
        String addMarkClass
) {
}
