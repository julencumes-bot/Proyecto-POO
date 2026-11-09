#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p bin
javac --release 17 -encoding UTF-8 -Xlint:all -d bin src/*.java tests/*.java
for prueba in PruebasSistema PruebasWeb PruebasCoordinacion PruebasGestionCoordinador PruebasHorariosBecaPerfiles; do
    java -cp 'bin:lib/*' "$prueba"
done
