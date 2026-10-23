$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$archivo = Join-Path $repo '.env'
if (Test-Path -LiteralPath $archivo) { Write-Host '.env ya existe: se conserva'; exit 0 }
$lineas = @('MYSQL_HOST=localhost', 'MYSQL_PORT=3306')
foreach ($servicio in @('catalogo','pedidos','pagos','logistica','facturacion','calificaciones')) {
  $secreto = [Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
  $lineas += $servicio.ToUpper() + '_DB_PASSWORD=' + $secreto
}
[IO.File]::WriteAllLines($archivo, $lineas)
Write-Host 'Configuración local creada. Las claves no se muestran ni se versionan.'
