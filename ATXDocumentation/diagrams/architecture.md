# Architecture Diagrams

## System Context
```
                    ┌──────────────────────────────┐
                    │  End user / HTTP client      │
                    │  (browser, curl, download    │
                    │   manager — supports         │
                    │   conditional GET)           │
                    └──────────────┬───────────────┘
                                   │ HTTP/1.1 (GET, HEAD)
                                   ▼
          ╔════════════════════════════════════════════════════╗
          ║  SYSTEM BOUNDARY: download-server                  ║
          ║  Spring Boot 1.2.3 app, embedded Tomcat, Java 8    ║
          ║                                                    ║
          ║   /download/{uuid}            → 301 redirect       ║
          ║   /download/{uuid}/{filename} → 200/304 content    ║
          ║   Actuator endpoints          → operational data   ║
          ╚═══════════════════════╦════════════════════════════╝
                                  │ java.io / java.nio file read
                                  ▼
                    ┌──────────────────────────────┐
                    │ Local filesystem / classpath │
                    │ (only external resource)     │
                    └──────────────────────────────┘

External systems NOT present: database, object store, cache, message broker,
identity provider, downstream HTTP services, AWS services.
```

## Integration Patterns
| Integration | Direction | Mechanism | Notes |
|---|---|---|---|
| HTTP clients | inbound | Spring MVC `@RestController`, synchronous request/response | Streaming body via `InputStreamResource` |
| Filesystem | outbound | `java.io.FileInputStream`, `java.nio.file.Files.probeContentType`, Guava `Files.hash` | Synchronous, blocking |
| Operations tooling | inbound | Spring Boot Actuator HTTP endpoints | No code in this repo configures or secures them |
| Logging sink | outbound | Logback `ConsoleAppender` (stdout) | `logback.xml`; no file/remote appender |

Integration style is strictly request/response; there is no asynchronous, event-driven, or batch integration.

## Service Map and Communication Flows
```
   client ──HTTP──▶ [DispatcherServlet] ──▶ [DownloadController] ──in-process──▶ [FileStorage adapter]
                                                      │                                   │
                                                      └──in-process──▶ [ExistingFile] ──▶ [FilePointer]
                                                                              │
                                                                              ▼
                                                                    [ThrottlingInputStream] ──▶ response body
```
All internal communication is in-process method invocation inside a single deployable unit — there are no network hops between components and therefore no retry, circuit-breaker, or service-discovery concerns.

## Security Boundaries and Access Patterns
```
┌───────────────── UNTRUSTED ─────────────────┐
│ HTTP request: path variables {uuid},        │
│ {filename}; headers If-None-Match,          │
│ If-Modified-Since                           │
└──────────────────────┬──────────────────────┘
                       │  Spring type conversion: {uuid} → java.util.UUID
                       │  (malformed UUID rejected before controller code)
                       ▼
┌─────────────────── TRUSTED (in-JVM) ────────────────────┐
│ DownloadController → FileStorage → FilePointer          │
│ No authentication, no authorization, no rate limiting   │
│ per principal. Bandwidth throttle is per request only.  │
└──────────────────────┬──────────────────────────────────┘
                       │ file path chosen by the storage adapter,
                       │ never by the client ⇒ no path traversal
                       ▼
┌──────────────── Filesystem (trusted) ───────────────────┐
│ Stub adapter always resolves classpath /logback.xml     │
└─────────────────────────────────────────────────────────┘

Unprotected surfaces:
 • Every download route is anonymous — any party holding a UUID can fetch the file.
 • Spring Boot 1.x Actuator endpoints are reachable on the application port with no
   security configuration present in the repository.
 • No TLS configuration — transport security must be terminated upstream.
```
UUIDs act as bearer capabilities ("security by obscurity of the identifier"). That is acceptable only if UUIDs are unguessable, which holds for the random v4 UUIDs used in tests but is not enforced anywhere in production code.

Related: [../analysis/security-patterns.md](../analysis/security-patterns.md) · [system context details](../architecture/system-overview.md)
