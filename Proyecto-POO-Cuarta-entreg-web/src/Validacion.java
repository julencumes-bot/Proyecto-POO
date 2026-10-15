/** Validaciones sencillas compartidas por el modelo y las vistas. */
public class Validacion {
    public static String texto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(campo + " no puede estar vacio.");
        }
        return valor.trim();
    }

    public static void positivo(int valor, String campo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(campo + " debe ser mayor que cero.");
        }
    }

    public static void noNegativo(double valor, String campo) {
        if (Double.isNaN(valor) || Double.isInfinite(valor) || valor < 0) {
            throw new IllegalArgumentException(campo + " debe ser un numero no negativo.");
        }
    }

    public static int minutos(String hora) {
        if (hora == null || hora.length() != 5 || hora.charAt(2) != ':') {
            throw new IllegalArgumentException("Use el formato HH:mm, por ejemplo 14:30.");
        }
        for (int i = 0; i < hora.length(); i++) {
            if (i != 2 && (hora.charAt(i) < '0' || hora.charAt(i) > '9')) {
                throw new IllegalArgumentException("La hora debe contener digitos.");
            }
        }
        int horas = Integer.parseInt(hora.substring(0, 2));
        int minutos = Integer.parseInt(hora.substring(3, 5));
        if (horas > 23 || minutos > 59) {
            throw new IllegalArgumentException("Hora fuera de rango.");
        }
        return horas * 60 + minutos;
    }

    public static void fecha(String fecha) {
        if (fecha == null || fecha.length() != 10 || fecha.charAt(4) != '-'
                || fecha.charAt(7) != '-') {
            throw new IllegalArgumentException("Use el formato AAAA-MM-DD.");
        }
        for (int i = 0; i < fecha.length(); i++) {
            if (i != 4 && i != 7 && (fecha.charAt(i) < '0' || fecha.charAt(i) > '9')) {
                throw new IllegalArgumentException("La fecha debe contener digitos.");
            }
        }
        int anio = Integer.parseInt(fecha.substring(0, 4));
        int mes = Integer.parseInt(fecha.substring(5, 7));
        int dia = Integer.parseInt(fecha.substring(8, 10));
        if (anio < 1 || mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Fecha fuera de rango.");
        }
        int[] dias = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (anio % 400 == 0 || (anio % 4 == 0 && anio % 100 != 0)) {
            dias[1] = 29;
        }
        if (dia < 1 || dia > dias[mes - 1]) {
            throw new IllegalArgumentException("Ese dia no existe en el mes indicado.");
        }
    }
}
