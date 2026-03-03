/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.customers.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * Security configuration that applies YAML-based authorization rules dynamically.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final JwtUtil jwtUtil;

    public SecurityConfig(SecurityProperties securityProperties, JwtUtil jwtUtil) {
        this.securityProperties = securityProperties;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        http.authorizeHttpRequests(auth -> {
            // Always permit actuator endpoints
            auth.requestMatchers("/actuator/**").permitAll();

            // Apply rules from YAML configuration
            for (AuthorizationRule rule : securityProperties.getAuthorizationRules()) {
                List<String> paths = rule.getPaths();
                String method = rule.getMethod();
                List<String> authorities = rule.getAuthorities();

                if (paths == null || paths.isEmpty()) {
                    continue;
                }

                String[] pathArray = paths.toArray(new String[0]);
                HttpMethod httpMethod = parseHttpMethod(method);

                if (isPermitAll(authorities)) {
                    if (httpMethod != null) {
                        auth.requestMatchers(httpMethod, pathArray).permitAll();
                    } else {
                        auth.requestMatchers(pathArray).permitAll();
                    }
                } else if (authorities != null && !authorities.isEmpty()) {
                    String[] authArray = authorities.toArray(new String[0]);
                    if (httpMethod != null) {
                        auth.requestMatchers(httpMethod, pathArray).hasAnyAuthority(authArray);
                    } else {
                        auth.requestMatchers(pathArray).hasAnyAuthority(authArray);
                    }
                }
            }

            // Deny all other requests by default
            auth.anyRequest().authenticated();
        });

        return http.build();
    }

    private HttpMethod parseHttpMethod(String method) {
        if (method == null || method.equals("*") || method.isEmpty()) {
            return null;
        }
        try {
            return HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isPermitAll(List<String> authorities) {
        return authorities != null &&
            authorities.stream().anyMatch(a -> "permitAll".equalsIgnoreCase(a));
    }
}
