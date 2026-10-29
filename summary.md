# Java 25 Migration – Summary

## Status: ✅ BUILD SUCCESS — 12/12 tests passing

## Changes Made

### 1. `src/test/java/org/springframework/web/filter/Sha512ShallowEtagHeaderFilter.java`
**Problem:** `generateETagHeaderValue(byte[] bytes)` overrode a method that no longer exists in Spring Framework 6.x (Spring Boot 3.x). The Spring 6 `ShallowEtagHeaderFilter` changed the signature to `generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException`.

**Fix:** Updated the override to match the new Spring 6 signature, reading the InputStream via Guava's `ByteStreams.toByteArray()`.

---

### 2. `pom.xml`
Multiple issues addressed:

| Change | Reason |
|--------|--------|
| Removed stale `<spring.version>4.1.1.RELEASE</spring.version>` property | Unused leftover; Spring Boot 3.5.16 manages Spring Framework 6.x versions via its BOM |
| Updated `dependencyManagement` Groovy from `org.codehaus.groovy:groovy-all:2.4.16` → `org.apache.groovy:groovy:4.0.32` | Groovy 4.x changed groupId; Groovy 2.4 is incompatible with Java 21+ class file format |
| Updated `spock-spring` from `1.0-groovy-2.4` → `2.4-groovy-4.0` | Spock 1.x requires Groovy 2.x and JUnit 4; incompatible with Java 25 and Spring Boot 3 |
| Updated `spock-core` from `1.0-groovy-2.4` → `2.4-groovy-4.0` | Same as above; Spock 2.x uses JUnit 5 Platform |
| Updated `commons-io` from `2.4` → `2.18.0` | Java 17+ compatibility (minimum 2.11.0 per knowledge base) |
| Removed `cglib-nodep:3.1` | Spock 2.x uses ByteBuddy instead of cglib; old cglib incompatible with Java 25 |
| Replaced `org.codehaus.groovy:groovy-all` runtime dep with `org.apache.groovy:groovy` (test scope) | New groupId/artifactId for Groovy 4.x |
| Added `org.codehaus.gmavenplus:gmavenplus-plugin:3.0.2` | Required to compile Groovy test sources — was missing entirely, causing Groovy specs to never be compiled or run |
| Added `<includes>` to Surefire for `**/*Spec.class` | Surefire's default pattern `**/*Test` misses `DownloadControllerSpec`; required to run Spock specs via JUnit 5 Platform |

---

### 3. `src/test/java/com/nurkiewicz/download/FileExamples.java`
**Problem:** `TXT_FILE_UUID = UUID.randomUUID()` produced a different UUID when the class was initialized twice — once in the test classloader and once during Spring component scanning (triggered by `FileStorageStub` referencing `FileExamples` in its static import, while `FileExamples.txtFile()` references `FileStorageStub.class`). The stub and the test therefore had different UUIDs, causing `FileStorageStub.findFile()` to never match the request UUID → HTTP 404 for all expected-200 tests.

**Fix:** Replaced `UUID.randomUUID()` with deterministic `UUID.fromString(...)` constants. This is safe — the UUIDs just need to be unique within the test, not random.

---

### 4. `src/main/resources/logback.xml`
**Problem:** `<appender>` element was nested inside `<root>` instead of being a top-level element, causing logback to log `Missing attribute [name]` and `AppenderRefModelHandler` errors on every test run.

**Fix:** Moved `<appender name="CONSOLE" ...>` to the top level of `<configuration>`, keeping `<appender-ref ref="CONSOLE"/>` inside `<root>`.

---

## Build Result

```
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Next Steps

- Guava 29.0-jre uses `sun.misc.Unsafe.objectFieldOffset` which is flagged as "terminally deprecated" on Java 25. It still works but will be blocked in a future release. Consider upgrading to Guava 33.4.0-jre.
- `commons-io` updated to 2.18.0; `commons-codec` version is managed by Spring Boot BOM — both are compatible.
- The `spring.version` property was removed; Spring Framework is now fully managed by Spring Boot 3.5.16 (Spring Framework 6.2.x).
- The jansi native access warning (`java.lang.System::load`) is from the Maven wrapper's jansi library; harmless but can be suppressed by adding `--enable-native-access=ALL-UNNAMED` to `MAVEN_OPTS`.
