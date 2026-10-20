# Structural Diagrams

## Component Diagram
```
┌──────────────────────────── download-server (single JAR) ────────────────────────────┐
│                                                                                      │
│  ┌──────────────────┐   bootstraps   ┌────────────────────────────────────────────┐   │
│  │ MainApplication  │ ─────────────▶ │ Spring ApplicationContext                  │   │
│  └──────────────────┘                │  (auto-config, component scan)             │   │
│                                      └───────────────┬────────────────────────────┘   │
│                                                      │ manages                        │
│          ┌───────────────────────────────────────────┴──────────────────┐             │
│          ▼                                                             ▼             │
│  ┌──────────────────┐        uses        ┌──────────────────┐   ┌──────────────────┐  │
│  │ DownloadController│ ───────────────▶  │  FileStorage     │◀──│ FileSystemStorage│  │
│  └────────┬─────────┘                    │  «interface»     │   │ @Profile("!test")│  │
│           │ creates                      └──────────────────┘   └────────┬─────────┘  │
│           ▼                                                              │ creates    │
│  ┌──────────────────┐        reads       ┌──────────────────┐            ▼            │
│  │   ExistingFile   │ ───────────────▶   │  FilePointer     │◀── FileSystemPointer    │
│  └────────┬─────────┘                    │  «interface»     │                        │
│           │ wraps stream                 └──────────────────┘                        │
│           ▼                                                                           │
│  ┌────────────────────────┐     throttles via    ┌──────────────────────┐             │
│  │ ThrottlingInputStream  │ ───────────────────▶ │ Guava RateLimiter    │             │
│  └────────────────────────┘                      └──────────────────────┘             │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

## Class Diagram
```
                 «interface» FileStorage
                 + findFile(UUID): Optional<FilePointer>
                          △                △
                          │                │
         ┌────────────────┘                └────────────────┐
┌──────────────────────────┐                   ┌──────────────────────────┐
│ FileSystemStorage        │                   │ FileStorageStub  (test)  │
│ @Component @Profile(!test)│                  │ @Component @Profile(test)│
│ - log: Logger            │                   │ - log: Logger            │
│ + findFile(UUID)         │                   │ + findFile(UUID)         │
└──────────────────────────┘                   └──────────────────────────┘

                 «interface» FilePointer
                 + open(): InputStream
                 + getSize(): long
                 + getOriginalName(): String
                 + getEtag(): String
                 + getMediaType(): Optional<MediaType>
                 + matchesEtag(String): boolean
                 + getLastModified(): Instant
                 + modifiedAfter(Instant): boolean
                          △
                          │ implements
┌─────────────────────────────────────────────┐
│ FileSystemPointer                           │
│ - target: File                              │
│ - tag: HashCode                             │
│ - mediaTypeOrNull: com.google.common.net.MediaType │
│ + FileSystemPointer(File)                   │
│ + toString(): String                        │
└─────────────────────────────────────────────┘

┌──────────────────────────────────────────┐        ┌──────────────────────────────┐
│ DownloadController  @RestController      │ uses   │ ExistingFile                 │
│ - storage: FileStorage                   ├───────▶│ - method: HttpMethod         │
│ + redirect(...): ResponseEntity<Resource>│        │ - filePointer: FilePointer   │
│ + download(...): ResponseEntity<Resource>│        │ - uuid: UUID                 │
│ - findExistingFile(...)                  │        │ + redirect(...)              │
└──────────────────────────────────────────┘        │ + handle(...)                │
                                                    │ - serveWithCaching(...)      │
                                                    │ - cached(...)                │
                                                    │ - redirectDownload(...)      │
                                                    │ - serveDownload(...)         │
                                                    │ - resourceToReturn(...)      │
                                                    │ - buildResource(...)         │
                                                    │ - notModified(...)           │
                                                    │ - response(...)              │
                                                    │ - toMediaType(...)           │
                                                    └──────────────────────────────┘

        java.io.InputStream
                 △
                 │ extends
┌────────────────────────────────────────┐
│ ThrottlingInputStream                  │
│ - target: InputStream                  │
│ - maxBytesPerSecond: RateLimiter       │
│ + read() / read(byte[]) / read(b,o,l)  │
│ + skip / available / mark / reset      │
│ + markSupported / close                │
└────────────────────────────────────────┘

org.springframework.web.filter.ShallowEtagHeaderFilter
                 △
                 │ extends (test scope)
        Sha512ShallowEtagHeaderFilter
        # generateETagHeaderValue(byte[]): String
```

## Package Dependency Graph
```
com.nurkiewicz.download  (main)
   ├─▶ org.springframework.{web.bind.annotation, http, core.io, beans.factory.annotation,
   │                        context.annotation, stereotype, boot, boot.autoconfigure}
   ├─▶ com.google.common.{base, hash, io, net, util.concurrent}
   ├─▶ org.apache.commons.io (FileUtils.ONE_MB)
   ├─▶ org.slf4j
   └─▶ java.{io, net, nio.file, time, util, util.function}

com.nurkiewicz.download  (test)
   ├─▶ com.nurkiewicz.download (main)
   ├─▶ spock.lang
   └─▶ org.springframework.test.{context, web.servlet}

org.springframework.web.filter (test)  ─▶ com.google.common.hash
```
Single production package; no inter-package cycles.

## Deployment Diagram
```
┌──────────────┐   HTTP/1.1 GET|HEAD /download/...   ┌─────────────────────────────┐
│ HTTP client  │ ──────────────────────────────────▶ │ JVM (Java 8)                │
│ (browser/CLI)│ ◀────── 200 / 301 / 304 / 404 ───── │  download-server-*.jar      │
└──────────────┘                                     │   embedded Tomcat :8080     │
                                                     │   Spring Boot 1.2.3         │
                                                     └──────────────┬──────────────┘
                                                                    │ file read
                                                                    ▼
                                                     ┌─────────────────────────────┐
                                                     │ Local filesystem / classpath│
                                                     │  (stub: /logback.xml)       │
                                                     └─────────────────────────────┘
No load balancer, database, cache, or message broker is defined in the repository.
```

Related: [behavioral.md](behavioral.md) · [architecture.md](architecture.md) · [../reference/program-structure.md](../reference/program-structure.md)
