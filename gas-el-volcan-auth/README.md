# Gas El Volcán — Microservicio de autenticación (EA3)

Emite el JWT que usa todo el sistema. Es el **único** servicio que crea usuarios y firma tokens. `gas-el-volcan-pedidos` (la carpeta vecina en este repositorio) solo los **valida**, con la misma clave y el mismo emisor. Los dos servicios no comparten tablas ni clases.

```text
React (5173) ──login/registro──▶ Auth (8081) ── MySQL gas_volcan_auth
     │                              │ firma JWT (JWT_SECRET)
     └──Authorization: Bearer──▶ Pedidos (8082) ── MySQL gas_volcan_pedidos
                                    valida firma, emisor, vencimiento y rol
```

## Estructura

```text
distribuidora-el-volcan-backend/     # este repositorio
├── gas-el-volcan-auth/              # este microservicio (puerto 8081)
└── gas-el-volcan-pedidos/           # catalogo y boletas (puerto 8082), trae el docker-compose.yml de MySQL
```

```text
gas-el-volcan-auth/
├── pom.xml
├── .env.example                 # copiar como .env (no se sube a Git)
└── src/
    ├── main/java/gaselvolcan/auth/
    │   ├── AuthApplication.java
    │   ├── config/              # SecurityConfig (BCrypt, CORS, reglas), Swagger, usuarios iniciales
    │   ├── controller/          # AuthController: /api/v1/auth/...
    │   ├── dto/                 # LoginRequest, RegistroRequest, SesionResponse
    │   ├── exception/           # ApiError (mismo formato que Pedidos) y manejador global
    │   ├── model/               # Usuario, Rol
    │   ├── repository/          # UsuarioRepository
    │   ├── security/            # JwtService (firma/valida), JwtAuthenticationFilter
    │   └── service/             # AuthService: login, registro, sesión actual
    └── test/java/gaselvolcan/auth/
        ├── security/JwtServiceTest.java
        └── service/AuthServiceTest.java
```

## Cómo ejecutarlo

1. MySQL: desde la carpeta `gas-el-volcan-pedidos/`, `docker compose up -d`. Es el mismo contenedor para los dos servicios: cada uno usa su propia base (`gas_volcan_auth` y `gas_volcan_pedidos`).
2. En esta carpeta (`gas-el-volcan-auth/`), copiar `.env.example` como `.env` y completar:
   - `DB_PASSWORD`: la clave de root (`MYSQL_ROOT_PASSWORD` del `docker-compose.yml` de Pedidos).
   - `JWT_SECRET`: el **mismo** Base64 que tiene configurado Pedidos.
   - `SEED_*_PASSWORD`: las contraseñas de los tres usuarios de prueba.
3. Ejecutar `AuthApplication.java` desde el IDE (con esta carpeta como directorio de trabajo) o `mvnw.cmd spring-boot:run`.
4. Abrir `http://localhost:8081/swagger-ui.html`.

Spring Boot lee el `.env` solo (`spring.config.import` en `application.properties`). Si el archivo no existe, se usan las variables de entorno del sistema o de la configuración de ejecución del IDE. La base `gas_volcan_auth` se crea sola la primera vez.

Pruebas: `mvnw.cmd test` (18 pruebas de JwtService y AuthService, sin necesidad de MySQL).

## Endpoints

| Método | Ruta | Acceso | Recibe | Responde |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | Público | `{ "correo", "password" }` | 200 `{ token, correo, rol, nombre }` · 400 formato · 401 credenciales |
| POST | `/api/v1/auth/registro` | Público | `{ "nombre", "correo", "password", "telefono" }` | 201 igual que login, rol `CLIENTE` · 400 · 409 correo repetido |
| GET | `/api/v1/auth/me` | Con token | `Authorization: Bearer <token>` | 200 `{ correo, rol, nombre }` · 401 sin token, vencido, alterado o usuario desactivado |
| POST | `/api/v1/auth/refresh` | Con token vigente | `Authorization: Bearer <token>` | 200 igual que login, con un token nuevo · 401 vencido, alterado, usuario desactivado o sesión sobre 8 horas |

### Renovación del token

React llama a `/refresh` 5 minutos antes de que venza el token, así la sesión no se corta mientras el usuario trabaja:

- Solo se renueva un token **todavía vigente**. Uno vencido o alterado no sirve para pedir otro.
- Se vuelve a leer el usuario en la base: si lo desactivaron no se renueva, y si le cambiaron el rol, el token nuevo trae el rol actual.
- El token lleva el claim `sesionHasta` (8 horas desde el login, `JWT_MAX_SESSION_MS`). Al renovar se conserva, y ningún token vence después de ese límite. Pasadas las 8 horas hay que volver a ingresar.

Cualquier otra ruta responde 401 (sin token) o 403 (con token). Los errores usan el mismo JSON que Pedidos:

```json
{ "timestamp": "...", "status": 401, "error": "Unauthorized",
  "mensaje": "Correo o contrasena incorrectos", "ruta": "/api/v1/auth/login", "detalles": {} }
```

## Contrato del JWT (lo que valida Pedidos)

| Campo | Valor |
|---|---|
| Algoritmo | HS256, clave `JWT_SECRET` (Base64, 32 bytes o más) |
| `iss` | `gas-el-volcan-auth` (`JWT_ISSUER`) |
| `sub` | correo del usuario, en minúsculas |
| `rol` | `ADMIN`, `VENDEDOR` o `CLIENTE` |
| `nombre` | nombre para mostrar |
| `exp` | emisión + `JWT_EXPIRATION_MS` (1 hora por defecto), sin pasar de `sesionHasta` |
| `sesionHasta` | límite para renovar: login + `JWT_MAX_SESSION_MS` (8 horas). Pedidos lo ignora |

## Seguridad

- Las contraseñas se guardan solo como hash BCrypt (`$2a$10$...`).
- El registro público **siempre** crea un `CLIENTE`. El rol no se acepta desde el navegador.
- El login responde el mismo mensaje si el correo no existe o si la clave es incorrecta, así no revela qué correos están registrados.
- Un usuario desactivado no puede iniciar sesión, y `/me` rechaza su token aunque todavía no haya vencido.
- Sin sesión de servidor (`STATELESS`). CORS abierto solo hacia `FRONTEND_URL`.
- Ni la clave JWT, ni las contraseñas de prueba, ni la clave de MySQL están en el código: vienen del `.env`.

## Usuarios de prueba

Se crean al arrancar si su correo no existe y si su variable `SEED_*_PASSWORD` tiene valor.

| Correo | Rol |
|---|---|
| `admin@volcan.cl` | ADMIN |
| `vendedor@volcan.cl` | VENDEDOR |
| `cliente@volcan.cl` | CLIENTE |
