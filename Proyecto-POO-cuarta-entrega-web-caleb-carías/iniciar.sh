#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p bin data
if [ ! -f lib/h2-2.5.252.jar ]; then
  echo 'Descarga los JAR indicados en lib/DEPENDENCIAS.md antes de iniciar.' >&2
  exit 1
fi
javac --release 17 -encoding UTF-8 -Xlint:all -d bin src/*.java
exec java -cp 'bin:lib/*' WebMain
