# Maintenance Burden

Where ongoing effort and risk concentrate, ranked by burden.

## 1. `pom.xml` — Build Configuration (Burden: High)
Fourteen of the project's debt items live in a single 150-line file. Every dependency change requires reasoning about three competing version sources (Boot parent management, nine explicit `4.1.6.RELEASE` pins, an unused `spring.version` property), a duplicated `spring-test` declaration, redundant starter versions, two test libraries at compile scope, and a non-default repository. There is also no Groovy compiler plugin, so the test source root has no configured build path.

Burden characteristics: every upgrade touches this file; mistakes surface as mixed-framework `NoSuchMethodError` at runtime rather than at compile time. **Complex, and the mandatory first step** for any other work.

## 2. Test Suite Modernization (Burden: High)
The entire behavioral safety net is 11 Spock feature methods in `DownloadControllerTest.groovy`, running on Groovy 2.4.3 and Spock 1.0-groovy-2.4 — neither of which runs on a modern JDK. This creates a circular dependency: the runtime upgrade needs the tests for validation, and the tests need the runtime upgrade to run.

Compounding issues:
- The file is named `DownloadControllerTest.groovy` but declares `DownloadControllerSpec`; surefire's default naming rules may therefore skip it entirely, meaning the suite's current pass/fail status cannot be assumed.
- No Groovy compiler plugin is bound at all.
- Spock 2.x requires the JUnit 5 platform and a surefire version that supports it.
- `ThrottlingInputStream`, `ExistingFile`, and `FileSystemStorage` have no direct unit tests, so all coverage is end-to-end and slow to diagnose.

**Significant effort, high coupling to the platform upgrade.** Must be modernized in the same effort, not after.

## 3. Guava Coupling (Burden: Medium)
Guava appears in five of eight production classes and, critically, in the project's own public interface: `FilePointer.getMediaType()` returns `Optional<com.google.common.net.MediaType>`. A Guava upgrade is therefore not a dependency bump but an API change rippling through `FilePointer`, `FileSystemPointer`, `ExistingFile`, and `FileExamples`.

The specific breakage point is `ExistingFile.toMediaType` (lines 115-119), which chains Guava's `MediaType.charset()` with Guava's own `Optional.transform(...).or(...)` — an API surface that shifted in later releases.

**Moderate effort, well-localized**: four files, and the fix (a small local media-type value object, or `java.util.Optional` plus `MessageDigest`) also reduces the project's third-party surface permanently.

## 4. The Spring Boot 1.x → 3.x Jump (Burden: High)
Two major versions plus a namespace migration:
- `javax.servlet` → `jakarta.servlet` across the framework.
- Actuator's security and endpoint model changed completely between 1.x, 2.x, and 3.x; since no `application.properties` exists, all current behavior is framework default and the new defaults will differ.
- `@RequestHeader` binding of `Optional<Date>` and the `HttpMethod` handler argument are long-standing but should be re-verified against modern Spring MVC argument resolution.
- `ResponseEntity.body(null)` behavior for HEAD/304 responses needs re-verification — Spring's handling of null bodies and `Content-Length` on 304 has changed over time.

Mitigating factor: the application uses a very small slice of Spring (annotations, `ResponseEntity`, `InputStreamResource`, `@Profile`). An intermediate stop at Boot 2.7.x is the lower-risk route.

## 5. Stub Storage Implementation (Burden: Medium)
`FileSystemStorage` is explicitly documented as a stub returning `logback.xml` for every UUID. The service cannot serve real workloads regardless of platform state, and the production profile currently discloses an internal configuration file to anonymous callers. Any real backend must also address what the stub lets the code ignore: UUID-to-file mapping and lifecycle, ETag caching instead of per-request hashing, and sanitization of `getOriginalName()` before it reaches the redirect `Location` header.

**Moderate effort, but it is feature work rather than debt repayment** — and it is where the eager-hashing and unencoded-filename issues become live rather than latent.

## 6. Performance Characteristics (Burden: Medium)
| Issue | Location | Consequence |
|---|---|---|
| Full-file SHA-512 computed in `FileSystemPointer`'s constructor, once per request | `FileSystemPointer:19-30`, `FileSystemStorage:28` | Cost is O(file size) on *every* request, including requests that end in 304. Large files make ETag generation the dominant cost. |
| Blocking `RateLimiter.acquire()` on the container thread | `ThrottlingInputStream:20,26,32` | A 1 MB/s cap means a 1 GB download occupies a Tomcat thread for a very long time; the default thread pool is the scalability ceiling. |
| New `RateLimiter` per request | `ExistingFile:92` | No aggregate bandwidth cap — N concurrent downloads consume N MB/s. |
| Inconsistency window | `tag` cached at construction vs. live `getSize()`/`getLastModified()` | A file modified mid-flight yields a mismatched ETag and `Content-Length`. |
No caching layer, no async/streaming response support, and no metrics beyond default Actuator exist to observe any of this.

## 7. Operational Blind Spots (Burden: Medium)
- No `application.properties`/`application.yml`: port, thread pool, Actuator exposure, and error-response detail are all framework defaults, undocumented and unversioned.
- `logback.xml` sets root to `DEBUG` and `com.nurkiewicz` to `ALL`, and nests the `ConsoleAppender` inside `<root>` rather than declaring it top-level with `<appender-ref>`. Production logs will be verbose and will contain capability UUIDs and absolute filesystem paths.
- No `@ControllerAdvice`: wrapped `IllegalArgumentException`s reach Boot's default error handler as 500s with cause detail.
- No Dockerfile, CI workflow, or infrastructure-as-code in the repository, so deployment knowledge is entirely external to it.

## 8. Low-Burden Cleanups (Burden: Low)
| Item | Location | Fix |
|---|---|---|
| Dead local with deprecated constructor | `MainApplication:10` | delete the line |
| Unused `Sha512ShallowEtagHeaderFilter` in a framework package | `src/test/java/org/springframework/web/filter/` | delete, or wire it up deliberately |
| Unused `commons-codec` dependency | `pom.xml` | delete |
| commons-io used only for `ONE_MB` | `ExistingFile:21,92` | inline the constant, drop the dependency |
| Committed `target/` output and `.DS_Store` | repository root | add to `.gitignore`, remove from version control |
| Two-line `README.md` with no build/run/API instructions | `README.md` | document build, run, and the two endpoints |
| No license headers despite a repository `LICENSE` | all `src/` files | add headers if project policy requires them |

## Burden Summary
| Area | Burden | Blocking? |
|---|---|---|
| `pom.xml` version untangling | High | Yes — blocks everything |
| Test suite modernization | High | Yes — blocks upgrade validation |
| Boot 1.x → 3.x migration | High | Core of the work |
| Guava decoupling | Medium | No, but required for the Guava bump |
| Stub storage replacement | Medium | No — feature work |
| Performance rework | Medium | No |
| Operational configuration | Medium | No |
| Cleanups | Low | No |

Sequencing: [remediation-plan.md](remediation-plan.md) · Debt evidence: [../analysis/tech-debt.md](../analysis/tech-debt.md)
