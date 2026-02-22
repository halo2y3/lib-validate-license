# Email Troubleshooting Guide

Esta guía te ayudará a resolver problemas comunes de configuración de email en el servicio de validación de licencias.

## Tabla de Contenidos

- [Error: SSL Handshake Failed](#error-ssl-handshake-failed)
- [Error: Authentication Failed](#error-authentication-failed)
- [Error: Connection Timeout](#error-connection-timeout)
- [Gmail: Configuración Específica](#gmail-configuración-específica)
- [Verificar Configuración](#verificar-configuración)
- [Habilitar Logs de Debug](#habilitar-logs-de-debug)

---

## Error: SSL Handshake Failed

### Síntomas

```
Failed to send license creation email to: user@example.com.
Error: Mail server connection failed; nested exception is javax.mail.MessagingException:
Unable to convert connection to SSL (javax.net.ssl.SSLHandshakeException:
No appropriate protocol (protocol is disabled or cipher suites are inappropriate))
```

### Causa

Este error ocurre cuando:
1. Los protocolos TLS antiguos (TLSv1.0, TLSv1.1) están deshabilitados en Java 17
2. La configuración SSL/TLS no está correctamente especificada
3. El servidor SMTP requiere protocolos específicos

### Solución

**Paso 1**: Asegúrate de que tu `application.yml` tenga esta configuración:

```yaml
spring:
  mail:
    host: smtp.gmail.com  # O tu proveedor SMTP
    port: 587
    username: tu-email@gmail.com
    password: tu-contraseña-o-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          ssl:
            protocols: TLSv1.2 TLSv1.3  # IMPORTANTE: Agregar esta línea
            trust: smtp.gmail.com        # IMPORTANTE: Confiar en el host
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
        transport:
          protocol: smtp
        debug: false
```

**Paso 2**: Verifica que las líneas críticas estén presentes:

```yaml
ssl:
  protocols: TLSv1.2 TLSv1.3  # ✅ Fuerza protocolos seguros
  trust: smtp.gmail.com        # ✅ Confía en el servidor SMTP
```

**Paso 3**: Reinicia la aplicación y prueba nuevamente.

---

## Error: Authentication Failed

### Síntomas

```
Authentication failed; nested exception is javax.mail.AuthenticationFailedException:
535-5.7.8 Username and Password not accepted
```

### Causa

1. **Gmail**: Contraseña regular en lugar de App Password
2. Credenciales incorrectas
3. 2FA habilitado sin App Password configurado
4. Less secure app access deshabilitado (deprecado por Gmail)

### Solución para Gmail

**Paso 1**: Habilita 2-Factor Authentication

1. Ve a https://myaccount.google.com/security
2. Habilita "2-Step Verification"

**Paso 2**: Genera un App Password

1. Ve a https://myaccount.google.com/apppasswords
2. Selecciona "Mail" como app y "Other" como dispositivo
3. Dale un nombre descriptivo: "License Service"
4. Click en "Generate"
5. Copia el password de 16 caracteres (formato: xxxx xxxx xxxx xxxx)

**Paso 3**: Actualiza tu configuración

```yaml
spring:
  mail:
    username: tu-email@gmail.com
    password: xxxx xxxx xxxx xxxx  # App Password de 16 caracteres (sin espacios)
```

O usando variables de entorno:

```bash
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=xxxxxxxxxxxxxxxx  # Sin espacios
```

**Paso 4**: Prueba la configuración

```bash
# Reinicia la aplicación
mvn spring-boot:run

# Prueba creando una licencia (envía email)
curl -X POST http://localhost:8199/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"subject": "test"}'

# Usa el token para crear licencia
curl -X POST http://localhost:8199/api/license/create \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"licenseKey": "TEST-123", "email": "destino@example.com", "validDays": 365}'
```

---

## Error: Connection Timeout

### Síntomas

```
Failed to send license creation email to: user@example.com.
Error: Mail server connection failed; nested exception is
javax.mail.MessagingException: Could not connect to SMTP host: smtp.gmail.com, port: 587
```

### Causa

1. Firewall bloqueando puerto SMTP
2. Puerto incorrecto
3. Host SMTP incorrecto
4. Problema de red

### Solución

**Paso 1**: Verifica la conectividad

```bash
# Windows
telnet smtp.gmail.com 587

# Si telnet no está disponible
Test-NetConnection smtp.gmail.com -Port 587

# Linux/Mac
nc -zv smtp.gmail.com 587
```

**Paso 2**: Prueba diferentes puertos

| Puerto | Descripción | Configuración |
|--------|-------------|---------------|
| 587 | STARTTLS (Recomendado) | `starttls.enable: true` |
| 465 | SSL/TLS (Legacy) | `ssl.enable: true`, `starttls.enable: false` |
| 25 | No seguro (No recomendado) | Sin SSL/TLS |

**Configuración para puerto 465 (SSL):**

```yaml
spring:
  mail:
    port: 465
    properties:
      mail:
        smtp:
          auth: true
          ssl:
            enable: true
            protocols: TLSv1.2 TLSv1.3
            trust: smtp.gmail.com
          starttls:
            enable: false  # Deshabilitado para SSL puro
```

**Paso 3**: Verifica el firewall

```bash
# Windows: Verifica reglas de firewall
netsh advfirewall firewall show rule name=all | findstr 587

# Agrega regla si es necesario (como administrador)
netsh advfirewall firewall add rule name="SMTP Port 587" dir=out action=allow protocol=TCP localport=587
```

---

## Gmail: Configuración Específica

### Configuración Completa Recomendada

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: tu-email@gmail.com
    password: xxxx xxxx xxxx xxxx  # App Password (16 chars)
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          ssl:
            protocols: TLSv1.2 TLSv1.3
            trust: smtp.gmail.com
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
        transport:
          protocol: smtp
        debug: false

email:
  from: tu-email@gmail.com
  enabled: true
```

### Requisitos Previos

✅ **Obligatorio**:
1. Cuenta de Gmail activa
2. 2-Factor Authentication habilitado
3. App Password generado

❌ **NO funciona**:
- Contraseña regular de Gmail
- "Less secure app access" (deprecado)
- Cuentas corporativas sin permisos SMTP

### Verificación

```bash
# 1. Verifica que 2FA esté habilitado
# Ve a: https://myaccount.google.com/security

# 2. Genera App Password
# Ve a: https://myaccount.google.com/apppasswords

# 3. Prueba con curl
curl -X POST http://localhost:8199/api/license/create \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"licenseKey": "TEST-001", "email": "destino@example.com", "validDays": 30}'

# 4. Verifica logs
tail -f logs/application.log | grep -i mail
```

---

## Verificar Configuración

### Checklist de Configuración

- [ ] **MAIL_HOST** está configurado correctamente
- [ ] **MAIL_PORT** es 587 (STARTTLS) o 465 (SSL)
- [ ] **MAIL_USERNAME** es el email completo
- [ ] **MAIL_PASSWORD** es App Password (Gmail) o contraseña correcta
- [ ] **EMAIL_FROM** está configurado
- [ ] **EMAIL_ENABLED** es true
- [ ] Propiedad **ssl.protocols** incluye TLSv1.2 y TLSv1.3
- [ ] Propiedad **ssl.trust** coincide con MAIL_HOST
- [ ] 2FA habilitado (si usas Gmail)
- [ ] App Password generado (si usas Gmail)

### Comando de Verificación

```bash
# Muestra configuración actual (oculta password)
curl -X GET http://localhost:8199/actuator/configprops | grep mail

# O revisa el archivo directamente
cat src/main/resources/application.yml | grep -A 20 "mail:"
```

---

## Habilitar Logs de Debug

### Paso 1: Habilita debug de SMTP

```yaml
spring:
  mail:
    properties:
      mail:
        debug: true  # Habilita logs detallados
```

### Paso 2: Configura nivel de log

`src/main/resources/log4j2.xml`:

```xml
<Logger name="org.springframework.mail" level="DEBUG"/>
<Logger name="javax.mail" level="DEBUG"/>
<Logger name="com.sun.mail" level="DEBUG"/>
```

### Paso 3: Reinicia y revisa logs

```bash
# Inicia aplicación
mvn spring-boot:run

# En otra terminal, sigue los logs
tail -f logs/application.log

# Prueba enviando un email
curl -X POST http://localhost:8199/api/license/create \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"licenseKey": "DEBUG-001", "email": "test@example.com", "validDays": 30}'
```

### Qué Buscar en los Logs

✅ **Conexión Exitosa**:
```
DEBUG: setDebug: JavaMail version 1.6.2
DEBUG: getProvider() returning javax.mail.Provider[TRANSPORT,smtp,com.sun.mail.smtp.SMTPTransport,Oracle]
DEBUG SMTP: useEhlo true, useAuth true
DEBUG SMTP: trying to connect to host "smtp.gmail.com", port 587, isSSL false
220 smtp.gmail.com ESMTP ...
DEBUG SMTP: connected to host "smtp.gmail.com", port: 587
```

❌ **SSL Handshake Error**:
```
javax.net.ssl.SSLHandshakeException: No appropriate protocol (protocol is disabled or cipher suites are inappropriate)
```
👉 **Solución**: Agrega `ssl.protocols: TLSv1.2 TLSv1.3`

❌ **Authentication Error**:
```
535-5.7.8 Username and Password not accepted
```
👉 **Solución**: Usa App Password en lugar de contraseña regular

❌ **Connection Timeout**:
```
Could not connect to SMTP host: smtp.gmail.com, port: 587
```
👉 **Solución**: Verifica firewall y conectividad de red

---

## Proveedores Alternativos

Si Gmail no funciona, considera estos proveedores:

### SendGrid (Recomendado para producción)

```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey
    password: <tu-sendgrid-api-key>
```

### AWS SES

```yaml
spring:
  mail:
    host: email-smtp.us-east-1.amazonaws.com
    port: 587
    username: <aws-smtp-username>
    password: <aws-smtp-password>
```

### Mailgun

```yaml
spring:
  mail:
    host: smtp.mailgun.org
    port: 587
    username: postmaster@<tu-dominio>.mailgun.org
    password: <mailgun-smtp-password>
```

Ver `application-email-examples.yml` para más ejemplos.

---

## Contacto y Soporte

Si después de seguir esta guía sigues teniendo problemas:

1. **Revisa logs**: `logs/application.log`
2. **Habilita debug**: `mail.debug: true`
3. **Verifica configuración**: Revisa todas las propiedades SMTP
4. **Prueba conectividad**: `telnet smtp.gmail.com 587`
5. **Contacta soporte**: Incluye logs y configuración (oculta contraseñas)

---

**Última actualización**: 2026-01-29
**Versión**: 1.0.0
