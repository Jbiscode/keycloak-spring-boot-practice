package com.example.keycloakdemo.config;

import com.example.keycloakdemo.config.security.KeycloakRoleConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.keycloakdemo.dto.ErrorResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
            // Stateless JWT 인증 방식이므로 세션 쿠키를 사용하지 않아 CSRF 공격 벡터 없음
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/user/**").hasRole("USER")
                .requestMatchers("/api/manager/**").hasRole("MANAGER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/token/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ErrorResponse error = ErrorResponse.of(
                            "UNAUTHORIZED", "Bearer token is missing or invalid");
                    response.getWriter().write(objectMapper.writeValueAsString(error));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ErrorResponse error = ErrorResponse.of(
                            "FORBIDDEN", "Required role is missing");
                    response.getWriter().write(objectMapper.writeValueAsString(error));
                })
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }
}
