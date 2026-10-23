# Guion de defensa — cinco minutos
0:00–0:40. RapiEntrega conecta clientes, restaurantes y repartidores. El riesgo principal es coordinar cobro y decisión sin aceptar tarde ni devolver dos veces. La facturación heredada se conserva.
0:40–1:30. Presentar cinco dominios y seis servicios. Cada base tiene un único propietario. Pagos y facturación comparten dominio, pero uno posee transacciones y el otro comprobantes. La matriz relaciona cada actividad con una capacidad camelCase.
1:30–2:20. Explicar los cinco pasos provisionales y su validación pendiente con clase. Mostrar Entity Abstraction, Process Abstraction y Legacy Wrapper; Event-Driven Messaging desacopla consumidores. Separar evidencia implementada de diseño.
2:20–3:20. Registrar pedido obtiene precios desde catálogo y usa BigDecimal. Tras confirmar pago, pedidos fija límite UTC cinco minutos adelante. Aceptar exige now menor al límite; a la igualdad ya vence. Rechazo o vencimiento solicitan compensación. Idempotencia evita duplicar efecto.
3:20–4:10. Logística seleccionará por Haversine y reserva atómica. WebSocket comunicará ubicaciones. Entrega generará eventos independientes para pedidos, factura y calificación. Un outbox evita perder eventos entre SQL y RabbitMQ; inbox tolera reentrega.
4:10–5:00. Demostrar flujo mediante Postman y seguimiento, cuando Compose esté levantado. Mostrar resultados reales de pruebas, compensación y limitaciones. Presentar la prueba temporal con Clock para no consumir cinco minutos de exposición.

## Objeciones
- ¿Microservicios equivalen a SOA? No basta dividir procesos: justificamos capacidades, contratos, reutilización, composición y propietarios. Spring Boot es el medio.
- ¿La cancelación sucede exactamente a los cinco minutos? La validez termina en el instante calculado; un scheduler puede materializar el estado después. La aceptación valida el límite independientemente del scheduler.
- ¿Exactly-once? No lo prometemos para la red. Se obtiene efecto idempotente con restricciones locales y contrato idempotente del proveedor.
- ¿Por qué no reemplazar el legado? El caso lo prohíbe; el wrapper protege el dominio y traduce el protocolo.
- ¿Ya escala? La arquitectura permite replicar consumidores, pero falta medir carga y resolver concurrencia; no hay evidencia experimental todavía.
- ¿Cinco pasos oficiales? Propuesta explícita pendiente de cotejo; no atribución al profesor.
- ¿Por qué seis servicios para un ejercicio? Permite mostrar límites exigidos; tiene costes de despliegue, observabilidad y consistencia documentados.

## Réplica al otro equipo
Preguntar cómo resuelven timeout después de cobrar y consumidores de una misma cola. Explicar consulta por clave estable, outbox/inbox y tres suscripciones independientes. Pedir evidencia de aceptación simultánea al vencimiento y de reserva concurrente, sin atribuir defectos sin observar su solución.
