# System Overview

## Purpose and Scope
`download-server` is a single-process Spring Boot web application exposing a read-only HTTP download API under `/download`. Its design goal is HTTP correctness: conditional requests, strong ETags, `Last-Modified`, `HEAD` parity with `GET`, content-type detection, and server-side bandwidth throttling.

## Layered View
```
HTTP client
    │
    ▼
Spring MVC DispatcherServlet (embedded Tomcat, from spring-boot-starter-web)
    │
    ▼
DownloadController          — request mapping, header binding, 404 fallback
    │
    ▼
FileStorage (interface)     — UUID → Optional<FilePointer>
    ├── FileSystemStorage   (@Profile("!test"))
    └── FileStorageStub     (@Profile("test"), test sources)
    │
    ▼
FilePointer (interface)     — metadata + stream supplier
    └── FileSystemPointer   — java.io.File backed, SHA-512 ETag, probed media type
    │
    ▼
ExistingFile                — caching decision + ResponseEntity construction
    │
    ▼
ThrottlingInputStream       — Guava RateLimiter decorator over the file stream
```

## Key Architectural Decisions
1. **Storage is abstracted behind two narrow interfaces** (`FileStorage` for lookup, `FilePointer` for metadata/content). The controller never touches the filesystem, so a different backend (object store, database blob) is a drop-in replacement.
2. **Profile-based implementation selection.** `FileSystemStorage` is annotated `@Profile("!test")`; the test-scope `FileStorageStub` is `@Profile("test")`. No conditional wiring code is required.
3. **Caching logic is a per-request value object.** `ExistingFile` holds `(HttpMethod, FilePointer, UUID)` and exposes `redirect(...)` and `handle(...)`; the cached/not-cached branch is expressed by passing a `Function<FilePointer, ResponseEntity<Resource>>` to `serveWithCaching`.
4. **Strong ETags from content hashing.** `FileSystemPointer` computes `Hashing.sha512()` over the file in its constructor and quotes it as the ETag, rather than relying on size/mtime heuristics.
5. **Streaming with backpressure.** Responses carry an `InputStreamResource` wrapping `ThrottlingInputStream`, so the file is never buffered in memory, and read rate is capped via a Guava `RateLimiter`.
6. **Redirect-to-named-URL.** `GET /download/{uuid}` answers `301 Moved Permanently` to `/download/{uuid}/{originalName}` so browsers save a meaningful filename.
7. **No persistence, no messaging, no external integrations.** The only I/O is the local filesystem.

## Deployment Model
- Build: Maven (`mvnw`), Spring Boot repackaging inherited from the parent POM; artifact `target/download-server-0.0.1-SNAPSHOT.jar`.
- Run: `java -jar download-server-0.0.1-SNAPSHOT.jar` — embedded Tomcat, default port 8080.
- Operations: Spring Boot Actuator 1.x endpoints (`/health`, `/info`, `/env`, `/metrics`, …) are on the application port with no security configuration present.
- No Dockerfile, Kubernetes manifest, CloudFormation/SAM template, Terraform, or CI workflow exists in the repository.

## Cross-references
- [components.md](components.md) · [dependencies.md](dependencies.md) · [patterns.md](patterns.md)
- [../diagrams/architecture.md](../diagrams/architecture.md)
- [../technical-debt-report.md](../technical-debt-report.md)
