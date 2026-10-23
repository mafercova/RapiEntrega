function Read-LocalConfig {
  param([Parameter(Mandatory=$true)][string]$Ruta)
  if (!(Test-Path -LiteralPath $Ruta)) { throw 'Primero ejecuta scripts/preparar-entorno.ps1' }
  $valores = @{}
  foreach ($linea in Get-Content -LiteralPath $Ruta) {
    if ($linea.Trim() -and !$linea.Trim().StartsWith('#')) {
      $partes = $linea.Split('=', 2)
      if ($partes.Count -eq 2) { $valores[$partes[0].Trim()] = $partes[1].Trim() }
    }
  }
  return $valores
}
