# RapiEntrega · Reto 2
Plataforma académica de entregas con Java 21, Spring Boot, Maven, MySQL, JPA, Bean Validation, REST y WebSocket. Se compila con Maven y se ejecuta con los JAR generados. Cada servicio conserva su base y sus responsabilidades.

## Instalación

```powershell
git clone https://github.com/mafercova/RapiEntrega.git
cd RapiEntrega
```

## Requisitos e inicio local
Instalar JDK 21, MySQL Server y PowerShell 7. Agregar mysql.exe a PATH y configurar JAVA_HOME al JDK 21. Maven Wrapper descarga Maven 3.9.9 en su primera ejecución.

Los siguientes bloques se ejecutan en **PowerShell 7**, desde la raíz de RapiEntrega. Si tu terminal muestra una ruta terminada en `>` y estás en CMD, abre PowerShell 7 en esa misma carpeta escribiendo:
```cmd
pwsh
```
Si `pwsh` no se reconoce, instala PowerShell 7 o abre su aplicación desde Inicio. Los comandos `$env:...`, `Invoke-RestMethod` y los scripts `.ps1` no se ejecutan directamente en CMD. Python 3.11+ es opcional y se usa para las pruebas del flujo completo.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21' # adaptar a tu instalación
.\scripts\preparar-entorno.ps1
.\scripts\crear-bases.ps1
.\mvnw.cmd verify
```
crear-bases pide la contraseña del administrador de MySQL, crea seis bases y usuarios locales y conserva datos existentes. Las claves de aplicaciones se generan en .env y no se muestran ni se incluyen en Git. Si un usuario MySQL ya existe con otra contraseña, actualízala de forma explícita para que coincida con .env; el script no sobrescribe cuentas existentes. El SQL de solo creación de bases está en pruebas/sql/01-databases.sql para consulta en Workbench.

En ocho terminales diferentes, desde el repositorio, ejecutar una aplicación por terminal:
```powershell
.\scripts\iniciar-servicio.ps1 -Servicio catalogo
.\scripts\iniciar-servicio.ps1 -Servicio pedidos
.\scripts\iniciar-servicio.ps1 -Servicio pagos
.\scripts\iniciar-servicio.ps1 -Servicio logistica
.\scripts\iniciar-servicio.ps1 -Servicio facturacion
.\scripts\iniciar-servicio.ps1 -Servicio calificaciones
.\scripts\iniciar-servicio.ps1 -Servicio pasarela-mock
.\scripts\iniciar-servicio.ps1 -Servicio facturacion-legado-mock
```
Cada comando ejecuta el JAR compilado con un máximo de 128 MB de heap, recolector Serial y dos procesadores disponibles para la JVM. Este ajuste permitió iniciar las ocho aplicaciones en el equipo de prueba. Después de modificar código, ejecutar de nuevo `.\mvnw.cmd verify` y reiniciar el servicio para usar el JAR actualizado. Cada comando permanece ejecutándose en su terminal. Ctrl+C detiene esa aplicación. Los dos mocks no requieren base. MySQL debe estar iniciado para los otros seis servicios. El perfil demo incorpora un restaurante, un producto de 65.50 MXN y dos repartidores. Las tablas de catálogo se preparan con schema.sql; las demás aplicaciones usan Hibernate update como simplificación académica.

| Aplicación | Puerto | Propietario de datos |
|---|---:|---|
| catálogo | 8081 | restaurantes y productos |
| pedidos | 8082 | pedido, importes y plazo |
| pagos | 8083 | cobros y compensación |
| logística | 8084 | repartidores, asignación y posición |
| facturación | 8085 | trabajos y folios |
| calificaciones | 8086 | entrega proyectada y opinión |
| pasarela mock | 8091 | operaciones ficticias en memoria |
| legado mock | 8092 | folios ficticios en memoria |

## Comunicación entre servicios
REST solicitud-respuesta resuelve consultas, pago, aceptación y comandos inmediatos. Para eventos, el servicio guarda una entrega por suscriptor en su tabla Outbox dentro de la transacción de negocio. Una tarea de Spring envía HTTP a POST /interno/eventos en segundo plano; el receptor responde 204 después de persistir su efecto e inbox. Si falla, solo esa entrega se reintenta con espera 2/4/8/... hasta 60 segundos. Las entregas ya confirmadas no se repiten por el fallo de otro suscriptor. Si se pierde una respuesta, puede haber reenvío: inbox por eventId y claves de negocio protegen los efectos.

Publicación-suscripción es una distribución estática implementada por la aplicación: pedido.entregado genera tres entregas independientes a pedidos, facturación y calificaciones. La petición HTTP de cada entrega es síncrona; el proceso de publicación es asíncrono respecto al comando original. No se promete exactly-once de red. Las suscripciones están declaradas en Eventos y los destinos se configuran con variables de entorno.

Logística transmite las posiciones mediante WebSocket integrado de Spring (/ws, STOMP, /topic/pedidos/{pedidoId}). El stream funciona dentro de una instancia del servicio; para esta entrega se ejecuta una sola instancia de logística. La posición queda persistida para recuperarla con REST al reconectar.

## Probar el flujo
Importar pruebas/postman/RapiEntrega.postman_collection.json y cambiar runId para cada compra. Esperar los estados indicados porque las notificaciones se entregan en segundo plano.
```powershell
Invoke-RestMethod http://localhost:8081/api/v1/restaurantes
Invoke-RestMethod http://localhost:8081/api/v1/restaurantes/1/menu
$pedido = Invoke-RestMethod -Method Post -Uri http://localhost:8082/api/v1/pedidos -ContentType application/json -Headers @{'Idempotency-Key'=[guid]::NewGuid().ToString()} -Body '{"clienteId":"cliente-demo","restauranteId":1,"items":[{"productoId":1,"cantidad":2}],"entrega":{"latitud":19.44,"longitud":-99.14,"direccion":"Campus"}}'
Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/v1/pagos -ContentType application/json -Headers @{'Idempotency-Key'=[guid]::NewGuid().ToString()} -Body (@{pedidoId=$pedido.id;tokenTarjeta='ok'} | ConvertTo-Json)
Invoke-RestMethod "http://localhost:8082/api/v1/pedidos/$($pedido.id)"
# Esperar PENDIENTE_ACEPTACION y aceptar antes de cinco minutos:
Invoke-RestMethod -Method Post "http://localhost:8082/api/v1/pedidos/$($pedido.id)/aceptacion"
Invoke-RestMethod "http://localhost:8084/api/v1/asignaciones/$($pedido.id)"
python pruebas/smoke.py
```
El script smoke crea sus propias compras y requiere las ocho aplicaciones levantadas. Incluye pago/entrega, posición, opinión, factura, rechazo/reembolso y tarjeta rechazada. La vista http://localhost:8084/seguimiento.html?pedidoId=ID_DEL_PEDIDO muestra ubicación y actualizaciones. Desde Postman se envían posiciones y se completa entrega.

Alternativas: token reject rechaza el pago y timeout-once simula una respuesta perdida después de cobrar. POST /api/v1/pedidos/{id}/rechazo cancela. Si no se acepta en 300 segundos desde pendienteAceptacionAt, se cancela y solicita reembolso; aceptar en el límite ya no es válido. Reembolso y factura tienen tres intentos y luego estado de revisión. Para recuperar: POST /api/v1/pagos/pedido/{id}/reintentar-reembolso o POST /api/v1/facturas/{id}/reintentar.

## Configuración
.env contiene MYSQL_HOST, MYSQL_PORT y una clave {SERVICIO}_DB_PASSWORD por aplicación; los scripts las cargan sin imprimirlas. Spring recibe DB_URL, DB_USER, DB_PASSWORD, PORT y URLs de servicios: CATALOGO_URL, PEDIDOS_URL, PAGOS_URL, LOGISTICA_URL, FACTURACION_URL, CALIFICACIONES_URL, PASARELA_URL y LEGADO_URL. EVENTOS_HABILITADOS=false desactiva el envío de notificaciones únicamente para pruebas aisladas.

## Pruebas y verificación
Las pruebas automáticas no requieren MySQL: utilizan H2 y respuestas HTTP controladas. Para ejecutar la suite con Java 21:
```powershell
.\mvnw.cmd verify
```
Para comprobar la comunicación HTTP real entre las ocho aplicaciones en un entorno temporal, después de compilar:
```powershell
python pruebas/flujo-local.py
```
Este comando requiere Python 3.11+ y JAVA_HOME. Inicia sus propios procesos Java en puertos libres, usa H2 y los detiene al terminar. No utiliza ni modifica las bases MySQL de la aplicación. Se diferencia de `python pruebas/smoke.py`, que comprueba las aplicaciones que tú ya tienes levantadas en sus puertos habituales.

Archivos disponibles:
- [Colección Postman](pruebas/postman/RapiEntrega.postman_collection.json).
- [Prueba sobre aplicaciones iniciadas](pruebas/smoke.py).
- [Prueba con procesos temporales](pruebas/flujo-local.py).
- [Resultados registrados de las 32 pruebas y del flujo completo](pruebas/resultados/resultado-local.json).
- [Configuración de ejemplo](.env.example).
- [Verificación en GitHub Actions](.github/workflows/verificar.yml).

GitHub Actions ejecuta Maven verify y después la prueba con procesos Java temporales. Los resultados registrados corresponden a H2: no equivalen a una verificación contra MySQL real. Los mocks de pasarela y legado son simulaciones HTTP en memoria y pierden su información al reiniciar. No hay autenticación productiva ni resultados de pruebas de carga. Los endpoints internos están destinados a una red local de confianza.


## Endpoints principales

Las rutas usan `http://localhost:PUERTO`. Los identificadores de pedido se obtienen al crearlo.

| Puerto | Método | Ruta | Operación | Éxito |
|---|---|---|---|---|
| 8081 | GET | `/api/v1/restaurantes` | Listar restaurantes | 200 |
| 8081 | GET | `/api/v1/restaurantes/{id}/menu` | Consultar menú | 200 |
| 8082 | POST | `/api/v1/pedidos` | Crear pedido | 201 |
| 8082 | GET | `/api/v1/pedidos/{id}` | Consultar estado | 200 |
| 8082 | POST | `/api/v1/pedidos/{id}/aceptacion` | Aceptar pedido | 200 |
| 8082 | POST | `/api/v1/pedidos/{id}/rechazo` | Rechazar pedido | 200 |
| 8083 | POST | `/api/v1/pagos` | Procesar pago simulado | 201 |
| 8083 | GET | `/api/v1/pagos/pedido/{id}` | Consultar pago y reembolso | 200 |
| 8084 | GET | `/api/v1/asignaciones/{id}` | Consultar repartidor y posición | 200 |
| 8084 | POST | `/api/v1/asignaciones/{id}/ubicaciones` | Actualizar posición | 200 |
| 8084 | POST | `/api/v1/asignaciones/{id}/entrega` | Completar entrega | 200 |
| 8085 | GET | `/api/v1/facturas/{id}` | Consultar factura por pedido | 200 |
| 8086 | POST | `/api/v1/calificaciones` | Registrar opinión | 201 |

## Ejemplos en Postman

Para los POST con cuerpo, seleccionar **Body > raw > JSON** y usar `Content-Type: application/json`.

### Crear pedido

`POST http://localhost:8082/api/v1/pedidos`

Agregar el encabezado `Idempotency-Key` con una clave nueva por compra, por ejemplo `pedido-demo-001`. Repetir la misma solicitud con la misma clave recupera el pedido; cambiar el cuerpo con esa clave produce conflicto.

```json
{
  "clienteId": "cliente-demo",
  "restauranteId": 1,
  "items": [{"productoId": 1, "cantidad": 2}],
  "entrega": {"latitud": 19.44, "longitud": -99.14, "direccion": "Campus"}
}
```

Resultado esperado: **201**, estado `PAGO_PENDIENTE` y total `131.00` con el producto demo. Guardar el `id` devuelto.

### Pagar y aceptar

`POST http://localhost:8083/api/v1/pagos`

Agregar `Idempotency-Key: pago-demo-001` y sustituir `ID_DEL_PEDIDO`:

```json
{"pedidoId": "ID_DEL_PEDIDO", "tokenTarjeta": "ok"}
```

Resultado esperado: **201**, estado `AUTORIZADO`. Consultar el pedido hasta que alcance `PENDIENTE_ACEPTACION`, y enviar `POST http://localhost:8082/api/v1/pedidos/ID_DEL_PEDIDO/aceptacion` sin cuerpo antes de cinco minutos. Después consultar la asignación hasta obtener `ASIGNADO`. Las notificaciones son asíncronas: una consulta inmediata puede mostrar el estado anterior.

La colección incluida contiene las solicitudes restantes de ubicación, entrega, factura y calificación. Para cada caso alternativo crear un pedido nuevo: `reject` simula tarjeta rechazada y `timeout-once` simula respuesta perdida después del cobro. Las respuestas 400 indican datos inválidos; 404, un recurso inexistente; 409, un conflicto de estado o idempotencia; 503, una dependencia indisponible.

## Comprobaciones locales del 23 de octubre de 2026

- `mvnw.cmd verify`: **32 pruebas, cero fallos y errores**, con persistencia H2 y adaptadores controlados.
- `python pruebas/flujo-local.py`: recorrido HTTP completo aprobado con ocho procesos y H2 temporal.
- `python pruebas/smoke.py`: recorrido completo aprobado con las ocho aplicaciones conectadas a **MySQL local**. Incluyó entrega, ubicación, calificación, factura, respuesta perdida conciliada, rechazo/reembolso y pago rechazado.
- Postman: `GET http://localhost:8081/api/v1/restaurantes` devolvió **200 OK** con el restaurante de demostración.

Estas comprobaciones corresponden al entorno local; los resultados versionados en `pruebas/resultados/resultado-local.json` describen la ejecución con H2. Pasarela y legado continúan siendo simulaciones HTTP.
