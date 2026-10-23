param([Parameter(Mandatory=$true)][ValidateSet('catalogo','pedidos','pagos','logistica','facturacion','calificaciones','pasarela-mock','facturacion-legado-mock')][string]$Servicio)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
if ($Servicio -in @('pasarela-mock','facturacion-legado-mock')) {
  $modulo = "integraciones/$Servicio"
} else {
  . (Join-Path $PSScriptRoot 'configuracion.ps1')
  $config = Read-LocalConfig -Ruta (Join-Path $repo '.env')
  $servidor = if ($config['MYSQL_HOST']) { $config['MYSQL_HOST'] } else { 'localhost' }
  $puerto = if ($config['MYSQL_PORT']) { $config['MYSQL_PORT'] } else { '3306' }
  $env:DB_URL = "jdbc:mysql://${servidor}:${puerto}/$Servicio"
  $env:DB_USER = $Servicio
  $env:DB_PASSWORD = $config[$Servicio.ToUpper() + '_DB_PASSWORD']
  if (!$env:DB_PASSWORD) { throw "Falta la clave local de $Servicio" }
  $env:SPRING_PROFILES_ACTIVE = 'demo'
  $modulo = "servicios/$Servicio-service"
}
Push-Location $repo
try {
  if (!$env:JAVA_HOME) { throw 'Configura JAVA_HOME al JDK 21.' }
  $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
  if (!(Test-Path -LiteralPath $java)) { throw 'No se encontró java.exe en JAVA_HOME.' }
  $jars = @(Get-ChildItem -LiteralPath (Join-Path $repo "$modulo/target") -Filter '*.jar' -ErrorAction SilentlyContinue)
  if ($jars.Count -ne 1) { throw 'Compila primero el proyecto con .\mvnw.cmd verify.' }
  & $java '-Xms16m' '-Xmx128m' '-XX:+UseSerialGC' '-XX:ActiveProcessorCount=2' '-XX:ReservedCodeCacheSize=32m' '-jar' $jars[0].FullName
  if ($LASTEXITCODE -ne 0) { throw 'El servicio terminó con error.' }
}
finally { Pop-Location }
