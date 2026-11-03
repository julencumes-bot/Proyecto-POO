public enum ModalidadTutoria {
    EN_LINEA,
    PRESENCIAL;

    public static ModalidadTutoria desdeTexto(String valor) {
        // Las solicitudes y los clientes anteriores no registraban modalidad.
        if (valor == null) { return null; }
        try { return valueOf(valor); }
        catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Selecciona una modalidad válida: en línea o presencial.");
        }
    }
}
