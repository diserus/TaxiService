package com.taxi.notification.config;

import com.taxi.notification.service.DeliveryGateway;
import com.taxi.notification.service.LoggingDeliveryGateway;
import com.taxi.notification.worker.WorkerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WorkerProperties.class)
public class AppConfig {

    @Bean
    @ConditionalOnMissingBean(DeliveryGateway.class)
    DeliveryGateway deliveryGateway() {
        return new LoggingDeliveryGateway();
    }
}
