# RapiEntrega · Reto 2 de Diseño de servicios
Monorepositorio académico de seis servicios independientes y dos integraciones HTTP simuladas. Java 21, Spring Boot, Maven, MySQL, JPA, Bean Validation, RabbitMQ, REST y WebSocket. El cliente consulta oferta, registra y paga; el restaurante acepta o rechaza; tras aceptación se asigna el disponible más cercano; se actualizan posiciones, entrega, factura y calificación. Sin aceptación en cinco minutos: cancelación y reembolso idempotente.

Se aplican cinco pasos propuestos y cuatro patrones del catálogo de Erl. La presentación completa de Unidad 2 no fue proporcionada: falta cotejar sus nombres/orden, selección de patrones y encabezados exactos de plantilla. El PDF del reto por sí solo no los explica. La defensa oral y la entrega institucional corresponden al equipo.

## Inicio con Docker en PowerShell
Requisitos: Docker Desktop con Compose, PowerShell 7 para generar secretos y Python 3 opcional para smoke. La compilación de aplicaciones ocurre dentro de Docker con Java 21/Maven.
```powershell
.\scripts\preparar-entorno.ps1
docker compose up -d --build
docker compose ps
docker compose logs -f pedidos pagos logistica
```
El script conserva un .env existente y genera claves aleatorias sin mostrarlas. No subir .env. Las seis contraseñas de aplicaciones son alfanuméricas y cada usuario solo accede a su base. La inicialización SQL se ejecuta únicamente al crear un volumen nuevo; conservar o respaldar datos antes de cambiar credenciales o infraestructura. No se incluye ningún comando que borre datos automáticamente.

El arranque puede tardar varios minutos mientras se descargan imágenes y compila. Consultar catálogo para comprobar disponibilidad:
```powershell
Invoke-RestMethod http://localhost:8081/api/v1/restaurantes
Invoke-RestMethod http://localhost:8081/api/v1/restaurantes/1/menu
python pruebas/smoke.py
# Detener conservando volumen:
docker compose down
```
Compose y smoke no se ejecutaron localmente por falta de Docker; el trabajo flujo-completo de GitHub Actions los ejecuta en Linux y guarda logs. Consultar su resultado real antes de dar esa verificación por aprobada.

## Compilar y probar sin Docker
Requisitos: JDK 21 y Maven 3.9+ o Maven Wrapper. Configurar JAVA_HOME al JDK 21; el wrapper descarga Maven 3.9.9 desde Maven Central en su primer uso.
```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21' # adaptar a la instalación
.\mvnw.cmd verify
# Con Maven instalado:
mvn verify
```
Las pruebas usan H2, MockRestServiceServer y MockMvc; no necesitan infraestructura. Surefire precarga el agente Mockito para evitar attach dinámico. En este sandbox Windows la ejecución se realizó sin fork y con agente Byte Buddy precargado: detalle en resultados-pruebas.md. Esa configuración local no se exige a otros entornos.

Para ejecutar un servicio contra MySQL/RabbitMQ locales: crear su base/usuario, asignar DB_URL, DB_USER, DB_PASSWORD y credenciales del broker. Maven no carga .env automáticamente.
```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/catalogo'
$env:DB_USER = 'catalogo'
$env:DB_PASSWORD = 'COPIAR_VALOR_LOCAL_DE_CATALOGO_DB_PASSWORD'
$env:RABBITMQ_USER = 'rapientrega'
$env:RABBITMQ_PASSWORD = 'COPIAR_VALOR_LOCAL_DE_RABBITMQ_PASSWORD'
$env:SPRING_PROFILES_ACTIVE = 'demo'
.\mvnw.cmd -pl servicios/catalogo-service spring-boot:run
```
Otros módulos se ejecutan igual cambiando base, usuario y directorio. Los mocks no necesitan base ni broker: `-pl integraciones/pasarela-mock` o `-pl integraciones/facturacion-legado-mock`. Los servicios con mensajería necesitan RabbitMQ; MESSAGING_ENABLED=false permite pruebas aisladas, pero no ejecuta el proceso distribuido.

## Puertos y variables
| Aplicación | Puerto | Datos propios |
|---|---:|---|
| catálogo | 8081 | restaurantes/productos |
| pedidos | 8082 | pedido, partidas, estados, límite |
| pagos | 8083 | pago, referencia, compensación |
| logística | 8084 | repartidores, asignaciones, posiciones |
| facturación | 8085 | trabajos, folios del legado |
| calificaciones | 8086 | proyección de entrega y opinión |
| pasarela mock | 8091 | operaciones en memoria, simulación |
| legado mock | 8092 | folios en memoria, simulación |
| MySQL / RabbitMQ | 3306 / 5672 | bases independientes / eventos |
| RabbitMQ administración | 15672 | colas, DLQ y diagnóstico |

Compose limita puertos publicados a 127.0.0.1. STOMP 61613 es interno. Variables por servicio: PORT, DB_URL, DB_USER, DB_PASSWORD, RABBITMQ_HOST, RABBITMQ_USER, RABBITMQ_PASSWORD, CATALOGO_URL, PEDIDOS_URL, PASARELA_URL, LEGADO_URL, MESSAGING_ENABLED. WEBSOCKET_RELAY=true activa el relay entre réplicas; requiere plugin rabbitmq_stomp (Compose lo habilita). Por defecto fuera de Compose se usa broker WebSocket simple local. El perfil demo crea catálogo de muestra y dos repartidores cuando no existen; catálogo genera esquema SQL explícito y el resto usa Hibernate update, decisión académica que debe sustituirse por migraciones para producción.

## Demostración manual
Importar pruebas/postman/RapiEntrega.postman_collection.json. Cambiar runId antes de iniciar cada compra y esperar los cambios asíncronos indicados en los nombres de peticiones. La petición alternativa de rechazo se usa con un pedido nuevo: no ejecutar sobre el ya entregado. Producto 1 cuesta 65.50 MXN; dos unidades suman 131.00.
```powershell
$pedido = Invoke-RestMethod -Method Post -Uri http://localhost:8082/api/v1/pedidos -ContentType application/json -Headers @{'Idempotency-Key'=[guid]::NewGuid().ToString()} -Body '{"clienteId":"cliente-demo","restauranteId":1,"items":[{"productoId":1,"cantidad":2}],"entrega":{"latitud":19.44,"longitud":-99.14,"direccion":"Campus universitario"}}'
$pago = Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/v1/pagos -ContentType application/json -Headers @{'Idempotency-Key'=[guid]::NewGuid().ToString()} -Body (@{pedidoId=$pedido.id;tokenTarjeta='ok'} | ConvertTo-Json)
Invoke-RestMethod "http://localhost:8082/api/v1/pedidos/$($pedido.id)"
# Esperar PENDIENTE_ACEPTACION y ejecutar antes de cinco minutos:
Invoke-RestMethod -Method Post "http://localhost:8082/api/v1/pedidos/$($pedido.id)/aceptacion"
Invoke-RestMethod "http://localhost:8084/api/v1/asignaciones/$($pedido.id)"
```
Después de asignar, abrir `http://localhost:8084/seguimiento.html?pedidoId=ID_DEL_PEDIDO` en un navegador y enviar posición desde Postman. La vista muestra última posición y cambios WebSocket. Completar entrega, esperar el evento y registrar calificación; consultar factura hasta EMITIDA. Endpoints completos, payloads, canales y políticas están en documentacion/contratos-api/contratos.md y documentacion/especificaciones/capacidades.md.

Alternativas: token reject rechaza la tarjeta; timeout-once cobra y pierde una respuesta para demostrar conciliación. Rechazar restaurante usa POST /api/v1/pedidos/{id}/rechazo. Para vencimiento, dejar el pedido pagado sin aceptar durante 300 segundos desde pendienteAceptacionAt y consultar CANCELADO y REEMBOLSADO. Las pruebas con Clock verifican el instante exacto sin espera real. POST de aceptación vencida devuelve 409 y conserva cancelación.

## Fiabilidad y recuperación
Outbox transaccional, publisher confirms, mandatory returns, inbox por eventId y restricciones por pedido. Entrega al menos una vez; efectos idempotentes, sin prometer exactly-once de red. RabbitMQ: tres intentos totales, espera 1/2 s y DLQ. Trabajos de reembolso/factura: tres intentos, espera 2/4 s y revisión. Recuperar con POST /api/v1/pagos/pedido/{id}/reintentar-reembolso o POST /api/v1/facturas/{id}/reintentar tras resolver el fallo. Los mocks aceptan X-Mock-Failures en llamadas directas para demostrar fallos transitorios. No hay un endpoint público para fabricar eventos.

Limitaciones explícitas: actores identificados sin autenticación de producción; pasarela y legado son simulaciones HTTP y pierden estado al reiniciar; sin pruebas de carga; difusión de posición mejor esfuerzo y recuperación REST. La selección de candidatos y outbox deben optimizarse para gran volumen. Las aplicaciones no comparten tablas ni lógica de negocio. FitZone no se modifica.
