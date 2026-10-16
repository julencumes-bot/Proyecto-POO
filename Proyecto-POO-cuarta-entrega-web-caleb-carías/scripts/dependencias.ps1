$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
New-Item -ItemType Directory -Force lib | Out-Null
$descargas = @(
  @{ Url='https://github.com/h2database/h2database/releases/download/version-2.5.252/h2-2.5.252.jar'; Archivo='lib/h2-2.5.252.jar' },
  @{ Url='https://github.com/pgjdbc/pgjdbc/releases/download/REL42.7.14/postgresql-42.7.14.jar'; Archivo='lib/postgresql-42.7.14.jar' }
)
foreach ($item in $descargas) {
  if (!(Test-Path -LiteralPath $item.Archivo)) { Invoke-WebRequest -Uri $item.Url -OutFile $item.Archivo -UseBasicParsing }
}
Write-Host 'Controladores JDBC disponibles en lib.'
