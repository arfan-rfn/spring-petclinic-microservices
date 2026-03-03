# YAML-Based Role Authorization for Customers Service

## Overview
Implemented Train Ticket-style declarative YAML-based role authorization with JWT authentication support for the customers-service.

## Implementation Date
2026-03-03

## Files Created

### 1. `spring-petclinic-customers-service/src/main/java/org/springframework/samples/petclinic/customers/security/AuthorizationRule.java`
POJO representing an authorization rule with paths, method, and authorities fields.

### 2. `spring-petclinic-customers-service/src/main/java/org/springframework/samples/petclinic/customers/security/SecurityProperties.java`
Configuration properties class with `@Configuration` and `@ConfigurationProperties(prefix = "security")` annotations (matching Train Ticket pattern). Contains JWT secret and list of authorization rules.

### 3. `spring-petclinic-customers-service/src/main/java/org/springframework/samples/petclinic/customers/security/JwtUtil.java`
Utility class for JWT token parsing and validation. Extracts username and roles from JWT claims and creates Spring Security Authentication objects.

### 4. `spring-petclinic-customers-service/src/main/java/org/springframework/samples/petclinic/customers/security/JwtAuthenticationFilter.java`
Extends OncePerRequestFilter to extract Bearer token from Authorization header, validate it, and set SecurityContext.

### 5. `spring-petclinic-customers-service/src/main/java/org/springframework/samples/petclinic/customers/security/SecurityConfig.java`
Main security configuration class with `@EnableWebSecurity`, `@EnableMethodSecurity(prePostEnabled = true)`, and `@EnableConfigurationProperties(SecurityProperties.class)`. Dynamically applies authorization rules from YAML using Train Ticket's exact authority resolution pattern:
- Empty authorities → `denyAll()`
- `"permitAll"` → `permitAll()`
- `"authenticated"` → `authenticated()`
- `"ROLE_*"` → `hasAnyRole()` (strips "ROLE_" prefix)

## Files Modified

### 1. `spring-petclinic-customers-service/pom.xml`
Added dependencies:
- spring-boot-starter-security
- jjwt-api (0.12.6)
- jjwt-impl (0.12.6, runtime)
- jjwt-jackson (0.12.6, runtime)
- spring-security-test (test scope)

### 2. `spring-petclinic-customers-service/src/main/java/org/springframework/samples/petclinic/customers/CustomersServiceApplication.java`
No changes needed - `@EnableConfigurationProperties` is placed on `SecurityConfig.java` instead (following Train Ticket pattern).

### 3. `spring-petclinic-customers-service/src/main/resources/application.yml`
Added security configuration with authorization rules.

### 4. `spring-petclinic-customers-service/src/test/resources/application-test.yml`
Added permitAll rule for all paths during tests.

## Authorization Rules

| Endpoint | Method | Required Role |
|----------|--------|---------------|
| `/owners` | GET | Public (permitAll) |
| `/owners/{ownerId}` | GET | Public (permitAll) |
| `/petTypes` | GET | Public (permitAll) |
| `/owners` | POST | ROLE_USER or ROLE_ADMIN |
| `/owners/{ownerId}` | PUT | ROLE_USER or ROLE_ADMIN |
| `/owners/*/pets/**` | ALL | ROLE_USER or ROLE_ADMIN |
| `/actuator/**` | ALL | Public (permitAll) |

## Configuration

### YAML Structure
```yaml
security:
  jwt-secret: ${JWT_SECRET:petclinic-secret-key-minimum-32-chars}
  authorization-rules:
    - paths: ["/owners", "/petTypes"]
      method: GET
      authorities: ["permitAll"]
    - paths: ["/owners/{ownerId}"]
      method: GET
      authorities: ["permitAll"]
    - paths: ["/owners"]
      method: POST
      authorities: ["ROLE_USER", "ROLE_ADMIN"]
```

### Environment Variables
- `JWT_SECRET`: Secret key for JWT validation (minimum 32 characters recommended)

## Testing

### Build
```bash
./mvnw clean install -pl spring-petclinic-customers-service
```

### Run
```bash
./mvnw spring-boot:run -pl spring-petclinic-customers-service
```

### Test Public Endpoint
```bash
curl http://localhost:8081/owners
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

## Train Ticket Pattern Compliance

This implementation exactly matches the Train Ticket microservices authorization pattern:

1. **SecurityProperties.java**: Uses both `@Configuration` and `@ConfigurationProperties(prefix = "security")`
2. **SecurityConfig.java**: Includes `@EnableWebSecurity`, `@EnableMethodSecurity(prePostEnabled = true)`, `@EnableConfigurationProperties`
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
5. **Default catch-all**: `authorize.anyRequest().authenticated()`

## Notes
- No changes to controllers (security enforced at filter level)
- Existing tests continue to work via test profile with permitAll
- JWT secret should be set via environment variable in production
