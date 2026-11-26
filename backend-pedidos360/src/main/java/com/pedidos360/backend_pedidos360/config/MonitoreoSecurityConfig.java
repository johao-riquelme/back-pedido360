package com.pedidos360.backend_pedidos360.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class MonitoreoSecurityConfig {
    @Bean
    @Order(1)
    public SecurityFilterChain monitoreoFilterChain(HttpSecurity http,
            @Value("${PEDIDOS360_MONITOR_ADMIN_OIDS:}") String adminOids,
            @Value("${PEDIDOS360_MONITOR_ISSUER:https://login.microsoftonline.com/fd7c1051-f107-4307-a49d-b7901ba3e873/v2.0}") String issuer,
            @Value("${PEDIDOS360_MONITOR_AUDIENCE:a17cda30-9d3b-4af4-8bfe-75e8ad13aa24}") String audience,
            @Value("${PEDIDOS360_MONITOR_JWKS:https://login.microsoftonline.com/fd7c1051-f107-4307-a49d-b7901ba3e873/discovery/v2.0/keys}") String jwks) throws Exception {
        Set<String> admins = Arrays.stream(adminOids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
        OAuth2TokenValidator<Jwt> aud = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audiencia incorrecta", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), aud));
        http.securityMatcher("/api/monitoreo/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.anyRequest().access((authentication, context) -> {
                var principal = authentication.get();
                boolean allowed = principal instanceof JwtAuthenticationToken jwt && principal.isAuthenticated()
                        && jwt.getToken().getClaimAsString("oid") != null
                        && admins.contains(jwt.getToken().getClaimAsString("oid"))
                        && jwt.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("SCOPE_Orders.Read"));
                return new org.springframework.security.authorization.AuthorizationDecision(allowed);
            }))
            .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(decoder)));
        return http.build();
    }
}
