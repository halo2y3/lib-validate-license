# Guía de Configuración de Gmail para el Servicio de Licencias

Esta guía paso a paso te ayudará a configurar Gmail para enviar emails desde el servicio de validación de licencias.

## ⚠️ Importante

Gmail requiere **App Passwords** (contraseñas de aplicación) para aplicaciones externas cuando tienes la verificación en dos pasos (2FA) habilitada. **NO uses tu contraseña regular de Gmail**.

## 📋 Requisitos Previos

- ✅ Cuenta de Gmail activa
- ✅ Acceso a la configuración de seguridad de Google
- ✅ Navegador web

## 🔐 Paso 1: Habilitar la Verificación en Dos Pasos (2FA)

1. **Abre tu navegador** y ve a:
   ```
   https://myaccount.google.com/security
   ```

2. **Inicia sesión** con tu cuenta de Gmail si aún no lo has hecho

3. **Busca la sección "Cómo accedes a Google"**

4. **Haz clic en "Verificación en dos pasos"**

5. **Sigue las instrucciones** para habilitar 2FA:
   - Verifica tu número de teléfono
   - Elige el método de verificación (SMS, llamada, o Google Authenticator)
   - Completa el proceso de configuración

6. **Verifica que esté habilitado**:
   - Deberías ver "Activado" en la sección de Verificación en dos pasos

## 🔑 Paso 2: Generar un App Password

Una vez que 2FA esté habilitado:

1. **Ve a la página de App Passwords**:
   ```
   https://myaccount.google.com/apppasswords
   ```

   O:
   - Ve a https://myaccount.google.com/security
   - Desplázate hasta "Cómo accedes a Google"
   - Haz clic en "Contraseñas de aplicaciones"

2. **Es posible que te pidan verificar tu identidad** nuevamente

3. **Selecciona la aplicación y el dispositivo**:
   - En "Selecciona la aplicación": Elige **Correo** o **Otra**
   - En "Selecciona el dispositivo": Elige **Otro (Nombre personalizado)**
   - Escribe un nombre descriptivo: `License Validation Service`

4. **Haz clic en "GENERAR"**

5. **Copia el App Password**:
   - Verás un password de 16 caracteres en formato: `xxxx xxxx xxxx xxxx`
   - **COPIA ESTE PASSWORD** (lo necesitarás en el siguiente paso)
   - **IMPORTANTE**: Este password solo se muestra UNA VEZ

6. **Haz clic en "LISTO"**

## ⚙️ Paso 3: Configurar la Aplicación

### Opción A: Usando Variables de Entorno (Recomendado)

Crea un archivo `.env` en la raíz del proyecto o configura las variables de entorno del sistema:

```bash
# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tu-email@gmail.com
MAIL_PASSWORD=xxxxxxxxxxxx  # App Password SIN ESPACIOS
EMAIL_FROM=tu-email@gmail.com
EMAIL_ENABLED=true
MAIL_DEBUG=false
```

**IMPORTANTE**:
- Elimina los espacios del App Password
- Si el password es `xxxx xxxx xxxx xxxx`, escríbelo como `xxxxxxxxxxxxxxxx`

### Opción B: Modificando application.yml

Edita `src/main/resources/application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: tu-email@gmail.com
    password: xxxxxxxxxxxxxxxx  # App Password (16 caracteres, sin espacios)
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

**⚠️ ADVERTENCIA**: No subas este archivo a un repositorio público con credenciales reales.

## 🧪 Paso 4: Probar la Configuración

### 1. Inicia la aplicación

```bash
# Windows (CMD)
set JAVA_HOME=C:\Program Files\Java\jdk-17
mvn spring-boot:run

# Windows (PowerShell)
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
mvn spring-boot:run

# Linux/Mac
export JAVA_HOME=/path/to/jdk-17
mvn spring-boot:run
```

### 2. Genera un token de autenticación

```bash
curl -X POST http://localhost:8199/api/auth/token \
  -H "Content-Type: application/json" \
  -d "{\"subject\": \"test-client\"}"
```

Respuesta esperada:
```json
{
  "token": "eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIn0...",
  "type": "Bearer",
  "subject": "test-client"
}
```

**Copia el valor de `token`** para el siguiente paso.

### 3. Crea una licencia (esto enviará un email)

```bash
curl -X POST http://localhost:8199/api/license/create \
  -H "Authorization: Bearer TU_TOKEN_AQUI" \
  -H "Content-Type: application/json" \
  -d "{\"licenseKey\": \"TEST-001\", \"email\": \"tu-email-destino@example.com\", \"validDays\": 30}"
```

Reemplaza:
- `TU_TOKEN_AQUI` con el token del paso anterior
- `tu-email-destino@example.com` con el email donde quieres recibir la notificación

### 4. Verifica el resultado

✅ **Éxito**:
```json
{
  "id": 1,
  "licenseKey": "TEST-001",
  "email": "tu-email-destino@example.com",
  "expirationDate": "2026-02-28T...",
  "active": false,
  "hwid": null
}
```

**Y deberías recibir un email** en tu bandeja de entrada con:
- Asunto: "License Created Successfully - TEST-001"
- Contenido HTML con el logo y detalles de la licencia

❌ **Error**:
Si ves un error, revisa los logs:
```bash
tail -f logs/application.log
```

## ❓ Problemas Comunes

### "Username and Password not accepted"

**Causa**: Estás usando la contraseña regular en lugar del App Password

**Solución**:
1. Verifica que 2FA esté habilitado
2. Genera un nuevo App Password
3. Usa el App Password de 16 caracteres (sin espacios)
4. NO uses tu contraseña regular de Gmail

### "No appropriate protocol"

**Causa**: Configuración SSL/TLS incorrecta

**Solución**:
Asegúrate de que tu `application.yml` tenga:
```yaml
ssl:
  protocols: TLSv1.2 TLSv1.3
  trust: smtp.gmail.com
```

### "Mail server connection failed"

**Causa**: Puerto bloqueado o configuración de red

**Solución**:
1. Verifica conectividad:
   ```bash
   telnet smtp.gmail.com 587
   ```
2. Verifica que el puerto 587 no esté bloqueado por firewall
3. Intenta con puerto 465 (SSL) si 587 no funciona

### "Authentication failed" después de cambiar contraseña de Gmail

**Causa**: El App Password sigue siendo válido aunque cambies tu contraseña

**Solución**:
1. Si cambiaste tu contraseña de Gmail, el App Password NO se invalida
2. Si revocaste el App Password, genera uno nuevo
3. Ve a https://myaccount.google.com/apppasswords
4. Genera un nuevo App Password

## 🔒 Seguridad

### Mejores Prácticas

✅ **SÍ hacer**:
- Usa variables de entorno para credenciales
- Genera un App Password único para esta aplicación
- Revoca App Passwords que ya no uses
- Mantén el App Password seguro (no lo compartas)
- Usa diferentes App Passwords para diferentes aplicaciones

❌ **NO hacer**:
- Usar tu contraseña regular de Gmail
- Compartir tu App Password
- Subir credenciales a repositorios públicos
- Reutilizar el mismo App Password en múltiples aplicaciones
- Deshabilitar 2FA

### Revocar un App Password

Si necesitas revocar un App Password:

1. Ve a https://myaccount.google.com/apppasswords
2. Encuentra el App Password en la lista
3. Haz clic en el ícono de eliminación (🗑️)
4. Confirma la revocación

## 📧 Límites de Gmail

Gmail tiene límites de envío para prevenir spam:

| Tipo de Cuenta | Límite Diario | Límite por Conexión |
|----------------|---------------|---------------------|
| Gmail Gratuito | 500 emails/día | 100 emails |
| Google Workspace | 2,000 emails/día | 100 emails |

**Para producción con alto volumen**, considera usar:
- SendGrid
- AWS SES
- Mailgun
- Mailchimp Transactional

Ver `application-email-examples.yml` para configuraciones alternativas.

## 📚 Recursos Adicionales

- **App Passwords**: https://support.google.com/accounts/answer/185833
- **2-Step Verification**: https://support.google.com/accounts/answer/185839
- **Gmail SMTP Settings**: https://support.google.com/a/answer/176600
- **Troubleshooting Guide**: Ver `EMAIL_TROUBLESHOOTING.md`

## ✅ Checklist Final

Antes de usar en producción, verifica:

- [ ] 2FA habilitado en tu cuenta de Gmail
- [ ] App Password generado (16 caracteres)
- [ ] Variables de entorno configuradas (NO en application.yml)
- [ ] Configuración SSL/TLS correcta (`protocols: TLSv1.2 TLSv1.3`)
- [ ] Propiedad `ssl.trust` coincide con `mail.host`
- [ ] Email de prueba enviado exitosamente
- [ ] Email recibido en bandeja de entrada (revisa spam)
- [ ] Logs sin errores de autenticación o SSL
- [ ] App Password seguro (no compartido, no en repositorio)

---

**¿Necesitas ayuda?** Consulta `EMAIL_TROUBLESHOOTING.md` para solución de problemas detallada.

**Última actualización**: 2026-01-29
