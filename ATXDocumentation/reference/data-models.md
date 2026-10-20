# Data Models

The application has no persistent entities, no ORM, and no serialized DTOs. "Data model" here means the in-memory types that carry request and file state.

## Domain Types

### `FilePointer` (interface) — file descriptor abstraction
| Attribute (via accessor) | Type | Meaning |
|---|---|---|
| `size` | `long` | content length in bytes |
| `originalName` | `String` | filename for the redirect target |
| `etag` | `String` | quoted strong entity tag |
| `mediaType` | `Optional<com.google.common.net.MediaType>` | detected content type |
| `lastModified` | `Instant` | modification timestamp |
| content | `InputStream` via `open()` | file bytes |

### `FileSystemPointer` — concrete state
| Field | Type | Derivation |
|---|---|---|
| `target` | `java.io.File` | constructor argument |
| `tag` | `com.google.common.hash.HashCode` | `Files.hash(target, Hashing.sha512())` |
| `mediaTypeOrNull` | `com.google.common.net.MediaType` (nullable) | `Files.probeContentType(target.toPath())`, parsed; `null` when probing yields nothing |
All fields `final`; the instance is immutable, though `getSize()`/`getLastModified()` read live filesystem metadata and can therefore disagree with the eagerly computed hash if the file changes underneath.

### `ExistingFile` — per-request value object
| Field | Type | Source |
|---|---|---|
| `method` | `org.springframework.http.HttpMethod` | resolved by Spring from the request |
| `filePointer` | `FilePointer` | from `FileStorage.findFile` |
| `uuid` | `java.util.UUID` | path variable |

### `ThrottlingInputStream` — stream state
| Field | Type | Role |
|---|---|---|
| `target` | `InputStream` | delegate |
| `maxBytesPerSecond` | `com.google.common.util.concurrent.RateLimiter` | permit source, 1 permit = 1 byte |

## Request/Response Model
| Element | Type | Binding |
|---|---|---|
| `{uuid}` path variable | `java.util.UUID` | `@PathVariable`; Spring converts the string, rejecting malformed values |
| `{filename}` path variable | not bound | present in the mapping only, never read |
| `If-None-Match` | `Optional<String>` | `@RequestHeader(IF_NONE_MATCH)` |
| `If-Modified-Since` | `Optional<Date>` | `@RequestHeader(IF_MODIFIED_SINCE)`, RFC 1123 parsed by Spring into `java.util.Date`, converted via `Date::toInstant` |
| response | `ResponseEntity<org.springframework.core.io.Resource>` | body is an `InputStreamResource` for GET, `null` for HEAD/304/301 |

## Type Conversions
```
String (header)  ──Spring──▶ java.util.Date ──toInstant()──▶ java.time.Instant ──▶ FilePointer.modifiedAfter
long (millis)    ──Instant.ofEpochMilli──▶ Instant ──toEpochMilli()──▶ Last-Modified header
HashCode         ──toString + quoting──▶ ETag header
com.google.common.net.MediaType ──ExistingFile.toMediaType──▶ org.springframework.http.MediaType
    (charset propagated when present: new MediaType(type, subtype, charset))
InputStream      ──ThrottlingInputStream──▶ InputStreamResource ──▶ response body
```

## Test Fixtures
| Constant | Type | Value |
|---|---|---|
| `FileExamples.TXT_FILE_UUID` | `UUID` | `UUID.randomUUID()` at class-init |
| `FileExamples.NOT_FOUND_UUID` | `UUID` | `UUID.randomUUID()` at class-init |
| `FileExamples.TXT_FILE` | `FilePointer` | `FileSystemPointer` over classpath `/download.txt` (content `foobar`) |

## Relationships
```
FileStorage  1 ──── 0..1 ▶ FilePointer      (lookup by UUID)
ExistingFile 1 ──── 1    ▶ FilePointer      (composition, per request)
ExistingFile 1 ──── 1    ▶ ThrottlingInputStream (created for GET only)
ThrottlingInputStream 1 ─ 1 ▶ RateLimiter   (one limiter per request, 1 MB/s)
```

Related: [interfaces.md](interfaces.md) · [program-structure.md](program-structure.md)
