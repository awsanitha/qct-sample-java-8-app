# Technical Debt — Summary

The AWS transformation recommendation and the full prioritized finding table live in the root report: [../technical-debt-report.md](../technical-debt-report.md).

## Verdict
`download-server` is a small, cleanly designed Spring Boot service carrying **heavy version debt and light code debt**. 652 lines of well-factored code sit on a platform generation from 2014-2015 that no longer receives security patches. There are no architectural rewrites required — the remediation is almost entirely a platform upgrade plus a handful of localized code fixes.

## Severity Distribution
| Severity | Count | Category |
|---|---|---|
| High | 5 | EOL/outdated runtime and frameworks (Java 8, Spring Boot 1.2.3, Spring 4.1.6, version-property conflict, unsecured Boot 1.x Actuator) |
| Medium | 6 | Outdated runtime/production dependencies (Guava 18.0, commons-io 2.4, unused commons-codec, mis-scoped groovy-all and cglib, non-default repository) |
| Low | 11 | Code quality and architectural issues (dead code, inverted `modifiedAfter`, stub storage, null bodies, eager hashing, hard-coded throttle, test naming, package squatting, missing Groovy plugin, verbose logging, committed artifacts) |

## The Four Things That Matter
1. **Runtime and framework are end-of-life.** Spring Boot 1.2.3 (2015) and Spring Framework 4.1.6 receive no security fixes, and Java 8 is past free public support for most distributions. Everything else is secondary. → [outdated-components.md](outdated-components.md)
2. **The build has three conflicting statements of the Spring version.** The Boot parent manages one, nine artifacts are pinned to `4.1.6.RELEASE`, and an unused `spring.version` property says `4.1.1.RELEASE`. This must be collapsed to parent-managed versions before any upgrade. → [outdated-components.md](outdated-components.md)
3. **The test suite blocks the upgrade it is needed for.** Spock 1.0-groovy-2.4 and Groovy 2.4.3 will not run on a modern JDK, and no Groovy compiler plugin is bound in the first place. The tests must be modernized in the same effort as the runtime. → [maintenance-burden.md](maintenance-burden.md)
4. **Actuator is exposed with no security configuration.** Boot 1.x serves sensitive endpoints on the application port by default, and the repository contains no `application.properties` or Spring Security configuration. → [../analysis/security-patterns.md](../analysis/security-patterns.md)

## What Is Healthy
- No dependency cycles; interfaces (`FileStorage`, `FilePointer`) provide clean replacement seams.
- Strong immutability: every production field is `final`; no shared mutable state.
- Low complexity: no loops, no `switch`, max method length 12 lines, max cyclomatic complexity 4.
- No hardcoded secrets, no dynamic code execution, no deprecated cryptography (ETags use SHA-512).
- Eleven end-to-end specifications already cover the HTTP caching contract, which gives the upgrade a behavioral safety net once they are made runnable.

## Debt Hotspots by File
| File | Debt items |
|---|---|
| `pom.xml` | 14 (all version and build-configuration debt) |
| `ExistingFile.java` | 4 (null sentinel, hard-coded throttle, exception laundering, unencoded `Location`) |
| `FileSystemPointer.java` | 3 (eager hashing, exception laundering, inverted `modifiedAfter`) |
| `FileSystemStorage.java` | 2 (stub implementation, discloses `logback.xml`) |
| `MainApplication.java` | 1 (dead local with deprecated constructor) |
| `logback.xml` | 2 (verbose levels, nested appender) |
| test tree | 4 (naming mismatch, unused class, no unit tests, tests encode inverted semantics) |

## Navigation
- [outdated-components.md](outdated-components.md) — version-by-version EOL analysis
- [maintenance-burden.md](maintenance-burden.md) — where effort concentrates
- [remediation-plan.md](remediation-plan.md) — prioritized action items
- [../analysis/tech-debt.md](../analysis/tech-debt.md) — debt grouped by category with line-level evidence
- [../analysis/security-patterns.md](../analysis/security-patterns.md) — security gaps
