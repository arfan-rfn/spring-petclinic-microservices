# YAML-Based Role Authorization for All Microservices

## Overview
Implemented Train Ticket-style declarative YAML-based role authorization with JWT authentication support for all 4 business microservices.

## Implementation Date
2026-03-03

## Services Covered

| Service | Port | Security Type |
|---------|------|---------------|
| customers-service | 8081 | Servlet (Spring MVC) |
| vets-service | 8083 | Servlet (Spring MVC) |
| visits-service | 8082 | Servlet (Spring MVC) |
| genai-service | 8084 | Reactive (WebFlux) |

## Files Created Per Service

Each service has a `security` package with the following classes:

### Servlet Services (customers, vets, visits)
1. `AuthorizationRule.java` - POJO for authorization rules
2. `SecurityProperties.java` - `@Configuration` + `@ConfigurationProperties(prefix = "security")`
3. `JwtUtil.java` - JWT parsing and authentication creation
4. `JwtAuthenticationFilter.java` - `OncePerRequestFilter` for JWT extraction
5. `SecurityConfig.java` - `@EnableWebSecurity`, `@EnableMethodSecurity(prePostEnabled = true)`

### Reactive Service (genai)
1. `AuthorizationRule.java` - POJO for authorization rules
2. `SecurityProperties.java` - `@Configuration` + `@ConfigurationProperties(prefix = "security")`
3. `JwtUtil.java` - JWT parsing and authentication creation
4. `JwtAuthenticationWebFilter.java` - Reactive `WebFilter` for JWT extraction
5. `SecurityConfig.java` - `@EnableWebFluxSecurity`, `@EnableReactiveMethodSecurity`

## Authorization Rules by Service

### customers-service (8081)
| Endpoint | Method | Access |
|----------|--------|--------|
| `/owners`, `/petTypes` | GET | Public |
| `/owners/{ownerId}` | GET | Public |
| `/owners` | POST | ROLE_USER, ROLE_ADMIN |
| `/owners/{ownerId}` | PUT | ROLE_USER, ROLE_ADMIN |
| `/owners/*/pets/**` | ALL | ROLE_USER, ROLE_ADMIN |
| `/actuator/**` | ALL | Public |

### vets-service (8083)
| Endpoint | Method | Access |
|----------|--------|--------|
| `/vets` | GET | Public |
| `/actuator/**` | ALL | Public |

### visits-service (8082)
| Endpoint | Method | Access |
|----------|--------|--------|
| `/owners/*/pets/*/visits`, `/pets/visits` | GET | Public |
| `/owners/*/pets/*/visits` | POST | ROLE_USER, ROLE_ADMIN |
| `/actuator/**` | ALL | Public |

### genai-service (8084)
| Endpoint | Method | Access |
|----------|--------|--------|
| `/chatclient` | POST | ROLE_USER, ROLE_ADMIN |
| `/actuator/**` | ALL | Public |

## Dependencies Added to Each pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

## YAML Configuration Structure
```yaml
security:
  jwt-secret: ${JWT_SECRET:petclinic-secret-key-minimum-32-chars}
  authorization-rules:
    - paths: ["/endpoint1", "/endpoint2"]
      method: GET
      authorities: ["permitAll"]
    - paths: ["/protected"]
      method: POST
      authorities: ["ROLE_USER", "ROLE_ADMIN"]
```

## Train Ticket Pattern Compliance

This implementation exactly matches the Train Ticket microservices authorization pattern:

1. **SecurityProperties.java**: Uses both `@Configuration` and `@ConfigurationProperties(prefix = "security")`
2. **SecurityConfig.java**: Includes appropriate security annotations
3. **Authority Resolution** (exact Train Ticket logic):
   ```java
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
   ```
4. **HTTP Method handling**: Uses `StringUtils.hasText(method)` check
5. **Default catch-all**: `anyRequest().authenticated()`

## Testing

### Build All Services
```bash
./mvnw clean install -pl spring-petclinic-customers-service,spring-petclinic-vets-service,spring-petclinic-visits-service,spring-petclinic-genai-service
```

### Test Public Endpoint
```bash
curl http://localhost:8081/owners
curl http://localhost:8083/vets
```

### Test Protected Endpoint (without token)
```bash
curl -X POST http://localhost:8081/owners \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User"}'
# Returns 401 Unauthorized
```

### Test with JWT
```bash
curl -X POST http://localhost:8081/owners \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <valid-jwt-token>" \
  -d '{"firstName":"Test","lastName":"User"}'
# Returns 201 Created (if token has ROLE_USER or ROLE_ADMIN)
```

## JWT Token Structure
Expected claims:
- `sub`: Username (subject)
- `roles`: List of role strings (e.g., ["ROLE_USER", "ROLE_ADMIN"])

## Environment Variables
- `JWT_SECRET`: Secret key for JWT validation (minimum 32 characters recommended for production)

## Notes
- No changes to controllers (security enforced at filter level)
- Existing tests continue to work via test profile with permitAll rules
- genai-service uses reactive security (WebFlux) due to its reactive web application type
- All services share the same JWT secret for token validation across the system
