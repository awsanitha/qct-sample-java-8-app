# Program Structure

Complete structural hierarchy of all source artifacts. Line numbers refer to the current sources.

## Source Tree
```
src/
├── main/
│   ├── java/com/nurkiewicz/download/
│   │   ├── MainApplication.java          (13 lines)
│   │   ├── DownloadController.java       (63 lines)
│   │   ├── ExistingFile.java            (121 lines)
│   │   ├── FilePointer.java              (26 lines, interface)
│   │   ├── FileSystemPointer.java        (86 lines)
│   │   ├── FileStorage.java               (8 lines, interface)
│   │   ├── FileSystemStorage.java        (33 lines)
│   │   └── ThrottlingInputStream.java    (67 lines)
│   └── resources/
│       └── logback.xml
└── test/
    ├── java/
    │   ├── com/nurkiewicz/download/
    │   │   ├── FileExamples.java         (20 lines)
    │   │   └── FileStorageStub.java      (27 lines)
    │   └── org/springframework/web/filter/
    │       └── Sha512ShallowEtagHeaderFilter.java (13 lines)
    ├── groovy/com/nurkiewicz/download/
    │   └── DownloadControllerTest.groovy (176 lines, class DownloadControllerSpec)
    └── resources/
        └── download.txt
```

## Type Hierarchy
```
java.lang.Object
├── MainApplication                        (package-private, @SpringBootApplication)
├── DownloadController                     (@RestController @RequestMapping("/download"))
├── ExistingFile
├── FileSystemPointer                  implements FilePointer
├── FileSystemStorage                  implements FileStorage  (@Component @Profile("!test"))
├── FileExamples                 (test)
├── FileStorageStub              (test) implements FileStorage (@Component @Profile("test"))
└── java.io.InputStream
    └── ThrottlingInputStream

«interface» FilePointer
«interface» FileStorage

org.springframework.web.filter.ShallowEtagHeaderFilter
└── Sha512ShallowEtagHeaderFilter     (test)

spock.lang.Specification
└── DownloadControllerSpec            (test)
```

## Member Inventory

### MainApplication
| Member | Signature | Line |
|---|---|---|
| `main` | `public static void main(String[] args)` | 9 |

### DownloadController
| Member | Signature | Line |
|---|---|---|
| `storage` | `private final FileStorage` | 26 |
| ctor | `public DownloadController(FileStorage storage)` | 29 |
| `redirect` | `public ResponseEntity<Resource> redirect(HttpMethod, @PathVariable UUID, @RequestHeader(IF_NONE_MATCH) Optional<String>, @RequestHeader(IF_MODIFIED_SINCE) Optional<Date>)` | 34 |
| `download` | `public ResponseEntity<Resource> download(HttpMethod, @PathVariable UUID, @RequestHeader(IF_NONE_MATCH) Optional<String>, @RequestHeader(IF_MODIFIED_SINCE) Optional<Date>)` | 46 |
| `findExistingFile` | `private Optional<ExistingFile> findExistingFile(HttpMethod, UUID)` | 57 |

### ExistingFile
| Member | Signature | Line |
|---|---|---|
| `log` | `private static final Logger` | 26 |
| `method` / `filePointer` / `uuid` | `private final HttpMethod` / `FilePointer` / `UUID` | 28-30 |
| ctor | `public ExistingFile(HttpMethod, FilePointer, UUID)` | 32 |
| `redirect` | `public ResponseEntity<Resource> redirect(Optional<String>, Optional<Date>)` | 38 |
| `handle` | `public ResponseEntity<Resource> handle(Optional<String>, Optional<Date>)` | 42 |
| `serveWithCaching` | `private ResponseEntity<Resource> serveWithCaching(Optional<String>, Optional<Date>, Function<FilePointer, ResponseEntity<Resource>>)` | 46 |
| `cached` | `private boolean cached(Optional<String>, Optional<Date>)` | 54 |
| `redirectDownload` | `private ResponseEntity<Resource> redirectDownload(FilePointer)` | 65 |
| `serveDownload` | `private ResponseEntity<Resource> serveDownload(FilePointer)` | 77 |
| `resourceToReturn` | `private InputStreamResource resourceToReturn(FilePointer)` | 83 |
| `buildResource` | `private InputStreamResource buildResource(FilePointer)` | 90 |
| `notModified` | `private ResponseEntity<Resource> notModified(FilePointer)` | 97 |
| `response` | `private ResponseEntity<Resource> response(FilePointer, HttpStatus, Resource)` | 102 |
| `toMediaType` | `private org.springframework.http.MediaType toMediaType(com.google.common.net.MediaType)` | 115 |

### FileSystemPointer
| Member | Signature | Line |
|---|---|---|
| `target` / `tag` / `mediaTypeOrNull` | `private final File` / `HashCode` / `MediaType` | 15-17 |
| ctor | `public FileSystemPointer(File target)` | 19 |
| `open` | `public InputStream open()` | 33 |
| `getSize` | `public long getSize()` | 42 |
| `getOriginalName` | `public String getOriginalName()` | 47 |
| `getEtag` | `public String getEtag()` | 52 |
| `getMediaType` | `public Optional<MediaType> getMediaType()` | 57 |
| `toString` | `public String toString()` | 62 |
| `matchesEtag` | `public boolean matchesEtag(String requestEtag)` | 73 |
| `getLastModified` | `public Instant getLastModified()` | 78 |
| `modifiedAfter` | `public boolean modifiedAfter(Instant clientTime)` | 83 |

### FileSystemStorage
| Member | Signature | Line |
|---|---|---|
| `log` | `private static final Logger` | 21 |
| `findFile` | `public Optional<FilePointer> findFile(UUID uuid)` | 24 |

### ThrottlingInputStream
| Member | Signature | Line |
|---|---|---|
| `target` / `maxBytesPerSecond` | `private final InputStream` / `RateLimiter` | 10-11 |
| ctor | `public ThrottlingInputStream(InputStream, RateLimiter)` | 13 |
| `read` | `public int read()` | 19 |
| `read` | `public int read(byte[] b)` | 25 |
| `read` | `public int read(byte[] b, int off, int len)` | 31 |
| `skip` | `public long skip(long n)` | 39 |
| `available` | `public int available()` | 44 |
| `mark` | `public synchronized void mark(int readlimit)` | 49 |
| `reset` | `public synchronized void reset()` | 54 |
| `markSupported` | `public boolean markSupported()` | 59 |
| `close` | `public void close()` | 64 |

### Test types
| Type | Members |
|---|---|
| `FileExamples` | `public static final UUID TXT_FILE_UUID` (9), `public static final FilePointer TXT_FILE` (10), `public static final UUID NOT_FOUND_UUID` (11), `private static FileSystemPointer txtFile()` (13) |
| `FileStorageStub` | `private static final Logger log` (18), `public Optional<FilePointer> findFile(UUID)` (20) |
| `Sha512ShallowEtagHeaderFilter` | `protected String generateETagHeaderValue(byte[] bytes)` (9) |
| `DownloadControllerSpec` | `private MockMvc mockMvc` (27), `private static final String TEXT_FILE` (29), `@Autowired public void setWebApplicationContext(WebApplicationContext)` (32), 11 feature methods (36-169), `private static String toDateHeader(Instant)` (171) |

Related: [interfaces.md](interfaces.md) · [data-models.md](data-models.md) · [api-reference.md](api-reference.md) · [modules.md](modules.md)
