public enum ModalidadTutoria {
    EN_LINEA,
    PRESENCIAL;

    public String getEtiqueta() { return this == EN_LINEA ? "Virtual" : "Presencial"; }

    public static ModalidadTutoria desdeTexto(String valor) {
        // Las solicitudes y los clientes anteriores no registraban modalidad.
        if (valor == null) { return null; }
        if ("VIRTUAL".equals(valor)) { return EN_LINEA; }
        try { return valueOf(valor); }
        catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Selecciona una modalidad válida: en línea o presencial.");
        }
    }
}
