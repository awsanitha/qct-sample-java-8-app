# Components

Each entry lists responsibility, collaborators, and source location.

## MainApplication
- Source: `src/main/java/com/nurkiewicz/download/MainApplication.java` (package-private class, lines 1-13)
- Responsibility: Spring Boot bootstrap. `@SpringBootApplication` enables auto-configuration and component scanning of `com.nurkiewicz.download`.
- Public surface: `public static void main(String[] args)`.
- Note: line 10 creates an unused `Integer temp = new Integer("1234")` — dead code (see [../technical-debt-report.md](../technical-debt-report.md) L1).
- Also serves as the `@ContextConfiguration` class for the Spock specification.

## DownloadController
- Source: `src/main/java/com/nurkiewicz/download/DownloadController.java`
- Responsibility: HTTP entry point, mapped at class level to `/download` via `@RestController` + `@RequestMapping`.
- Collaborators: constructor-injected `FileStorage` (`@Autowired`, line 28-31); creates `ExistingFile` instances.
- Handlers:
  - `redirect(HttpMethod, UUID, Optional<String>, Optional<Date>)` — `GET|HEAD /download/{uuid}` (line 33)
  - `download(HttpMethod, UUID, Optional<String>, Optional<Date>)` — `GET|HEAD /download/{uuid}/{filename}` (line 45)
  - private `findExistingFile(HttpMethod, UUID)` → `Optional<ExistingFile>` (line 57)
- Behavior: missing file yields `new ResponseEntity<>(NOT_FOUND)`; conditional headers `If-None-Match` and `If-Modified-Since` are bound as `Optional`.

## ExistingFile
- Source: `src/main/java/com/nurkiewicz/download/ExistingFile.java`
- Responsibility: all response-shaping and cache-validation logic for one request against one file.
- State: `HttpMethod method`, `FilePointer filePointer`, `UUID uuid` (immutable).
- Public methods: `redirect(Optional<String>, Optional<Date>)`, `handle(Optional<String>, Optional<Date>)`.
- Private methods: `serveWithCaching` (strategy dispatch), `cached` (ETag OR modified-since check), `redirectDownload` (301 + `Location`), `serveDownload` (200 + body), `resourceToReturn` (GET→body, HEAD→null), `buildResource` (throttled stream), `notModified` (304), `response` (common header builder), `toMediaType` (Guava→Spring `MediaType`).
- Collaborators: `FilePointer`, `ThrottlingInputStream`, Guava `RateLimiter`, `InputStreamResource`.

## FilePointer (interface)
- Source: `src/main/java/com/nurkiewicz/download/FilePointer.java`
- Contract: `open()`, `getSize()`, `getOriginalName()`, `getEtag()`, `getMediaType()`, `matchesEtag(String)`, `getLastModified()`, `modifiedAfter(Instant)`.
- Implementations: `FileSystemPointer` (production), used directly as a test fixture in `FileExamples`.

## FileSystemPointer
- Source: `src/main/java/com/nurkiewicz/download/FileSystemPointer.java`
- Responsibility: `java.io.File`-backed `FilePointer`.
- Constructor work: SHA-512 hash of the whole file via `Files.hash`, media type via `java.nio.file.Files.probeContentType`; `IOException` is rethrown as `IllegalArgumentException`.
- `open()` returns `BufferedInputStream(FileInputStream(target))`.
- `getEtag()` returns the hash wrapped in double quotes (strong ETag).
- `toString()` uses Guava `MoreObjects.toStringHelper`.

## FileStorage (interface)
- Source: `src/main/java/com/nurkiewicz/download/FileStorage.java`
- Contract: `Optional<FilePointer> findFile(UUID uuid)`.

## FileSystemStorage
- Source: `src/main/java/com/nurkiewicz/download/FileSystemStorage.java`
- `@Component @Profile("!test")`. Documented stub: returns a `FileSystemPointer` over the classpath resource `/logback.xml` for *any* UUID. Intended to be replaced by a real backend.

## ThrottlingInputStream
- Source: `src/main/java/com/nurkiewicz/download/ThrottlingInputStream.java`
- Responsibility: decorator over an `InputStream` that acquires N permits from a Guava `RateLimiter` before each read of N bytes. All other `InputStream` methods delegate unchanged.

## Test-scope components
| Component | Role |
|---|---|
| `FileStorageStub` (`@Component @Profile("test")`) | Returns `FileExamples.TXT_FILE` for `TXT_FILE_UUID`, `Optional.empty()` otherwise. |
| `FileExamples` | Static fixtures: `TXT_FILE_UUID`, `NOT_FOUND_UUID` (random UUIDs) and `TXT_FILE`, a `FileSystemPointer` over classpath `/download.txt`. |
| `Sha512ShallowEtagHeaderFilter` | Subclass of Spring's `ShallowEtagHeaderFilter` overriding `generateETagHeaderValue` with SHA-512. Declared in the `org.springframework.web.filter` package to access the protected method; currently unreferenced. |
| `DownloadControllerSpec` | Spock specification driving `MockMvc` against the full context (`@WebAppConfiguration`, `@ContextConfiguration(classes=[MainApplication])`, `@ActiveProfiles("test")`). 11 feature methods. |

## Interaction Summary
`DownloadController` → `FileStorage.findFile` → `ExistingFile` → (`FilePointer` metadata + `ThrottlingInputStream`) → `ResponseEntity<Resource>`.

See [../diagrams/structural.md](../diagrams/structural.md) and [../diagrams/behavioral.md](../diagrams/behavioral.md).
