# RapiEntrega

Plataforma de entregas construida como un monorepositorio de microservicios con Java 21 y Spring Boot.

El sistema permite gestionar restaurantes, pedidos, pagos, logística, facturación y calificaciones mediante APIs REST y comunicación asíncrona con RabbitMQ.

## Tecnologías

- Java 21
- Spring Boot
- Maven
- MySQL
- RabbitMQ
- REST
- WebSocket
- Docker Compose

## Arquitectura

El proyecto está dividido en los siguientes servicios:

| Servicio | Puerto | Responsabilidad |
|---|---:|---|
| Catálogo | 8081 | Restaurantes y productos |
| Pedidos | 8082 | Creación y seguimiento de pedidos |
| Pagos | 8083 | Procesamiento y compensación de pagos |
| Logística | 8084 | Asignación y seguimiento de repartidores |
| Facturación | 8085 | Generación de facturas |
| Calificaciones | 8086 | Opiniones y calificaciones |
| Pasarela mock | 8091 | Simulación del proveedor de pagos |
| Facturación legado mock | 8092 | Simulación del sistema de facturación externo |

Infraestructura:

| Componente | Puerto |
|---|---:|
| MySQL | 3306 |
| RabbitMQ | 5672 |
| Panel de administración de RabbitMQ | 15672 |

## Requisitos

Para ejecutar el proyecto completo:

- Docker Desktop
- Docker Compose
- PowerShell 7

Para compilar y probar sin Docker:

- JDK 21
- Maven 3.9+ o Maven Wrapper

## Ejecución rápida con Docker

Desde la raíz del repositorio:

```powershell
.\scripts\preparar-entorno.ps1
docker compose up -d --build
```

Para comprobar el estado de los servicios:

```powershell
docker compose ps
```

Para consultar los logs:

```powershell
docker compose logs -f pedidos pagos logistica
```

Para detener los servicios:

```powershell
docker compose down
```

Los servicios quedan disponibles en `localhost` usando los puertos indicados en la tabla de arquitectura.

## Compilar y ejecutar pruebas

Con PowerShell:

```powershell
.\mvnw.cmd verify
```

También es posible utilizar una instalación local de Maven:

```powershell
mvn verify
```

Las pruebas unitarias y de integración utilizan bases de datos y servicios simulados cuando corresponde, por lo que no requieren levantar toda la infraestructura para ejecutarse.

## Ejemplo de flujo principal

### 1. Consultar restaurantes

```powershell
Invoke-RestMethod http://localhost:8081/api/v1/restaurantes
```

### 2. Consultar el menú de un restaurante

```powershell
Invoke-RestMethod http://localhost:8081/api/v1/restaurantes/1/menu
```

### 3. Crear un pedido

```powershell
$pedido = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8082/api/v1/pedidos `
  -ContentType application/json `
  -Headers @{
    'Idempotency-Key' = [guid]::NewGuid().ToString()
  } `
  -Body '{
    "clienteId": "cliente-1",
    "restauranteId": 1,
    "items": [
      {
        "productoId": 1,
        "cantidad": 1
      }
    ],
    "entrega": {
      "latitud": 19.44,
      "longitud": -99.14,
      "direccion": "Campus universitario"
    }
  }'
```

### 4. Consultar el pedido

```powershell
Invoke-RestMethod "http://localhost:8082/api/v1/pedidos/$($pedido.id)"
```

Para consultar el flujo completo y las peticiones disponibles, puede importarse la colección:

```text
pruebas/postman/RapiEntrega.postman_collection.json
```

## Smoke tests

Con los servicios levantados:

```powershell
python pruebas/smoke.py
```

## Seguimiento en tiempo real

Después de asignar un pedido, puede abrirse la vista de seguimiento en:

```text
http://localhost:8084/seguimiento.html?pedidoId=ID_DEL_PEDIDO
```

La vista muestra la última posición del pedido y las actualizaciones recibidas mediante WebSocket.

## Estructura del repositorio

```text
.
├── servicios/
│   ├── catalogo-service/
│   ├── pedidos-service/
│   ├── pagos-service/
│   ├── logistica-service/
│   ├── facturacion-service/
│   └── calificaciones-service/
├── integraciones/
│   ├── pasarela-mock/
│   └── facturacion-legado-mock/
├── pruebas/
├── scripts/
├── docker-compose.yml
└── pom.xml
```

## Notas

Los servicios mock están pensados para desarrollo y pruebas. Utilizan almacenamiento en memoria, por lo que su información se pierde al reiniciarlos.

Las variables de entorno y credenciales locales deben mantenerse fuera del repositorio. No subas archivos `.env` con información sensible.

## Entregables académicos del Reto 2

- [Documento de diseño de 13 páginas](documentacion/reporte-final/RapiEntrega-Reto2.pdf).
- [Diagrama de dominios](documentacion/dominios/diagrama-dominios.mmd).
- [Especificaciones y contratos](documentacion/especificaciones/capacidades.md).
- [Defensa de cinco minutos](documentacion/reporte-final/defensa.md).
- [Resultados reales y límites](documentacion/reporte-final/resultados-pruebas.md).

Los cinco pasos y los patrones deben cotejarse con la presentación completa de Unidad 2, que aún no se ha proporcionado. Los mocks y las pruebas no sustituyen esa validación académica.
