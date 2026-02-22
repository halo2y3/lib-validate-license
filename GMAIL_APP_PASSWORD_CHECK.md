# Gmail App Password Verification Checklist

## Current Configuration Analysis

Your `application.yml` currently has:
- **Username**: ``
- **Password**: `` ⚠️ **This appears to be a regular password**

## Problem Identification

The SSL/TLS handshake error you're experiencing is caused by using a **regular Gmail password** instead of an **App Password**.

Gmail requires **App Passwords** for SMTP authentication when 2-Factor Authentication is enabled. Regular passwords are no longer accepted for third-party applications.

## Immediate Steps to Fix

### Step 1: Verify 2FA is Enabled

1. Go to: https://myaccount.google.com/security
2. Look for "2-Step Verification" section
3. **If NOT enabled**: You MUST enable it first before creating an App Password

### Step 2: Generate Gmail App Password

1. Go to: https://myaccount.google.com/apppasswords
2. You may be asked to verify your identity
3. Select:
   - **App**: Choose "Mail" or "Other (Custom name)"
   - **Device**: Choose "Other (Custom name)"
   - **Name**: Enter "License Validation Service"
4. Click **"GENERATE"**
5. You will see a **16-character password** like: `abcd efgh ijkl mnop`

### Step 3: Update Configuration

**Option A: Using Environment Variables (RECOMMENDED for security)**

Set these environment variables:
```bash
# Windows CMD
set MAIL_USERNAME=
set MAIL_PASSWORD=abcdefghijklmnop

# Windows PowerShell
$env:MAIL_USERNAME=""
$env:MAIL_PASSWORD="abcdefghijklmnop"

# Linux/Mac
export MAIL_USERNAME=
export MAIL_PASSWORD=abcdefghijklmnop
```

Then run:
```bash
mvn spring-boot:run
```

**Option B: Update application.yml (NOT recommended - exposes credentials)**

Edit `src/main/resources/application.yml`:
```yaml
spring:
  mail:
    username: 
    password: abcdefghijklmnop  # 16-character App Password WITHOUT SPACES
```

⚠️ **IMPORTANT**: Remove spaces from the App Password. If Gmail shows `abcd efgh ijkl mnop`, write it as `abcdefghijklmnop`

### Step 4: Test the Configuration

1. **Restart the application** (if it was running):
   ```bash
   # Stop current instance (Ctrl+C if running in terminal)
   mvn spring-boot:run
   ```

2. **Generate a JWE token**:
   ```bash
   curl -X POST http://localhost:8199/api/auth/token \
     -H "Content-Type: application/json" \
     -d "{\"subject\": \"test-client\"}"
   ```

3. **Create a license** (replace `<TOKEN>` with actual token):
   ```bash
   curl -X POST http://localhost:8199/api/license/create \
     -H "Authorization: Bearer <TOKEN>" \
     -H "Content-Type: application/json" \
     -d "{\"licenseKey\": \"TEST-GMAIL-001\", \"email\": \"\", \"validDays\": 30}"
   ```

4. **Check the logs** for detailed SMTP communication:
   ```bash
   tail -f logs/application.log
   ```

   Look for these success indicators:
   ```
   DEBUG SMTP: trying to connect to host "smtp.gmail.com", port 587
   DEBUG SMTP: connected to host "smtp.gmail.com", port: 587
   220 smtp.gmail.com ESMTP ...
   DEBUG SMTP: EHLO ...
   250-smtp.gmail.com at your service
   DEBUG SMTP: use8bit false
   DEBUG SMTP: Attempt to authenticate using mechanisms: LOGIN PLAIN DIGEST-MD5 NTLM XOAUTH2
   DEBUG SMTP: AUTH LOGIN command trace suppressed
   235 2.7.0 Accepted
   ```

## Common Errors After Fix

### Error: "Username and Password not accepted"
- **Cause**: Wrong App Password or 2FA not enabled
- **Solution**: Generate a new App Password, ensure 2FA is enabled

### Error: "Invalid credentials" or "534-5.7.9"
- **Cause**: Using regular password instead of App Password
- **Solution**: Follow Step 2 above to generate App Password

### Error: Still getting SSL handshake error
- **Cause**: Application not restarted after configuration change
- **Solution**: Stop application completely and restart with `mvn spring-boot:run`

## Alternative Configuration (If Port 587 Doesn't Work)

Try using port 465 with full SSL:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 465
    username: 
    password: abcdefghijklmnop  # App Password
    properties:
      mail:
        smtp:
          auth: true
          ssl:
            enable: true
            protocols: TLSv1.2 TLSv1.3
            trust: smtp.gmail.com
          starttls:
            enable: false  # Disabled for full SSL
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
        transport:
          protocol: smtp
        debug: true
```

## Verification Checklist

Before contacting support, verify:

- [ ] 2FA is enabled on Gmail account
- [ ] App Password generated (16 characters)
- [ ] App Password copied WITHOUT spaces
- [ ] Configuration updated with App Password
- [ ] Application fully restarted
- [ ] Firewall allows port 587 (or 465)
- [ ] Debug logging enabled (`mail.debug: true`)
- [ ] Logs checked for actual error details

## Security Reminder

🔒 **NEVER**:
- Commit App Passwords to version control
- Share your App Password
- Use regular Gmail password for SMTP

✅ **ALWAYS**:
- Use environment variables for credentials
- Generate unique App Passwords per application
- Revoke unused App Passwords at https://myaccount.google.com/apppasswords

## Need More Help?

1. Check logs with debug enabled: `tail -f logs/application.log`
2. Review `GMAIL_SETUP_GUIDE.md` for detailed step-by-step instructions
3. Review `EMAIL_TROUBLESHOOTING.md` for comprehensive troubleshooting
4. Test connectivity: `telnet smtp.gmail.com 587` (should connect)

---

**Last Updated**: 2026-01-29
