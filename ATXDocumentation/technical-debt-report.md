# Technical Debt Report

## 🎯 AWS Transformation Recommendation

### **RECOMMENDED TRANSFORMATIONS: AWS/java-version-upgrade, AWS/spring-boot-version-upgrade**

The project compiles at Java 1.8 (`maven-compiler-plugin` source/target `1.8`) and depends on Spring Boot 1.2.3.RELEASE with Spring Framework 4.1.x — all long past end of support, so `AWS/java-version-upgrade` is the highest-value starting point and also handles the Jakarta EE and Spring ecosystem dependency modernization required along the way. Once the project is on a supported JDK and Spring Boot 3.x, `AWS/spring-boot-version-upgrade` moves it to Spring Boot 4.x. No AWS SDK, Log4j, or application-server artifacts are present, so the SDK-migration and server-migration transformations do not apply.

---

## Executive Summary
`download-server` is a small (652 LOC), well-factored single-module Spring Boot service. Its functional design is clean; essentially all of its technical debt is version debt. Every framework and runtime version in the build is end-of-life, including Spring Boot 1.2.3.RELEASE (2015) and Spring Framework 4.1.x, which no longer receive security patches. Code-level issues are few and localized.

## Prioritized Findings

### High — EOL / deprecated / outdated runtimes and frameworks
| # | Finding | Location | Severity |
|---|---|---|---|
| H1 | Java 1.8 source/target; Java 8 is past free public support for most distributions | `pom.xml` (`maven-compiler-plugin` 3.0 `<source>/<target>`) | High |
| H2 | Spring Boot 1.2.3.RELEASE parent and starters — EOL since 2016, no security patches | `pom.xml` parent + `spring-boot-starter-web/-actuator/-test` | High |
| H3 | Spring Framework 4.1.6.RELEASE pinned explicitly across 9 artifacts — EOL, known CVEs in the 4.1 line | `pom.xml` | High |
| H4 | Unused `spring.version=4.1.1.RELEASE` property conflicts with the 4.1.6 pins and with the Boot parent's managed versions | `pom.xml` properties | High |
| H5 | Spring Boot 1.x Actuator exposes management endpoints unauthenticated by default | `spring-boot-starter-actuator` dependency; no security config present | High |

### Medium — outdated runtime/production dependencies
| # | Finding | Location | Severity |
|---|---|---|---|
| M1 | Guava 18.0 (2014); many releases behind, used for hashing, rate limiting, `MediaType` | `pom.xml`; `FileSystemPointer`, `ExistingFile`, `ThrottlingInputStream` | Medium |
| M2 | commons-io 2.4 — used only for the `ONE_MB` constant | `pom.xml`; `ExistingFile:21` | Medium |
| M3 | commons-codec 1.10 declared but not referenced by any source file — unused dependency | `pom.xml` | Medium |
| M4 | `groovy-all` 2.4.3 declared at compile scope although Groovy is used only by tests | `pom.xml` | Medium |
| M5 | `cglib-nodep` 3.1 at compile scope; needed only for Spock mocking of classes | `pom.xml` | Medium |
| M6 | Non-default repository `https://repo.spring.io` configured for milestone artifacts although all declared versions are GA | `pom.xml` `<repositories>` | Medium |

### Low — code quality and architectural issues
| # | Finding | Location | Severity |
|---|---|---|---|
| L1 | `new Integer("1234")` assigned to a dead local `temp`; constructor deprecated since Java 9 | `MainApplication.java:10` | Low |
| L2 | `modifiedAfter` returns `!clientTime.isBefore(getLastModified())`, i.e. true when the client copy is *newer or equal* — the method name states the opposite of its behavior, and the test `should not return file if server has older version than the client` encodes this inversion | `FileSystemPointer.java:83-85`; `DownloadControllerTest.groovy:100-111` | Low |
| L3 | `FileSystemStorage` is an acknowledged stub returning `logback.xml` for every UUID; no real storage backend | `FileSystemStorage.java:13-30` | Low |
| L4 | `resourceToReturn` returns `null` for non-GET methods, propagating a null body through `ResponseEntity` | `ExistingFile.java:83-88` | Low |
| L5 | `FileSystemPointer` constructor reads and hashes the whole file (SHA-512) eagerly and throws `IllegalArgumentException` on I/O failure — cost and error semantics both hidden in a constructor | `FileSystemPointer.java:19-30` | Low |
| L6 | Hard-coded 1 MB/s throttle and a fresh `RateLimiter` per request — no global bandwidth cap, not configurable | `ExistingFile.java:92` |Low |
| L7 | Groovy file `DownloadControllerTest.groovy` declares class `DownloadControllerSpec`; filename/class mismatch can cause the spec to be skipped by surefire naming rules | `src/test/groovy/.../DownloadControllerTest.groovy:25` | Low |
| L8 | Test-scope class placed in the `org.springframework.web.filter` package to reach a protected method; `Sha512ShallowEtagHeaderFilter` is never referenced | `src/test/java/org/springframework/web/filter/Sha512ShallowEtagHeaderFilter.java` | Low |
| L9 | No build plugin binding for Groovy compilation (`gmavenplus`/`groovy-eclipse`) is declared, so Spock specs may not compile | `pom.xml` | Low |
| L10 | `logback.xml` sets root level `DEBUG` with `com.nurkiewicz` at `ALL` — verbose production logging | `src/main/resources/logback.xml` | Low |
| L11 | Committed build output (`target/`) and `.DS_Store` in the repository | repository root | Low |

## Navigation
- [technical-debt/summary.md](technical-debt/summary.md)
- [technical-debt/outdated-components.md](technical-debt/outdated-components.md)
- [technical-debt/maintenance-burden.md](technical-debt/maintenance-burden.md)
- [technical-debt/remediation-plan.md](technical-debt/remediation-plan.md)
- [analysis/tech-debt.md](analysis/tech-debt.md)
- [analysis/security-patterns.md](analysis/security-patterns.md)
