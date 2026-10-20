# Security Patterns

Static, code-level review. No scanning or execution was performed.

## Security Controls Present
| Control | Implementation | Assessment |
|---|---|---|
| Strong content-based ETags | `FileSystemPointer:22,52` — quoted SHA-512 of file content | Good. SHA-512 is a current-generation hash; no MD5/SHA-1 is used anywhere in project code. (Spring's own `ShallowEtagHeaderFilter` default is MD5, which the unused test-scope `Sha512ShallowEtagHeaderFilter` was written to replace.) |
| Typed path-variable binding | `@PathVariable UUID uuid` | Good. Spring rejects malformed UUIDs before controller logic runs, so the identifier cannot carry arbitrary strings. |
| No client-controlled file paths | The storage adapter resolves the file; `{filename}` is never used for lookup | Good — eliminates path traversal on the download routes. |
| Per-request bandwidth throttling | `ExistingFile:92`, `ThrottlingInputStream` | Partial mitigation of bandwidth exhaustion, but per-request only. |
| Streaming response | `InputStreamResource` over a buffered stream | Good — avoids unbounded memory use per download. |
| No secrets in source | No credentials, keys, tokens, or connection strings appear in any source or resource file | Good. |
| No dynamic code execution | No `eval`-style constructs, no `Runtime.exec`, no reflection, no deserialization of untrusted input | Good. |
| Parameterized logging | SLF4J `log.debug("Downloading {}", uuid)` | Good — no concatenated user input, though UUIDs are logged (see below). |

## Gaps and Risks
| # | Gap | Location | Severity |
|---|---|---|---|
| S1 | **No authentication or authorization on any endpoint.** Possession of a UUID is the only access control; the UUID functions as an unexpirable bearer capability. Spring Security is not a dependency and no filter chain is configured. | `DownloadController` (all routes) | High |
| S2 | **Actuator endpoints unsecured.** `spring-boot-starter-actuator` 1.2.3 is on the classpath with no `application.properties`, no `management.*` settings, and no security config. In Boot 1.x, sensitive endpoints (`/env`, `/beans`, `/configprops`, `/trace`, `/dump`, `/mappings`) are exposed on the application port and will leak configuration and environment detail. | `pom.xml` actuator dependency | High |
| S3 | **End-of-life framework stack.** Spring Framework 4.1.x and Spring Boot 1.2.x receive no security patches; the 4.1 line and its transitive Tomcat 8.0.x / Jackson 2.4.x carry publicly known vulnerabilities. This is the single largest security exposure in the project. | `pom.xml` | High |
| S4 | **No transport security.** No TLS/HTTPS configuration and no `requiresChannel`/HSTS equivalent; file content and ETags travel in cleartext unless TLS is terminated by an upstream proxy that is not part of this repository. | repository-wide | Medium |
| S5 | **No rate limiting per client or global bandwidth cap.** Each request gets its own 1 MB/s `RateLimiter`, so N concurrent requests consume N MB/s. Combined with blocking `acquire()` on container threads, a modest number of slow downloads can exhaust the Tomcat thread pool. | `ExistingFile:92` | Medium |
| S6 | **Information disclosure through error translation.** `IOException`, `FileNotFoundException`, and `URISyntaxException` are rethrown as `IllegalArgumentException` with the original exception as cause and no `@ControllerAdvice` to intercept them. Boot 1.x default error handling can surface stack-trace detail, including local filesystem paths. | `FileSystemPointer:27,36`; `ExistingFile:72` | Medium |
| S7 | **Unvalidated filename placed in a `Location` header.** `redirectDownload` concatenates `filePointer.getOriginalName()` into a URI without URL-encoding. A storage backend returning a name with CR/LF, `?`, `#`, or `..` would produce a malformed or attacker-influenced redirect target. Currently unreachable because the stub controls the name, but it is a latent header-injection/open-redirect vector for any real backend. | `ExistingFile:70` | Medium |
| S8 | **Verbose logging configuration.** `logback.xml` sets the root logger to `DEBUG` and `com.nurkiewicz` to `ALL`; `FileSystemPointer.toString()` (logged at TRACE/DEBUG) includes the absolute filesystem path, and UUID capability tokens are logged on every request. Logs therefore contain both access tokens and internal paths. | `src/main/resources/logback.xml`; `ExistingFile:67,78,98` | Medium |
| S9 | **Timing-unsafe ETag comparison.** `matchesEtag` uses `String.equals`. Low practical impact since the ETag is derived from public content rather than a secret, but it is a non-constant-time comparison of a request-supplied value. | `FileSystemPointer:74` | Low |
| S10 | **Stub storage serves an internal config file.** `FileSystemStorage` returns `logback.xml` from the classpath for *any* UUID — the production-profile implementation discloses an application configuration file to anonymous callers. | `FileSystemStorage:26-29` | Low |
| S11 | **Non-default repository in the build.** `https://repo.spring.io` is declared although no milestone artifacts are needed, widening the build's trusted artifact sources. | `pom.xml` `<repositories>` | Low |

## Input Validation Summary
| Input | Validation |
|---|---|
| `{uuid}` | Framework type conversion to `java.util.UUID` (strong) |
| `{filename}` | None — accepted and ignored |
| `If-None-Match` | None; exact string comparison against the generated ETag. Weak validators (`W/"..."`), lists, and `*` are not handled |
| `If-Modified-Since` | Framework RFC 1123 parsing into `java.util.Date`; a malformed value yields a framework-level 400 |
| Request body | Not read (GET/HEAD only) |

## Cryptography
| Use | Algorithm | Assessment |
|---|---|---|
| ETag generation | SHA-512 via Guava `Hashing.sha512()` | Appropriate; non-security use (integrity/validator), no deprecated algorithm present |
| Password/secret handling | none | — |
| TLS | not configured in-repo | see S4 |
No use of MD5, SHA-1, DES, RC4, ECB mode, `Random` for security purposes, or hardcoded keys was found.

## Recommendations
1. Upgrade the runtime and framework stack first (see [../technical-debt/remediation-plan.md](../technical-debt/remediation-plan.md)) — it closes S3 and the bulk of inherited CVE exposure.
2. Add Spring Security (or an upstream gateway policy) to authenticate download routes and to move/secure Actuator endpoints on a separate management port with restricted exposure.
3. Add a `@ControllerAdvice` that maps storage/I-O failures to 404 or 500 without leaking cause detail, and replace the `IllegalArgumentException` laundering with distinct exception types.
4. URL-encode `getOriginalName()` before building the redirect `Location` and reject names containing control characters.
5. Replace the per-request `RateLimiter` with a shared, configurable limiter, and reduce the Logback root level to `INFO` with a pattern that omits capability UUIDs and absolute paths.
6. Remove the unused `commons-codec` dependency and the `repo.spring.io` repository to shrink the dependency and supply-chain surface.

Related: [../diagrams/architecture.md](../diagrams/architecture.md) (security boundaries) · [tech-debt.md](tech-debt.md)
