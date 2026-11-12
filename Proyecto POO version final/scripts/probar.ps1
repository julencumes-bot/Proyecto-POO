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
& java -cp 'bin;lib/*' PruebasCoordinacion
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de demanda, iconos y migración.' }
& java -cp 'bin;lib/*' PruebasGestionCoordinador
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de gestión del coordinador.' }

& java -cp 'bin;lib/*' PruebasHorariosBecaPerfiles
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de horarios, beca y perfiles.' }

& java -cp 'bin;lib/*' PruebasValoracionesAusencias
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas de valoraciones, ausencias y sanciones.' }

& java -cp 'bin;lib/*' PruebasHttp78
if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas HTTP de los cambios 7 y 8.' }
