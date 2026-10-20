# Outdated Components

Every version below was read directly from `pom.xml` or from the compiler configuration. Support status reflects publicly documented vendor lifecycles.

## Runtime and Language (Severity: High)
| Component | Declared | Status | Evidence |
|---|---|---|---|
| Java language/bytecode level | 1.8 | Java 8 is past free public support for most distributions; LTS releases 17 and 21 are the current targets. Amazon Corretto 8 remains supported, but the language level blocks records, sealed types, pattern matching, and virtual threads. | `pom.xml` — `maven-compiler-plugin` `<source>1.8</source><target>1.8</target>` |
| Groovy | 2.4.3 | 2.x line is end-of-life; Groovy 4.x is current. Groovy 2.4 cannot run on modern JDKs. | `pom.xml` `org.codehaus.groovy:groovy-all` |

Note: the POM uses the legacy `<source>`/`<target>` pair rather than `<release>`, and `maven-compiler-plugin` 3.0 predates `<release>` support entirely.

## Frameworks (Severity: High)
| Component | Declared | Status | Evidence |
|---|---|---|---|
| Spring Boot (parent + starters) | 1.2.3.RELEASE (2015) | End of life since 2016. No OSS or commercial patches. Boot 3.x is current; Boot 4.x is the next target. Upgrading crosses two major versions and the `javax` → `jakarta` namespace migration. | `pom.xml` `<parent>`, `spring-boot-starter-web`, `-actuator`, `-test` |
| Spring Framework | 4.1.6.RELEASE | End of life. The 4.1 line has publicly known vulnerabilities and receives no fixes. Spring 6.x accompanies Boot 3.x. | `pom.xml` — nine pinned artifacts: `spring-core`, `-beans`, `-context`, `-context-support`, `-aop`, `-expression`, `-web`, `-webmvc`, `-test` |
| Spring Boot Actuator | 1.2.3.RELEASE | In Boot 1.x, sensitive endpoints are exposed on the application port by default and the `management.*` security model differs entirely from Boot 2/3. No configuration file exists to restrict them. | `pom.xml` |
| Embedded Tomcat (transitive) | 8.0.x via Boot 1.2.3 | Tomcat 8.0 reached end of life in 2018. Tomcat 10.1+/11 accompany modern Boot. | transitive through `spring-boot-starter-web` |
| Servlet API | 3.1 generation (`javax.servlet`) | Superseded by `jakarta.servlet`; the namespace change is mandatory for Boot 3.x. | transitive |

## Version Conflict (Severity: High)
Three different, mutually inconsistent statements of the Spring version coexist:
| Source | Value |
|---|---|
| `spring-boot-starter-parent:1.2.3.RELEASE` dependency management | Spring 4.1.6-era managed version |
| Explicit `<version>` on nine `org.springframework:*` dependencies | `4.1.6.RELEASE` |
| `<properties><spring.version>` — defined but never referenced | `4.1.1.RELEASE` |

Additionally, `org.springframework:spring-test:4.1.6.RELEASE` is declared twice in test scope, and the three Boot starters repeat a `<version>` that the parent already manages. Resolution must precede any upgrade: delete the orphan property, delete the nine explicit pins, and delete the starter versions so the parent is the single source of truth.

## Production Dependencies (Severity: Medium)
| Component | Declared | Status | Usage |
|---|---|---|---|
| Google Guava | 18.0 (2014) | Many major releases behind. `MediaType.charset().transform(...).or(...)` relies on Guava's own `Optional` API, whose surface changed in later releases — this call site will need rework on upgrade. | `Hashing`, `Files.hash`, `MoreObjects`, `net.MediaType`, `RateLimiter` across 5 classes |
| Apache Commons IO | 2.4 (2012) | Outdated; the 2.1x line is current. | `FileUtils.ONE_MB` constant only — removable |
| Apache Commons Codec | 1.10 | Outdated **and unused** — no source file imports it. | none — delete the dependency |
| Jackson (transitive) | 2.4.x | Outdated generation; carried even though the application never serializes JSON. | transitive |
| Logback / SLF4J (transitive) | 1.1.x / 1.7.x | Outdated; SLF4J 2.x and Logback 1.5.x are current. | `Logger`/`LoggerFactory` in 3 classes, `logback.xml` |

## Developer / Build Dependencies (Severity: Low)
| Component | Declared | Status | Note |
|---|---|---|---|
| Spock | 1.0-groovy-2.4 | End of life. Spock 2.x runs on the JUnit 5 platform and is required for modern JDKs. | `spock-core`, `spock-spring`; the only test framework actually used |
| cglib-nodep | 3.1 | Outdated, and declared at **compile** scope although only Spock class-mocking needs it. Modern Spock supplies ByteBuddy/Objenesis itself. | removable after the Spock upgrade |
| `groovy-all` | 2.4.3 at **compile** scope | Test-only language runtime shipped in the production artifact. | rescope to `test`, then upgrade with Spock |
| maven-compiler-plugin | 3.0 (2012) | Far behind; does not support `<release>`. | 3.13.x+ is current |
| Maven wrapper | present (`mvnw`, `.mvn/wrapper`) | Distribution version should be verified against the wrapper properties when upgrading. | — |
| Groovy compiler plugin | **absent** | No `gmavenplus-plugin` or `groovy-eclipse-compiler` is bound, so `src/test/groovy` has no configured compilation path despite containing the entire test suite. | must be added |

## Non-Default Repository (Severity: Medium)
```xml
<repository>
  <id>io.spring.repo.maven.milestone</id>
  <url>https://repo.spring.io</url>
  <snapshots><enabled>false</enabled></snapshots>
</repository>
```
Every declared version is a GA release available from Maven Central, so this repository is unnecessary and should be removed to keep the build's trusted artifact sources minimal.

## Deprecated APIs and Conventions in Code
| Item | Location | Replacement |
|---|---|---|
| `new Integer(String)` | `MainApplication.java:10` | Deprecated since Java 9 — and the local `temp` is unused, so delete the line entirely |
| `<source>`/`<target>` compiler config | `pom.xml` | `<release>` (requires a newer compiler plugin) |
| Guava `Optional.transform(...).or(...)` | `ExistingFile.java:115-119` | `java.util.Optional.map(...).orElse(...)` |
| `java.util.Date` in handler signatures | `DownloadController.java:38,50` | `java.time.Instant`/`ZonedDateTime`; Spring supports `@RequestHeader` conversion to `java.time` types |
| `javax.*` namespace (transitive servlet API) | framework-level | `jakarta.*` as part of the Boot 3.x migration |

No deprecated or broken cryptographic algorithm is used — ETags are SHA-512.

## Upgrade Path Summary
```
Java 8          ──▶ Java 17 (then optionally 21)
Spring Boot 1.2.3 ──▶ 2.7.x (intermediate, resolves most API moves)
                  ──▶ 3.x   (javax → jakarta, Spring Framework 6)
                  ──▶ 4.x   (optional follow-on)
Spring Framework 4.1.6 pins ──▶ removed; inherit from the Boot parent
Groovy 2.4 + Spock 1.0 ──▶ Groovy 4.x + Spock 2.x (JUnit 5 platform)
Guava 18.0      ──▶ current release (rework toMediaType)
commons-io 2.4  ──▶ removed (inline the 1 MB constant)
commons-codec   ──▶ removed (unused)
cglib-nodep     ──▶ removed (Spock 2.x supplies its own)
maven-compiler-plugin 3.0 ──▶ 3.13.x with <release>
```

Detailed sequencing: [remediation-plan.md](remediation-plan.md). Effort concentration: [maintenance-burden.md](maintenance-burden.md).
