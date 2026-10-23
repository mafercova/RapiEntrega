# RapiEntrega · Reto 2
Plataforma académica de entregas con Java 21, Spring Boot, Maven, MySQL, JPA, Bean Validation, REST y WebSocket. Se ejecuta directamente con Maven. Cada servicio conserva su base y sus responsabilidades.

## Requisitos e inicio local
Instalar JDK 21, MySQL Server y PowerShell 7. Agregar mysql.exe a PATH y configurar JAVA_HOME al JDK 21. Maven Wrapper descarga Maven 3.9.9 en su primera ejecución.

Los siguientes bloques se ejecutan en **PowerShell 7**, desde la raíz de RapiEntrega. Si tu terminal muestra una ruta terminada en `>` y estás en CMD, abre PowerShell 7 en esa misma carpeta escribiendo:
```cmd
pwsh
```
Si `pwsh` no se reconoce, instala PowerShell 7 o abre su aplicación desde Inicio. Los comandos `$env:...`, `Invoke-RestMethod` y los scripts `.ps1` no se ejecutan directamente en CMD. Python 3.12+ es opcional y se usa para las pruebas del flujo completo.

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
Cada comando permanece ejecutándose en su terminal. Ctrl+C detiene esa aplicación. Los dos mocks no requieren base. MySQL debe estar iniciado para los otros seis servicios. El perfil demo incorpora un restaurante, un producto de 65.50 MXN y dos repartidores. Las tablas de catálogo se preparan con schema.sql; las demás aplicaciones usan Hibernate update como simplificación académica.

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
Este comando requiere Python 3.12+ y JAVA_HOME. Inicia sus propios procesos Java en puertos libres, usa H2 y los detiene al terminar. No utiliza ni modifica las bases MySQL de la aplicación. Se diferencia de `python pruebas/smoke.py`, que comprueba las aplicaciones que tú ya tienes levantadas en sus puertos habituales.

Archivos disponibles:
- [Colección Postman](pruebas/postman/RapiEntrega.postman_collection.json).
- [Prueba sobre aplicaciones iniciadas](pruebas/smoke.py).
- [Prueba con procesos temporales](pruebas/flujo-local.py).
- [Resultados registrados de las 32 pruebas y del flujo completo](pruebas/resultados/resultado-local.json).
- [Configuración de ejemplo](.env.example).
- [Verificación en GitHub Actions](.github/workflows/verificar.yml).

GitHub Actions ejecuta Maven verify y después la prueba con procesos Java temporales. Los resultados registrados corresponden a H2: no equivalen a una verificación contra MySQL real. Los mocks de pasarela y legado son simulaciones HTTP en memoria y pierden su información al reiniciar. No hay autenticación productiva ni resultados de pruebas de carga. Los endpoints internos están destinados a una red local de confianza.
