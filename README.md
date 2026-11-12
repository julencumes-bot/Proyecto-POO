# Proyecto-POO
Proyecto grupal de Programación Orientada a Objetos


## Iniciar

1. Instala un **JDK 17 o superior**. Deben funcionar `java -version` y `javac -version`.
2. Extrae el ZIP completo en una carpeta con permisos de escritura.
3. En Windows, abre **Iniciar web.cmd**. En Linux/macOS, ejecuta `sh iniciar.sh` desde esta carpeta.
4. Abre **http://127.0.0.1:8080**. Mantén abierta la ventana del servidor y no abras `web/index.html` directamente.
5. Detén el servidor con Ctrl+C.

Si el puerto está ocupado, en PowerShell: `powershell -ExecutionPolicy Bypass -File scripts/iniciar.ps1 -Puerto 8081`. En Linux/macOS: `PORT=8081 sh iniciar.sh`.

La ejecución local no requiere Node.js ni una conexión a internet. Node solo se utiliza para pruebas de la interfaz. Los JAR necesarios ya vienen en `lib/`.
