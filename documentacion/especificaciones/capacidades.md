# Cuatro capacidades y su implementación
Plantilla de trabajo: servicio, capacidad, entradas/tipos, salidas/tipos, reglas, errores y calidad. El PDF del reto no reproduce la plantilla completa: queda pendiente cotejar sus encabezados con la presentación de Unidad 2. Todos los identificadores de API son strings; pedidoId, pagoId y eventId se generan como UUID. Los objetivos de rendimiento siguientes son criterios de aceptación, no mediciones de carga obtenidas.

## registrarPedido
Servicio: pedidos-service; tipo tarea. Código: services/DominioService.registrarPedido; POST /api/v1/pedidos, 201.

Entradas: Idempotency-Key string no vacío de máximo 128; clienteId string no vacío; restauranteId int64 positivo; items array de 1..100 elementos {productoId int64 positivo, cantidad int32 de 1..99}; entrega {latitud double [-90,90], longitud double [-180,180], direccion string de 1..300}. El DTO se llama RegistrarPedido y aplica Bean Validation. No se admite precio enviado por el cliente como autoridad.

Salidas: Pedido con id string UUID, version int64, estado string PAGO_PENDIENTE, total BigDecimal con escala 2, clienteId string, restauranteId int64, dirección y coordenadas; partidas string JSON con productoId int64, cantidad int32 y precioUnitario decimal. Incluye clave y solicitud serializada para trazabilidad idempotente, sin información de tarjeta. pagoId, limite y pendienteAceptacionAt son nulos antes de confirmar el pago. La moneda de la solución académica es MXN.

Reglas: consultar menú y restaurante por REST; todos los productos deben estar disponibles y pertenecer al mismo restaurante. Rechazar productos duplicados en vez de sumarlos implícitamente. Calcular suma de precio*cantidad mediante BigDecimal; máximo total 9999999999.99. Capturar precios históricos en las partidas. Misma clave y mismo DTO retornan el mismo pedido; misma clave y otro cuerpo generan conflicto. Registrar no inicia los cinco minutos: la espera comienza cuando se confirma el pago en pedidos.

Errores: 400 DATOS_INVALIDOS o CLAVE_INVALIDA; 422 PRODUCTO_DUPLICADO, PRODUCTO_NO_DISPONIBLE, RESTAURANTE_NO_DISPONIBLE o IMPORTE_EXCESIVO; 409 IDEMPOTENCY_CONFLICT o CONFLICTO_CONCURRENCIA_REINTENTAR; 503 DEPENDENCIA_NO_DISPONIBLE cuando catálogo falla. Las respuestas de error usan ProblemDetail con code y correlationId. La ausencia comunicada por un servicio remoto se traduce actualmente como dependencia indisponible, no como un 404 local.

Calidad verificable: pruebas de precio 131.00 por dos productos de 65.50, clave idempotente y estados posteriores. Objetivo p95 <=3 s con 20 clientes y catálogo sano; pendiente prueba de carga. Cada llamada HTTP conecta en <=2 s y espera lectura hasta 3 s. No se garantiza respuesta total de 3 s si se encadenan dependencias. La restricción UNIQUE en clave impide crear duplicados durante carreras; un perdedor puede recibir 409 y debe repetir con la misma clave.

## procesarPago
Servicio: pagos-service; tipo utilidad. Código: services/DominioService.procesarPago y PasarelaAdapter.cobrar; POST /api/v1/pagos, 201.

Entradas: Idempotency-Key string de 1..128; ProcesarPago {pedidoId string no vacío, tokenTarjeta string no vacío}. Importe BigDecimal y estado se consultan a pedidos mediante GET /api/v1/pedidos/{id}; no se aceptan del cliente. El token es ficticio: ok autoriza, reject rechaza, timeout-once autoriza y simula una respuesta perdida.

Salidas: Pago con id string UUID, pedidoId string, clave string, fingerprint string SHA-256, importe BigDecimal, estado enum de negocio AUTORIZADO o RECHAZADO, referencia string externa simulada, campos de compensación inicialmente nulos y contador de reintentos int32. El token no se persiste ni se devuelve.

Reglas: pedido debe estar PAGO_PENDIENTE y no tener otro pago. Un pago por pedido y una clave por operación mediante restricciones UNIQUE. Comparar fingerprint de pedido y token para detectar clave reutilizada con otro cuerpo. El adaptador HTTP cobra con clave estable charge-{pedidoId}, incluso si dos llamadas cliente usan claves diferentes; esta segunda barrera evita un doble cargo externo. Ante fallo del POST consulta /mock/operaciones/{key} antes de devolver error. Si la operación aún no existe, el cliente puede repetir el POST original con la misma clave. No hay un bucle automático de cobro ciego. Autorización y rechazo generan entregas HTTP diferidas en outbox dentro de la transacción SQL.

Compensación: pedido.cancelado marca REEMBOLSO_PENDIENTE. Un trabajo consulta pendientes y reembolsa con refund-{pagoId}. La misma referencia de operación se conserva ante reintento y ante respuesta perdida. Un fallo programa esperas de 2 y 4 segundos; al fallar el tercer intento queda REEMBOLSO_REVISION. POST /api/v1/pagos/pedido/{id}/reintentar-reembolso permite recuperación manual. Un pago reembolsado no vuelve a entrar a pendientes por un evento repetido.

Errores: 400 DATOS_INVALIDOS o CLAVE_INVALIDA; 409 IDEMPOTENCY_CONFLICT, PEDIDO_YA_PROCESADO, PEDIDO_NO_PAGABLE o conflicto de concurrencia; 503 DEPENDENCIA_NO_DISPONIBLE. Rechazo de tarjeta es un resultado de negocio 201 con estado RECHAZADO, no una autorización ni un fallo técnico.

Calidad verificable: pruebas de autorización/rechazo, consulta posterior a respuesta perdida, reembolso único y reintento con la misma clave. No se promete exactly-once de red; se busca un efecto idempotente. El mock pierde operaciones al reiniciarse: esa limitación impide extrapolar la garantía a un proveedor productivo. Objetivo de carga p95 <=4 s con proveedores sanos, pendiente medir. El log no registra tokens ni números de tarjeta.

## asignarRepartidor
Servicio: logistica-service; tipo entidad. Código: services/DominioService.consumir y asignarRepartidor; POST /api/v1/asignaciones, 200 ASIGNADO o 202 ASIGNACION_PENDIENTE.

Entradas REST: Solicitud {pedidoId string}. La recogida no se acepta arbitrariamente desde el cliente: se obtiene del evento pedido.aceptado.v1 con pedidoId string, clienteId string, restauranteId int64, latitud double [-90,90] y longitud double [-180,180]. El sobre incorpora eventId UUID y versión 1. El eventId protege la reentrega; pedidoId es la clave natural idempotente de la asignación.

Salidas: Asignacion {pedidoId string, clienteId string, repartidorId string o null, estado string, distanciaKm double, recogidaLat double, recogidaLon double, latitud double, longitud double, secuencia int64, capturedAt instant o null, receivedAt instant o null}. No existe un segundo asignacionId: la asignación pertenece de forma única al pedido.

Reglas: solo asignar pedidos incorporados a partir de un evento de aceptación. Comparar únicamente repartidores disponibles. Distancia Haversine: a=sin²(deltaLat/2)+cos(lat1)*cos(lat2)*sin²(deltaLon/2); d=2*6371*atan2(sqrt(a),sqrt(1-a)), ángulos en radianes y a limitado a [0,1]. Es distancia geodésica en km; no considera tráfico ni rutas viales. Desempate por identificador lexicográfico. Bloqueo pesimista sobre candidatos y asignación para reservar sin duplicar. Si no hay disponibles, conservar ASIGNACION_PENDIENTE y emitir asignacion.pendiente. Un trabajo reintenta cada diez segundos. Entregar libera al repartidor. REST de asignación repite el resultado existente y no cambia el repartidor de una entrega ya asignada.

Errores: 409 PEDIDO_NO_ACEPTADO para solicitud sin aceptación recibida; 404 ASIGNACION_NO_EXISTE en consulta; 409 CONFLICTO_CONCURRENCIA_REINTENTAR por disputa SQL. Ausencia de repartidores devuelve 202, no error técnico. El receptor rechaza eventos malformados con 400 EVENTO_INVALIDO. La entrega permanece pendiente en el emisor para revisión y reintento.

Calidad verificable: prueba de un grado de longitud en ecuador aproximadamente 111.195 km con tolerancia 0.01 km; selección del más cercano; reserva de distintos repartidores en dos solicitudes concurrentes. Objetivo p95 <=1 s con 1000 repartidores, pendiente medir. El bloqueo de todos los candidatos prioriza corrección para el ejercicio y limita rendimiento: un despliegue mayor requiere índice geoespacial y selección por lotes, manteniendo reserva atómica.

## actualizarUbicacion
Servicio: logistica-service; tipo entidad. Código: services/DominioService.actualizarUbicacion y WebSocketConfig; POST /api/v1/asignaciones/{pedidoId}/ubicaciones, 200.

Entradas: pedidoId string en ruta; Ubicacion {repartidorId string no vacío, latitud double [-90,90], longitud double [-180,180], capturedAt instant ISO-8601 no nulo, sequence int64 >=0}. Identidad por identificador académico: no hay autenticación de producción. La validación Bean Validation aplica a REST.

Salidas: Asignacion con posición persistida, secuencia y timestamps UTC. Tras commit se emite mensaje {pedidoId string, latitud double, longitud double, sequence int64, capturedAt instant, receivedAt instant} a /topic/pedidos/{pedidoId}; conexión WebSocket STOMP en /ws. GET /api/v1/asignaciones/{pedidoId} permite recuperar la última posición. La vista /seguimiento.html facilita la demostración.

Reglas: solo repartidor asignado y estado ASIGNADO. Rechazar fecha futura mayor a 30 s. Exigir secuencia creciente; repetición exacta de secuencia, coordenadas y capturedAt retorna el resultado anterior; cualquier otro mensaje atrasado genera conflicto. Persistir posición y actualizar coordenadas del repartidor. Emitir únicamente tras commit para no divulgar datos revertidos. Si la difusión WebSocket falla, la posición permanece consultable: la difusión es mejor esfuerzo, sin promesa de replay. Se utiliza el soporte STOMP/WebSocket integrado de Spring y una sola instancia de logística; no se distribuyen suscripciones entre procesos.

Errores: 400 DATOS_INVALIDOS o FECHA_FUTURA; 404 ASIGNACION_NO_EXISTE; 403 REPARTIDOR_NO_ASIGNADO; 409 ENTREGA_FINALIZADA, POSICION_ATRASADA o CONFLICTO_CONCURRENCIA_REINTENTAR.

Calidad verificable: persistencia de coordenadas, rechazo de secuencia atrasada, repetición exacta y emisión a SimpMessagingTemplate después del commit. Esta prueba no equivale a comprobar una conexión de navegador completa. Objetivo p95 <=1 s entre recepción y emisión con 20 conexiones, pendiente medir. Los servicios escuchan en 127.0.0.1 por defecto (SERVER_ADDRESS); falta autenticación/autorización para uso real.
