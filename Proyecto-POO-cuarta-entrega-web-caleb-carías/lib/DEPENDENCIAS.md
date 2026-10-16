# Controladores JDBC

Versiones incluidas y usadas en las pruebas:

| Archivo | Origen | SHA256 |
| --- | --- | --- |
| h2-2.5.252.jar | https://github.com/h2database/h2database/releases/download/version-2.5.252/h2-2.5.252.jar | 90b11dc413da82070ef15fc943282c1408e6ffd34fa68998335fea8603b5ff20 |
| postgresql-42.7.14.jar | https://github.com/pgjdbc/pgjdbc/releases/download/REL42.7.14/postgresql-42.7.14.jar | 73914527305a40cce504b0d3d90b23caf565136912d607ae8ae7c5895512332c |

`powershell -ExecutionPolicy Bypass -File scripts/dependencias.ps1` descarga los archivos faltantes. En macOS/Linux se pueden descargar desde esos mismos enlaces y colocarlos en esta carpeta. Los JAR no se versionan en Git; sí se incluyen en el ZIP de entrega para iniciar sin descargar dependencias.

Las licencias se conservan en `licenses/`. La licencia de H2 procede de https://h2database.com/html/license.html y las licencias del controlador PostgreSQL y componentes incluidos fueron extraídas de su JAR. Los controladores son bibliotecas de terceros, no código del equipo.
