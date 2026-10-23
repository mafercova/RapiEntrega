# Resultados reales de verificación
Fecha: 23-10-2026. Entorno Windows, JDK 21.0.8 y Maven 3.9.9 descargado en la carpeta de trabajo.

Última ejecución de verify: BUILD SUCCESS; 29 pruebas, 0 fallos/errores. Los ocho módulos de aplicaciones produjeron JAR ejecutable.

| Clase | Pruebas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| mx.edu.rapientrega.calificaciones.CalificacionIntegrationTest | 2 | 0 | 0 | 0 |
| mx.edu.rapientrega.catalogo.CatalogoIntegrationTest | 2 | 0 | 0 | 0 |
| mx.edu.rapientrega.facturacion.FacturaIntegrationTest | 1 | 0 | 0 | 0 |
| mx.edu.rapientrega.logistica.LogisticaConcurrencyTest | 1 | 0 | 0 | 0 |
| mx.edu.rapientrega.logistica.LogisticaIntegrationTest | 6 | 0 | 0 | 0 |
| mx.edu.rapientrega.pagos.PagoIntegrationTest | 5 | 0 | 0 | 0 |
| mx.edu.rapientrega.pedidos.PedidoIntegrationTest | 8 | 0 | 0 | 0 |
| mx.edu.rapientrega.legado.LegadoHttpTest | 1 | 0 | 0 | 0 |
| mx.edu.rapientrega.pasarela.PasarelaHttpTest | 3 | 0 | 0 | 0 |

## Comando realmente ejecutado
Se usó JAVA_HOME apuntando al JDK 21, repositorio Maven local dentro de la carpeta de trabajo y mvn -B -DforkCount=0 verify. En este sandbox Windows la conexión de attach de Mockito produjo acceso denegado; se precargó byte-buddy-agent mediante JAVA_TOOL_OPTIONS y se evitó el fork. La configuración versionada de Surefire precarga mockito-core como agente para ejecuciones normales. No se oculta este ajuste del entorno.

## Alcance y límites
- H2 en modo MySQL para persistencia y bloqueos; MockRestServiceServer para adaptadores; MockMvc para los mocks HTTP. No equivalen a pruebas contra MySQL y RabbitMQ reales.
- La prueba de WebSocket confirma emisión después del commit hacia SimpMessagingTemplate, no un navegador real ni el relay de RabbitMQ.
- La prueba concurrente reserva dos repartidores distintos desde dos solicitudes simultáneas.
- Docker no está disponible localmente: Compose y pruebas/smoke.py no se ejecutaron aquí. GitHub Actions incluye un trabajo separado que construye la infraestructura, ejecuta el flujo HTTP completo y conserva sus logs. Su resultado se consulta en Actions; no se presupone aprobado.
- Sin pruebas de carga: los valores p95 de las especificaciones son objetivos pendientes de medición.
- Mocks con almacenamiento en memoria: un reinicio pierde su registro idempotente. Es una simulación académica, no una garantía de proveedor real.
- La defensa oral requiere ensayo y participación del equipo; no puede acreditarse por generación de archivos.

## Historial de correcciones
El primer intento de prueba se detuvo por attach del agente de Mockito. Se corrigió la precarga del agente. Después las pruebas de pagos detectaron expectativas HTTP agregadas demasiado tarde y una aserción sobre una instancia JPA separada; se corrigieron las pruebas. Se corrigió también la actualización de la asignación sobre la instancia gestionada devuelta por saveAndFlush. La suite posterior pasó completa.

## Revisión del repositorio publicado
GitHub Actions, ejecución 40004962894, commit b64c08b: trabajo pruebas aprobado con Maven verify en Linux. Trabajo flujo-completo falló al iniciar MySQL, antes de smoke.py. El artefacto compose.log registra MYSQL_ONETIME_PASSWORD: unbound variable. Causa: el entrypoint carga 01-usuarios.sh mediante source y heredaba su set -u. Corrección: encerrar el script en un subshell; prueba de regresión pruebas/test-init.sh para comprobar que nounset no se filtra. La corrección necesita una nueva ejecución de Actions; no se afirma todavía que el E2E esté aprobado.
