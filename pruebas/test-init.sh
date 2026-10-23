#!/bin/sh
# Reproduce el source que hace el entrypoint sin necesitar un servidor MySQL.
set -e
mock_bin=$(mktemp -d)
trap 'rm -f "$mock_bin/mysql"; rmdir "$mock_bin"' EXIT
printf '#!/bin/sh\nexit 0\n' > "$mock_bin/mysql"
chmod +x "$mock_bin/mysql"
PATH="$mock_bin:$PATH"
export PATH MYSQL_ROOT_PASSWORD=PruebaLocal CATALOGO_DB_PASSWORD=PruebaLocal
export PEDIDOS_DB_PASSWORD=PruebaLocal PAGOS_DB_PASSWORD=PruebaLocal
export LOGISTICA_DB_PASSWORD=PruebaLocal FACTURACION_DB_PASSWORD=PruebaLocal
export CALIFICACIONES_DB_PASSWORD=PruebaLocal
set +u
. ./pruebas/sql/01-usuarios.sh
case "$-" in *u*) echo 'ERROR: la inicialización filtró nounset al entrypoint' >&2; exit 1;; esac
unset MYSQL_ONETIME_PASSWORD
: "$MYSQL_ONETIME_PASSWORD"
echo 'OK: source de inicialización conserva las opciones del entrypoint'
