# Validation Criteria

Criteria for judging a migration of `download-server` successful. Grouped by category; each is objectively checkable.

## 1. Build Integrity
| # | Criterion |
|---|---|
| B1 | The project compiles cleanly at the target release level with no new warnings suppressed rather than fixed |
| B2 | `mvn dependency:tree` resolves exactly **one** Spring Framework version and one Spring Boot version |
| B3 | No `<version>` remains on any `org.springframework:*` or `spring-boot-starter-*` dependency (all parent-managed) |
| B4 | The `spring.version` property and the duplicate `spring-test` declaration are gone |
| B5 | `mvn dependency:analyze` reports no unused-declared dependencies (`commons-codec` removed) and no used-undeclared dependencies |
| B6 | `groovy-all` is `test`-scoped; the packaged JAR contains no Groovy or cglib classes |
| B7 | Only standard public repositories are configured (Maven Central); `repo.spring.io` removed |
| B8 | A Groovy compiler plugin is bound and `src/test/groovy` is a configured test source root |
| B9 | The executable JAR is produced and its `Main-Class`/`Start-Class` resolve to `MainApplication` |

## 2. Test Integrity
| # | Criterion |
|---|---|
| T1 | All 11 original Spock feature methods still exist, none removed, none `@Ignore`d or commented out |
| T2 | All 11 appear in the surefire/failsafe report as executed (not skipped) and pass |
| T3 | Any assertion change is attributable to a documented framework-behavior change, not to a weakened expectation |
| T4 | New unit tests exist for `ThrottlingInputStream`, `ExistingFile.cached`, and `FileSystemPointer` (specs N1-N18 in [test-specifications.md](test-specifications.md)) |
| T5 | If `modifiedAfter` behavior changed (R11), specification T7 was updated deliberately with a recorded rationale |

## 3. Functional Equivalence — HTTP Contract
| # | Criterion |
|---|---|
| F1 | `GET /download/{uuid}/{filename}` returns 200 with the exact file bytes |
| F2 | `GET /download/{uuid}` returns 301 with `Location: /download/{uuid}/{originalName}` |
| F3 | An unknown UUID returns 404 with a zero-length body on both routes |
| F4 | A malformed UUID returns a 400-class status, never 500 |
| F5 | `If-None-Match` matching the current ETag returns 304 and echoes the `ETag` header |
| F6 | `If-None-Match` not matching returns 200 with the body |
| F7 | Absent conditional headers return 200 (the `orElse(false)` default is preserved) |
| F8 | `If-Modified-Since` equal to or later than the file's last-modified returns 304 |
| F9 | `If-Modified-Since` earlier than the file's last-modified returns 200 |
| F10 | `HEAD` returns the same status and headers as `GET` with a zero-length body |
| F11 | `Content-Length` equals `FilePointer.getSize()` on both 200 and 304 responses |
| F12 | `Last-Modified` is present and derived from the file's mtime |
| F13 | `Content-Type` is present when the media type is detected (`text/plain` for the fixture) and omitted when `getMediaType()` is empty |
| F14 | The ETag format remains a **quoted SHA-512 hex digest**, byte-identical to the pre-migration value for the same content |
| F15 | The `{filename}` segment continues not to affect file selection (or, if changed, the change is documented) |

## 4. Structural / API Preservation
| # | Criterion |
|---|---|
S1 | All public types retain their names: `MainApplication`, `DownloadController`, `ExistingFile`, `FilePointer`, `FileSystemPointer`, `FileStorage`, `FileSystemStorage`, `ThrottlingInputStream` |
| S2 | Each source file retains its primary type declaration |
| S3 | Public method signatures are unchanged except for deliberately decided changes (R8 media-type return type, R11 rename) — each such change documented |
| S4 | `FileStorage` and `FilePointer` remain interfaces with their implementations intact |
| S5 | Profile-based bean selection still works: `FileSystemStorage` under the default profile, `FileStorageStub` under `test` |
| S6 | `ThrottlingInputStream` still extends `java.io.InputStream` and honors its full contract |
| S7 | No duplicate method signatures introduced in any scope |
| S8 | Copyright/license content in `LICENSE` is untouched; comment blocks of five or more consecutive lines are preserved unless the code they describe changed |

## 5. Platform Targets
| # | Criterion |
|---|---|
| P1 | Compiler configuration uses `<release>` at the agreed target (17 or 21), not `<source>`/`<target>` 1.8 |
| P2 | Spring Boot is at the agreed target (2.7.x intermediate, then 3.x) with no `javax.*` servlet imports remaining after the 3.x hop |
| P3 | Groovy 4.x and Spock 2.x on the JUnit 5 platform |
| P4 | `maven-compiler-plugin` and surefire are at versions supporting the target JDK and test platform |
| P5 | `new Integer(String)` and other APIs deprecated for removal are gone |
| P6 | The application starts on the target JDK and serves both routes |

## 6. Security Posture (no regression, measurable improvement)
| # | Criterion |
|---|---|
| X1 | No hardcoded credentials, keys, or tokens introduced anywhere |
| X2 | ETag generation still uses SHA-512; no MD5/SHA-1 or other weak algorithm introduced |
| X3 | Every dependency version in the final build has been checked for known vulnerabilities, and any accepted exception is documented |
| X4 | Actuator sensitive endpoints are not anonymously reachable on the application port (R6) |
| X5 | Error responses contain no stack traces, exception causes, or absolute filesystem paths (R12) |
| X6 | No new `eval`-style execution, `Runtime.exec`, reflection on untrusted input, or deserialization of untrusted data |
| X7 | No existing security control was removed or weakened — specifically, typed `UUID` path binding and the server-side (never client-supplied) file path resolution remain in place |
| X8 | If R15 was performed, `getOriginalName()` is URL-encoded before entering the `Location` header |

## 7. Operational Characteristics
| # | Criterion |
|---|---|
| O1 | Throttling still applies to download responses; the rate is configurable rather than hardcoded (R13) |
| O2 | Responses still stream — no full-file buffering in memory |
| O3 | Logging still emits on the existing SLF4J logger names; the root level is `INFO` and the appender is declared top-level with `<appender-ref>` (R9) |
| O4 | Capability UUIDs and absolute filesystem paths are not logged at default production levels |
| O5 | If lazy hashing was adopted (R14), a repeated conditional request performs no full file read |

## 8. Documentation and Hygiene
| # | Criterion |
|---|---|
| D1 | `target/` and `.DS_Store` are git-ignored and no longer tracked |
| D2 | `README.md` documents how to build, run, and call both endpoints |
| D3 | `ATXDocumentation/` remains present in the project root and reflects the post-migration state |
| D4 | Every deliberate behavioral change (R11, R8, R15) is recorded with its rationale |

## Validation Procedure
1. Verify build integrity (section 1) — static inspection of `pom.xml` plus dependency reports.
2. Run the full test suite and confirm section 2, including that all 11 original specifications executed.
3. Exercise the HTTP contract (section 3) against a running instance, comparing ETag, `Content-Length`, `Last-Modified`, and `Content-Type` against pre-migration captures.
4. Diff the public API surface for section 4.
5. Confirm platform targets (section 5) from the resolved dependency tree and the running JVM.
6. Re-run a dependency vulnerability check and the security checklist (section 6).
7. Confirm operational behavior (section 7) with a large-file download and a log inspection.
8. Review hygiene items (section 8).

A migration is successful only when sections 1-5 pass fully and no item in section 6 is a regression.

Related: [component-order.md](component-order.md) · [test-specifications.md](test-specifications.md) · [../technical-debt/remediation-plan.md](../technical-debt/remediation-plan.md)
