$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $repo '.env'
if (Test-Path -LiteralPath $envFile) { Write-Host '.env ya existe: se conserva'; exit 0 }
function New-LocalSecret { return [Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(24)) }
$lines = @('MYSQL_ROOT_PASSWORD=' + (New-LocalSecret))
$lines += 'CATALOGO_DB_PASSWORD=' + (New-LocalSecret)
$lines += 'PEDIDOS_DB_PASSWORD=' + (New-LocalSecret)
$lines += 'PAGOS_DB_PASSWORD=' + (New-LocalSecret)
$lines += 'LOGISTICA_DB_PASSWORD=' + (New-LocalSecret)
$lines += 'FACTURACION_DB_PASSWORD=' + (New-LocalSecret)
$lines += 'CALIFICACIONES_DB_PASSWORD=' + (New-LocalSecret)
$lines += 'RABBITMQ_USER=rapientrega'
$lines += 'RABBITMQ_PASSWORD=' + (New-LocalSecret)
[IO.File]::WriteAllLines($envFile, $lines)
Write-Host 'Configuración local creada. No compartir ni versionar .env.'
