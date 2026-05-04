package com.taxi.trip.config;

import com.taxi.trip.client.NotificationServiceClient;
import com.taxi.trip.client.UserServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientsConfig {

    @Bean
    public UserServiceClient userServiceClient(@Value("${services.user-service.url}") String baseUrl) {
        return buildClient(baseUrl, UserServiceClient.class);
    }

    @Bean
    public NotificationServiceClient notificationServiceClient(@Value("${services.notification-service.url}") String baseUrl) {
        return buildClient(baseUrl, NotificationServiceClient.class);
    }

    private <T> T buildClient(String baseUrl, Class<T> type) {
        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    // Пробрасываем JWT входящего HTTP-запроса в межсервисные
                    // вызовы, чтобы вышестоящий сервис увидел того же пользователя.
                    String auth = currentAuthorizationHeader();
                    if (auth != null && request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION) == null) {
                        request.getHeaders().add(HttpHeaders.AUTHORIZATION, auth);
                    }
                    return execution.execute(request, body);
                })
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(type);
    }

    private static String currentAuthorizationHeader() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest req = sra.getRequest();
            return req.getHeader(HttpHeaders.AUTHORIZATION);
        }
        return null;
    }
}
