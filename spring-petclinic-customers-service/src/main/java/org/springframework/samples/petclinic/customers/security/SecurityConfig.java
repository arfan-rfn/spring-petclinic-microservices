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

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Security configuration that applies YAML-based authorization rules dynamically.
 * Follows the Train Ticket pattern for declarative role-based authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final JwtUtil jwtUtil;

    public SecurityConfig(SecurityProperties securityProperties, JwtUtil jwtUtil) {
        this.securityProperties = securityProperties;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.httpBasic(t -> t.disable())
            .csrf(t -> t.disable())
            .sessionManagement(t -> t.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests((authorize) -> {
            for (AuthorizationRule rule : securityProperties.getAuthorizationRules()) {
                String[] paths = rule.getPaths().toArray(new String[0]);
                String method = rule.getMethod();
                List<String> authorities = rule.getAuthorities();

                AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl authorizedUrl;

                if (StringUtils.hasText(method) && !"*".equals(method)) {
                    authorizedUrl = authorize.requestMatchers(HttpMethod.valueOf(method.toUpperCase()), paths);
                } else {
                    authorizedUrl = authorize.requestMatchers(paths);
                }

                if (authorities == null || authorities.isEmpty()) {
                    authorizedUrl.denyAll();
                } else if (authorities.contains("permitAll")) {
                    authorizedUrl.permitAll();
                } else if (authorities.contains("authenticated")) {
                    authorizedUrl.authenticated();
                } else {
                    String[] roles = authorities.stream()
                        .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                        .toArray(String[]::new);
                    authorizedUrl.hasAnyRole(roles);
                }
            }
            authorize.anyRequest().authenticated();
        });

        http.addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);
        http.headers(headers -> headers.cacheControl(cache -> cache.disable()));

        return http.build();
    }
}
