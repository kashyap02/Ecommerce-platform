package com.ecommerce.productservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Internal Saga hooks — called by Order Service, no user token present
                        .requestMatchers(HttpMethod.POST, "/api/products/*/reserve").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/release").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products/*/confirm").permitAll()

                        // Catalog writes — ADMIN only
                        .requestMatchers(HttpMethod.POST,   "/api/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasRole("ADMIN")

                        // Catalog reads — any authenticated user
                        .requestMatchers(HttpMethod.GET,    "/api/products/**").authenticated()
                        .requestMatchers(HttpMethod.GET,    "/api/products").authenticated()

                        // Deny everything else by default
                        .anyRequest().denyAll()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}