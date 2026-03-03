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
package org.springframework.samples.petclinic.genai.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Reactive security configuration that applies YAML-based authorization rules dynamically.
 * Follows the Train Ticket pattern for declarative role-based authorization.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private final SecurityProperties securityProperties;
    private final JwtUtil jwtUtil;

    public SecurityConfig(SecurityProperties securityProperties, JwtUtil jwtUtil) {
        this.securityProperties = securityProperties;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable);

        http.authorizeExchange(exchanges -> {
            for (AuthorizationRule rule : securityProperties.getAuthorizationRules()) {
                List<String> paths = rule.getPaths();
                String method = rule.getMethod();
                List<String> authorities = rule.getAuthorities();

                if (paths == null || paths.isEmpty()) {
                    continue;
                }

                String[] pathArray = paths.toArray(new String[0]);

                if (authorities == null || authorities.isEmpty()) {
                    if (StringUtils.hasText(method) && !"*".equals(method)) {
                        exchanges.pathMatchers(HttpMethod.valueOf(method.toUpperCase()), pathArray).denyAll();
                    } else {
                        exchanges.pathMatchers(pathArray).denyAll();
                    }
                } else if (authorities.contains("permitAll")) {
                    if (StringUtils.hasText(method) && !"*".equals(method)) {
                        exchanges.pathMatchers(HttpMethod.valueOf(method.toUpperCase()), pathArray).permitAll();
                    } else {
                        exchanges.pathMatchers(pathArray).permitAll();
                    }
                } else if (authorities.contains("authenticated")) {
                    if (StringUtils.hasText(method) && !"*".equals(method)) {
                        exchanges.pathMatchers(HttpMethod.valueOf(method.toUpperCase()), pathArray).authenticated();
                    } else {
                        exchanges.pathMatchers(pathArray).authenticated();
                    }
                } else {
                    String[] roles = authorities.stream()
                        .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                        .toArray(String[]::new);
                    if (StringUtils.hasText(method) && !"*".equals(method)) {
                        exchanges.pathMatchers(HttpMethod.valueOf(method.toUpperCase()), pathArray).hasAnyRole(roles);
                    } else {
                        exchanges.pathMatchers(pathArray).hasAnyRole(roles);
                    }
                }
            }
            exchanges.anyExchange().authenticated();
        });

        http.addFilterAt(new JwtAuthenticationWebFilter(jwtUtil), SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }
}
