# Resultados reales de la ejecución local
Fecha: 23-10-2026. JDK 21.0.8 y Maven 3.9.9. Transporte de eventos HTTP diferido y persistencia por servicio.

Maven verify: BUILD SUCCESS; 32 pruebas, cero errores, cero fallos y cero omitidas. Ocho módulos producen JAR ejecutables. En este sandbox Windows se precargó el agente Byte Buddy y se usó forkCount=0 para evitar el attach dinámico restringido; Surefire tiene configurado el agente Mockito para ejecución normal.

| Clase | Pruebas | Fallos | Errores |
|---|---:|---:|---:|
| mx.edu.rapientrega.legado.LegadoHttpTest | 1 | 0 | 0 |
| mx.edu.rapientrega.pasarela.PasarelaHttpTest | 3 | 0 | 0 |
| mx.edu.rapientrega.calificaciones.CalificacionIntegrationTest | 2 | 0 | 0 |
| mx.edu.rapientrega.catalogo.CatalogoIntegrationTest | 2 | 0 | 0 |
| mx.edu.rapientrega.facturacion.FacturaIntegrationTest | 1 | 0 | 0 |
| mx.edu.rapientrega.logistica.LogisticaConcurrencyTest | 1 | 0 | 0 |
| mx.edu.rapientrega.logistica.LogisticaIntegrationTest | 6 | 0 | 0 |
| mx.edu.rapientrega.pagos.PagoIntegrationTest | 5 | 0 | 0 |
| mx.edu.rapientrega.pedidos.EntregaEventosIntegrationTest | 2 | 0 | 0 |
| mx.edu.rapientrega.pedidos.PedidoIntegrationTest | 9 | 0 | 0 |

## Flujo completo ejecutado
Se iniciaron ocho procesos Spring Boot reales en puertos temporales libres, con una H2 independiente por servicio. Se ejecutó el recorrido de pruebas/smoke.py redirigiendo únicamente los puertos a esas instancias temporales. Resultado APROBADO: pedido con importe de catálogo, pago, asignación, posición, entrega, opinión, factura, respuesta perdida conciliada, rechazo/reembolso y tarjeta rechazada. Los procesos fueron detenidos al terminar y no se conectaron a MySQL ni modificaron datos existentes.

Se repitió el recorrido con el script portable pruebas/flujo-local.py sobre la versión final y también fue APROBADO. Para reproducir: después de mvn verify, ejecutar python pruebas/flujo-local.py con JDK 21 en JAVA_HOME y Python 3.12+. Inicia las aplicaciones, usa H2, registra logs en target/flujo-local y detiene solo sus propios procesos. GitHub Actions ejecuta la suite y este recorrido; el resultado remoto de esta revisión debe confirmarse tras subir el commit.

## Verificaciones nuevas del transporte
- Una publicación de entrega crea tres registros independientes para suscriptores.
- El fallo de facturación no repite las entregas ya confirmadas de pedidos y calificaciones.
- Un reintento posterior completa solo la entrega pendiente.
- POST /interno/eventos responde 204 después de persistir y deduplica una recepción repetida.

Se mantienen las pruebas del límite exacto de cinco minutos, aceptación/rechazo, reembolso idempotente, respuesta ambigua, reserva concurrente, Haversine, posición y opinión de entregado. Los scripts PowerShell se comprobaron con el parser de PowerShell sin ejecutar operaciones de base de datos.

## Límites explícitos
MySQL y su cliente no están disponibles en PATH de este entorno; crear-bases.ps1 y la ejecución sobre MySQL real no se ejecutaron aquí. H2 no demuestra compatibilidad completa de MySQL. WebSocket se prueba hacia SimpMessagingTemplate después del commit; no es una sesión de navegador. Los objetivos p95 requieren prueba de carga. Los mocks son simulaciones HTTP en memoria, pierden operaciones al reiniciar y no representan proveedores productivos. La presentación completa de clase sigue pendiente para el cotejo académico.
