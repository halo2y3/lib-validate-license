# Postman Collection Guide - License Validation Service

Este documento describe cómo usar la colección de Postman para probar el servicio de validación de licencias.

## Tabla de Contenidos

- [Importar la Colección](#importar-la-colección)
- [Configurar el Entorno](#configurar-el-entorno)
- [Estructura de la Colección](#estructura-de-la-colección)
- [Flujo de Trabajo Básico](#flujo-de-trabajo-básico)
- [Casos de Prueba](#casos-de-prueba)
- [Variables de Entorno](#variables-de-entorno)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Troubleshooting](#troubleshooting)

## Importar la Colección

### Opción 1: Desde archivo local

1. Abre Postman
2. Click en **Import** (botón superior izquierdo)
3. Selecciona el archivo `postman_collection.json`
4. Click en **Import**

### Opción 2: Desde URL

```bash
# Si el archivo está en un repositorio Git
https://raw.githubusercontent.com/your-repo/lib-validate-license/main/postman_collection.json
```

## Configurar el Entorno

### Crear un nuevo entorno en Postman

1. Click en **Environments** en la barra lateral
2. Click en **+** para crear un nuevo entorno
3. Nombra el entorno: `License Service - Local`
4. Agrega las siguientes variables:

| Variable | Initial Value | Current Value | Descripción |
|----------|--------------|---------------|-------------|
| `base_url` | `http://localhost:8199` | `http://localhost:8199` | URL base del servicio |
| `auth_token` | *(vacío)* | *(vacío)* | Token JWE (auto-generado) |
| `license_key` | *(vacío)* | *(vacío)* | Clave de licencia (auto-generada) |
| `test_hwid` | *(vacío)* | *(vacío)* | HWID de prueba (auto-generado) |

5. Guarda el entorno
6. Selecciona el entorno en el dropdown superior derecho

### Entornos Adicionales (Opcional)

Puedes crear entornos adicionales para diferentes ambientes:

**License Service - Docker**
```
base_url: http://localhost:8199
```

**License Service - Production**
```
base_url: https://your-production-domain.com
```

## Estructura de la Colección

La colección está organizada en las siguientes carpetas:

### 1. **Authentication**
Endpoints para generar y gestionar tokens JWE.

- ✅ `Generate Auth Token` - Genera un token JWE válido
- ❌ `Generate Token - Empty Subject (Negative Test)` - Prueba con subject vacío

### 2. **License Management**
Endpoints principales para gestionar licencias.

- ✅ `Create License` - Crea una nueva licencia
- ❌ `Create License - Duplicate Key (Negative Test)` - Intenta crear licencia duplicada
- ❌ `Create License - No Auth (Negative Test)` - Intenta crear sin autenticación
- ✅ `Activate License (First Time)` - Primera activación de licencia
- ✅ `Activate License (Reactivation - Same HWID)` - Reactivación con mismo HWID
- ❌ `Activate License - Wrong HWID (Negative Test)` - Intenta usar en otra máquina
- ❌ `Activate License - Nonexistent Key (Negative Test)` - Intenta activar licencia inexistente

### 3. **Health & Monitoring**
Endpoints para monitoreo del servicio.

- `Health Check (if actuator enabled)` - Verifica estado del servicio
- `H2 Console (if enabled)` - Acceso a la consola de base de datos H2

### 4. **Complete Workflow Test**
Suite completa de pruebas end-to-end.

- `Step 1 - Generate Token`
- `Step 2 - Create License`
- `Step 3 - Activate License`
- `Step 4 - Verify Reactivation`

## Flujo de Trabajo Básico

### Paso 1: Iniciar el Servicio

```bash
# Opción 1: Maven
mvn spring-boot:run

# Opción 2: Docker Compose
docker-compose up -d

# Opción 3: JAR ejecutable
java -jar target/lib-validate-license-0.0.1-SNAPSHOT.jar
```

### Paso 2: Generar Token de Autenticación

1. Abre la carpeta **Authentication**
2. Ejecuta el request **Generate Auth Token**
3. Verifica que el test pase (Status: 200 OK)
4. El token se guarda automáticamente en `{{auth_token}}`

**Request:**
```json
POST /api/auth/token
{
    "subject": "postman-test-client"
}
```

**Response:**
```json
{
    "token": "eyJhbGciOiJkaXIiLCJlbmMiOiJBMjU2R0NNIn0...",
    "type": "Bearer",
    "subject": "postman-test-client"
}
```

### Paso 3: Crear Licencia

1. Abre la carpeta **License Management**
2. Ejecuta el request **Create License**
3. La clave de licencia se guarda automáticamente en `{{license_key}}`

**Request:**
```json
POST /api/license/create
Authorization: Bearer {{auth_token}}
{
    "licenseKey": "POSTMAN-TEST-12345",
    "email": "test@example.com",
    "validDays": 365
}
```

**Response:**
```json
{
    "id": 1,
    "licenseKey": "POSTMAN-TEST-12345",
    "email": "test@example.com",
    "expirationDate": "2027-01-29T10:30:00.000+00:00",
    "active": false,
    "hwid": null
}
```

### Paso 4: Activar Licencia

1. Ejecuta el request **Activate License (First Time)**
2. El HWID se guarda automáticamente en `{{test_hwid}}`

**Request:**
```json
POST /api/license/activate
Authorization: Bearer {{auth_token}}
{
    "licenseKey": "{{license_key}}",
    "hwid": "POSTMAN-PC-67890"
}
```

**Response:**
```json
{
    "description": "LICENCIA_OK",
    "expirationDate": "2027-01-29T10:30:00.000+00:00"
}
```

### Paso 5: Verificar Reactivación

1. Ejecuta el request **Activate License (Reactivation - Same HWID)**
2. Debe retornar `LICENCIA_OK` si el HWID coincide

## Casos de Prueba

### ✅ Casos de Éxito (Happy Path)

| Test | Endpoint | Expected Result |
|------|----------|----------------|
| Generar token | `POST /api/auth/token` | 200 OK, token JWE válido |
| Crear licencia | `POST /api/license/create` | 200 OK, licencia inactiva |
| Primera activación | `POST /api/license/activate` | 200 OK, LICENCIA_OK |
| Reactivación mismo HWID | `POST /api/license/activate` | 200 OK, LICENCIA_OK |

### ❌ Casos de Error (Negative Tests)

| Test | Scenario | Expected Result |
|------|----------|----------------|
| Token sin subject | Subject vacío | 400 Bad Request |
| Crear sin auth | Sin token | 401 Unauthorized |
| Licencia duplicada | Clave existente | 400 Bad Request |
| Licencia inexistente | Clave no existe | 403 Forbidden |
| HWID incorrecto | Diferente máquina | 403 Forbidden |
| Licencia vencida | expirationDate pasado | 403 Forbidden |

## Variables de Entorno

### Variables Automáticas

Estas variables se llenan automáticamente mediante scripts de test:

- **`auth_token`**: Token JWE generado (válido por 1 hora)
- **`license_key`**: Clave de licencia creada
- **`test_hwid`**: HWID usado en la activación
- **`workflow_token`**: Token para workflow tests
- **`workflow_license_key`**: Licencia para workflow tests
- **`workflow_hwid`**: HWID para workflow tests

### Variables Manuales

Estas variables debes configurarlas manualmente:

- **`base_url`**: URL del servicio (default: `http://localhost:8199`)

## Ejemplos de Uso

### Ejecutar Suite Completa

1. Selecciona la colección **License Validation Service**
2. Click derecho → **Run collection**
3. Deja seleccionadas todas las requests
4. Click en **Run License Validation Service**
5. Revisa los resultados en el Collection Runner

### Ejecutar Solo Tests Positivos

1. En Collection Runner, deselecciona todos los tests que tengan "(Negative Test)"
2. Click en **Run**

### Ejecutar Workflow Completo

1. Expande la carpeta **Complete Workflow Test**
2. Click derecho → **Run folder**
3. Los 4 pasos se ejecutan en secuencia

### Exportar Resultados

1. Después de ejecutar tests en Collection Runner
2. Click en **Export Results**
3. Guarda el archivo JSON con los resultados

## Troubleshooting

### Error: "Could not get any response"

**Causa**: El servicio no está ejecutándose.

**Solución**:
```bash
# Verifica que el servicio esté corriendo
curl http://localhost:8199/actuator/health

# Si no responde, inicia el servicio
mvn spring-boot:run
```

### Error: 401 Unauthorized

**Causa**: Token ausente, inválido o expirado.

**Solución**:
1. Ejecuta nuevamente **Generate Auth Token**
2. Verifica que `{{auth_token}}` esté en tu entorno
3. Los tokens expiran después de 1 hora

### Error: 403 Forbidden - "Licencia usada en otro PC"

**Causa**: Estás intentando activar con un HWID diferente al original.

**Solución**:
- Las licencias están ligadas permanentemente al primer HWID usado
- Usa el mismo HWID guardado en `{{test_hwid}}`
- O crea una nueva licencia para un nuevo HWID

### Error: 400 Bad Request - "La llave de licencia ya existe"

**Causa**: Ya existe una licencia con esa clave.

**Solución**:
```json
{
    "licenseKey": "POSTMAN-TEST-{{$randomInt}}",  // Usa variable dinámica
    "email": "test@example.com",
    "validDays": 365
}
```

### Test Scripts No Funcionan

**Causa**: Los scripts de test usan sintaxis de Postman específica.

**Solución**:
- Asegúrate de usar Postman (no Insomnia u otro cliente)
- Verifica que la versión de Postman sea reciente (v9+)

### Variables de Entorno No Se Actualizan

**Causa**: Entorno no seleccionado o problema de sincronización.

**Solución**:
1. Verifica que el entorno correcto esté seleccionado (dropdown superior derecho)
2. Click en el ícono de ojo 👁️ para ver las variables
3. Actualiza manualmente si es necesario

## Scripts de Test Personalizados

### Ver Token en Console

```javascript
// En Tests tab de "Generate Auth Token"
var jsonData = pm.response.json();
console.log("Token completo:", jsonData.token);
console.log("Token length:", jsonData.token.length);
console.log("Token parts:", jsonData.token.split('.').length); // Debe ser 5
```

### Validar Expiración

```javascript
// En Tests tab de "Activate License"
var jsonData = pm.response.json();
var expirationDate = new Date(jsonData.expirationDate);
var now = new Date();
var daysRemaining = Math.floor((expirationDate - now) / (1000 * 60 * 60 * 24));

console.log("Días restantes:", daysRemaining);
pm.test("License expires in more than 0 days", function() {
    pm.expect(daysRemaining).to.be.above(0);
});
```

### Limpiar Variables de Entorno

```javascript
// Ejecuta en Console (Postman Console)
pm.environment.unset("auth_token");
pm.environment.unset("license_key");
pm.environment.unset("test_hwid");
console.log("Environment variables cleared");
```

## Recursos Adicionales

- **Documentación del Proyecto**: Ver `CLAUDE.md`
- **Código Fuente**: Ver carpeta `src/`
- **Tests Automatizados**: Ver carpeta `src/test/`
- **Documentación Spring Boot**: https://spring.io/projects/spring-boot
- **Documentación Nimbus JOSE+JWT**: https://connect2id.com/products/nimbus-jose-jwt

## Preguntas Frecuentes

### ¿Puedo usar esta colección en Newman (CLI)?

Sí, puedes ejecutar la colección desde línea de comandos:

```bash
# Instala Newman
npm install -g newman

# Ejecuta la colección
newman run postman_collection.json -e environment.json

# Con reporte HTML
newman run postman_collection.json -e environment.json -r html
```

### ¿Cómo cambio el puerto del servicio?

Modifica la variable `base_url` en tu entorno:

```
base_url: http://localhost:9000
```

O configura la variable de entorno al iniciar el servicio:

```bash
SERVER_PORT=9000 mvn spring-boot:run
```

### ¿Los tokens expiran?

Sí, por defecto expiran después de **3600 segundos (1 hora)**. Puedes configurar esto con:

```bash
JWE_EXPIRATION_SECONDS=7200 mvn spring-boot:run  # 2 horas
```

### ¿Cómo pruebo con licencias expiradas?

Crea una licencia con `validDays: 0` o negativo:

```json
{
    "licenseKey": "EXPIRED-TEST",
    "email": "test@example.com",
    "validDays": -1
}
```

## Contribuciones

Si encuentras bugs o tienes sugerencias para mejorar esta colección:

1. Reporta issues en el repositorio del proyecto
2. Crea un Pull Request con mejoras
3. Documenta nuevos casos de prueba en este archivo

---

**Versión**: 2.0.0
**Última actualización**: 2026-01-29
**Maintainer**: Claude Code
