# Interfaces

## `com.nurkiewicz.download.FileStorage`
`src/main/java/com/nurkiewicz/download/FileStorage.java`

```java
public interface FileStorage {
    Optional<FilePointer> findFile(UUID uuid);
}
```
**Contract**
- Input: a non-null `UUID` identifying a file.
- Output: `Optional.of(pointer)` when the identifier is known; `Optional.empty()` when it is not. Implementations must not return `null` and must not throw for an unknown identifier — the 404 path depends on an empty `Optional`.
- Implementations are Spring singletons and must therefore be thread-safe.

**Implementations**
| Class | Profile | Behavior |
|---|---|---|
| `FileSystemStorage` | `!test` | Stub: returns a `FileSystemPointer` over classpath `/logback.xml` for every UUID. |
| `FileStorageStub` (test) | `test` | Returns `FileExamples.TXT_FILE` for `TXT_FILE_UUID`, otherwise `Optional.empty()`. |

## `com.nurkiewicz.download.FilePointer`
`src/main/java/com/nurkiewicz/download/FilePointer.java`

```java
public interface FilePointer {
    InputStream open();
    long getSize();
    String getOriginalName();
    String getEtag();
    Optional<MediaType> getMediaType();          // com.google.common.net.MediaType
    boolean matchesEtag(String requestEtag);
    Instant getLastModified();
    boolean modifiedAfter(Instant isModifiedSince);
}
```
**Member contracts**
| Method | Expectation |
|---|---|
| `open()` | Returns a fresh, independently closeable stream positioned at byte 0 on every call. The caller (Spring, via `InputStreamResource`) owns closing it. |
| `getSize()` | Byte length used for the `Content-Length` header; must agree with the bytes produced by `open()`. |
| `getOriginalName()` | Filename used to build the redirect `Location`; must be URL-safe (not validated today). |
| `getEtag()` | A complete HTTP entity-tag value **including surrounding double quotes** (`FileSystemPointer` returns `"\"" + sha512 + "\""`). |
| `getMediaType()` | `Optional.empty()` means "omit `Content-Type`". |
| `matchesEtag(String)` | Compares against the raw `If-None-Match` header value; `FileSystemPointer` does exact string equality, so weak validators and multi-value headers are not handled. |
| `getLastModified()` | Instant used for the `Last-Modified` header. |
| `modifiedAfter(Instant)` | As implemented, returns `true` when the client timestamp is **not before** the file's last-modified instant, i.e. "the client copy is current". The name reads as the opposite of the behavior — see technical debt L2. |

**Implementations**: `FileSystemPointer` (production and test fixture). No other implementation exists; the interface is also the natural seam for an object-store adapter.

## Inherited / Extended External Types
| Project type | Supertype | Contract notes |
|---|---|---|
| `ThrottlingInputStream` | `java.io.InputStream` | Honors the full `InputStream` contract; adds a blocking `RateLimiter.acquire(n)` before each read. `mark`/`reset` are delegated and `synchronized`. |
| `Sha512ShallowEtagHeaderFilter` (test) | `org.springframework.web.filter.ShallowEtagHeaderFilter` | Overrides the protected `generateETagHeaderValue(byte[])` to produce a quoted SHA-512 instead of the framework's MD5-based value. Requires residing in the framework's package. |
| `DownloadControllerSpec` (test) | `spock.lang.Specification` | Spock feature-method contract (`given`/`expect` blocks). |

## Functional Interfaces Consumed
| Interface | Usage |
|---|---|
| `java.util.function.Function<FilePointer, ResponseEntity<Resource>>` | `ExistingFile.serveWithCaching` strategy parameter; bound to `this::redirectDownload` or `this::serveDownload` |
| `com.google.common.base.Function` (implicit via `Optional.transform`) | `ExistingFile.toMediaType` lambda `c -> new MediaType(...)` |

## Contracts Between Components
1. `DownloadController` depends only on `FileStorage`; it never assumes a concrete pointer type.
2. `ExistingFile` depends only on `FilePointer` and on Spring's `ResponseEntity`/`Resource` types.
3. Spring supplies the `HttpMethod` argument to controller handlers, which `ExistingFile` uses to decide whether to emit a body — the HEAD/GET distinction is the only method-sensitive behavior.

Related: [data-models.md](data-models.md) · [api-reference.md](api-reference.md) · [../architecture/components.md](../architecture/components.md)
