# Summary

## Status
Build passes: ✅  
Tests: 12 run, 0 failures, 0 errors

## Changes Made

### 1. `pom.xml` — Major upgrades
- **Spring Boot**: 3.0.13 → 3.5.0 (required for ASM that supports Java 25 class files)
- **Groovy**: `org.codehaus.groovy:groovy-all:2.4.16` → `org.apache.groovy:groovy:4.0.32` (via BOM; Groovy 4 supports Java 25 runtime)
- **Spock**: 1.0-groovy-2.4 → 2.3-groovy-4.0 (Spock 2.x targets JUnit 5 Platform)
- **Surefire**: 2.22.2 → 3.2.5 (needed for proper JUnit Platform / Spock 2 test discovery)
- Added `gmavenplus-plugin:3.0.2` to compile `.groovy` test sources (was missing entirely)
  - `skipBytecodeCheck=true` — Groovy 4.0.32 does not yet enumerate Java 25 as a known bytecode target
  - `targetBytecode=21` — Spring's ASM component scans test classpath; compiling Groovy tests to Java 25 bytecode causes an `Unsupported class file major version 69` error during context loading
- Removed explicit Spring version overrides (spring-beans, spring-core, etc. @ 6.0.23); Spring Boot 3.5.0 manages Spring 6.2.7
- Removed `cglib-nodep:3.1` (bundled in Spring since Spring 6)
- Removed `jakarta.servlet-api` explicit test dependency (managed by Spring Boot)
- Removed `spring-test` explicit dependency and its exclusion from `spring-boot-starter-test`

### 2. `src/main/resources/logback.xml` — Fix invalid configuration
The appender was defined inline inside `<root>` which is invalid Logback XML syntax (causes `IllegalStateException: Logback configuration error detected` on Spring Boot startup). Moved `<appender>` to top level and referenced via `<appender-ref>`.

### 3. `src/test/groovy/.../DownloadControllerTest.groovy` — Modernise test
- Replaced `@WebAppConfiguration` + `@ContextConfiguration` + manual `MockMvcBuilders.webAppContextSetup()` with `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Autowired MockMvc`
- Added `@ContextConfiguration(classes = [MainApplication])` alongside `@SpringBootTest` — required because Spock 2's `SpringExtension.isSpringSpec()` checks for `@ContextConfiguration` directly (the `MetaAnnotationUtils.findAnnotationDescriptorForTypes` path used to detect `@BootstrapWith`/`@SpringBootTest` was removed in Spring 6)
- Replaced `static final TEXT_FILE` field (evaluated at Groovy class-load time, before Spring context, causing UUID mismatch) with `textFile()` and `txtFile()` methods evaluated at test-execution time within the Spring context classloader
- Injected `FileStorage` bean and used it to resolve `FilePointer` from the live Spring context (eliminates the static `FileExamples.TXT_FILE` classloader split)

### 4. `src/test/java/.../Sha512ShallowEtagHeaderFilter.java` — Fix overridden method signature
`ShallowEtagHeaderFilter.generateETagHeaderValue` signature changed from `(byte[])` to `(InputStream, boolean) throws IOException` in Spring 6. Updated override accordingly.

## Next Steps
- The `Hashing.sha512()` method in Guava 29 is `@Deprecated`; consider upgrading Guava (current: 29.0-jre) to 33.x and switching to `Hashing.sha512()` or `MessageDigest`-based approach — however this is a separate concern from the Java 25 upgrade.
- The `commons-io` dependency is pinned at 2.4 (released 2012); consider upgrading to 2.16+.
- The `skipBytecodeCheck=true` in GMavenPlus is a workaround until Groovy adds official Java 25 bytecode support.
- Groovy test classes are compiled to Java 21 bytecode (`targetBytecode=21`) while the main application targets Java 25. This is intentional: Spring's internal ASM classfile parser is used during component scanning of `target/test-classes`, and Spring 6.2.7's ASM tops out at Java 25 (V25 present but may have edge cases). Monitor for issues when upgrading Spring Boot further.
