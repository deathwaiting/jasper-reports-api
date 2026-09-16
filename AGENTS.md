# AGENTS.md

Spring Boot 3.3.2 REST service (Java 21, Maven wrapper) that generates JasperReports from JRXML files on disk.

## Commands
- Build/test: `./mvnw test` (use `./mvnw`, not `mvn`). All 11 tests are `@SpringBootTest` integration tests.
- Single test class: `./mvnw test -Dtest=GetJdbcReportTest`
- Dev server: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` — H2 in-memory, security disabled, serves `src/test/resources/test-reports`. Profile `localdev` additionally enables the admin API.
- Docker image (two-step, must run together): `./mvnw clean spring-boot:build-image docker:build` — first builds `:temp`, docker-maven-plugin retags to `${project.version}`.

## Tests
- No external DB needed: embedded H2 is auto-configured (H2 and many JDBC drivers are on the classpath; H2 wins for embedded use).
- Fonts are required at runtime for Jasper even if unused — on minimal systems install `fontconfig`/`ttf-dejavu` before `./mvnw test` (CI installs `freetype-dev fontconfig-dev ttf-dejavu`; Dockerfile installs `fontconfig fonts-dejavu-core`).
- `AdminApiTest` asserts the exact list of 6 `.jrxml` fixtures under `src/test/resources/test-reports` — adding/renaming a fixture file there breaks it.
- `GetJdbcReportTest`/`AdminApiTest` run under the default profile and hit endpoints with HTTP Basic auth `user`/`pass` (`src/test/resources/application-default.properties`). `SecurityOffTest` overrides with the `security-off` profile.
- Passing tests write generated reports to temp files and log the path — these are side artifacts, not assertions.

## Config quirks
- Cache TTL/size keys have a duplicated-prefix typo that is loaded and documented everywhere — use exactly `dev.galal.dev.galal.jasper-rest-server.reports.cache.ttl` / `.size` (see `CacheConfig.java` and `application.properties`). All other keys use the sane `dev.galal.jasper-rest-server.*` prefix (e.g. `reports-dir`, `admin.api.enabled`). Do not "fix" the typo without updating `CacheConfig`, `application.properties`, and README together.
- Admin API is `GET /admin/server/info`, disabled by default; gate with `dev.galal.jasper-rest-server.admin.api.enabled=true`.

## Architecture / API
- `GET /report/<relative-path>.<ext>` serves any JRXML under `reports-dir` (default `/var/reports`). Supported extensions: `pdf`, `docx`, `xlsx`, `html`, `csv`; the `.jrxml` path is derived by swapping the extension, so nested subdirs work. Unknown extension or missing file → 406 with a ProblemDetail whose `detail` is resolved from `messages.properties` (`error.report.*`).
- All query params are passed as report parameters and parsed to Java types by declared parameter class (see `JRParameterParser`). Passing a param not declared in the report throws. `JR_force_compile=true` triggers sub-report recompilation for reports with sub-reports or report books.
- Flow: `ReportController` → `JdbcReportGeneratorService` (compile-or-cache via Caffeine cache `reports`, fill with `JRFiller` + JDBC connection) → format exporter in `ReportHandlers`.
- Compiled `JasperReport`s are cached in-memory (Caffeine); cache is keyed by requested report path. Sub-reports use relative resource resolution (compile-time and via `JRFiller` context), so fixtures reference sibling files.
- Security: default profile uses Spring's in-memory basic auth (`spring.security.user.*`); profiles `dev` and `security-off` disable it (`DevProfileSecurity`).
- Docker `ENTRYPOINT` puts `/var/reports` on the classpath so font jars/other report resources placed there are loadable.
- In the Docker image, install `libfreetype6-dev fontconfig fonts-dejavu-core` — the image fails to generate reports without them.
- `.gitlab-ci.yml` currently runs only a `dockerize` stage (no tests in CI); `config.toml` is a privileged Docker runner config for GitLab.