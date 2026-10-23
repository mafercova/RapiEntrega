> Registro histórico de la fase inicial. Para el estado posterior a la implementación, consultar RapiEntrega-Reto2.md y resultados-pruebas.md. Las afirmaciones de pendientes de este registro no describen el código actual.

# Verificación del Reto 2 contra la consigna original
Fecha: 23 de octubre de 2026. Resultado: cumplimiento parcial; no listo para entrega.
Fuente: Retos_Unidad_2_Diseno_de_servicios.pdf, páginas 10–17. Se considera únicamente Reto 2. La fecha de entrega del documento está en blanco: no se inventa una fecha.

## Requisitos y evidencia
| Exigencia de la consigna | Página | Evidencia actual | Veredicto / acción de cierre |
|---|---|---|---|
| Modelar Pedido de comida en cinco pasos | 13 | modelado/proceso-y-matriz.md enumera cinco pasos provisionales | Parcial: desarrollar resultados por paso y cotejar orden/nombres con presentación de Unidad 2 |
| Matriz actividades–servicios | 13 | 19 filas con actividad, servicio, tipo, capacidad e interacción | Presente como diseño; comprobar cobertura al implementar y mantener trazabilidad |
| Mínimo cuatro dominios | 13 | dominios/arquitectura.md: cinco dominios, seis servicios, propietarios | Cubierto a nivel de diseño; datos separados previstos |
| Mínimo tres patrones concretos justificados | 13–14 | patrones/justificacion.md: cuatro candidatos con problema, aplicación y coste | Parcial: verificar nombres contra catálogo de clase; tres aplicaciones aún pendientes en código |
| Síncrono/asíncrono por interacción y pub/sub | 13 | matriz y contratos-api/contratos.md, exchange, colas y consumidores | Parcial: justificar por interacción y corregir omisiones REST de consulta de posición; RabbitMQ y WebSocket pendientes |
| Cuatro capacidades completas con plantilla Unidad 2 | 13 | especificaciones/capacidades.md: entradas, salidas, reglas, errores y calidad para cuatro | Contenido base presente; falta cotejar plantilla exacta y enlazar a métodos implementados |
| Implementar código | 14 | catálogo REST/JPA, seis aplicaciones; demás paquetes contienen principalmente package-info | Parcial: el proceso Pedido de comida no está implementado ni demostrado |
| Justificar cada decisión con problema del caso | 14 | problemas y costes en arquitectura, contratos y patrones | Parcial: añadir justificación explícita por interacción y límites de escalabilidad |
| Documento integrado de 10–15 páginas | 15 | Markdown separados de diseño; ningún documento final paginado | No cumplido: consolidar y verificar extensión real, legibilidad y coherencia |
| Diagrama de dominios, servicios e interacciones | 15 | diagrama-dominios.mmd creado en esta revisión | Diseño presente: cinco agrupaciones y flujos etiquetados; pendiente exportación visual para entrega |
| Defensa oral de cinco minutos y réplica | 13,15 | reporte-final/defensa.md: guion cronometrado y objeciones | Preparación parcial: agregar réplica explícita, ensayar; debate real no se acredita con un archivo |

## Rúbrica literal de evaluación y riesgos
| Criterio | Peso | Riesgo actual |
|---|---|---|
| Modelado del proceso | 20 % | Cinco pasos provisionales sin resultados desarrollados suficientes |
| Mapeo y dominios | 20 % | Base favorable; revisar matriz y diagrama integrado |
| Patrones de diseño | 20 % | Falta confirmación del catálogo visto en clase y evidencia de aplicación |
| Comunicación y mensajes | 15 % | Diseño definido, integración sin implementar y algunas justificaciones implícitas |
| Especificaciones | 15 % | Cuatro capacidades diseñadas, plantilla exacta pendiente |
| Defensa en debate | 10 % | Guion preparado; desempeño y réplica requieren ensayo y debate |

No se calcula una calificación ni porcentaje de cumplimiento: la rúbrica requiere juicio docente y evidencia aún no disponible.

## Alcance: consigna frente a decisiones del proyecto
La consigna exige código, cinco pasos, matriz, dominios, patrones, comunicación, cuatro capacidades, documento de 10–15 páginas, diagrama y debate. No especifica Java 21, Spring Boot, Maven, MySQL, RabbitMQ, WebSocket, seis aplicaciones, Docker, Postman ni una batería particular de pruebas. Esas tecnologías y verificaciones provienen de la solicitud previa del proyecto y siguen vigentes; no deben atribuirse al profesor. El Reto 2 no fija los nombres de las cuatro capacidades: registrarPedido, procesarPago, asignarRepartidor y actualizarUbicacion son la selección del proyecto.

El PDF tampoco explica los cinco pasos ni reproduce el catálogo ni la plantilla completa. No resuelve esas incertidumbres; hace falta la presentación de Unidad 2 para verificarlas. draw.io y Lucidchart aparecen como herramientas de apoyo, no como una restricción explícita de formato. El Reto 1 de FitZone y su límite de cinco páginas no aplican a esta entrega.

## Orden necesario para cerrar la entrega
1. Cotejar cinco pasos, catálogo de patrones y plantilla con la presentación completa cuando esté disponible; avanzar sin inventar atribuciones mientras tanto.
2. Implementar pedidos y pagos con precios de catálogo, pasarela HTTP simulada y estados; probar autorización y rechazo.
3. Implementar aceptación, rechazo y límite exacto de cinco minutos; cancelación y reembolso idempotente ante concurrencia y reentregas.
4. Conectar eventos RabbitMQ con outbox/inbox; implementar selección geográfica, reserva, posiciones WebSocket y entrega.
5. Completar calificación y adaptador HTTP de facturación heredada simulada. Verificar errores y reintentos.
6. Compilar y ejecutar pruebas reales; registrar resultados y límites. Las dos pruebas de catálogo actuales están escritas, sin ejecución acreditada. En la fase anterior mvn test falló porque mvn no estaba en PATH; no se ha comprobado ejecución Docker.
7. Consolidar documento de 10–15 páginas con diagrama y referencias; exportar y revisar paginación. Ensayar defensa y réplica usando resultados reales.

## Réplica preparada para otro equipo
Pregunta: ¿Cómo evitan que una aceptación simultánea al vencimiento deje un pedido cobrado y cancelado de forma contradictoria?
Argumento propio: el estado y el límite se validan en la misma actualización protegida por versión o bloqueo; aceptar exige now < limite. El temporizador no es la única protección. La compensación se identifica por pago y clave estable.
Pregunta: ¿Qué ocurre si el proveedor cobra pero se pierde su respuesta?
Argumento propio: consultar estado con la misma clave idempotente antes de crear otro intento. Un timeout no demuestra rechazo.
Pregunta: ¿Una sola cola para factura, pedidos y calificaciones entrega el evento a los tres?
Argumento propio: una cola con consumidores competidores distribuye mensajes; hacen falta tres suscripciones independientes para que los tres reciban cada hecho.
Estos argumentos describen el diseño objetivo: deben respaldarse con implementación y pruebas antes de presentarlos como comportamiento demostrado.
