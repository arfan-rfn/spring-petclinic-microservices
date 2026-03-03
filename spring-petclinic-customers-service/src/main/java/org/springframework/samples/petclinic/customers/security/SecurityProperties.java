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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for security settings including JWT secret and authorization rules.
 */
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private String jwtSecret;
    private List<AuthorizationRule> authorizationRules = new ArrayList<>();

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public List<AuthorizationRule> getAuthorizationRules() {
        return authorizationRules;
    }

    public void setAuthorizationRules(List<AuthorizationRule> authorizationRules) {
        this.authorizationRules = authorizationRules;
    }
}
