package com.xyz.gumall.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseCookie;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

import java.util.function.Consumer;

@Configuration
@EnableRedisWebSession(maxInactiveIntervalInSeconds = 10*60*60, redisNamespace = "my:spring:session")
public class WebSessionConfig {

    @Bean
    public WebSessionIdResolver webSessionIdResolver() {
        CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName("SESSIONID_SECURITY");

        Consumer<ResponseCookie.ResponseCookieBuilder> consumer = responseCookieBuilder -> {
            responseCookieBuilder.path("/");
            responseCookieBuilder.domain("gumall.com");
        };
        resolver.addCookieInitializer(consumer);
        return resolver;
    }
}
