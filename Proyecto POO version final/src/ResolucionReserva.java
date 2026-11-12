/** Conserva el motivo de cancelar o sustituir una reserva afectada por una sanción. */
public record ResolucionReserva(int id, int solicitud, Integer nuevaSolicitud,
        String coordinador, String fecha, String motivo) { }
