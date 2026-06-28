# Java 25 Migration Summary

## Status: BUILD PASSING

## Changes Made

### Fixed: `Sha512ShallowEtagHeaderFilter.java`
- **File**: `src/test/java/org/springframework/web/filter/Sha512ShallowEtagHeaderFilter.java`
- **Issue**: Spring 6 (used by Spring Boot 3.x) changed `ShallowEtagHeaderFilter.generateETagHeaderValue` signature from `(byte[] bytes)` to `(InputStream inputStream, boolean isWeak) throws IOException`
- **Fix**: Updated the `@Override` method to match the new Spring 6 signature, reading bytes from the `InputStream` via `readAllBytes()`

## Next Steps / Notes

- **Groovy/Spock tests not running**: The Spock tests in `src/test/groovy` are not compiled or executed because there is no Groovy Maven plugin (e.g., `gmavenplus-plugin`) configured in `pom.xml`. Spock 1.0 with Groovy 2.4 is also incompatible with Java 25. To restore test coverage, upgrade Spock to 2.x, Groovy to 4.x, and add `gmavenplus-plugin` to the build.
- **`spring.version` property**: `pom.xml` declares `<spring.version>4.1.1.RELEASE</spring.version>` which appears to be a leftover from the original project. Spring Boot 3.x uses `spring-framework.version` as its override property, so this property has no effect. It can be safely removed.
- **Deprecated API warning**: `FileSystemPointer.java` uses a deprecated API (likely `Files.hash` or `Hashing.sha512()` from Guava). This is a warning only and does not block the build.
- **`cglib-nodep 3.1`**: Old CGLIB dependency may cause issues at runtime with Java 25. Spring Boot 3.x uses its own proxy mechanism and this dependency may be unnecessary.
