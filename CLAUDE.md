# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build all services
./mvnw clean install

# Build Docker images
./mvnw clean install -P buildDocker

# Build for ARM64 (Apple Silicon)
./mvnw clean install -P buildDocker -Dcontainer.platform="linux/arm64"

# Build with Podman instead of Docker
./mvnw clean install -P buildDocker -Dcontainer.executable=podman

# Run a single service
./mvnw spring-boot:run -pl spring-petclinic-<service-name>

# Run tests for a single service
./mvnw test -pl spring-petclinic-<service-name>

# Compile CSS (API Gateway only)
cd spring-petclinic-api-gateway && mvn generate-resources -P css
```

## Running the Application

**Startup order matters:**
1. Config Server (8888) - must start first
2. Discovery Server (8761) - must start second
3. Business services (customers, vets, visits, genai) - any order
4. API Gateway (8080) - start after services register with Eureka

**With Docker:**
```bash
./mvnw clean install -P buildDocker
docker compose up
```

**Hybrid mode (Docker infra + local Java):**
```bash
./scripts/run_all.sh
```

**GenAI service requires:**
```bash
export OPENAI_API_KEY="your_key"  # or use "demo" for testing
# OR for Azure:
export AZURE_OPENAI_ENDPOINT="https://your_resource.openai.azure.com"
export AZURE_OPENAI_KEY="your_key"
```

## Architecture

This is a Spring Cloud microservices demo with 8 services:

**Business Services:**
- `customers-service` (8081): Owner and pet management
- `vets-service` (8083): Veterinarian data
- `visits-service` (8082): Pet visit records
- `genai-service` (8084): Spring AI chatbot (OpenAI/Azure)

**Infrastructure:**
- `api-gateway` (8080): Spring Cloud Gateway + AngularJS frontend
- `config-server` (8888): Centralized config from external Git repo
- `discovery-server` (8761): Eureka service registry
- `admin-server` (9090): Spring Boot Admin monitoring

**External config repository:** https://github.com/spring-petclinic/spring-petclinic-microservices-config

## Key Patterns

**Service Communication:**
- Services register with Eureka, discovered via `@EnableDiscoveryClient`
- API Gateway routes via `lb://service-name` (load-balanced)
- Circuit breaker: Resilience4j with 10-second timeout

**Configuration:**
- Each service imports from Config Server: `optional:configserver:http://localhost:8888/`
- Use `docker` profile for container deployments
- Native profile with `GIT_REPO` env var for local config: `-Dspring.profiles.active=native -DGIT_REPO=/path/to/config`

**Database:**
- Default: HSQLDB in-memory
- MySQL: Add `--spring.profiles.active=mysql` (schema/data in `db/mysql/`)

**Observability:**
- Custom metrics: `@Timed` annotations (petclinic.owner, petclinic.pet, petclinic.visit)
- Tracing: Micrometer + OpenTelemetry to Zipkin (9411)
- Metrics: Prometheus (9091), Grafana (3030)

## Tech Stack

- Java 17+, Spring Boot 4.0.1, Spring Cloud 2025.1.0 (Oakwood)
- Spring AI 2.0.0-M1 (genai-service)
- Frontend: AngularJS 1.8.3, Bootstrap 5.3.3

## Chaos Engineering

Services start with `chaos-monkey` profile via `run_all.sh`. Enable assaults:
```bash
./scripts/chaos/call_chaos.sh <customers|visits|vets> <assault_type> <watcher_type>
# Example: ./scripts/chaos/call_chaos.sh visits attacks_enable_exception watcher_enable_restcontroller
```
