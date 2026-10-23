# Verificación del Reto 2
Fuente: Retos_Unidad_2_Diseno_de_servicios.pdf, páginas 10–17. Se aplica únicamente el Reto 2.

| Requisito | Evidencia y límite |
|---|---|
| Cinco pasos | Reporte, capítulo 2: propuesta aplicada con resultados; cotejo docente pendiente |
| Matriz actividades–servicios | Capítulo 3 y modelado/proceso-y-matriz.md |
| Cuatro dominios mínimo | Cinco dominios con seis servicios y propietarios de datos |
| Tres patrones mínimo | Cuatro seleccionados del catálogo de Erl; cotejo con clase pendiente |
| Comunicación y pub/sub | REST inmediata y entregas HTTP diferidas por suscriptor, sin tablas compartidas |
| Cuatro capacidades | Especificaciones con entradas, salidas, reglas, errores y calidad |
| Implementar código | Ocho aplicaciones Java; notificaciones internas por HTTP y seguimiento WebSocket |
| Documento de 10–15 páginas | PDF de 13 páginas y fuente Markdown |
| Diagrama de dominios | Archivos Mermaid y SVG, figura dentro del reporte |
| Defensa y réplica | Guion cronometrado y argumentos; debate real a cargo del equipo |

La consigna exige código, contratos, estilos de comunicación y justificaciones. La elección de Java, Maven y MySQL es del proyecto. El mecanismo de notificación es asíncrono respecto al comando original y usa HTTP solicitud-respuesta dentro de una tarea de entrega; no se debe llamar asíncrona a la llamada HTTP individual. La entrega a tres suscriptores se registra por separado para tolerar fallos parciales.
