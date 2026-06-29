# Java 25 Migration Summary

## Status: BUILD SUCCESS — 12/12 tests passing

## Changes Made

### 1. pom.xml — Groovy/Spock upgrade
- Replaced `org.codehaus.groovy:groovy-all:2.4.16` with `org.apache.groovy:groovy:4.0.32` (new groupId for Groovy 4, test scope only)
- Upgraded `spock-core` and `spock-spring` from `1.0-groovy-2.4` to `2.4-groovy-4.0` (required for JUnit 5 / JUnit Platform support)
- Added `gmavenplus-plugin:4.0.1` (`addTestSources` + `compileTests` goals) so Groovy test sources in `src/test/groovy` are compiled
- Updated Surefire `<includes>` to `**/*Test` and `**/*Spec` so Spock `*Spec` classes are discovered and executed

### 2. Sha512ShallowEtagHeaderFilter.java
- Updated `generateETagHeaderValue` override from the removed Spring 5 `byte[]` signature to the Spring 6 `InputStream + boolean` signature
- Reads all bytes from the `InputStream` using Guava `ByteStreams.toByteArray`, then computes SHA-512 hash

### 3. FileExamples.java
- Changed `TXT_FILE_UUID` and `NOT_FOUND_UUID` from `UUID.randomUUID()` to fixed `UUID.fromString(...)` constants
- **Root cause**: Spock 2 + Spring's test context uses classloader isolation; `UUID.randomUUID()` produced different values in the Spring bean classloader vs the test classloader, causing all `TXT_FILE_UUID` lookups in `FileStorageStub` to miss. Fixed UUIDs compare by value (not identity) so they match across classloaders.

## Build Verification

```
./mvnw clean test
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Next Steps / Notes

- The `logback.xml` file has a missing `name` attribute on an `<appender>` element (non-fatal at test time, logged as ERROR by Logback). This is unrelated to the Java 25 migration.
- `commons-io:2.4` and `guava:29.0-jre` are older versions; consider upgrading in a subsequent cycle for security patches.
- `cglib-nodep:3.1` is obsolete; Spring 6 / Spring Boot 3 uses its own CGLIB fork. This dependency can be removed.
- The `spring.version=4.1.1.RELEASE` property in pom.xml is overridden by the Spring Boot parent BOM (6.2.19 is the effective version). The property is unused and can be removed.
