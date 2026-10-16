import java.util.Scanner;

/** Una sola entrada de consola compartida por las vistas. */
public class Entrada {
    private Scanner scanner;

    public Entrada(Scanner scanner) { this.scanner = scanner; }
    public String leerTexto(String mensaje) {
        String texto = "";
        while (texto.isEmpty()) {
            System.out.print(mensaje);
            texto = scanner.nextLine().trim();
            if (texto.isEmpty()) { System.out.println("Escriba un valor."); }
        }
        return texto;
    }
    public String leerOpcional(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }
    public int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            try {
                int numero = Integer.parseInt(leerTexto(mensaje));
                if (numero >= minimo && numero <= maximo) { return numero; }
            } catch (NumberFormatException error) {
                System.out.println("Debe escribir un numero entero.");
            }
            System.out.println("Ingrese un valor entre " + minimo + " y " + maximo + ".");
        }
    }
    public double leerDecimal(String mensaje) {
        while (true) {
            try {
                double numero = Double.parseDouble(leerTexto(mensaje));
                Validacion.noNegativo(numero, "Valor");
                return numero;
            } catch (IllegalArgumentException error) {
                System.out.println("Escriba un numero no negativo; use punto decimal.");
            }
        }
    }
    public boolean leerSiNo(String mensaje) {
        while (true) {
            String respuesta = leerTexto(mensaje + " (s/n): ");
            if (respuesta.equalsIgnoreCase("s")) { return true; }
            if (respuesta.equalsIgnoreCase("n")) { return false; }
            System.out.println("Responda s o n.");
        }
    }
    public int elegirIndice(int cantidad) {
        if (cantidad == 0) {
            System.out.println("No hay registros disponibles.");
            return -1;
        }
        return leerEntero("Seleccione un numero (0 para volver): ", 0, cantidad) - 1;
    }
}
