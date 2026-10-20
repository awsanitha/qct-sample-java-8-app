# Project Overview

## Identity
- Maven coordinates: `com.nurkiewicz.download:download-server:0.0.1-SNAPSHOT`
- Packaging: JAR (Spring Boot executable, inherited from `spring-boot-starter-parent`)
- Purpose: HTTP file-download server demonstrating correct HTTP caching semantics (ETag, `If-None-Match`, `If-Modified-Since`), `HEAD` support, 301 redirect to a filename-bearing URL, and bandwidth throttling.
- README states the repository is a sample Java 8 Maven project used to exercise code-transformation tooling.

## Technology Stack
| Layer | Technology | Version |
|---|---|---|
| Language | Java | 1.8 (`maven-compiler-plugin` source/target) |
| Test language | Groovy | 2.4.3 |
| Framework | Spring Boot | 1.2.3.RELEASE |
| Web | Spring Web MVC / embedded Tomcat | 4.1.6.RELEASE (Spring Framework) |
| Ops | Spring Boot Actuator | 1.2.3.RELEASE |
| Utilities | Google Guava | 18.0 |
| Utilities | Apache Commons IO / Codec | 2.4 / 1.10 |
| Logging | SLF4J + Logback (via starter), config `src/main/resources/logback.xml` | transitive |
| Testing | Spock (`spock-core`, `spock-spring`) 1.0-groovy-2.4, Spring Test 4.1.6, cglib-nodep 3.1 | — |
| Build | Maven (wrapper `mvnw`), `maven-compiler-plugin` 3.0 | — |

## Repository Layout
```
/data/code
├── pom.xml                  single-module Maven build
├── mvnw, mvnw.cmd, .mvn/    Maven wrapper
├── README.md, LICENSE, CODE_OF_CONDUCT.md
├── src/main/java/com/nurkiewicz/download/   8 production classes
├── src/main/resources/logback.xml
├── src/test/java/...        Java test fixtures + Spring filter subclass
├── src/test/groovy/...      Spock specification
├── src/test/resources/download.txt
└── target/                  build output (excluded from analysis)
```

## Size
- 652 lines across Java and Groovy sources (excluding `target/`).
- 8 production types, 4 test types. Not a large codebase.

## Runtime Model
A single Spring Boot process started by `MainApplication.main`. Component scanning picks up `DownloadController` and a `FileStorage` implementation chosen by Spring profile (`FileSystemStorage` when profile `test` is not active; `FileStorageStub` under profile `test`). No database, message broker, or external service is used. There is no infrastructure-as-code, Dockerfile, or CI configuration in the repository.

See [architecture/system-overview.md](architecture/system-overview.md) and [technical-debt-report.md](technical-debt-report.md).
