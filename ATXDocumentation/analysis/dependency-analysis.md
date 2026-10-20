# Dependency Analysis

## Internal Dependencies

### Adjacency list (production)
| Source | Depends on |
|---|---|
| `MainApplication` | (context-wired) `DownloadController`, `FileSystemStorage` |
| `DownloadController` | `FileStorage`, `ExistingFile` |
| `ExistingFile` | `FilePointer`, `ThrottlingInputStream` |
| `FileSystemStorage` | `FileStorage`, `FileSystemPointer` |
| `FileSystemPointer` | `FilePointer` |
| `ThrottlingInputStream` | — |
| `FilePointer`, `FileStorage` | — |

### Fan-in / fan-out
| Type | Fan-in | Fan-out | Criticality |
|---|---|---|---|
| `FilePointer` | 4 (`ExistingFile`, `FileSystemPointer`, `FileStorage`, `FileExamples`) | 0 | **High** — changing it touches the whole stack |
| `FileStorage` | 3 (`DownloadController`, `FileSystemStorage`, `FileStorageStub`) | 0 | **High** — the injection seam |
| `ExistingFile` | 1 | 2 | Medium — all response behavior lives here |
| `FileSystemPointer` | 2 (`FileSystemStorage`, `FileExamples`) | 1 | Medium |
| `ThrottlingInputStream` | 1 | 0 | Low — isolated leaf |
| `DownloadController` | 0 (entry point) | 2 | High — only HTTP surface |
| `MainApplication` | 1 (test `@ContextConfiguration`) | 0 | Medium |

No dependency cycles exist. Longest path: `MainApplication → DownloadController → ExistingFile → ThrottlingInputStream` (4).

## External Dependency Usage Map
| Library | Imported by | Specific API used |
|---|---|---|
| Guava 18.0 | `FileSystemPointer`, `ExistingFile`, `ThrottlingInputStream`, `DownloadControllerSpec`, `Sha512ShallowEtagHeaderFilter` | `Hashing.sha512()`, `HashCode`, `Files.hash`, `MoreObjects.toStringHelper`, `net.MediaType` (+`charset().transform().or()`), `util.concurrent.RateLimiter`, `net.HttpHeaders` |
| Spring Web MVC 4.1.6 | `DownloadController` | `@RestController`, `@RequestMapping`, `@PathVariable`, `@RequestHeader`, `RequestMethod` |
| Spring Web 4.1.6 | `DownloadController`, `ExistingFile` | `HttpMethod`, `HttpStatus`, `HttpHeaders`, `MediaType`, `ResponseEntity` |
| Spring Core 4.1.6 | `ExistingFile` | `Resource`, `InputStreamResource` |
| Spring Beans/Context 4.1.6 | `DownloadController`, `FileSystemStorage`, `FileStorageStub` | `@Autowired`, `@Component`, `@Profile` |
| Spring Boot 1.2.3 | `MainApplication` | `SpringApplication`, `@SpringBootApplication` |
| Commons IO 2.4 | `ExistingFile` | `FileUtils.ONE_MB` (constant only) |
| Commons Codec 1.10 | — | **unused** |
| SLF4J | `ExistingFile`, `FileSystemStorage`, `FileStorageStub` | `Logger`, `LoggerFactory` |
| Logback | `logback.xml` | `ConsoleAppender` |
| Spock 1.0-groovy-2.4 | `DownloadControllerSpec` | `spock.lang.Specification` |
| Spring Test 4.1.6 | `DownloadControllerSpec` | `MockMvc`, `MockMvcBuilders`, `@WebAppConfiguration`, `@ContextConfiguration`, `@ActiveProfiles`, request/result matchers |
| Groovy 2.4.3 | `DownloadControllerSpec` | language runtime |
| cglib-nodep 3.1 | — (runtime support for Spock class mocking) | none directly |

## Criticality Ranking of External Dependencies
| Rank | Dependency | Why | Replaceability |
|---|---|---|---|
| 1 | Spring Boot + Spring Framework | The entire web layer, DI, and response model | Very difficult — framework-level |
| 2 | Guava | Hashing (ETag generation), rate limiting (throttling), `MediaType` leaked into `FilePointer`'s signature | Moderate — `MessageDigest`, a custom limiter, and a local media-type value object are all feasible |
| 3 | Groovy + Spock | The only test harness | Moderate — specs could be rewritten in JUnit 5 |
| 4 | Commons IO | One constant | Trivial — replace with `1024L * 1024` |
| 5 | Commons Codec | Unused | Trivial — remove |
| 6 | cglib-nodep | Test mocking support | Low risk — modern Spock/Mockito bundle their own |

## Version Management Issues
1. **Explicit Spring pins fight the Boot parent.** Nine `org.springframework:*` artifacts are pinned to `4.1.6.RELEASE` while `spring-boot-starter-parent:1.2.3.RELEASE` manages its own Spring version. Any Boot upgrade must also remove or re-align these pins, or the build will mix framework versions.
2. **Orphan property.** `spring.version = 4.1.1.RELEASE` is defined but never interpolated — a third, inconsistent version statement.
3. **Duplicate declaration.** `org.springframework:spring-test:4.1.6.RELEASE` (test scope) is declared twice; Maven keeps the last and warns.
4. **Starters pinned redundantly.** The three Boot starters repeat `<version>1.2.3.RELEASE</version>` although the parent already manages them.
5. **Compile-scope test libraries.** `groovy-all` and `cglib-nodep` ship in the production artifact unnecessarily.
6. **Milestone repository without milestone artifacts.** `https://repo.spring.io` is configured although every declared version is GA; it is a non-default repository source that can be dropped.

## Transitive Dependency Notes
Boot 1.2.3 pulls Tomcat 8.0.x, Jackson 2.4.x, SLF4J 1.7.x/Logback 1.1.x, JUnit 4.x, Mockito 1.x, Hamcrest 1.3. None of these are referenced by project code except SLF4J/Logback, but they are all in the 2014-2015 generation and therefore inherit the same end-of-life exposure as the parent. Jackson 2.4.x in particular is carried transitively even though the application never serializes JSON bodies.

Related: [../architecture/dependencies.md](../architecture/dependencies.md) · [../technical-debt/outdated-components.md](../technical-debt/outdated-components.md)
