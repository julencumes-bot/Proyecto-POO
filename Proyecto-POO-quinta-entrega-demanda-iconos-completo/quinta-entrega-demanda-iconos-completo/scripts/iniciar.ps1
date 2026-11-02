param([int]$Puerto = 8080)
$ErrorActionPreference = 'Stop'
$proyecto = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $proyecto
if (!(Get-Command javac -ErrorAction SilentlyContinue)) { throw 'Instala un JDK 17 o superior y agrega java y javac al PATH.' }
if (!(Test-Path 'lib/h2-2.5.252.jar') -or !(Test-Path 'lib/postgresql-42.7.14.jar')) { & "$PSScriptRoot/dependencias.ps1" }
New-Item -ItemType Directory -Force bin,data | Out-Null
& javac --release 17 -encoding UTF-8 -Xlint:all -d bin src/*.java
if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar el proyecto.' }
$env:PORT = "$Puerto"
Write-Host "Abre http://127.0.0.1:$Puerto en tu navegador. Ctrl+C detiene el servidor."
& java -cp 'bin;lib/*' WebMain
if ($LASTEXITCODE -ne 0) { throw 'El servidor no pudo iniciar. Revisa el puerto y la base de datos.' }
