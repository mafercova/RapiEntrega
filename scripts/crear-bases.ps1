$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
. (Join-Path $PSScriptRoot 'configuracion.ps1')
$config = Read-LocalConfig -Ruta (Join-Path $repo '.env')
if (!(Get-Command mysql -ErrorAction SilentlyContinue)) { throw 'Agrega mysql.exe de MySQL a PATH.' }
$sql = ''
foreach ($servicio in @('catalogo','pedidos','pagos','logistica','facturacion','calificaciones')) {
  $clave = $config[$servicio.ToUpper() + '_DB_PASSWORD']
  if (!$clave -or $clave -notmatch '^[a-zA-Z0-9]+$') { throw "Clave local inválida para $servicio; usa el generador de entorno." }
  $sql += "CREATE DATABASE IF NOT EXISTS $servicio; CREATE USER IF NOT EXISTS '$servicio'@'localhost' IDENTIFIED BY '$clave'; GRANT ALL PRIVILEGES ON $servicio.* TO '$servicio'@'localhost';`n"
}
$claveRoot = Read-Host 'Contraseña del administrador root de MySQL' -AsSecureString
$anterior = $env:MYSQL_PWD
try {
  $env:MYSQL_PWD = [System.Net.NetworkCredential]::new('', $claveRoot).Password
  $servidor = if ($config['MYSQL_HOST']) { $config['MYSQL_HOST'] } else { 'localhost' }
  $puerto = if ($config['MYSQL_PORT']) { $config['MYSQL_PORT'] } else { '3306' }
  $sql | & mysql --user=root --host=$servidor --port=$puerto
  if ($LASTEXITCODE -ne 0) { throw 'No se pudieron preparar las bases.' }
} finally { $env:MYSQL_PWD = $anterior }
Write-Host 'Bases y usuarios locales preparados. Se conserva la información existente.'
