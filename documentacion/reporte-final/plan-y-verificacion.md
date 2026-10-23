> Registro histórico de la fase inicial. Para el estado posterior a la implementación, consultar RapiEntrega-Reto2.md y resultados-pruebas.md. Las afirmaciones de pendientes de este registro no describen el código actual.

# Plan incremental y resultados reales
Fecha: 23-10-2026.

Semana 1, fase inicial creada: modelado provisional, matriz, dominios, patrones candidatos, cuatro especificaciones, contratos, seis aplicaciones Spring Boot, consulta funcional de catálogo, SQL demo, Compose de infraestructura y Postman de catálogo.
Semana 1 restante: pedidos/pagos con precios de catálogo, estados, claves idempotentes, adaptador HTTP de pasarela y pruebas de compra/rechazo.
Semana 2: outbox/inbox RabbitMQ, aceptación/expiración concurrentes, compensación, reserva logística, WebSocket, calificaciones, wrapper de facturación HTTP, pruebas de fallos, ejecución Docker de aplicaciones y documento final de 10–15 páginas con PDF revisado. No se considera completada la entrega final ni se exporta un PDF que sugiera resultados inexistentes.

## Registro de comprobaciones
- Inspección inicial: únicamente .git; no había archivos de aplicación que preservar.
- Remoto: origin https://github.com/mafercova/RapiEntrega.git; rama main sin commits.
- git status normal informó “must be run in a work tree” en el entorno de ejecución; git --work-tree=. status funciona. No se alteró configuración para ocultar este comportamiento.
- FitZone: URL solicitada no recuperada por consulta web; no se modificó ni se afirmó haber comprobado su estilo.
- Get-Command: java y Python disponibles; mvn y docker no encontrados en PATH.
- Pruebas Java: escritas dos pruebas de integración para menú/precio y restaurante inexistente. NO EJECUTADAS: falta Maven.
- Docker Compose: NO EJECUTADO: falta Docker en PATH.
- Validación estática Python: parseo XML de los siete POM, JSON Postman y existencia de estructura; consultar resultado de ejecución de esta fase.

## Matriz de pruebas pendientes
| Caso | Nivel previsto | Estado |
|---|---|---|
| Consulta de menú y 404 | Integración MockMvc/H2 escrita | Sin ejecutar |
| Pedido y pago autorizados | Integración HTTP + SQL + broker | Pendiente implementar |
| Pago rechazado | Unitaria + HTTP mock | Pendiente |
| Aceptación antes de límite | Unitaria Clock + integración | Pendiente |
| Rechazo restaurante | Integración y compensación | Pendiente |
| Expiración en igualdad y después | Unitaria Clock, concurrencia SQL | Pendiente |
| Reembolso único ante duplicados/timeout | Integración HTTP con respuesta ambigua | Pendiente |
| Sin repartidores | Unitaria e integración | Pendiente |
| Más cercano y reserva concurrente | Unitaria Haversine + integración SQL | Pendiente |
| Ubicación y difusión WebSocket | Integración WebSocket | Pendiente |
| Calificación de entregado y rechazo de no entregado | Integración | Pendiente |
| Fallos, reintentos y DLQ | Integración broker/adaptadores | Pendiente |

La colección Postman actual verifica solo catálogo. El documento académico completo se redactará con evidencia de las siguientes fases; esta documentación es la base trazable de diseño.

Resultado estático ejecutado: siete POM parseados como XML, colección Postman parseada como JSON y seis clases Application presentes. Comprobación satisfactoria; no sustituye compilación ni pruebas de Spring.
