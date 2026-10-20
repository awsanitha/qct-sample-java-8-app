# Complexity Analysis

## Overall Assessment
Complexity is **Low**. There are no loops, no `switch` statements, and no method exceeds 12 lines. Cyclomatic complexity per method is 1-4 throughout. The real difficulty in this codebase is not control flow but *semantic* complexity: HTTP cache-validation rules and the lazy/eager interaction between the pointer's cached hash and live filesystem metadata.

## Cyclomatic Complexity (estimated, per method)
| Method | CC | Drivers |
|---|---|---|
| `ExistingFile.cached` | 4 | two `Optional.map(...).orElse(false)` chains plus `||` |
| `FileSystemPointer` ctor | 3 | ternary on `contentType` + catch block |
| `DownloadController.redirect` | 2 | `Optional.map/orElseGet` |
| `DownloadController.download` | 2 | `Optional.map/orElseGet` |
| `ExistingFile.response` | 2 | `ifPresent` branch |
| `ExistingFile.serveWithCaching` | 2 | `if` on `cached` |
| `ExistingFile.resourceToReturn` | 2 | `if/else` on method |
| `ExistingFile.toMediaType` | 2 | `transform().or()` |
| `ExistingFile.redirectDownload` | 2 | catch block |
| `FileSystemPointer.open` | 2 | catch block |
| `FileStorageStub.findFile` (test) | 2 | `if` on UUID |
| all `ThrottlingInputStream` methods | 1 | straight delegation |
| all remaining accessors | 1 | — |

## Hotspots
| Rank | Location | Why it is a hotspot | Severity |
|---|---|---|---|
| 1 | `ExistingFile.cached` + `FileSystemPointer.modifiedAfter` (`ExistingFile:54-63`, `FileSystemPointer:83-85`) | Highest CC in the codebase *and* the logic is subtly inverted relative to the method name. Correctness here determines every 200-vs-304 decision, and the test suite encodes the current (questionable) semantics. Any change must be made jointly across both files and the spec. | High |
| 2 | `FileSystemPointer` constructor (`:19-30`) | Performs a full-file SHA-512 read plus a content-type probe, and converts `IOException` to `IllegalArgumentException`. Cost is O(file size) per request because a new pointer is built on every `findFile` call in `FileSystemStorage`. This is the main performance risk in the system. | High |
| 3 | `ExistingFile.response` (`:102-113`) | Central header-assembly point for 200/304/301; mixes builder calls with an `Optional.ifPresent` side effect and accepts a `null` body. Every response path funnels through it. | Medium |
| 4 | `ExistingFile.buildResource` (`:90-95`) | Creates a new `RateLimiter` per request, so throttling has per-request rather than global semantics; also where stream ownership passes to the framework (nothing in this class closes it). | Medium |
| 5 | `ExistingFile.toMediaType` (`:115-119`) | Bridges Guava's `MediaType`/`Optional` to Spring's types using Guava's deprecated-in-later-versions `transform`/`or` API — a known breakage point when Guava is upgraded. | Medium |
| 6 | `DownloadController` handler pair (`:33-55`) | Structurally duplicated; divergence between the two paths is easy to introduce accidentally. | Low |

## Coupling and Cohesion
| Class | Fan-out (project types) | Fan-out (external) | Cohesion |
|---|---|---|---|
| `DownloadController` | 3 (`FileStorage`, `ExistingFile`, `FilePointer` indirectly) | Spring MVC | High — only request handling |
| `ExistingFile` | 2 (`FilePointer`, `ThrottlingInputStream`) | Spring HTTP, Guava, commons-io | Medium — mixes cache decisions, body selection, and header assembly |
| `FileSystemPointer` | 1 (`FilePointer`) | Guava, java.io/nio | High |
| `FileSystemStorage` | 2 (`FileStorage`, `FileSystemPointer`) | Spring context, SLF4J | High |
| `ThrottlingInputStream` | 0 | Guava | High — single purpose |
| `MainApplication` | 0 | Spring Boot | High |

`ExistingFile` is the least cohesive production class and also carries the most external coupling (three third-party libraries). It is the natural candidate for a split into "cache validation" and "response assembly" if the class grows.

## Structural Complexity
- Inheritance depth: 2 (`ThrottlingInputStream` → `InputStream`; `Sha512ShallowEtagHeaderFilter` → `ShallowEtagHeaderFilter`).
- Interface implementations: 3 (`FileSystemPointer`, `FileSystemStorage`, `FileStorageStub`).
- No generic type parameters are declared by project code beyond use-site generics (`Optional<T>`, `ResponseEntity<Resource>`, `Function<,>`).
- No reflection, no dynamic proxies, no bytecode generation in production code (cglib is test-only in practice).

## Concurrency and Threading
| Aspect | Finding |
|---|---|
| Thread model | Servlet container thread per request (embedded Tomcat); no application-managed threads, executors, or async controllers |
| Shared mutable state | None — all singleton beans are stateless and all fields `final` |
| Synchronization | Only `ThrottlingInputStream.mark`/`reset`, marked `synchronized` to mirror `InputStream` semantics |
| Blocking | `RateLimiter.acquire(n)` blocks the request thread; a 1 MB/s cap means large files occupy a container thread for a long time. With Tomcat's default thread pool this is the principal scalability limit |
| Rate limiter scope | One `RateLimiter` per request ⇒ no aggregate bandwidth ceiling; N concurrent downloads consume N MB/s |
| Thread-safety of Guava types used | `RateLimiter` is thread-safe; `HashCode`/`MediaType` are immutable |
| Race condition | `tag` is computed once at construction while `getSize()`/`getLastModified()` read live metadata — a file modified between construction and response yields inconsistent ETag/length |

Related: [code-metrics.md](code-metrics.md) · [../architecture/patterns.md](../architecture/patterns.md) · [tech-debt.md](tech-debt.md)
