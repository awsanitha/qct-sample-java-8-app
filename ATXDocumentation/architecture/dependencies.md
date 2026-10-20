# Dependencies

## Internal Dependency Graph (class → class)
```
MainApplication
    └── (Spring context) DownloadController, FileSystemStorage | FileStorageStub

DownloadController
    ├── FileStorage            (constructor injection)
    └── ExistingFile           (instantiation)

ExistingFile
    ├── FilePointer            (field)
    └── ThrottlingInputStream  (instantiation)
            └── com.google.common.util.concurrent.RateLimiter

FileSystemStorage ──implements──▶ FileStorage
        └── FileSystemPointer ──implements──▶ FilePointer

FileStorageStub (test) ──implements──▶ FileStorage
        └── FileExamples (test) ──▶ FileSystemPointer

DownloadControllerSpec (test) ──▶ MainApplication, FileExamples, MockMvc
```
No cycles. Depth from controller to leaf is 3. Interfaces `FileStorage` and `FilePointer` have no outgoing dependencies on concrete project classes, so they form clean seams.

## Declared External Dependencies (`pom.xml`)
| GroupId:ArtifactId | Version | Scope | Used by |
|---|---|---|---|
| org.springframework.boot:spring-boot-starter-parent | 1.2.3.RELEASE | parent | build-wide dependency management |
| org.springframework.boot:spring-boot-starter-web | 1.2.3.RELEASE | compile | MVC + embedded Tomcat for `DownloadController` |
| org.springframework.boot:spring-boot-starter-actuator | 1.2.3.RELEASE | compile | operational endpoints (no code reference) |
| org.springframework.boot:spring-boot-starter-test | 1.2.3.RELEASE | test (excludes `spring-test`) | test harness |
| org.springframework:spring-core / -beans / -context / -context-support / -aop / -expression / -web / -webmvc | 4.1.6.RELEASE | compile | explicit pins overriding the Boot parent |
| org.springframework:spring-test | 4.1.6.RELEASE | test (declared twice) | `MockMvc` in the Spock spec |
| com.google.guava:guava | 18.0 | compile | `Hashing`, `Files.hash`, `MediaType`, `MoreObjects`, `RateLimiter`, `HttpHeaders` (test) |
| commons-io:commons-io | 2.4 | compile | `FileUtils.ONE_MB` in `ExistingFile` |
| commons-codec:commons-codec | 1.10 | compile | **no source reference — unused** |
| org.codehaus.groovy:groovy-all | 2.4.3 | compile | Spock specification language (test-only usage) |
| org.spockframework:spock-core | 1.0-groovy-2.4 | test | `spock.lang.Specification` |
| org.spockframework:spock-spring | 1.0-groovy-2.4 | test (excludes `groovy-all`) | Spring context support in Spock |
| cglib:cglib-nodep | 3.1 | compile | class mocking support for Spock |

### Build Plugins
| Plugin | Version | Configuration |
|---|---|---|
| org.apache.maven.plugins:maven-compiler-plugin | 3.0 | `<source>1.8</source> <target>1.8</target>` |
| (inherited) spring-boot-maven-plugin | 1.2.3.RELEASE | from parent, repackages executable JAR |

No Groovy compiler plugin is bound, which is required for `src/test/groovy` to be compiled.

### Repositories
- `io.spring.repo.maven.milestone` → `https://repo.spring.io` (snapshots disabled). All declared versions are GA, so this repository is not required.

### Notable Transitive Dependencies
Via `spring-boot-starter-web` 1.2.3: `spring-boot`, `spring-boot-autoconfigure`, `spring-boot-starter-tomcat` (Tomcat 8.0.x), `spring-boot-starter-logging` (SLF4J 1.7.x + Logback 1.1.x), Jackson 2.4.x, `spring-webmvc`/`spring-web` (overridden to 4.1.6 by the explicit pins). Via `spring-boot-starter-test`: JUnit 4.x, Mockito 1.x, Hamcrest 1.3, AssertJ 1.x.

### Version Conflict Risk
The `spring.version` property is set to `4.1.1.RELEASE` but never referenced; the nine Spring artifacts are hard-pinned to `4.1.6.RELEASE`, which also overrides the Boot 1.2.3 parent's managed Spring version. This three-way divergence (parent-managed vs. property vs. pin) is the main build-fragility source.

## Declarative / Non-Code Definitions
A repository-wide scan for declarative service definitions found only one non-code configuration artifact:

| File | Type | Contents |
|---|---|---|
| `src/main/resources/logback.xml` | Logback configuration | Loggers `com` (INFO), `com.nurkiewicz` (ALL), `net` (INFO), `org` (INFO); root at DEBUG with an inline `ch.qos.logback.core.ConsoleAppender` and pattern `%d{HH:mm:ss.SSS} \| %thread \| %-5level \| %logger{1} \| %msg%n`. Note the appender is nested inside `<root>` rather than declared top-level and referenced by `<appender-ref>`. |

Absent: Spring XML bean/context files, `web.xml`, `persistence.xml`, MyBatis/Hibernate mappings, WSDL, OpenAPI/Swagger, GraphQL/`.proto`/Thrift/Avro IDL, Camel routes, BPMN/Airflow DAGs, CloudFormation/SAM, Kubernetes manifests, Docker Compose, `application.properties`/`application.yml`. All wiring is annotation-driven, so there are no declarative-definition → implementation mappings to record beyond the profile-based bean selection described in [components.md](components.md).

See [../analysis/dependency-analysis.md](../analysis/dependency-analysis.md) and [../technical-debt/outdated-components.md](../technical-debt/outdated-components.md).
