# Patrones de Thomas Erl: selección provisional para clase
Fuente primaria verificada: https://www.thomaserl.com/book/soa-design-patterns/table-of-contents/index.html y https://www.thomaserl.com/book/soa-design-patterns/chapter-descriptions/index.html (consulta 23-10-2026). Los nombres aparecen en el catálogo del libro; falta verificar su inclusión en las diapositivas de clase. No confundir principios de orientación a servicios con patrones.

| Patrón | Problema del caso | Aplicación prevista | Coste / evidencia requerida |
|---|---|---|---|
| Entity Abstraction | Datos de restaurante reutilizados sin propietario claro | Catálogo encapsula restaurantes y productos | Contrato independiente; consultas REST iniciales implementadas |
| Process Abstraction | Compra mezcla cobro, aceptación y entrega | Pedidos centraliza reglas y estados de compra | Coordinador puede ser cuello de botella; probar carreras de aceptación/expiración |
| Legacy Wrapper | Sistema de facturación de 2012 no reemplazable | Facturación traduce HTTP de dominio al contrato legado simulado | Mayor latencia; probar traducción y fallos del adaptador |
| Event-Driven Messaging | Entrega interesa a varios servicios desacoplados | Evento entregado con tres suscripciones independientes | Consistencia eventual, duplicados; probar inbox/outbox |

Evidencia implementada: CatalogoService (Entity Abstraction), pedidos/DominioService (Process Abstraction), facturacion/LegadoAdapter (Legacy Wrapper), Eventos/OutboxPublisher/EventConsumer/Messaging (Event-Driven Messaging). La suite verifica reglas y efectos locales; el transporte real se valida mediante Compose/CI. El mock HTTP de legado representará su frontera, no supondrá que un sistema real de 2012 ofrece REST.
