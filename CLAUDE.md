# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.4.4 license validation library that provides REST API endpoints for creating and activating software licenses. The service validates licenses based on hardware IDs (HWID) and expiration dates, ensuring that licenses are tied to specific machines.

**Key Technologies:**
- Java 25 (LTS)
- Spring Boot 3.4.4
- Spring Security 6.4.x (with JWE authentication)
- Spring Data JPA
- Jakarta EE 10 (migrated from javax.* to jakarta.*)
- Maven
- Lombok
- Nimbus JOSE+JWT (JWE encryption)
- Log4j2 (custom logging configuration)
- JaCoCo (code coverage)
- MailerSend REST API (HTML email notifications)
- AWS SDK v2 S3 (`software.amazon.awssdk:s3` BOM 2.26.26) — used for Cloudflare R2 backups

## Build and Development Commands

### Building the Project
```bash
mvn clean install
```

### Running Tests
```bash
mvn test
```

### Running Tests with Coverage Report
```bash
mvn clean test jacoco:report
```
Coverage reports are generated in `target/site/jacoco/index.html`

**Current Test Coverage: ~90%**
- 47 tests (unit + integration)
- All tests passing successfully
- Controllers: 100% coverage
- Security layer: 97% coverage
- Models: 94% coverage

### Running the Application
```bash
mvn spring-boot:run
```

The server runs on port 8199 by default (configurable via `SERVER_PORT` environment variable).

### Packaging
```bash
mvn clean package
```

## Architecture

### Security Architecture

All `/api/license/**` endpoints are protected with **JWE (JSON Web Encryption)** authentication:

- **Token Generation**: Use `POST /api/auth/token` to generate a JWE token (no auth required)
- **Token Usage**: Include token in `Authorization: Bearer <token>` header for all license API calls
- **Encryption**: Uses Nimbus JOSE+JWT with Direct Encryption (DIR) and AES-256-GCM
- **Validation**: Filter (`JweAuthenticationFilter`) validates tokens on every request
- **Configuration**: Secret key, expiration, and issuer configurable via `application.yml`

**Security Flow:**
1. Client requests token from `/api/auth/token` with a subject (client identifier)
2. Server generates encrypted JWE token with expiration
3. Client includes token in Authorization header: `Bearer <token>`
4. Server validates token signature, expiration, and issuer before processing request
5. Invalid/expired tokens receive `401 Unauthorized` response

### License Validation Flow

The application implements a two-step license activation process (both require JWE authentication):

1. **License Creation** (`POST /api/license/create`):
   - Requires valid JWE token in Authorization header
   - Creates a new license with a unique license key
   - Sets expiration date based on `validDays` parameter
   - Initially created in inactive state (`active=false`)
   - Validates that license keys are unique

2. **License Activation** (`POST /api/license/activate`):
   - Requires valid JWE token in Authorization header
   - First-time activation: Binds license to specific hardware ID (HWID)
   - Subsequent activations: Validates that HWID matches the bound hardware
   - Validates expiration date
   - Returns `403 Forbidden` for invalid/expired/mismatched licenses

### Key Components

**Controller Layer** (`co.com.validate.license.controller`):
- `LicenseRestController`: REST controller exposing `/api/license/*` endpoints (requires JWE auth)
- `AuthController`: REST controller exposing `/api/auth/token` for JWE token generation (no auth required)
- CORS configured in `SecurityConfig` to allow requests from any origin

**Security Layer** (`co.com.validate.license.security`):
- `JweService`: Core service for JWE token generation and validation
- `JweProperties`: Configuration properties for JWE (secret key, expiration, issuer)
- `JweAuthenticationFilter`: Servlet filter that intercepts requests and validates JWE tokens
- `JweAuthenticationEntryPoint`: Handles unauthorized access (returns 401 with error message)
- `SecurityConfig`: Spring Security configuration (stateless, CSRF disabled, endpoints protection)

**Repository Layer** (`co.com.validate.license.repository`):
- `LicenseRepository`: Spring Data JPA repository with custom query methods
  - `findByLicenseKey(String)`: Finds license by key
  - `existsByLicenseKey(String)`: Checks for duplicate keys

**Model Layer** (`co.com.validate.license.model`):
- `License`: JPA entity with fields: `id`, `licenseKey`, `hwid`, `expirationDate`, `active`
- `CreateLicenseRequest`: Request DTO for license creation
- `LicenseRequest`: Request DTO for license activation (contains `licenseKey` and `hwid`)
- `LicenseResponse`: Response DTO with `description` and `expirationDate`

**Exception Handling** (`co.com.validate.license.exception`):
- `ResponseExceptionHandler`: Global `@RestControllerAdvice` handler
- Handles validation errors, data access errors, and custom `ZMessManager` exceptions
- Returns consistent `ExceptionResponse` format with timestamp, message, and details

**Backup Layer** (`co.com.validate.license.config` / `co.com.validate.license.service`):
- `BackupProperties`: `@ConfigurationProperties(prefix = "backup")` — enabled, cron, localDir, maxFiles, inner class `R2` (accountId, accessKeyId, secretAccessKey, bucketName)
- `R2Config`: `@ConditionalOnProperty(backup.enabled=true)` — creates `S3Client` bean with Cloudflare R2 endpoint override (`https://<accountId>.r2.cloudflarestorage.com`) and static credentials
- `BackupService`: Scheduled service (`@Scheduled`) that creates an H2 snapshot via `BACKUP TO`, uploads it to R2 with `putObject`, cleans old backups with `listObjectsV2` + `deleteObject` (keeps `maxFiles` most recent)
- `BackupStartupRunner`: `@ConditionalOnProperty(backup.run-on-startup=true)` — executes `performBackup()` on application startup

### Configuration

**Application Configuration** (`src/main/resources/application.yml`):
- Server port: `${SERVER_PORT:8199}` (defaults to 8199)
- Application name: `lib-validate-license`

**JWE Security Configuration**:
- `security.jwe.secret-key`: Encryption key (env: `JWE_SECRET_KEY`, minimum 32 chars for AES-256)
- `security.jwe.expiration-seconds`: Token lifetime in seconds (env: `JWE_EXPIRATION_SECONDS`, default: 3600)
- `security.jwe.issuer`: Token issuer identifier (env: `JWE_ISSUER`, default: lib-validate-license)
- **IMPORTANT**: Change the default secret key in production environments

**Logging Configuration**:
- Uses Log4j2 (Spring Boot default logging excluded in pom.xml)
- Configuration in `src/main/resources/log4j2.xml`

**Database**:
- H2 embedded database configured by default (file-based: `./data/licenses.db`)
- H2 Console enabled at `/h2-console` (can be disabled via `H2_CONSOLE_ENABLED=false`)
- DDL mode: `update` in development, `validate` in production
- Production profile available: `application-prod.yml`
- Supports migration to PostgreSQL, MySQL, or other RDBMS

**Email Configuration**:
- Uses **MailerSend REST API** (no SMTP — `spring-boot-starter-mail` removed)
- Endpoint: `POST https://api.mailersend.com/v1/email` with `Authorization: Bearer <token>`
- `RestClient` bean configured in `MailerSendConfig` (`co.com.validate.license.config`)
- HTML templates: `LicenseCreate.html` (creation) and `LicenseExpiration.html` (expiration warning)
- Environment variables:
  - `MAILERSEND_API_TOKEN`: API token from MailerSend dashboard (required in production)
  - `MAILERSEND_API_URL`: REST endpoint (default: `https://api.mailersend.com/v1/email`)
  - `EMAIL_FROM`: Sender address — must be a verified domain in MailerSend
  - `EMAIL_ENABLED`: Enable/disable email notifications (default: `true`)

**Backup Configuration** (Cloudflare R2):
- Backup activado/desactivado: `BACKUP_ENABLED` (default: `true`)
- Ejecutar al iniciar: `BACKUP_RUN_ON_STARTUP` (default: `true`)
- Programación: `BACKUP_CRON` — expresión cron (default: `0 0 2 * * ?` → diario a las 2 AM)
- Directorio temporal local: `BACKUP_LOCAL_DIR` (default: `/app/data/backups`)
- Máximo de backups a retener en R2: `BACKUP_MAX_FILES` (default: `7`)
- Credenciales R2:
  - `R2_ACCOUNT_ID`: Cloudflare Account ID (visible en la URL del dashboard)
  - `R2_ACCESS_KEY_ID`: Access Key generada en R2 → Manage R2 API Tokens
  - `R2_SECRET_ACCESS_KEY`: Secret correspondiente al Access Key
  - `R2_BUCKET_NAME`: Nombre del bucket (default: `licenses-backup`)
- El endpoint S3 se construye automáticamente: `https://<R2_ACCOUNT_ID>.r2.cloudflarestorage.com`
- Los backups de R2 son **best-effort**: un fallo en la subida no interrumpe la operación
- Nombre de archivo: `licenses-yyyyMMdd-HHmmss.zip` (snapshot H2 comprimido)

## JWE Token Usage Examples

### 1. Generate a JWE Token
```bash
curl -X POST http://localhost:8199/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"subject": "client-app-001"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIn0...",
  "type": "Bearer",
  "subject": "client-app-001"
}
```

### 2. Use Token to Create License
```bash
curl -X POST http://localhost:8199/api/license/create \
  -H "Authorization: Bearer eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIn0..." \
  -H "Content-Type: application/json" \
  -d '{"licenseKey": "ABC-123-XYZ", "validDays": 365}'
```

### 3. Use Token to Activate License
```bash
curl -X POST http://localhost:8199/api/license/activate \
  -H "Authorization: Bearer eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIn0..." \
  -H "Content-Type: application/json" \
  -d '{"licenseKey": "ABC-123-XYZ", "hwid": "HARDWARE-ID-12345"}'
```

## Important Notes

- **JWE Security**: All license endpoints require valid JWE token with unexpired signature
- **Secret Key**: MUST be changed in production (minimum 32 characters for AES-256-GCM)
- **Token Expiration**: Default 1 hour, configurable via `security.jwe.expiration-seconds`
- **Stateless Authentication**: No session storage, each request validated independently
- **HWID Binding**: Once a license is activated with a specific HWID, it cannot be used on a different machine
- **First Activation**: The first activation sets `hwid` and `active=true`
- **CORS**: Configured to allow all origins - consider restricting in production
- **Database**: H2 embedded database by default; supports migration to PostgreSQL/MySQL
- **Lombok**: Extensive use of Lombok annotations (`@Getter`, `@Setter`, `@Slf4j`, `@Generated`)
- **Email**: Non-blocking — email failures are logged but do not interrupt license operations
- **Backup**: Non-blocking — R2 upload/cleanup failures are logged but do not interrupt the backup flow; the H2 snapshot is always attempted first
- **Backup retention**: Only the `maxFiles` most recent backups are kept in R2; older ones are deleted automatically after each successful run

## Testing

### Test Structure

**Unit Tests** (`src/test/java/**/*Test.java`):
- `JweServiceTest`: Tests JWE token generation, validation, and expiration
- `JweAuthenticationFilterTest`: Tests authentication filter with mock requests
- `AuthControllerTest`: Tests token generation endpoint
- `LicenseRestControllerTest`: Tests license creation and activation endpoints
- `EmailServiceTest`: Tests MailerSend REST calls using `@Mock(answer = Answers.RETURNS_DEEP_STUBS) RestClient`
- `BackupServiceTest`: Tests backup scheduling with mocked `S3Client` and `JdbcTemplate`
  - Uses `doAnswer` on `jdbcTemplate.execute` to create the backup file on disk (required for `RequestBody.fromFile`)
  - Covers: disabled backup, successful upload, JDBC failure, R2 upload failure, old backup deletion

**Integration Tests** (`src/test/java/integration/*Test.java`):
- `LicenseIntegrationTest`: End-to-end tests covering complete workflows
  - Full flow: token generation → license creation → activation
  - Unauthorized access scenarios
  - Duplicate license key validation
  - License reactivation with same/different HWID

### Test Configuration

- **Test Database**: H2 in-memory database (configured in `application-test.yml`)
- **Test Profiles**: Uses `@ActiveProfiles("test")` to load test-specific configuration
- **JWE Secret**: Test secret key is exactly 32 characters for AES-256-GCM encryption
- **Security**: Spring Security Test provides `@WithMockUser` for authenticated contexts
- **MailerSend**: Test uses `api-token: test-token` — no real HTTP calls are made
- **Backup**: Test profile sets `backup.enabled: false`; `BackupServiceTest` uses `@TempDir` and `@Mock S3Client` — no real R2 calls are made

### Running Specific Tests

```bash
# Run a single test class
mvn test -Dtest=JweServiceTest

# Run a specific test method
mvn test -Dtest=JweServiceTest#testGenerateToken_Success

# Run all integration tests
mvn test -Dtest=*IntegrationTest
```

## Maven Plugins

- **spring-boot-maven-plugin**: Enables `mvn spring-boot:run` and creates executable JAR
- **maven-compiler-plugin**: Configured with explicit `annotationProcessorPaths` for Lombok and `spring-boot-configuration-processor` (required for Java 25 annotation processing)
- **maven-surefire-plugin**: Configured with `-Dnet.bytebuddy.experimental=true` to allow Mockito to run on Java 25 (Byte Buddy experimental mode)
- **jacoco-maven-plugin** (v0.8.14): Generates code coverage reports during `prepare-package` phase — 0.8.14+ required for Java 25 class file support
- **spring-security-test**: Provides security testing utilities and mock authentication

## Code Style Conventions

- Package structure follows reverse domain notation: `co.com.validate.license`
- Entities use Lombok for boilerplate reduction
- Controllers use constructor injection with `@Autowired`
- Exception handling returns appropriate HTTP status codes (400, 403, 404, 500)
- Spanish language used in error messages and some comments
