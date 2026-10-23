# Dominios y propiedad de datos
Seis aplicaciones independientes, cinco dominios y una base MySQL por servicio.

| Dominio | Servicio / tipo | Datos exclusivos | Responsabilidad |
|---|---|---|---|
| Catálogo y restaurantes | catalogo / entidad | Restaurantes, productos, precios, disponibilidad, coordenadas del restaurante | Consultar oferta; no decide estados del pedido |
| Pedidos | pedidos / tarea | Pedido, partidas con precio capturado, dirección, estado, fechas, claves idempotentes | Coordinar compra y decisión del restaurante |
| Pagos y facturación | pagos / utilidad | Intentos, referencias de pasarela, reembolsos | Cobrar y reembolsar por HTTP |
| Pagos y facturación | facturacion / utilidad | Solicitudes, folios y referencias del legado | Traducir contrato al legado; no cobrar |
| Logística y seguimiento | logistica / entidad | Repartidores, disponibilidad, asignaciones y posiciones | Reservar repartidor, actualizar posición, confirmar entrega |
| Calificaciones | calificaciones / entidad | Puntuación, comentario, cliente y pedido | Registrar opinión de entrega confirmada |


Ningún servicio consulta tablas ajenas. scripts/crear-bases.ps1 crea bases y usuarios locales; scripts/iniciar-servicio.ps1 ejecuta cada aplicación mediante Maven. El estado de entrega de eventos se guarda en Outbox del productor y la deduplicación en Inbox del suscriptor. Cada notificación tiene una entrega independiente por destinatario.

Cliente y restaurante se representan con identificadores académicos; no hay autenticación productiva. Los servicios escuchan por defecto en 127.0.0.1. La difusión WebSocket se mantiene dentro de una instancia de logística. Un despliegue mayor requiere optimizar selección de repartidores y barrido de entregas, medir carga y resolver distribución de suscripciones.

El diagrama en diagrama-dominios.mmd identifica los límites y los canales. Los adaptadores de pasarela y legado usan HTTP hacia aplicaciones de simulación separadas. MySQL se instala como servidor local; los procesos Java se inician directamente con Maven.
