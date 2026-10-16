$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
if (!(Test-Path 'lib/h2-2.5.252.jar')) { & "$PSScriptRoot/dependencias.ps1" }
New-Item -ItemType Directory -Force bin | Out-Null
& javac --release 17 -encoding UTF-8 -Xlint:all -d bin src/*.java tests/*.java
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación.' }
& java -cp 'bin;lib/*' PruebasSistema
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas originales.' }
& java -cp 'bin;lib/*' PruebasWeb
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas web y de persistencia.' }
