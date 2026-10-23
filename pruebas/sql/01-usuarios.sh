#!/bin/sh
set -eu
case "${CATALOGO_DB_PASSWORD}" in ''|*[!a-zA-Z0-9]*) echo "CATALOGO_DB_PASSWORD: usar solo caracteres alfanuméricos generados" >&2; exit 1;; esac
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root <<SQL
CREATE DATABASE IF NOT EXISTS catalogo;
CREATE USER IF NOT EXISTS 'catalogo'@'%' IDENTIFIED BY '${CATALOGO_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON catalogo.* TO 'catalogo'@'%';
SQL
case "${PEDIDOS_DB_PASSWORD}" in ''|*[!a-zA-Z0-9]*) echo "PEDIDOS_DB_PASSWORD: usar solo caracteres alfanuméricos generados" >&2; exit 1;; esac
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root <<SQL
CREATE DATABASE IF NOT EXISTS pedidos;
CREATE USER IF NOT EXISTS 'pedidos'@'%' IDENTIFIED BY '${PEDIDOS_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON pedidos.* TO 'pedidos'@'%';
SQL
case "${PAGOS_DB_PASSWORD}" in ''|*[!a-zA-Z0-9]*) echo "PAGOS_DB_PASSWORD: usar solo caracteres alfanuméricos generados" >&2; exit 1;; esac
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root <<SQL
CREATE DATABASE IF NOT EXISTS pagos;
CREATE USER IF NOT EXISTS 'pagos'@'%' IDENTIFIED BY '${PAGOS_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON pagos.* TO 'pagos'@'%';
SQL
case "${LOGISTICA_DB_PASSWORD}" in ''|*[!a-zA-Z0-9]*) echo "LOGISTICA_DB_PASSWORD: usar solo caracteres alfanuméricos generados" >&2; exit 1;; esac
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root <<SQL
CREATE DATABASE IF NOT EXISTS logistica;
CREATE USER IF NOT EXISTS 'logistica'@'%' IDENTIFIED BY '${LOGISTICA_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON logistica.* TO 'logistica'@'%';
SQL
case "${FACTURACION_DB_PASSWORD}" in ''|*[!a-zA-Z0-9]*) echo "FACTURACION_DB_PASSWORD: usar solo caracteres alfanuméricos generados" >&2; exit 1;; esac
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root <<SQL
CREATE DATABASE IF NOT EXISTS facturacion;
CREATE USER IF NOT EXISTS 'facturacion'@'%' IDENTIFIED BY '${FACTURACION_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON facturacion.* TO 'facturacion'@'%';
SQL
case "${CALIFICACIONES_DB_PASSWORD}" in ''|*[!a-zA-Z0-9]*) echo "CALIFICACIONES_DB_PASSWORD: usar solo caracteres alfanuméricos generados" >&2; exit 1;; esac
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root <<SQL
CREATE DATABASE IF NOT EXISTS calificaciones;
CREATE USER IF NOT EXISTS 'calificaciones'@'%' IDENTIFIED BY '${CALIFICACIONES_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON calificaciones.* TO 'calificaciones'@'%';
SQL
