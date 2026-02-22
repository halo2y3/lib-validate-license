# Quick Start Guide

Get the License Validation Service running in 2 minutes!

## Prerequisites Check

```bash
# Check Java (required: 25+)
java -version

# Check Maven (required: 3.6+)
mvn -version
```

## 1. Start the Application

### Option A: Using Scripts (Recommended)

**Windows:**
```bash
start.bat
```

**Linux/Mac:**
```bash
./start.sh
```

### Option B: Using Maven

```bash
# Build and run
mvn clean spring-boot:run
```

## 2. Verify It's Running

The application should start in about 5 seconds. You'll see:

```
H2 console available at '/h2-console'
Tomcat started on port(s): 8199 (http)
Started RunServer
```

**Quick Check:**
- Application: http://localhost:8199
- H2 Console: http://localhost:8199/h2-console

## 3. Test the API

### Generate a Token

```bash
curl -X POST http://localhost:8199/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"subject": "test-client"}'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJkaXIi...",
  "type": "Bearer",
  "subject": "test-client"
}
```

**Copy the token value for next steps!**

### Create a License

```bash
# Replace <TOKEN> with the token from previous step
curl -X POST http://localhost:8199/api/license/create \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "licenseKey": "TEST-LICENSE-001",
    "email": "user@example.com",
    "validDays": 365
  }'
```

**Response:**
```json
{
  "id": 1,
  "licenseKey": "TEST-LICENSE-001",
  "email": "user@example.com",
  "expirationDate": "2027-01-23",
  "active": false
}
```

**Note**: In development, email sending is disabled by default. To enable it, configure SMTP settings in `.env` file.

### Activate the License

```bash
curl -X POST http://localhost:8199/api/license/activate \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "licenseKey": "TEST-LICENSE-001",
    "hwid": "MY-COMPUTER-ID-123"
  }'
```

**Response:**
```json
{
  "description": "LICENCIA_OK",
  "expirationDate": "2027-01-23"
}
```

## 4. Access H2 Database Console

Open your browser: http://localhost:8199/h2-console

**Connection Settings:**
- JDBC URL: `jdbc:h2:file:./data/licenses`
- Username: `sa`
- Password: (leave empty)

Click "Connect" to view your database!

## 5. View Your Data

In H2 Console, run:

```sql
SELECT * FROM license;
```

You should see your created license!

## Common Commands

```bash
# Run tests
mvn test

# Generate coverage report
mvn jacoco:report
# View at: target/site/jacoco/index.html

# Build JAR
mvn clean package

# Run JAR directly
java -jar target/lib-validate-license-0.0.1-SNAPSHOT.jar
```

## Troubleshooting

### Port Already in Use

If port 8199 is taken:
```bash
export SERVER_PORT=9000
mvn spring-boot:run
```

### Database Locked

If you see "Database may be already in use":
1. Stop all running instances
2. Delete `./data/licenses.lock.db`
3. Restart

### Token Issues

If getting 401 Unauthorized:
- Ensure you're using `Authorization: Bearer <token>` header
- Generate a new token (they expire after 1 hour by default)
- Check token is not truncated when copying

## What's Next?

- 📖 Read [README.md](README.md) for full documentation
- 💾 See [DATABASE.md](DATABASE.md) for database configuration
- 🔒 Configure `JWE_SECRET_KEY` for production
- 🚀 Deploy to production with `--spring.profiles.active=prod`

## Quick Reference

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/auth/token` | POST | No | Generate token |
| `/api/license/create` | POST | Yes | Create license |
| `/api/license/activate` | POST | Yes | Activate license |
| `/h2-console` | GET | No | Database console |

---

**Need Help?** Check the full [README.md](README.md) or open an issue.

**Ready for Production?** See the Production Checklist in [README.md](README.md#production-checklist).
