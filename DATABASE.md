# Database Configuration

This document describes the database configuration for the License Validation Service.

## H2 Embedded Database (Default)

The application uses H2 as an embedded database by default, which is perfect for development and testing.

### Configuration

**Location**: `./data/licenses.db` (created automatically on first run)

**Access**:
- JDBC URL: `jdbc:h2:file:./data/licenses`
- Username: `sa`
- Password: (empty)

### H2 Console

The H2 web console is enabled by default in development mode.

**Access the console**:
1. Start the application: `mvn spring-boot:run`
2. Open browser: http://localhost:8199/h2-console
3. Enter connection details:
   - JDBC URL: `jdbc:h2:file:./data/licenses`
   - Username: `sa`
   - Password: (leave empty)
4. Click "Connect"

**Features**:
- View tables and data
- Execute SQL queries
- Monitor database schema
- Export/import data

**Security Note**: The H2 console is disabled in production mode (`application-prod.yml`).

### Database Schema

The application automatically creates the following table:

```sql
CREATE TABLE license (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_key VARCHAR(255) NOT NULL UNIQUE,
    hwid VARCHAR(255),
    expiration_date DATE,
    active BOOLEAN
);
```

### DDL Mode

- **Development** (`application.yml`): `ddl-auto: update`
  - Automatically updates schema on application start
  - Safe for development, changes are non-destructive

- **Production** (`application-prod.yml`): `ddl-auto: validate`
  - Only validates schema, doesn't modify
  - Requires manual schema migrations

## Production Database Configuration

### Using External Database

To use a different database in production, update `application-prod.yml`:

#### PostgreSQL Example

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/licenses
    driver-class-name: org.postgresql.Driver
    username: license_user
    password: ${DATABASE_PASSWORD}

  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
```

Add PostgreSQL dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

#### MySQL Example

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/licenses?useSSL=false&serverTimezone=UTC
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: license_user
    password: ${DATABASE_PASSWORD}

  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
    hibernate:
      ddl-auto: validate
```

Add MySQL dependency to `pom.xml`:
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

## Environment Variables

Configure database via environment variables:

```bash
# Database connection
export DATABASE_URL=jdbc:postgresql://localhost:5432/licenses
export DATABASE_DRIVER=org.postgresql.Driver
export DATABASE_USERNAME=license_user
export DATABASE_PASSWORD=secure_password

# Run with production profile
java -jar target/lib-validate-license-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

## Data Persistence

### Development
- Data is stored in `./data/` directory
- Survives application restarts
- Can be deleted to reset database

### Backup H2 Database

```bash
# Backup
cp -r data/ data_backup_$(date +%Y%m%d)/

# Restore
cp -r data_backup_20260123/ data/
```

### Migration from H2 to Production DB

1. Export data from H2:
   - Use H2 console: `SCRIPT TO 'backup.sql'`
   - Or use export tool

2. Modify SQL for target database (if needed)

3. Import to production database:
   ```bash
   psql -U license_user -d licenses -f backup.sql
   ```

## Performance Tuning

### Connection Pool (Production)

Add to `application-prod.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
```

### Indexes

For better performance with large datasets:

```sql
-- Index on license_key for faster lookups
CREATE INDEX idx_license_key ON license(license_key);

-- Index on hwid for HWID-based queries
CREATE INDEX idx_hwid ON license(hwid);

-- Index on expiration_date for cleanup queries
CREATE INDEX idx_expiration_date ON license(expiration_date);
```

## Monitoring

### Check Database Size (H2)

```sql
SELECT
    TABLE_NAME,
    ROW_COUNT_ESTIMATE as ROWS
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'PUBLIC';
```

### Active Licenses

```sql
SELECT COUNT(*) FROM license WHERE active = true;
```

### Expired Licenses

```sql
SELECT COUNT(*) FROM license WHERE expiration_date < CURRENT_DATE;
```

## Troubleshooting

### Database Locked

**Problem**: `Database may be already in use: "Locked by another process"`

**Solution**:
1. Stop all running instances of the application
2. Delete `./data/licenses.lock.db` file
3. Restart application

### Schema Mismatch

**Problem**: `Schema validation failed`

**Solution**:
1. Check entity definitions match database schema
2. Use `ddl-auto: update` in development to auto-fix
3. Create manual migration script for production

### Cannot Connect to H2 Console

**Problem**: 403 Forbidden or connection refused

**Solution**:
1. Verify `spring.h2.console.enabled=true`
2. Check URL: http://localhost:8199/h2-console (note the port)
3. Ensure JDBC URL matches: `jdbc:h2:file:./data/licenses`
4. Check SecurityConfig allows `/h2-console/**`

## Best Practices

1. **Never use H2 console in production** - Disable in prod profile
2. **Use environment variables** for sensitive credentials
3. **Regular backups** - Automate database backups
4. **Monitor disk space** - H2 grows with data
5. **Connection pooling** - Configure HikariCP for production
6. **Indexes** - Add indexes for frequently queried fields
7. **Validation mode** - Use `validate` DDL mode in production
8. **Database migrations** - Use Flyway or Liquibase for production

## References

- [H2 Database Documentation](http://www.h2database.com/html/main.html)
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
