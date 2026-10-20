# Code Metrics

All counts from static inspection; `target/` excluded.

## Size
| Metric | Value |
|---|---|
| Total Java + Groovy lines | 652 |
| Production Java lines | 417 |
| Test lines (Java + Groovy) | 236 |
| Production types | 8 (6 classes, 2 interfaces) |
| Test types | 4 |
| Packages | 2 (`com.nurkiewicz.download`, `org.springframework.web.filter` in test scope) |
| Maven modules | 1 |
| Non-code config files | 1 (`logback.xml`) |

## Per-File Metrics
| File | Lines | Types | Methods | Max method length | Notes |
|---|---|---|---|---|---|
| `ExistingFile.java` | 121 | 1 | 11 + ctor | 12 (`response`) | largest class; all helpers private |
| `FileSystemPointer.java` | 86 | 1 | 10 + ctor | 12 (ctor) | 8 interface overrides |
| `ThrottlingInputStream.java` | 67 | 1 | 10 + ctor | 4 | pure delegation |
| `DownloadController.java` | 63 | 1 | 3 + ctor | 11 | 2 handlers |
| `FileSystemStorage.java` | 33 | 1 | 1 | 7 | stub |
| `FilePointer.java` | 26 | 1 (interface) | 8 | — | |
| `MainApplication.java` | 13 | 1 | 1 | 4 | contains dead local |
| `FileStorage.java` | 8 | 1 (interface) | 1 | — | |
| `DownloadControllerTest.groovy` | 176 | 1 | 11 features + 2 helpers | 10 | |
| `FileStorageStub.java` | 27 | 1 | 1 | 7 | |
| `FileExamples.java` | 20 | 1 | 1 | 5 | |
| `Sha512ShallowEtagHeaderFilter.java` | 13 | 1 | 1 | 4 | unreferenced |

## Method-Level Indicators
| Indicator | Value |
|---|---|
| Production methods (incl. constructors) | 42 |
| Average method length | ~5 lines |
| Longest production method | 12 lines (`ExistingFile.response`, `FileSystemPointer` ctor) |
| Methods with more than 3 parameters | 1 (`ExistingFile.serveWithCaching`, 3 params; `response`, 3 params) |
| Methods with nesting depth > 2 | 0 |
| Deepest conditional nesting | 1 (`if/else` in `resourceToReturn`, `if` in `serveWithCaching`) |
| `switch` statements | 0 |
| Loops | 0 |
| Fields that are non-final | 1 (`DownloadControllerSpec.mockMvc`, test) |
| `null` literals returned/passed | 4 (`ExistingFile:71,87,99,102-path`) |

## Quality Indicators
| Indicator | Assessment |
|---|---|
| Immutability | Strong — every production field is `final` |
| Encapsulation | Strong — only 2 public methods on `ExistingFile`, all branching logic private |
| Interface segregation | Good — `FileStorage` has 1 method, `FilePointer` 8 cohesive accessors |
| Dependency inversion | Good — controller depends on interfaces only |
| Dead code | 4 occurrences (`MainApplication` local, `Sha512ShallowEtagHeaderFilter`, `spring.version` property, `commons-codec` dependency) |
| Comments | Sparse: one 4-line Javadoc on `FileSystemStorage`, one inline marker in `ThrottlingInputStream`. No license headers present in source files. |
| Logging | 3 classes use SLF4J with parameterized messages (no string concatenation) |
| Exception handling | 3 try/catch blocks, all translating checked exceptions to `IllegalArgumentException` |
| Test count | 11 Spock feature methods, all end-to-end through `MockMvc` |
| Unit-test coverage of leaf classes | `ThrottlingInputStream`, `FileSystemStorage`, `ExistingFile` have no direct unit tests |
| Duplication | Low; the only near-duplicate is the `redirect`/`download` handler pair in `DownloadController` (identical shape, different `ExistingFile` method) |

## Dependency Metrics
| Metric | Value |
|---|---|
| Declared direct dependencies | 20 (9 Spring Framework pins, 3 Boot starters, 2 Spock, Guava, commons-io, commons-codec, groovy-all, cglib-nodep; `spring-test` declared twice) |
| Unused declared dependencies | 1 (`commons-codec`) |
| Dependencies at wrong scope | 2 (`groovy-all`, `cglib-nodep` at compile) |
| Explicit version pins overriding the Boot parent | 9 |
| Build plugins declared | 1 (`maven-compiler-plugin` 3.0) |

Related: [complexity-analysis.md](complexity-analysis.md) · [tech-debt.md](tech-debt.md)
