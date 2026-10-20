# Behavioral Diagrams

Scope: core application business flows only (HTTP download and caching). Build, test, and packaging flows are intentionally excluded.

## Sequence — `GET /download/{uuid}/{filename}` (cache miss, full download)
```
Client      DispatcherServlet   DownloadController   FileStorage   FilePointer   ExistingFile   ThrottlingInputStream
  │                 │                   │                 │            │             │                 │
  │ GET /download/{uuid}/file.txt       │                 │            │             │                 │
  ├────────────────▶│                   │                 │            │             │                 │
  │                 │ download(method, uuid, etagOpt, sinceOpt)        │             │                 │
  │                 ├──────────────────▶│                 │            │             │                 │
  │                 │                   │ findFile(uuid)  │            │             │                 │
  │                 │                   ├────────────────▶│            │             │                 │
  │                 │                   │◀─ Optional.of(pointer) ──────┤             │                 │
  │                 │                   │ new ExistingFile(method, pointer, uuid)    │                 │
  │                 │                   ├───────────────────────────────────────────▶│                 │
  │                 │                   │ handle(etagOpt, sinceOpt)                  │                 │
  │                 │                   ├───────────────────────────────────────────▶│                 │
  │                 │                   │                 │  matchesEtag / modifiedAfter → false       │
  │                 │                   │                 │◀───────────┤             │                 │
  │                 │                   │                 │  open()    │             │                 │
  │                 │                   │                 │◀───────────┤             │                 │
  │                 │                   │                 │     new ThrottlingInputStream(in, RateLimiter.create(ONE_MB))
  │                 │                   │                 │            │             ├────────────────▶│
  │                 │                   │◀── ResponseEntity(200, ETag, Content-Length, Last-Modified,  │
  │                 │                   │     Content-Type, InputStreamResource) ────┤                 │
  │◀── 200 OK + throttled body ─────────┤                 │            │             │                 │
  │                 │                   │                 │            │   read(b,off,len) → acquire(len) → target.read
```

## Sequence — conditional request returning 304
```
Client                 DownloadController        ExistingFile            FilePointer
  │ GET ... If-None-Match: "<sha512>"  │                 │                    │
  ├──────────────────────────────────▶ │ handle(etag, -) │                    │
  │                                    ├───────────────▶ │ cached(...)        │
  │                                    │                 ├─ matchesEtag(etag)▶│
  │                                    │                 │◀── true ───────────┤
  │                                    │                 │ notModified(pointer)
  │◀── 304 Not Modified (ETag, Content-Length, Last-Modified, no body) ───────┤
```

## Sequence — `GET /download/{uuid}` (redirect)
```
Client                 DownloadController        ExistingFile
  │ GET /download/{uuid}               │                 │
  ├──────────────────────────────────▶ │ redirect(...)   │
  │                                    ├───────────────▶ │ redirectDownload(pointer)
  │                                    │                 │  Location = "/download/" + uuid + "/" + getOriginalName()
  │◀── 301 Moved Permanently + Location ──────────────────┤
  │ GET /download/{uuid}/{originalName}│                 │   (client follows → full download flow)
```

## Sequence — unknown UUID
```
Client → DownloadController.download → FileStorage.findFile(uuid) → Optional.empty()
       → orElseGet(() -> new ResponseEntity<>(NOT_FOUND))
Client ◀── 404 Not Found, empty body
```

## Activity — response selection (business rules)
```
            ┌─────────────────────────────┐
            │ request (method, uuid,      │
            │ If-None-Match, If-Modified-Since)
            └──────────────┬──────────────┘
                           ▼
                 storage.findFile(uuid)
                           │
            ┌──────────────┴──────────────┐
         empty                          present
            │                              │
            ▼                              ▼
       ┌─────────┐                cached = matchesEtag(ifNoneMatch)
       │ 404     │                      OR modifiedAfter(ifModifiedSince)
       └─────────┘                         │
                            ┌──────────────┴───────────────┐
                        cached = true                 cached = false
                            │                               │
                            ▼                               ▼
                     ┌─────────────┐          path has {filename}?
                     │ 304, no body│            ┌───────────┴───────────┐
                     │ + ETag,     │           no                      yes
                     │ Content-Len,│            │                       │
                     │ Last-Mod    │            ▼                       ▼
                     └─────────────┘     ┌──────────────┐    method == GET ?
                                         │ 301 + Location│      ┌────────┴────────┐
                                         └──────────────┘     GET              HEAD
                                                                │                 │
                                                                ▼                 ▼
                                                   ┌──────────────────┐  ┌──────────────────┐
                                                   │ 200 + throttled  │  │ 200, null body   │
                                                   │ InputStreamResource│ │ (headers only)  │
                                                   └──────────────────┘  └──────────────────┘
```

## Business Rules Extracted
| Rule | Source |
|---|---|
| A file is addressed by UUID; the filename segment is cosmetic and is not validated against the stored name | `DownloadController:45-55` |
| Both `GET` and `HEAD` are accepted on both routes | `DownloadController:33,45` |
| ETag is the quoted SHA-512 of the file content (strong validator) | `FileSystemPointer:22,52-54` |
| A request is considered cached if the ETag matches OR the client timestamp is not older than the file's last-modified | `ExistingFile:54-63`, `FileSystemPointer:83-85` |
| Absent conditional headers default to "not cached" (`orElse(false)`) | `ExistingFile:57,61` |
| `HEAD` responses carry full headers but a `null` body | `ExistingFile:83-88` |
| Download throughput is capped at 1 MB/s per request | `ExistingFile:92` |
| Unknown UUID yields 404 with no body | `DownloadController:42,54` |
| Media type is included only when `probeContentType` succeeds | `FileSystemPointer:23-26`, `ExistingFile:108-111` |

## State / Lifecycle
```
ExistingFile lifecycle (per request, immutable):
  constructed(method, pointer, uuid) ──▶ redirect() | handle() ──▶ discarded

Response stream lifecycle:
  FilePointer.open() ──▶ BufferedInputStream ──▶ ThrottlingInputStream
      ──▶ Spring writes body (read → RateLimiter.acquire) ──▶ close()
```
No explicit state machine or entity lifecycle exists; the application holds no mutable state between requests.

## Data Flow
```
UUID (path)  ──▶ FileStorage ──▶ FilePointer ──┬──▶ metadata (size, etag, lastModified, mediaType) ──▶ HTTP headers
                                               └──▶ InputStream ──▶ ThrottlingInputStream ──▶ HTTP body
If-None-Match / If-Modified-Since (headers) ──▶ ExistingFile.cached(...) ──▶ status selection (200/304)
```

## Exception Handling and Error Recovery
| Condition | Handling |
|---|---|
| `IOException` while hashing or probing content type | wrapped in `IllegalArgumentException` (`FileSystemPointer:27-29`) |
| `FileNotFoundException` in `open()` | wrapped in `IllegalArgumentException` (`FileSystemPointer:36-38`) |
| `URISyntaxException` building the redirect `Location` | wrapped in `IllegalArgumentException` (`ExistingFile:72-74`) |
| File not found by storage | explicit 404 response, no exception |
| Everything else | no `@ExceptionHandler` / `@ControllerAdvice`; falls through to Spring Boot's default error handling, so wrapped `IllegalArgumentException`s surface as 500 |

Related: [structural.md](structural.md) · [../reference/api-reference.md](../reference/api-reference.md)
