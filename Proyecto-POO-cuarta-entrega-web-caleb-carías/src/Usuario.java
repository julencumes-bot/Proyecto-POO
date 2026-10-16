public class Usuario {
    private String nombre;
    private String carnet;
    private String correo;

    public Usuario(String nombre, String carnet, String correo) {
        this.nombre = Validacion.texto(nombre, "Nombre");
        this.carnet = Validacion.texto(carnet, "Carne");
        this.correo = Validacion.texto(correo, "Correo");
    }

    public String getNombre() { return nombre; }
    public String getCarnet() { return carnet; }
    public String getCorreo() { return correo; }
    public void setNombre(String nombre) { this.nombre = Validacion.texto(nombre, "Nombre"); }
    public void setCorreo(String correo) { this.correo = Validacion.texto(correo, "Correo"); }

    public String toString() { return carnet + " - " + nombre + " (" + correo + ")"; }
}
