# TiendaOnline — Sistema de Gestión de Pedidos, Inventario y Clientes

Monorepo con arquitectura de microservicios para el TP final. Ver el enunciado completo en
[`tp_final_enunciado.md`](./tp_final_enunciado.md).

## Estructura del repositorio

```
tp/
├── servicio-clientes/        # API de clientes
├── servicio-inventario/      # API de stock/inventario
└── servicio-pedidos/         # API de pedidos
```

El repositorio es un multi-project build de Gradle: un único `settings.gradle`/`build.gradle`
en la raíz declara los tres módulos y la configuración común (Spring Boot 3.3, Java 17,
plugins, wrapper). Cada servicio tiene su propia base de datos PostgreSQL y su propio esquema
versionado con Flyway. No comparten código ni base de datos entre sí: toda comunicación entre
servicios será vía HTTP (o mocks en tests).

Para abrir el proyecto en IntelliJ: **File → Open** sobre la carpeta raíz `tp` (no sobre cada
subcarpeta) — IntelliJ importa los tres módulos de una sola vez como proyecto Gradle.

## Servicios y puertos

| Servicio             | Puerto app | Base de datos   |
|----------------------|-----------|------------------|
| servicio-clientes    | 8081      | clientes_db      |
| servicio-inventario  | 8082      | inventario_db    |
| servicio-pedidos     | 8083      | pedidos_db       |

Las tres bases viven en la misma instancia local de PostgreSQL, puerto `5432`, con usuario/contraseña
`postgres` / `postgres` (solo entorno local).

## Cómo levantar el entorno

1. Levantar una instancia de PostgreSQL (versión 16 o superior). Dos opciones, elegir una:

   **Opción A: instalador nativo**

   - Descargar el instalador desde https://www.postgresql.org/download/ para tu sistema operativo
     (en Windows, el instalador de EnterpriseDB incluye pgAdmin).
   - Durante la instalación, cuando pida la contraseña del superusuario `postgres`, poner `postgres`.
   - Dejar el puerto por defecto, `5432`.

   **Opción B: Docker**

   - Con Docker Desktop instalado y corriendo, ejecutar:

     ```bash
     docker run --name postgres-tp -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
     ```

   - Esto levanta un contenedor con el superusuario `postgres` / contraseña `postgres` en el
     puerto `5432`, igual que la opción A. pgAdmin no viene incluido en la imagen: instalarlo
     aparte desde https://www.pgadmin.org/download/ para poder hacer el paso 2.

2. Crear las tres bases de datos con pgAdmin:

   - Abrir pgAdmin, conectarse al servidor local (`localhost`, puerto `5432`, usuario `postgres`,
     contraseña `postgres`).
   - En el árbol de la izquierda, click derecho sobre **Databases** → **Create** → **Database...**
   - Crear una base llamada `clientes_db`, owner `postgres`. Repetir para `inventario_db` y `pedidos_db`.
   - No hace falta crear tablas a mano: cada servicio corre sus propias migraciones de Flyway al
     arrancar.

3. Correr cada servicio. Dos formas, elegir una:

   **Opción A: línea de comandos**, en terminales separadas desde la raíz del repo:

   ```bash
   ./gradlew :servicio-clientes:bootRun
   ./gradlew :servicio-inventario:bootRun
   ./gradlew :servicio-pedidos:bootRun
   ```

   **Opción B: desde IntelliJ**

   - Abrir la clase principal del servicio (por ejemplo
     `servicio-clientes/src/main/java/.../ServicioClientesApplication.java`).
   - Click en el ícono ▶ al lado de la declaración de la clase (o click derecho → **Run**).
   - Repetir para `ServicioInventarioApplication` y `ServicioPedidosApplication`.
   - Cada Run abre su propia pestaña en la consola de IntelliJ, así que los tres quedan
     corriendo en paralelo sin pisarse.

   Al arrancar, cada servicio ejecuta automáticamente sus migraciones de Flyway contra su base.
   Los tres pueden correr en paralelo sin conflicto, ya que usan puertos de app distintos y cada
   uno apunta a su propia base dentro de la misma instancia de Postgres.

4. Para correr los tests de un servicio (requieren la base levantada, ya que el contexto de
   Spring corre las migraciones de Flyway al arrancar):

   ```bash
   ./gradlew :servicio-clientes:test
   ```

   O `./gradlew test` desde la raíz para correr los tests de los tres servicios.

## servicio-clientes: feature flag `mask-pii`

`GET /api/v1/clientes/{id}` enmascara `name`, `surname` y `email` según el flag `mask-pii`, que se
evalúa en cada request. Hay dos implementaciones y se elige por perfil de Spring:

| Perfil | Origen del flag | Cuándo usarlo |
|---|---|---|
| (ninguno, por defecto) | `feature-flags.mask-pii` de `application.yml` (`true` = enmascara) | Desarrollo y tests, sin cuenta de Statsig |
| `statsig` | Gate `mask-pii` de Statsig, evaluado en runtime | Probar o mostrar el cambio del flag sin reiniciar |

**Configuración en Statsig (una sola vez):**

1. Crear un *Feature Gate* llamado exactamente `mask-pii`.
2. Darle una regla que lo deje pasar al 100% (el servicio evalúa con un usuario fijo,
   `servicio-clientes`, así que una regla por usuario puede dar un resultado inesperado).
3. Copiar la **Server Secret Key** del proyecto (empieza con `secret-`; la *Client Key* no sirve).

La clave siempre se pasa por la variable de entorno `STATSIG_SERVER_KEY`: no se escribe en ningún
archivo versionado.

**Cómo levantar clientes con Statsig, según el caso:**

- **Gradle, PowerShell (Windows):**

  ```powershell
  $env:STATSIG_SERVER_KEY = "secret-TU_KEY"; ./gradlew :servicio-clientes:bootRun --args="--spring.profiles.active=statsig"
  ```

- **Gradle, bash (Linux/macOS/Git Bash):**

  ```bash
  STATSIG_SERVER_KEY=secret-TU_KEY ./gradlew :servicio-clientes:bootRun --args="--spring.profiles.active=statsig"
  ```

- **IntelliJ:** en *Run → Edit Configurations* de `ServicioClientesApplication`, poner
  `statsig` en *Active profiles* y `STATSIG_SERVER_KEY=secret-TU_KEY` en *Environment variables*.

- **Sin Statsig:** no activar ningún perfil. Para cambiar el valor local sin editar el archivo,
  usar la variable `FEATURE_FLAGS_MASK_PII=false` (o `true`) al arrancar.

**Cómo verificar:** con el gate encendido el `GET` devuelve datos enmascarados (`J***`, `P***`,
`****@****.com`); apagando el gate en la consola de Statsig, sin reiniciar el servicio, la misma
consulta pasa a devolverlos sin enmascarar (puede demorar unos segundos en sincronizar).

Los tests automáticos no usan Statsig: corren con el perfil `test` y el flag local.

## servicio-inventario

Swagger UI: http://localhost:8082/swagger-ui.html — OpenAPI: http://localhost:8082/v3/api-docs

| Método | Ruta                                              | Descripción                                                        |
|--------|---------------------------------------------------|--------------------------------------------------------------------|
| GET    | `/api/v1/productos`                               | Listar productos                                                   |
| GET    | `/api/v1/productos/{id}`                          | Consultar un producto                                              |
| GET    | `/api/v1/productos/{id}/disponibilidad?cantidad=` | Stock disponible y precio (`cantidad` opcional → `suficiente`)     |
| POST   | `/api/v1/reservas`                                | Reservar todos los ítems de un pedido (todo o nada)                |
| GET    | `/api/v1/reservas/{id}`                           | Consultar una reserva                                              |
| GET    | `/api/v1/reservas?pedidoId=`                      | Reservas de un pedido                                              |
| POST   | `/api/v1/reservas/{id}/liberacion`                | Liberar una reserva (devuelve el stock)                            |
| POST   | `/api/v1/reservas/liberacion?pedidoId=`           | Liberar todas las reservas activas de un pedido (idempotente)      |

Ejemplo de reserva:

```http
POST /api/v1/reservas
{"pedidoId": 1, "items": [{"productoId": 1, "cantidad": 2}, {"productoId": 2, "cantidad": 1}]}
```

- `201` con las reservas creadas (incluyen `precioUnitario`, congelado al reservar, para que
  servicio-pedidos calcule el total).
- `409 stock-insuficiente` si algún ítem no alcanza: no se reserva ninguno y el cuerpo trae `faltantes`.
- `409 pedido-con-reservas-activas` si el pedido ya tiene reservas activas.
- `404` si algún producto no existe; `400` con `errores` por validación.

Todos los errores son `application/problem+json` (RFC 7807). Las reservas concurrentes usan
`SELECT ... FOR UPDATE` sobre los productos, por lo que el stock nunca queda negativo.
La migración `V2__seed_productos.sql` carga productos de ejemplo.

**Tests**: unitarios con Mockito (dominio, servicios, controllers con `@WebMvcTest`) e integración
con `@SpringBootTest` contra Postgres. Los de integración usan el perfil `test`, que apunta a una
base separada `inventario_test_db` (crearla igual que las otras). MockServer no aplica en este
servicio porque no llama a otros servicios.

**Observabilidad**:
- Logs en consola con `traceId`/`spanId`, y en JSON (uno por línea) en
  `logs/servicio-inventario.json.log` para filtrarlos por `traceId`, `level`, `logger`, etc.
- Traces exportados por OTLP a `http://localhost:4318/v1/traces` (configurable con
  `OTEL_TRACES_ENDPOINT`), por ejemplo a Jaeger.
- Actuator: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`.

## Estado actual

Por ahora cada servicio es un esqueleto: aplicación Spring Boot que levanta, se conecta a su
base y aplica las migraciones iniciales de Flyway con las tablas del modelo de datos del
enunciado. Todavía no hay endpoints REST, lógica de negocio, comunicación entre servicios,
job batch, feature flags ni observabilidad — se irán agregando de forma incremental.

### Próximos pasos (según el enunciado)

- [ ] Endpoints REST de cada servicio (OpenAPI/Swagger, validaciones, manejo de errores)
- [ ] Comunicación servicio-pedidos → servicio-clientes / servicio-inventario al crear un pedido
- [ ] Endpoint `GET /clientes/{id}/ordenes` (servicio-clientes → servicio-pedidos)
- [ ] Job batch nocturno de expiración de pedidos (Spring Batch)
- [ ] Feature flag de estrategia de asignación de stock
- [ ] Tests (TDD, Mockito, MockServer para integración entre servicios)
- [ ] Observabilidad: Actuator/Micrometer, logs estructurados con correlation id, tracing distribuido
