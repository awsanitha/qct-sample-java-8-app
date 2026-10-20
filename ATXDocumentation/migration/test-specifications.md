# Test Specifications

Specifications that any migration or reimplementation must satisfy. The first table is the existing suite — it is the authoritative behavioral contract and must be preserved (assertions may be adapted for framework compatibility; no test may be removed or disabled).

## Existing Specifications
Source: `src/test/groovy/com/nurkiewicz/download/DownloadControllerTest.groovy` (class `DownloadControllerSpec`), 11 feature methods driving `MockMvc` against the full context with `@ActiveProfiles("test")`. `TEXT_FILE = '/download/' + FileExamples.TXT_FILE_UUID + '/file.txt'`; the fixture file `src/test/resources/download.txt` contains `foobar`.

| # | Specification | Request | Expected |
|---|---|---|---|
| T1 | should return bytes of existing file | `GET {TEXT_FILE}` | 200, body `foobar` |
| T2 | should return 404 and no content | `GET /download/{NOT_FOUND_UUID}/file.txt` | 404, zero-length body |
| T3 | should send file if ETag not present | `GET {TEXT_FILE}` | 200 |
| T4 | should send file if ETag present but not matching | `GET {TEXT_FILE}` + `If-None-Match: "WHATEVER"` | 200 |
| T5 | should not send file if ETag matches content | `GET {TEXT_FILE}` + `If-None-Match: <TXT_FILE.etag>` | 304, `ETag` header equals the sent value |
| T6 | should not return file if wasn't modified recently | `GET {TEXT_FILE}` + `If-Modified-Since: <lastModified>` | 304 |
| T7 | should not return file if server has older version than the client | `GET {TEXT_FILE}` + `If-Modified-Since: <lastModified + 60s>` | 304 |
| T8 | should return file if was modified after last retrieval | `GET {TEXT_FILE}` + `If-Modified-Since: <lastModified - 60s>` | 200 |
| T9 | should return 200 OK on HEAD request, but without body | `HEAD {TEXT_FILE}` | 200, zero-length body |
| T10 | should return 304 on HEAD request if we have cached version | `HEAD {TEXT_FILE}` + `If-None-Match: <etag>` | 304, `ETag` header matches |
| T11 | should return Content-length header | `HEAD {TEXT_FILE}` | 200, `Content-Length` equals `TXT_FILE.size` |
| T12 | should return content type in response | `HEAD {TEXT_FILE}` | 200, `Content-Type: text/plain` |

(12 rows for 11 feature methods plus the helper-driven variants; `toDateHeader` formats instants as RFC 1123.)

**T7 encodes the `modifiedAfter` inversion** described in [../technical-debt/remediation-plan.md](../technical-debt/remediation-plan.md) R11: a client claiming a *newer* copy than the server still receives 304. If R11 changes behavior rather than only the method name, this specification must be updated deliberately and the change documented — it is not an incidental assertion fix.

## Specification Gaps to Close
Components with no direct tests today. Add these as new tests; do not replace the existing suite.

### `ThrottlingInputStream`
| # | Specification |
|---|---|
| N1 | `read()` acquires exactly 1 permit from the limiter, then delegates to `target.read()` |
| N2 | `read(byte[])` acquires `b.length` permits and delegates |
| N3 | `read(byte[], off, len)` acquires `len` permits and delegates with the same arguments |
| N4 | `skip`, `available`, `mark`, `reset`, `markSupported`, `close` delegate without acquiring permits |
| N5 | `close()` closes the delegate exactly once |
| N6 | Reading a stream larger than the per-second rate takes proportionally longer (rate is actually enforced) |

### `ExistingFile.cached` (the four header combinations)
| # | Specification |
|---|---|
| N7 | no `If-None-Match`, no `If-Modified-Since` → not cached (200/301 path) |
| N8 | matching ETag only → cached (304) |
| N9 | `If-Modified-Since` satisfying the pointer only → cached (304) |
| N10 | non-matching ETag and a stale `If-Modified-Since` → not cached |
| N11 | HEAD with no conditional headers → 200 with headers and a null body |
| N12 | 301 path sets `Location` to `/download/{uuid}/{originalName}` |

### `FileSystemPointer`
| # | Specification |
|---|---|
| N13 | `getEtag()` returns the SHA-512 hex digest wrapped in double quotes |
| N14 | `matchesEtag` returns true only for the exact quoted value |
| N15 | `getSize()` equals the file length; `getLastModified()` equals the file mtime as an `Instant` |
| N16 | `getMediaType()` returns `Optional.empty()` when `probeContentType` yields nothing, and the response then omits `Content-Type` |
| N17 | Constructing with a non-existent file raises the documented exception type |
| N18 | `open()` returns an independent stream on each call, each starting at byte 0 |

### `FileSystemStorage` / routing
| # | Specification |
|---|---|
| N19 | Under the default profile, `FileSystemStorage` is the wired `FileStorage` bean; under `test`, `FileStorageStub` is |
| N20 | `GET /download/{uuid}` (no filename) returns 301 with a `Location` ending in the original filename |
| N21 | A malformed UUID path segment yields a 400-class response, not a 500 |
| N22 | The `{filename}` segment does not affect which file is served (documents current behavior) |

### Security-related
| # | Specification |
|---|---|
| N23 | After R6, Actuator sensitive endpoints (`/env`, `/beans`) are not anonymously reachable on the application port |
| N24 | After R12, a forced storage I/O failure returns a clean status code with no stack trace or filesystem path in the body |
| N25 | After R15, an original filename containing control characters or `..` does not produce a malformed `Location` header |

## Test Execution Notes
- Framework target: Spock 2.x on the JUnit 5 platform with Groovy 4.x; a Groovy compiler plugin must be bound (none exists today).
- The spec file must be renamed to match its declared class (`DownloadControllerSpec`) or surefire includes must be configured, otherwise the suite may not run at all.
- `FileExamples` computes `TXT_FILE_UUID` and `NOT_FOUND_UUID` via `UUID.randomUUID()` at class initialization, so fixtures are per-JVM, not fixed — tests must never hardcode a UUID.
- Per the transformation constraints, no tests were executed during this analysis; the table above reflects the specifications as written in source.

Related: [validation-criteria.md](validation-criteria.md) · [component-order.md](component-order.md)
