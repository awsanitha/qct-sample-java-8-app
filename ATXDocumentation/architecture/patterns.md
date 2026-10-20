# Patterns

## Architectural Patterns
| Pattern | Evidence |
|---|---|
| Layered monolith (web → service abstraction → storage) | `DownloadController` → `FileStorage` → `FilePointer` |
| Hexagonal / ports-and-adapters (lightweight) | `FileStorage` and `FilePointer` are ports; `FileSystemStorage`/`FileSystemPointer` and `FileStorageStub` are adapters |
| Annotation-driven dependency injection | `@SpringBootApplication`, `@Component`, `@Autowired` constructor injection |
| Environment-based composition | `@Profile("!test")` vs `@Profile("test")` selects the storage adapter |
| REST resource addressing | `/download/{uuid}` and `/download/{uuid}/{filename}` |
| Stateless request handling | No session or mutable shared state; `ExistingFile` is created per request |

## Design Patterns
| Pattern | Location |
|---|---|
| Decorator | `ThrottlingInputStream` wraps any `InputStream`, adding rate limiting while preserving the `InputStream` contract |
| Strategy (via method reference) | `ExistingFile.serveWithCaching` accepts `Function<FilePointer, ResponseEntity<Resource>>`; callers pass `this::redirectDownload` or `this::serveDownload` |
| Builder | `ResponseEntity.status(...).eTag(...).contentLength(...).lastModified(...)` in `ExistingFile.response`; Guava `MoreObjects.toStringHelper` in `FileSystemPointer.toString` |
| Null Object / Optional-based absence | `Optional<FilePointer>` from `FileStorage`; `Optional<MediaType>`; `Optional` request-header binding, with `orElseGet` producing 404 |
| Adapter | `ExistingFile.toMediaType` converts Guava `MediaType` to Spring `MediaType` |
| Immutable value object | `ExistingFile`, `FileSystemPointer` — all fields `final`, set in the constructor |
| Test stub | `FileStorageStub` substituting for `FileSystemStorage` under the `test` profile |
| Object mother | `FileExamples` static fixtures |
| Specification-style BDD tests | Spock `given/expect` blocks in `DownloadControllerSpec` |

## Functional-Style Java 8 Usage
`Optional.map/orElse/orElseGet/ifPresent`, method references (`filePointer::matchesEtag`, `Date::toInstant`), lambdas (`c -> new MediaType(...)`), `java.time.Instant`, and `java.util.function.Function` are used idiomatically throughout `DownloadController` and `ExistingFile`.

## Anti-Patterns and Smells Identified
| Anti-pattern | Location | Note |
|---|---|---|
| Heavy work in a constructor | `FileSystemPointer` constructor hashes the entire file | Cost and `IOException`→`IllegalArgumentException` translation are invisible to callers |
| Exception type laundering | `FileSystemPointer` (×2), `ExistingFile.redirectDownload` | I/O and URI errors all become `IllegalArgumentException`, losing the distinction between client and server faults |
| Returning `null` to signal "no body" | `ExistingFile.resourceToReturn` / `redirectDownload` | Null flows into `ResponseEntity.body(...)` |
| Dead code | `MainApplication:10` (`Integer temp`), unused `Sha512ShallowEtagHeaderFilter`, unused `spring.version` property, unused `commons-codec` | |
| Misleading method name | `FilePointer.modifiedAfter` / `FileSystemPointer:83` returns true when the client copy is at least as new | Inverted relative to the name |
| Magic constant | hard-coded `ONE_MB` throttle in `ExistingFile:92` | Not externalized to configuration |
| Package squatting | test class in `org.springframework.web.filter` to reach a protected method | Fragile against framework changes |
| Scope misuse | `groovy-all` and `cglib-nodep` at compile scope though only tests need them | Bloats the production artifact |

See [../analysis/complexity-analysis.md](../analysis/complexity-analysis.md) and [../technical-debt/maintenance-burden.md](../technical-debt/maintenance-burden.md).
