package com.vieterp.hrm.config;

import org.springframework.context.annotation.Configuration;

/**
 * NATS configuration for Spring Cloud Stream.
 * StreamBridge is used for sending messages - no @EnableBinding needed in Spring Cloud Stream 4.x.
 * The actual binding destinations are configured in application.yml.
 */
@Configuration
public class NatsConfig {
    // NATS connection auto-configured by Spring Cloud Stream
    // spring.cloud.stream.nats.binder.url in application.yml
}
