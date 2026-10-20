# Modules

## Build Modules
The project is a **single-module Maven build**. There is no `<modules>` section and no aggregator POM.

| Coordinate | Packaging | Parent |
|---|---|---|
| `com.nurkiewicz.download:download-server:0.0.1-SNAPSHOT` | jar (Spring Boot executable) | `org.springframework.boot:spring-boot-starter-parent:1.2.3.RELEASE` |

Source roots:
| Root | Language | Compiled by |
|---|---|---|
| `src/main/java` | Java 8 | `maven-compiler-plugin` 3.0 |
| `src/main/resources` | — | resources plugin |
| `src/test/java` | Java 8 | `maven-compiler-plugin` 3.0 (test goal) |
| `src/test/groovy` | Groovy 2.4 | **no plugin bound** — see technical debt L9 |
| `src/test/resources` | — | resources plugin |

## Logical Modules (packages)
| Package | Scope | Types | Responsibility |
|---|---|---|---|
| `com.nurkiewicz.download` | main | `MainApplication`, `DownloadController`, `ExistingFile`, `FilePointer`, `FileSystemPointer`, `FileStorage`, `FileSystemStorage`, `ThrottlingInputStream` | the entire application: bootstrap, web layer, storage abstraction, streaming |
| `com.nurkiewicz.download` | test | `FileExamples`, `FileStorageStub`, `DownloadControllerSpec` | fixtures, stub adapter, end-to-end MockMvc specification |
| `org.springframework.web.filter` | test | `Sha512ShallowEtagHeaderFilter` | framework-package extension to override a protected method |

Everything production lives in one flat package. At 8 types this is reasonable, but the package mixes three concerns (web, storage, streaming) and offers no natural split point if the storage layer grows.

## Module Dependency Order
```
1. FileStorage, FilePointer          (interfaces — no project dependencies)
2. ThrottlingInputStream             (depends only on Guava + java.io)
3. FileSystemPointer                 (implements FilePointer)
4. ExistingFile                      (uses FilePointer, ThrottlingInputStream)
5. FileSystemStorage                 (implements FileStorage, creates FileSystemPointer)
6. DownloadController                (uses FileStorage, ExistingFile)
7. MainApplication                   (bootstraps the context)
8. test: FileExamples → FileStorageStub → DownloadControllerSpec
```
This order is acyclic and is reused as the migration order in [../migration/component-order.md](../migration/component-order.md).

## External Module Boundaries
| Boundary | Crossed by |
|---|---|
| Spring MVC | `DownloadController` annotations, `ResponseEntity`, `Resource`, `InputStreamResource`, `HttpMethod`, `HttpStatus`, `MediaType` |
| Spring context | `@Component`, `@Profile`, `@Autowired`, `@SpringBootApplication` |
| Guava | `Hashing`/`HashCode`, `Files.hash`, `MoreObjects`, `net.MediaType`, `RateLimiter`, `net.HttpHeaders` (test) |
| Apache Commons IO | `FileUtils.ONE_MB` only |
| SLF4J | `Logger`/`LoggerFactory` in three classes |
| JDK | `java.io`, `java.nio.file`, `java.time`, `java.util`, `java.util.function` |

Guava is the deepest external coupling: it appears in five of the eight production classes, including in the `FilePointer` public signature (`Optional<com.google.common.net.MediaType>`), which leaks a third-party type into the project's own API. That coupling makes Guava upgrades a cross-cutting change — see [../technical-debt/maintenance-burden.md](../technical-debt/maintenance-burden.md).

Related: [program-structure.md](program-structure.md) · [../architecture/dependencies.md](../architecture/dependencies.md)
