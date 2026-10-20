# Specialized Topics

Specialized areas are documented only where the codebase actually contains them. Each section below states what was searched for and what was found.

## Database Schemas and Query Patterns
**Not detected.** No JDBC usage, no `DataSource` bean, no JPA/Hibernate (`persistence.xml`, `@Entity`, `*.hbm.xml`), no MyBatis mappers, no Spring Data repositories, no `.sql` files, no Flyway/Liquibase migrations, and no database driver on the dependency list. The application's only data source is the local filesystem, accessed through `java.io.FileInputStream` and `java.nio.file.Files` in `FileSystemPointer`.

## API Documentation
**Detected — documented in full elsewhere.** Two HTTP endpoints exist, declared via Spring MVC annotations (no OpenAPI/Swagger specification, no `springfox`/`springdoc` dependency, no `.wsdl`, `.graphql`, or `.proto` files). Complete request/response documentation with examples is in [../reference/api-reference.md](../reference/api-reference.md):
- `GET|HEAD /download/{uuid}` → 301 redirect to the filename-bearing URL
- `GET|HEAD /download/{uuid}/{filename}` → 200/304 content with HTTP cache validation

Spring Boot Actuator 1.2.3 additionally contributes its default Boot 1.x endpoint set with no configuration restricting it — see [../analysis/security-patterns.md](../analysis/security-patterns.md) finding S2.

## UI Components
**Not detected.** No HTML, JSP, Thymeleaf, FreeMarker, or Velocity templates; no `src/main/webapp`; no CSS or JavaScript; no `package.json`; no static resources under `src/main/resources/static` or `/public`. The application is a pure HTTP API consumed directly by clients such as browsers or download managers.

## Message Queues and Event Patterns
**Not detected.** No JMS, AMQP/RabbitMQ, Kafka, SQS/SNS, or Spring Cloud Stream dependencies; no `@JmsListener`/`@KafkaListener`/`@RabbitListener`; no Spring application-event publishing or `@EventListener` in project code; no scheduled tasks (`@Scheduled`), no `@Async`, and no executor configuration. All processing is synchronous within the servlet request thread.

The one asynchrony-adjacent concern is blocking: `ThrottlingInputStream` calls `RateLimiter.acquire(n)` on the request thread, which holds a container thread for the duration of a throttled download. See [../analysis/complexity-analysis.md](../analysis/complexity-analysis.md).

## Infrastructure and Deployment Configuration
**Minimally detected.** The repository contains no Dockerfile, Docker Compose file, Kubernetes manifest, Helm chart, CloudFormation/SAM template, Terraform configuration, CDK project, Ansible playbook, or CI/CD workflow (`.github/`, `.gitlab-ci.yml`, `Jenkinsfile`, `buildspec.yml` are all absent).

What does exist:
| Artifact | Purpose |
|---|---|
| `pom.xml` + inherited `spring-boot-maven-plugin` | Produces the executable JAR `target/download-server-0.0.1-SNAPSHOT.jar` |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` | Maven wrapper for reproducible builds |
| `src/main/resources/logback.xml` | The only runtime configuration file in the repository |

Deployment model is therefore `java -jar download-server-0.0.1-SNAPSHOT.jar` with embedded Tomcat on the default port 8080. Notably **absent**: any `application.properties` or `application.yml`, so server port, thread pool sizing, Actuator exposure, and error-response verbosity are all framework defaults that are neither documented nor version-controlled. Addressing this is remediation item R6/R13 in [../technical-debt/remediation-plan.md](../technical-debt/remediation-plan.md).

## Logging Configuration (the one declarative artifact present)
`src/main/resources/logback.xml`:
| Element | Value |
|---|---|
| Logger `com` | INFO |
| Logger `com.nurkiewicz` | ALL |
| Logger `net` | INFO |
| Logger `org` | INFO |
| Root | DEBUG, with an inline `ch.qos.logback.core.ConsoleAppender` |
| Pattern | `%d{HH:mm:ss.SSS} \| %thread \| %-5level \| %logger{1} \| %msg%n` |

Two structural observations: the appender is nested inside `<root>` rather than declared top-level and referenced with `<appender-ref>`, and the combination of root `DEBUG` with `com.nurkiewicz` at `ALL` produces verbose output that includes request UUIDs and, via `FileSystemPointer.toString()`, absolute filesystem paths. There is no file, rolling, or remote appender, so logs go only to stdout.

## Caching
**Detected at the HTTP protocol level only.** There is no server-side cache (no Spring Cache abstraction, no `@Cacheable`, no Ehcache/Caffeine/Redis). Caching is entirely client-directed through strong ETags and `Last-Modified`, with validation logic in `ExistingFile.cached`. Because no server-side cache exists, every request recomputes the file's SHA-512 — see [../technical-debt/maintenance-burden.md](../technical-debt/maintenance-burden.md) section 6.

## Internationalization, Scheduling, Security Configuration
**Not detected.** No `MessageSource`/resource bundles, no locale handling, no `@Scheduled`/`@EnableScheduling`, no Spring Security dependency or `SecurityFilterChain`/`WebSecurityConfigurer`, and no TLS/keystore configuration.

Related: [../architecture/system-overview.md](../architecture/system-overview.md) · [../diagrams/architecture.md](../diagrams/architecture.md)
