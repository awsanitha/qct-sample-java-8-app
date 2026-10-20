# Technical Debt Assessment (Analysis View)

This file is the analysis-side companion to the root [../technical-debt-report.md](../technical-debt-report.md). Findings are grouped by debt category with evidence.

## Debt Profile
| Category | Volume | Dominant severity |
|---|---|---|
| Version / platform debt | 14 declared artifacts + runtime | High |
| Build-configuration debt | 6 issues | Medium |
| Code-quality debt | 11 issues | Low |
| Test debt | 4 issues | Low |
| Documentation debt | 3 issues | Low |

Version debt dominates. The application logic itself is clean and small; the risk is concentrated entirely in the platform generation.

## 1. Version / Platform Debt (High)
| Item | Evidence |
|---|---|
| Java 1.8 source/target | `pom.xml` `maven-compiler-plugin` 3.0 `<source>1.8</source><target>1.8</target>` |
| Spring Boot 1.2.3.RELEASE (2015) parent and three starters | `pom.xml` `<parent>`, `spring-boot-starter-{web,actuator,test}` |
| Spring Framework 4.1.6.RELEASE pinned on 9 artifacts | `pom.xml` |
| Transitive 2014-2015 generation: Tomcat 8.0.x, Jackson 2.4.x, Logback 1.1.x, JUnit 4.x, Mockito 1.x | via Boot 1.2.3 starters |
| Guava 18.0 (2014) | `pom.xml`; used in 5 classes |
| commons-io 2.4, commons-codec 1.10 | `pom.xml` |
| Groovy 2.4.3 / Spock 1.0-groovy-2.4 | `pom.xml`; Spock 1.0 does not support modern JDKs |
| maven-compiler-plugin 3.0 (2012) | `pom.xml` |
Consequence: no security patches reach this stack, and the Spock/Groovy generation present will not run on a modern JDK, so the test suite blocks any runtime upgrade until it is also modernized.

## 2. Build-Configuration Debt (Medium)
| Item | Evidence |
|---|---|
| Nine explicit Spring pins override the Boot parent's managed versions | `pom.xml` |
| Orphan `spring.version=4.1.1.RELEASE` property, never referenced | `pom.xml` `<properties>` |
| `spring-test` declared twice (both test scope, 4.1.6) | `pom.xml` |
| Boot starters repeat `<version>` already managed by the parent | `pom.xml` |
| `groovy-all` and `cglib-nodep` at compile scope though only tests use them | `pom.xml` |
| `https://repo.spring.io` milestone repository declared with no milestone artifacts in use | `pom.xml` `<repositories>` |
| No Groovy compiler plugin bound, so `src/test/groovy` has no configured compilation path | `pom.xml` `<build><plugins>` |
| No `application.properties`/`application.yml`; all runtime behavior is framework default | `src/main/resources/` |

## 3. Code-Quality Debt (Low)
| Item | Evidence |
|---|---|
| Dead local using a deprecated constructor: `Integer temp = new Integer("1234")` | `MainApplication.java:10` |
| `modifiedAfter` semantics inverted relative to its name | `FileSystemPointer.java:83-85` |
| Exception laundering to `IllegalArgumentException` in three places | `FileSystemPointer.java:27,36`; `ExistingFile.java:72` |
| `null` used as "no body" sentinel | `ExistingFile.java:71,87,99` |
| Full-file SHA-512 hash computed in a constructor, once per request | `FileSystemPointer.java:19-30` + `FileSystemStorage.java:28` |
| Hard-coded 1 MB/s throttle, new `RateLimiter` per request, no global cap | `ExistingFile.java:92` |
| Production storage is an acknowledged stub returning `logback.xml` for every UUID | `FileSystemStorage.java:13-30` |
| `{filename}` path variable accepted but never validated or used | `DownloadController.java:45` |
| `getOriginalName()` concatenated into a URI without encoding | `ExistingFile.java:70` |
| No `@ControllerAdvice` / global error handling | repository-wide |
| Root log level `DEBUG`, `com.nurkiewicz` at `ALL`; appender nested inside `<root>` instead of declared and referenced | `src/main/resources/logback.xml` |
| Guava's `MediaType` leaked into the project's own `FilePointer` interface | `FilePointer.java:19` |

## 4. Test Debt (Low)
| Item | Evidence |
|---|---|
| File/class name mismatch: `DownloadControllerTest.groovy` declares `DownloadControllerSpec`, which can prevent surefire from selecting it | `src/test/groovy/.../DownloadControllerTest.groovy:25` |
| Unreferenced test class kept in a framework package to reach a protected method | `src/test/java/org/springframework/web/filter/Sha512ShallowEtagHeaderFilter.java` |
| No direct unit tests for `ThrottlingInputStream`, `ExistingFile`, or `FileSystemStorage`; all 11 specs go through `MockMvc` | test tree |
| Tests assert the inverted `modifiedAfter` behavior, so they will fail if the semantics are corrected — the spec `should not return file if server has older version than the client` documents the inversion | `DownloadControllerTest.groovy:100-111` |

## 5. Documentation Debt (Low)
| Item | Evidence |
|---|---|
| `README.md` is two lines and describes the repository only as transformation-test material; no build/run/API instructions | `README.md` |
| No license headers in source files despite a repository `LICENSE` | all `src/` files |
| Committed build output `target/` and a `.DS_Store` file | repository root |

## Interest Being Paid
- **Security interest:** every month on an EOL stack adds unpatched CVE exposure (see [security-patterns.md](security-patterns.md) S3).
- **Upgrade interest:** the three-way Spring version divergence means each dependency change requires reasoning about parent-managed vs. pinned vs. property versions.
- **Performance interest:** re-hashing a file per request grows linearly with file size and request volume.
- **Capability interest:** the storage stub means the service cannot be used for real workloads without new code regardless of platform upgrades.

Related: [../technical-debt/summary.md](../technical-debt/summary.md) · [../technical-debt/remediation-plan.md](../technical-debt/remediation-plan.md) · [complexity-analysis.md](complexity-analysis.md)
