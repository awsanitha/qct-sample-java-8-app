# API Reference

## HTTP Endpoints
All endpoints are declared on `DownloadController` (`@RestController`, class-level `@RequestMapping("/download")`). No `server.servlet.context-path` is configured, so paths are absolute.

### 1. `GET|HEAD /download/{uuid}`
- Source: `DownloadController.redirect`, `src/main/java/com/nurkiewicz/download/DownloadController.java:33-43`
- Purpose: redirect to the canonical, filename-bearing download URL.

| Parameter | Location | Type | Required |
|---|---|---|---|
| `uuid` | path | `UUID` | yes |
| `If-None-Match` | header | String (quoted ETag) | no |
| `If-Modified-Since` | header | RFC 1123 date | no |

| Response | Condition | Headers | Body |
|---|---|---|---|
| `301 Moved Permanently` | file found, not cached | `Location: /download/{uuid}/{originalName}` | none |
| `304 Not Modified` | ETag matches or client copy current | `ETag`, `Content-Length`, `Last-Modified`, optional `Content-Type` | none |
| `404 Not Found` | unknown UUID | — | empty |
| `400 Bad Request` | `{uuid}` not a valid UUID (Spring conversion) | — | framework error |

Example:
```
GET /download/4b0c2a8e-9f2d-4e8a-bb5f-3c1a9d0e7f11 HTTP/1.1

HTTP/1.1 301 Moved Permanently
Location: /download/4b0c2a8e-9f2d-4e8a-bb5f-3c1a9d0e7f11/logback.xml
```

### 2. `GET|HEAD /download/{uuid}/{filename}`
- Source: `DownloadController.download`, lines 45-55
- Purpose: serve file content with HTTP cache validation and bandwidth throttling.
- `{filename}` is accepted but never read; any value resolves to the same file.

| Parameter | Location | Type | Required |
|---|---|---|---|
| `uuid` | path | `UUID` | yes |
| `filename` | path | String (ignored) | yes (syntactically) |
| `If-None-Match` | header | String (quoted ETag) | no |
| `If-Modified-Since` | header | RFC 1123 date | no |

| Response | Condition | Headers | Body |
|---|---|---|---|
| `200 OK` | GET, not cached | `ETag`, `Content-Length`, `Last-Modified`, `Content-Type` (when detected) | file bytes, throttled to 1 MB/s |
| `200 OK` | HEAD, not cached | same headers | empty |
| `304 Not Modified` | ETag matches or client copy current | `ETag`, `Content-Length`, `Last-Modified` | none |
| `404 Not Found` | unknown UUID | — | empty |

Examples:
```
GET /download/<uuid>/file.txt HTTP/1.1

HTTP/1.1 200 OK
ETag: "ddaf35a193617aba...(sha512)"
Content-Length: 6
Last-Modified: Tue, 15 Oct 2024 10:02:11 GMT
Content-Type: text/plain

foobar
```
```
GET /download/<uuid>/file.txt HTTP/1.1
If-None-Match: "ddaf35a193617aba...(sha512)"

HTTP/1.1 304 Not Modified
ETag: "ddaf35a193617aba...(sha512)"
```
```
HEAD /download/<uuid>/file.txt HTTP/1.1

HTTP/1.1 200 OK
Content-Length: 6
Content-Type: text/plain
(no body)
```

### 3. Spring Boot Actuator endpoints (dependency-provided, not declared in code)
`spring-boot-starter-actuator` 1.2.3 contributes the Boot 1.x endpoint set on the application port — `/health`, `/info`, `/metrics`, `/env`, `/beans`, `/mappings`, `/configprops`, `/trace`, `/dump`, `/autoconfig`, `/shutdown` (disabled by default). No `application.properties`/`application.yml` and no Spring Security configuration exist in the repository, so these are served with framework defaults.

## Public Java API (callable surface)

### `DownloadController`
| Method | Signature |
|---|---|
| ctor | `DownloadController(FileStorage storage)` |
| `redirect` | `ResponseEntity<Resource> redirect(HttpMethod, UUID, Optional<String>, Optional<Date>)` |
| `download` | `ResponseEntity<Resource> download(HttpMethod, UUID, Optional<String>, Optional<Date>)` |

### `ExistingFile`
| Method | Signature |
|---|---|
| ctor | `ExistingFile(HttpMethod method, FilePointer filePointer, UUID uuid)` |
| `redirect` | `ResponseEntity<Resource> redirect(Optional<String> requestEtagOpt, Optional<Date> ifModifiedSinceOpt)` |
| `handle` | `ResponseEntity<Resource> handle(Optional<String> requestEtagOpt, Optional<Date> ifModifiedSinceOpt)` |

### `FileStorage` / `FileSystemStorage`
| Method | Signature |
|---|---|
| `findFile` | `Optional<FilePointer> findFile(UUID uuid)` |

### `FilePointer` / `FileSystemPointer`
| Method | Signature |
|---|---|
| `open` | `InputStream open()` |
| `getSize` | `long getSize()` |
| `getOriginalName` | `String getOriginalName()` |
| `getEtag` | `String getEtag()` |
| `getMediaType` | `Optional<com.google.common.net.MediaType> getMediaType()` |
| `matchesEtag` | `boolean matchesEtag(String requestEtag)` |
| `getLastModified` | `Instant getLastModified()` |
| `modifiedAfter` | `boolean modifiedAfter(Instant clientTime)` |
| ctor | `FileSystemPointer(File target)` |

### `ThrottlingInputStream`
| Method | Signature |
|---|---|
| ctor | `ThrottlingInputStream(InputStream target, RateLimiter maxBytesPerSecond)` |
| reads | `int read()`, `int read(byte[])`, `int read(byte[], int, int)` |
| other | `long skip(long)`, `int available()`, `void mark(int)`, `void reset()`, `boolean markSupported()`, `void close()` |

### `MainApplication`
| Method | Signature |
|---|---|
| `main` | `public static void main(String[] args)` |

## Declarative (non-code) service definitions
None. A scan for XML/YAML/IDL service declarations (Spring bean XML, `web.xml`, WSDL, OpenAPI, GraphQL, `.proto`, MyBatis mappers, Camel routes, CloudFormation/SAM, Kubernetes) found no such files; the only non-code configuration is `src/main/resources/logback.xml`, which declares logging, not services. See [../architecture/dependencies.md](../architecture/dependencies.md).

Related: [../diagrams/behavioral.md](../diagrams/behavioral.md) · [interfaces.md](interfaces.md)
