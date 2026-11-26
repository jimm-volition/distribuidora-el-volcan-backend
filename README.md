# Gas El Volcán — Microservicio de productos y pedidos (EA3)

Backend de negocio del proyecto Gas El Volcán. **Solo contiene el paquete `gaselvolcan.pedidos`**: no hay directorios `duoc` ni clases de autenticación de usuarios. Mantiene la organización Spring Boot por capas (`config`, `controller`, `dto`, `exception`, `model`, `repository`, `security`, `service`).

**Documentación incluida:**
- [`GUIA_DIEGO.md`](GUIA_DIEGO.md): explicación de cada carpeta, configuración, pasos de ejecución, pruebas y conexión con React.
- [`PARA_JUAN.md`](PARA_JUAN.md): contrato que debe implementar Auth, JWT, roles, configuración y pruebas de integración.
- [`INTEGRACION_CON_AUTH.md`](INTEGRACION_CON_AUTH.md): resumen técnico del contrato entre los dos microservicios.
- [`EJEMPLOS_API.http`](EJEMPLOS_API.http): solicitudes HTTP para IntelliJ o VS Code REST Client.

## Estructura

```text
gas-el-volcan-pedidos/
├── pom.xml                        # dependencias Java + Spring Boot
├── docker-compose.yml             # MySQL de desarrollo
├── .env.example                   # guía de variables, no se carga automáticamente
├── README.md
├── GUIA_DIEGO.md
├── PARA_JUAN.md
├── INTEGRACION_CON_AUTH.md
├── EJEMPLOS_API.http
└── src/
    ├── main/
    │   ├── java/gaselvolcan/pedidos/
    │   │   ├── PedidosApplication.java
    │   │   ├── config/             # seguridad HTTP, CORS, Swagger, semilla
    │   │   ├── controller/         # rutas REST
    │   │   ├── dto/                # JSON de entrada/salida
    │   │   ├── exception/          # errores JSON
    │   │   ├── model/              # entidades JPA y estados
    │   │   ├── repository/         # consultas MySQL mediante JPA
    │   │   ├── security/           # validación JWT (NO emite tokens)
    │   │   └── service/            # lógica de negocio y transacciones
    │   └── resources/
    │       ├── application.properties
    │       └── seed/productos.json # 14 productos del Excel
    └── test/java/gaselvolcan/pedidos/
        ├── security/JwtServiceTest.java
        └── service/BoletaServiceTest.java
```

## Inicio rápido — Windows

1. Tener JDK **17 o superior**, Docker Desktop y conexión a Internet para descargar Maven/dependencias en el primer uso.
2. Abrir `gas-el-volcan-pedidos/` en IntelliJ como proyecto Maven (`pom.xml`).
3. En una terminal dentro del proyecto, iniciar MySQL: `docker compose up -d`.
4. Generar un `JWT_SECRET` Base64 de 32 bytes aleatorios (ver `GUIA_DIEGO.md`). Guardar la **misma** cadena de forma segura para el Auth de Juan. Definirla en variables de entorno o en Run Configuration de IntelliJ.
5. Ejecutar `PedidosApplication.java` o `mvnw.cmd spring-boot:run`.
6. Verificar `http://localhost:8082/api/v1/productos` (público según el plan EA3) y `http://localhost:8082/swagger-ui.html`.

> **Nota:** `.env.example` es documentación. Spring Boot no lee ese archivo automáticamente. Debes configurar variables de entorno de verdad en Windows/IntelliJ. `JWT_SECRET` es obligatorio incluso para iniciar el servidor y probar endpoints públicos.

## Configuración esperada

| Variable | Valor de desarrollo sugerido | Quién la configura |
|---|---|---|
| `SERVER_PORT` | `8082` | Diego |
| `DB_URL` | `jdbc:mysql://localhost:3306/gas_volcan_pedidos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Santiago` | Diego |
| `DB_USER` | `gasvolcan` | Diego |
| `DB_PASSWORD` | `gasvolcan_local` (**solo local**) | Diego |
| `FRONTEND_URL` | `http://localhost:5173` | Diego |
| `JWT_ISSUER` | `gas-el-volcan-auth` | Diego y Juan |
| `JWT_SECRET` | misma clave Base64 en Auth y Pedidos (**nunca Git**) | Diego y Juan |

Las propiedades se consultan en `src/main/resources/application.properties`.

## Endpoints de Pedidos

| Método | Ruta | Permisos |
|---|---|---|
| GET | `/api/v1/categorias` y `/api/v1/categorias/{id}` | Público (según plan EA3) |
| POST | `/api/v1/categorias` | ADMIN |
| PUT / DELETE | `/api/v1/categorias/{id}` | ADMIN |
| GET | `/api/v1/productos` y `/api/v1/productos/{codigo}` | Público (según plan EA3) |
| POST | `/api/v1/productos` | ADMIN |
| PUT / DELETE | `/api/v1/productos/{codigo}` | ADMIN (DELETE desactiva) |
| GET | `/api/v1/boletas` y `/api/v1/boletas/{id}` | ADMIN/VENDEDOR: todas; CLIENTE: propias |
| POST | `/api/v1/boletas` | ADMIN/CLIENTE |
| PUT | `/api/v1/boletas/{id}/estado` | ADMIN |

### Ejemplo de pedido

`POST http://localhost:8082/api/v1/boletas` con encabezados `Content-Type: application/json` y `Authorization: Bearer TU_TOKEN`:

```json
{
  "nombreCliente": "Ana Pérez",
  "direccion": "Las Rosas 123",
  "comuna": "Chillán",
  "tipoPrecio": "RESIDENCIAL",
  "detalles": [
    {"codigoProducto": "CL002", "cantidad": 2}
  ]
}
```

El servidor calcula el precio/total (no se manda en el JSON), crea una boleta, guarda sus detalles y descuenta stock. Usa el correo del `sub` del JWT como cliente; no acepta que el navegador elija ese correo. Una cancelación válida repone stock.

## Alcance y limitaciones

El proyecto implementa el **alcance reducido definido en `PLAN-EA3.md`**: roles `ADMIN`, `VENDEDOR` y `CLIENTE`, catálogo, pedidos y boletas, sin mapa, repartidor ni AWS. El documento original Forma C exige un flujo más amplio con operadora, repartidores y seguimiento; validar con el profesor cuál alcance aplica a esta entrega.

**Verificación:** se comprobaron la estructura y los archivos; no se ha confirmado una compilación Maven con dependencias ni pruebas de ejecución contra MySQL en este entorno. Ejecutar `mvnw.cmd test` y `mvnw.cmd spring-boot:run` en tu máquina.
