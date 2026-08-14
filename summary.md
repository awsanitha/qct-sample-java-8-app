# Java 25 Migration — Post-OpenRewrite Fix Summary

## Status: ✅ BUILD SUCCESS — 12/12 tests passing

```
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## What OpenRewrite did

The OpenRewrite `UpgradeToJava25` recipe updated:
- `java.version` property to `25`
- `maven-compiler-plugin` to `3.15.0` with `<release>25</release>`
- `commons-codec` to `1.17.2`

It did **not** upgrade Spring Boot (the `springBootTargetVersion` was `none`) or the test stack.

---

## What this cycle fixed

### 1. `pom.xml` — comprehensive upgrade

| Component | Before | After | Reason |
|---|---|---|---|
| Spring Boot parent | 1.2.3.RELEASE | **3.5.16** | Spring Boot 1.x cannot run on Java 25 — old ASM/CGLIB fails on class-file v69 |
| `spring.version` property | 4.1.1.RELEASE | removed | Managed by Spring Boot 3.5 BOM |
| Spring Framework explicit deps | 4.1.6.RELEASE ×8 | removed | Managed by BOM |
| `cglib-nodep` | 3.1 | removed | Spring 5+ bundles its own CGLIB |
| `groovy-all` groupId | `org.codehaus.groovy:2.4.3` | `org.apache.groovy:4.0.33` (pom type) | Groovy 2.4 cannot produce Java 21+ class files |
| `spock-core` | `1.0-groovy-2.4` | `2.4-groovy-4.0` | Spock 1.x is incompatible with Groovy 4 and JUnit Platform |
| `spock-spring` | `1.0-groovy-2.4` | `2.4-groovy-4.0` | Same |
| `maven-surefire-plugin` | `2.17` | `3.5.2` | Surefire 2.x cannot discover JUnit Platform / Spock 2 tests |
| `commons-io` | `2.4` | `2.18.0` | Java 17+ compatibility |
| `guava` | `29.0-jre` | `33.4.0-jre` | Java 17+ internals access |
| `byte-buddy-agent` | not declared | `1.15.11` (test) | Required by Spock 2 for mocking; referenced in surefire argLine |
| Duplicate `spring-test` dep | 2 declarations | removed (managed by BOM) | Maven model warning; BOM handles it |
| GMavenPlus plugin | missing | `5.1.0` | Without this, `.groovy` files in `src/test/groovy` were never compiled |
| Surefire `<includes>` | default (*.java patterns) | added `**/*Spec.groovy`, `**/*Test.groovy` | Default patterns miss Groovy spec class names |

### 2. `src/test/java/org/springframework/web/filter/Sha512ShallowEtagHeaderFilter.java`

Spring Framework 6 changed `ShallowEtagHeaderFilter.generateETagHeaderValue` from:
```java
// Spring 4.x / 5.x
protected String generateETagHeaderValue(byte[] bytes)
```
to:
```java
// Spring 6.x
protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException
```

Updated the override to use the new signature with `inputStream.readAllBytes()`.

### 3. `src/main/resources/logback.xml`

Spring Boot 3.5 uses Logback 1.5.x which **rejects inline `<appender>` elements without a `name` attribute** as a hard error. This caused `ApplicationContext failure threshold (1) exceeded` at test startup.

Fixed by moving the inline appender to a named top-level `<appender name="CONSOLE">` with `<appender-ref ref="CONSOLE"/>` in the root logger.

### 4. `src/test/groovy/com/nurkiewicz/download/DownloadControllerTest.groovy`

Replaced `@WebAppConfiguration` + `@ContextConfiguration(classes = [MainApplication])` + manual `MockMvcBuilders.webAppContextSetup(wac).build()` with `@SpringBootTest` + `@AutoConfigureMockMvc`. In Spring Boot 3 / Spring MVC 6, `@ContextConfiguration` bypasses Spring Boot auto-configuration, so the `DispatcherServlet` was initialized without handler mappings, causing all requests to return 404.

### 5. `src/test/java/com/nurkiewicz/download/FileExamples.java`

Changed `TXT_FILE_UUID = UUID.randomUUID()` and `NOT_FOUND_UUID = UUID.randomUUID()` to fixed deterministic UUIDs. In Spring Boot 3 test context, classloader isolation between the Spring-managed `FileStorageStub` and the Groovy test caused `FileExamples` to be loaded twice, generating different random UUIDs and making the stub never match the test's expected UUID.

---

## Next steps (notes for subsequent cycles)

- The `groovy-all` pom BOM pulls in Groovy templates which triggers a Spring Boot auto-configuration warning about missing `/templates/`. This is harmless (test scope only) but could be suppressed with `spring.groovy.template.check-template-location=false` in test `application.properties` if desired.
- The `MainApplication.main` still has an unused variable `Integer temp = Integer.valueOf("1234")`. This was present pre-migration and is a dead code smell but was not introduced by this migration cycle.
- The `@RequestHeader(IF_MODIFIED_SINCE) Optional<Date>` in `DownloadController` works in Spring MVC 6 but `Date` is deprecated in Java 8+. A future cycle could replace it with `Optional<Instant>` for a cleaner API.
- The `FileSystemPointer` uses deprecated `Hashing.sha512()` from Guava 29 (still present in 33.x but flagged). Not a blocker.
