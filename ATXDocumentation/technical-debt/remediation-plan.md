# Remediation Plan

Prioritized action items. Priority follows the fixed order: EOL/deprecated runtimes and frameworks first, then outdated dependencies, then code quality and architecture. All effort statements are qualitative.

Recommended AWS-managed transformations for the platform work: **AWS/java-version-upgrade**, then **AWS/spring-boot-version-upgrade** (see [../technical-debt-report.md](../technical-debt-report.md)).

---

## Priority 1 — High: Runtimes and Frameworks

### R1. Collapse the build to a single source of Spring versions
**Severity: High · Complexity: Low · Blocks everything else**
- Delete the unused `<spring.version>4.1.1.RELEASE</spring.version>` property.
- Delete the explicit `<version>` from all nine `org.springframework:*` dependencies so the Boot parent manages them.
- Delete the duplicate `org.springframework:spring-test` declaration (keep one, test scope).
- Delete the redundant `<version>` on the three `spring-boot-starter-*` dependencies.
- Remove the `io.spring.repo.maven.milestone` repository — all artifacts are GA on Maven Central.
- Verification: `pom.xml` contains exactly one statement of the Spring version (the parent), and `mvn dependency:tree` shows a single Spring Framework version.

### R2. Bind a Groovy compiler plugin and fix the spec's name
**Severity: High · Complexity: Low · Prerequisite for any validation**
- Add `gmavenplus-plugin` (or `groovy-eclipse-compiler`) bound to `compileTests`, with `src/test/groovy` as a test source root.
- Rename `DownloadControllerTest.groovy` to `DownloadControllerSpec.groovy` so the filename matches the declared class, and configure surefire `<include>**/*Spec.java</include>`.
- Verification: the 11 feature methods appear in the surefire report rather than being silently skipped. Record the pre-upgrade baseline result before changing anything else.

### R3. Upgrade the test stack to Groovy 4.x + Spock 2.x
**Severity: High · Complexity: Medium · Must accompany R4**
- Move to `spock-core`/`spock-spring` 2.x (JUnit 5 platform) and Groovy 4.x; set `groovy-all` to `test` scope.
- Upgrade surefire to a JUnit-5-platform-capable version.
- Remove `cglib-nodep` — Spock 2.x supplies ByteBuddy/Objenesis.
- Replace `@ContextConfiguration` + `@WebAppConfiguration` with `@SpringBootTest` + `@AutoConfigureMockMvc` as appropriate for the target Boot version.
- Verification: all 11 specifications run and pass on the target JDK.

### R4. Upgrade Java 8 → Java 17 (then optionally 21)
**Severity: High · Complexity: Medium · Recommended transformation: AWS/java-version-upgrade**
- Upgrade `maven-compiler-plugin` 3.0 → 3.13.x and replace `<source>`/`<target>` with `<release>17</release>`.
- Verify the Maven wrapper distribution supports the target JDK.
- Remove `new Integer("1234")` (R10) — deprecated since Java 9.
- Verification: clean compile at release 17 and a green test suite.

### R5. Upgrade Spring Boot 1.2.3 → 2.7.x → 3.x
**Severity: High · Complexity: High · Recommended transformation: AWS/java-version-upgrade (then AWS/spring-boot-version-upgrade for 4.x)**
Take it in two hops; do not jump directly.
- **Hop 1 (Boot 2.7.x):** parent version bump after R1; re-verify `@RequestHeader(IF_MODIFIED_SINCE) Optional<Date>` binding, the `HttpMethod` handler argument, `ResponseEntity.body(null)` semantics for HEAD/304, and `InputStreamResource` streaming behavior.
- **Hop 2 (Boot 3.x):** `javax.*` → `jakarta.*` namespace migration (transitive servlet API), Spring Framework 6, embedded Tomcat 10.1+.
- Re-verify each of the 11 HTTP caching specs after each hop — they are the regression contract.
- Optional follow-on: Boot 4.x via `AWS/spring-boot-version-upgrade`.

### R6. Secure the Actuator surface
**Severity: High · Complexity: Low**
- Add an `application.yml` that moves management endpoints to a separate port and exposes only `health` and `info` (`management.endpoints.web.exposure.include`, `management.server.port`).
- Add Spring Security (or an upstream gateway policy) so `/env`, `/beans`, `/configprops`, and similar are not anonymously reachable.
- Verification: a request to a sensitive endpoint on the application port returns 404/401.

---

## Priority 2 — Medium: Outdated Dependencies

### R7. Remove unused and mis-scoped dependencies
**Severity: Medium · Complexity: Low**
- Delete `commons-codec` 1.10 — no source file imports it.
- Replace `FileUtils.ONE_MB` (`ExistingFile:21,92`) with a local `private static final long ONE_MB = 1024L * 1024L` and delete the `commons-io` dependency.
- Set `groovy-all` to `test` scope; remove `cglib-nodep` (per R3).
- Verification: `mvn dependency:analyze` reports no used-undeclared or unused-declared dependencies; the packaged JAR no longer contains Groovy or cglib.

### R8. Upgrade Guava and stop leaking its types
**Severity: Medium · Complexity: Medium**
- Change `FilePointer.getMediaType()` to return `Optional<org.springframework.http.MediaType>` (or a small project-local value type) so `com.google.common.net.MediaType` no longer appears in the project's own interface. Touches `FilePointer`, `FileSystemPointer`, `ExistingFile`, `FileExamples`.
- Rewrite `ExistingFile.toMediaType` (lines 115-119) without Guava's `Optional.transform(...).or(...)`; use `java.util.Optional.map(...).orElse(...)`.
- Then bump Guava to a current release. Confirm the resolved version has no known vulnerabilities before committing it.
- Optional: replace `Files.hash(target, Hashing.sha512())` with `MessageDigest` and the per-request `RateLimiter` with a shared limiter, which removes Guava from production entirely.
- Verification: compile plus the content-type and ETag specifications pass.

### R9. Refresh the logging stack
**Severity: Medium · Complexity: Low**
- SLF4J 2.x / Logback 1.5.x arrive with the Boot upgrade; confirm no explicit pins reintroduce the old generation.
- Fix `logback.xml`: declare the `ConsoleAppender` top-level with an `<appender-ref>` from `<root>` instead of nesting it, set root to `INFO`, and drop `com.nurkiewicz` from `ALL` to `DEBUG` or `INFO`.
- Verification: application starts with the expected log levels and a single console appender.

---

## Priority 3 — Low: Code Quality and Architecture

### R10. Delete dead code
**Severity: Low · Complexity: Low**
- `MainApplication.java:10` — remove `Integer temp = new Integer("1234");`.
- Delete `src/test/java/org/springframework/web/filter/Sha512ShallowEtagHeaderFilter.java`, or wire it into the filter chain deliberately if SHA-512 shallow ETags are wanted.
- Add `target/` and `.DS_Store` to `.gitignore` and remove them from version control (delete files individually, not by recursive directory removal).

### R11. Resolve the `modifiedAfter` naming/semantics inversion
**Severity: Low · Complexity: Medium (behavioral decision required)**
`FileSystemPointer:83-85` returns `!clientTime.isBefore(getLastModified())` — true when the client copy is at least as new as the server's. The name states the opposite, and the spec `should not return file if server has older version than the client` (`DownloadControllerTest.groovy:100-111`) encodes the current behavior.
- Decide the intended contract, then either rename the method to describe what it does (e.g. `clientCopyIsCurrent`) — the lower-risk option, with no behavior change — or correct the comparison and update both the interface documentation and the affected specifications.
- Do not change behavior and naming in the same commit.
- Guardrail: `FilePointer` is a public interface; renaming it is an API change. Prefer renaming only if downstream consumers are known.

### R12. Introduce global error handling
**Severity: Low · Complexity: Low**
- Replace the three `IllegalArgumentException` wrappings (`FileSystemPointer:27,36`; `ExistingFile:72`) with distinct, meaningful exception types.
- Add a `@ControllerAdvice` mapping storage/I-O failures to 404 or 500 without leaking cause detail or filesystem paths.
- Verification: a forced I/O failure yields a clean status code with no stack trace in the response body.

### R13. Make throttling configurable and bounded
**Severity: Low · Complexity: Low**
- Externalize the rate to configuration (e.g. `download.max-bytes-per-second`).
- Share one `RateLimiter` bean across requests so there is an aggregate bandwidth cap rather than N MB/s for N concurrent downloads.
- Verification: concurrent downloads respect the global cap.

### R14. Avoid re-hashing files on every request
**Severity: Low · Complexity: Medium**
- Move the SHA-512 computation out of `FileSystemPointer`'s constructor (make it lazy) and cache ETags keyed by file identity plus last-modified, so 304 responses do not pay a full-file read.
- Verification: a repeated conditional request performs no full file read.

### R15. Replace the storage stub (feature work)
**Severity: Low · Complexity: High**
- Implement a real `FileStorage` with a genuine UUID-to-file mapping; `FileSystemStorage` currently returns `logback.xml` for every UUID and so discloses an internal configuration file.
- URL-encode `getOriginalName()` before concatenating it into the redirect `Location` (`ExistingFile:70`) and reject names containing control characters.
- Validate or drop the unused `{filename}` path variable (`DownloadController:45`).
- Keep `FileSystemStorage` and `FileStorageStub` in place as the profile-selected adapters — they are the designed seam.

### R16. Add unit tests and documentation
**Severity: Low · Complexity: Low**
- Add direct unit tests for `ThrottlingInputStream` (rate-limiter interaction, delegation), `ExistingFile.cached` (the four header combinations), and `FileSystemPointer` (ETag format, media-type absence).
- Expand `README.md` with build, run, and endpoint documentation.
- Preserve all existing specifications — modify assertions only where the target framework requires it.

---

## Execution Order
```
R1 ──▶ R2 ──▶ [baseline test result recorded]
   └──▶ R3 ──┬──▶ R4 ──▶ R5 (hop 1: Boot 2.7) ──▶ R5 (hop 2: Boot 3.x) ──▶ R6
             │
             └──▶ R7 ──▶ R8 ──▶ R9
                              └──▶ R10 … R16 (independent, any order)
```
R1 and R2 are strict prerequisites: without a single version source and a runnable test suite, no later step can be validated. R3 and R4 must land together — the old Spock/Groovy generation will not run on a modern JDK.

## Guardrails for All Steps
- Preserve all 11 Spock feature methods; assertions may be updated for framework compatibility but no test may be removed or disabled.
- Do not rename public types or methods (`DownloadController`, `ExistingFile`, `FilePointer`, `FileStorage`, `FileSystemPointer`, `FileSystemStorage`, `ThrottlingInputStream`, `MainApplication`) except as deliberately decided in R11.
- Do not downgrade any dependency below its current version.
- Add repositories only if they are standard public registries; prefer Maven Central alone.
- Check every new or changed dependency version for known vulnerabilities before committing it.

Validation criteria: [../migration/validation-criteria.md](../migration/validation-criteria.md) · Ordering detail: [../migration/component-order.md](../migration/component-order.md)
