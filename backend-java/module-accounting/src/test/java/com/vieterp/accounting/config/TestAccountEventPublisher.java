package com.vieterp.accounting.config;

import com.vieterp.accounting.domain.Account;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

@TestConfiguration
@Profile("test")
public class TestAccountEventPublisher {

    @Bean
    @Primary
    public com.vieterp.accounting.event.AccountEventPublisher accountEventPublisher() {
        return new com.vieterp.accounting.event.AccountEventPublisher(null) {
            @Override
            public void publishCreated(Account account) {
                // no-op in tests
            }

            @Override
            public void publishUpdated(Account account) {
                // no-op in tests
            }

            @Override
            public void publishDeleted(UUID accountId) {
                // no-op in tests
            }
        };
    }
}
