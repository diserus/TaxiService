package com.taxi.trip.config;

import com.taxi.trip.client.NotificationServiceClient;
import com.taxi.trip.client.UserServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientsConfig {

    @Bean
    public UserServiceClient userServiceClient(@Value("${services.user-service.url}") String baseUrl) {
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(UserServiceClient.class);
    }

    @Bean
    public NotificationServiceClient notificationServiceClient(@Value("${services.notification-service.url}") String baseUrl) {
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(NotificationServiceClient.class);
    }
}
