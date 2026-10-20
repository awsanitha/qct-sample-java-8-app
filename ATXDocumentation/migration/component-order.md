# Component Migration Order

Dependency-driven ordering for any platform upgrade or reimplementation. The project is a single Maven module, so the order below is intra-module and derives from the acyclic class graph in [../analysis/dependency-analysis.md](../analysis/dependency-analysis.md).

## Phase 0 — Build Foundation (must precede all code work)
| Step | Artifact | Rationale |
|---|---|---|
| 0.1 | `pom.xml` — collapse Spring versions to parent management, remove the orphan `spring.version` property, the duplicate `spring-test`, redundant starter versions, and the `repo.spring.io` repository | Nothing downstream can be validated while three version statements compete |
| 0.2 | `pom.xml` — bind a Groovy compiler plugin; rename `DownloadControllerTest.groovy` → `DownloadControllerSpec.groovy`; configure surefire includes | The test suite is the regression contract and may currently be skipped entirely |
| 0.3 | Record the baseline test result | Every later phase is judged against it |
| 0.4 | `pom.xml` — upgrade `maven-compiler-plugin`, switch to `<release>`; upgrade Groovy/Spock | Old Spock/Groovy cannot run on a modern JDK |

## Phase 1 — Leaf Types (no project dependencies)
| Order | Component | Depends on | Migration notes |
|---|---|---|---|
| 1.1 | `FileStorage` (interface) | — | One method; no change expected |
| 1.2 | `FilePointer` (interface) | — | If R8 is adopted, change `getMediaType()` to stop returning a Guava type. This is the single highest-ripple edit in the codebase — do it here, before implementors |
| 1.3 | `ThrottlingInputStream` | Guava `RateLimiter`, `java.io` | Fully isolated; migrate and unit-test first to establish a known-good leaf. If the Guava-removal option is taken, swap in a local limiter here |

## Phase 2 — Implementations
| Order | Component | Depends on | Migration notes |
|---|---|---|---|
| 2.1 | `FileSystemPointer` | `FilePointer`, Guava hashing/`MediaType`, `java.nio.file` | Carries the Guava hashing and media-type coupling; also where lazy hashing (R14) and the `modifiedAfter` decision (R11) land |
| 2.2 | `FileSystemStorage` | `FileStorage`, `FileSystemPointer` | Stub; migrate as-is, replace as feature work (R15) |

## Phase 3 — Response Layer
| Order | Component | Depends on | Migration notes |
|---|---|---|---|
| 3.1 | `ExistingFile` | `FilePointer`, `ThrottlingInputStream`, Spring HTTP, Guava, commons-io | Highest-risk class: `ResponseEntity.body(null)` semantics for HEAD/304, the Guava→Spring `MediaType` conversion, and the `ONE_MB` constant inlining all change here |

## Phase 4 — Web Layer
| Order | Component | Depends on | Migration notes |
|---|---|---|---|
| 4.1 | `DownloadController` | `FileStorage`, `ExistingFile`, Spring MVC | Re-verify `@RequestHeader(...) Optional<Date>` binding, the `HttpMethod` handler argument, and `@PathVariable UUID` conversion on the target Spring version |

## Phase 5 — Bootstrap and Configuration
| Order | Component | Migration notes |
|---|---|---|
| 5.1 | `MainApplication` | Remove the dead `Integer temp` line; verify `@SpringBootApplication` auto-configuration on the target Boot version |
| 5.2 | `logback.xml` | Declare the appender top-level with `<appender-ref>`; set root to `INFO` |
| 5.3 | New `application.yml` | Externalize port, Actuator exposure/management port, and the throttle rate |

## Phase 6 — Test Tree
| Order | Component | Migration notes |
|---|---|---|
| 6.1 | `FileExamples` | Follows the `FilePointer` signature change from 1.2 |
| 6.2 | `FileStorageStub` | Profile-based stub; no structural change |
| 6.3 | `DownloadControllerSpec` | Spock 1.0 → 2.x idioms; `@ContextConfiguration`/`@WebAppConfiguration` → `@SpringBootTest`/`@AutoConfigureMockMvc`. Preserve all 11 feature methods |
| 6.4 | `Sha512ShallowEtagHeaderFilter` | Unreferenced — delete, or wire in deliberately. If kept across a Boot 3.x migration, confirm `ShallowEtagHeaderFilter.generateETagHeaderValue` still has this signature |
| 6.5 | New unit tests | `ThrottlingInputStream`, `ExistingFile.cached`, `FileSystemPointer` (R16) |

## Dependency-Order Diagram
```
Phase 0: pom.xml + build plugins + test-stack versions
             │
Phase 1: FileStorage, FilePointer, ThrottlingInputStream        (leaves)
             │
Phase 2: FileSystemPointer ──▶ FileSystemStorage                 (implementations)
             │
Phase 3: ExistingFile                                            (response assembly)
             │
Phase 4: DownloadController                                      (HTTP surface)
             │
Phase 5: MainApplication, logback.xml, application.yml           (bootstrap/config)
             │
Phase 6: FileExamples ──▶ FileStorageStub ──▶ DownloadControllerSpec
```

## Checkpoints
| After phase | Checkpoint |
|---|---|
| 0 | Single Spring version resolves; all 11 specs are visible in the test report; baseline recorded |
| 1 | Interfaces compile; `ThrottlingInputStream` unit tests pass |
| 2 | `FileSystemPointer` produces the same quoted SHA-512 ETag format as before |
| 3 | 200/304/301 status selection unchanged; HEAD still returns headers with an empty body |
| 4 | Both routes resolve; 404 on unknown UUID |
| 5 | Application context starts; Actuator sensitive endpoints not anonymously reachable |
| 6 | Full suite green on the target JDK, with no specification removed |

## Risk Ordering
Highest-risk first, independent of dependency order: `pom.xml` version untangling → Spock/Groovy modernization → `ExistingFile` response semantics → `FilePointer` signature change → everything else. The interfaces and `ThrottlingInputStream` are near-zero risk and make good first commits for confidence.

Related: [test-specifications.md](test-specifications.md) · [validation-criteria.md](validation-criteria.md) · [../technical-debt/remediation-plan.md](../technical-debt/remediation-plan.md)
