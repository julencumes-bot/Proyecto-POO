import java.util.ArrayList;

public class Tutor extends Usuario {
    private ArrayList<Curso> cursosAsignados;
    private ArrayList<Horario> disponibilidad;
    private ArrayList<SesionTutoria> historialTutorias;
    private RegistroHorasBeca registroHorasBeca;
    private double remuneracionAcumulada;
    private boolean aprobado;
    private boolean recibeHorasBeca;
    private double tarifaHora;
    private int anioCarrera;
    private boolean activo = true;

    public Tutor(String nombre, String carnet, String correo, int anioCarrera,
            boolean recibeHorasBeca, double tarifaHora) {
        super(nombre, carnet, correo);
        Validacion.positivo(anioCarrera, "Anio de carrera");
        Validacion.noNegativo(tarifaHora, "Tarifa");
        if (recibeHorasBeca && tarifaHora != 0) {
            throw new IllegalArgumentException("Un tutor de horas beca debe tener tarifa cero.");
        }
        this.anioCarrera = anioCarrera;
        this.recibeHorasBeca = recibeHorasBeca;
        this.tarifaHora = tarifaHora;
        cursosAsignados = new ArrayList<Curso>();
        disponibilidad = new ArrayList<Horario>();
        historialTutorias = new ArrayList<SesionTutoria>();
        registroHorasBeca = new RegistroHorasBeca();
    }
    public int getAnioCarrera() { return anioCarrera; }
    public boolean isRecibeHorasBeca() { return recibeHorasBeca; }
    public double getTarifaHora() { return tarifaHora; }
    public boolean isAprobado() { return aprobado; }
    public boolean isActivo() { return activo; }
    void retirar() { activo = false; }
    public void actualizarDatos(String nombre, String correo, int anio, boolean beca, double tarifa) {
        // Validar todo antes de modificar el objeto compartido.
        String nuevoNombre = Validacion.texto(nombre, "Nombre");
        String nuevoCorreo = Validacion.texto(correo, "Correo");
        Validacion.positivo(anio, "Año de carrera");
        Validacion.noNegativo(tarifa, "Tarifa");
        if (beca && tarifa != 0) {
            throw new IllegalArgumentException("Un tutor de horas beca debe tener tarifa cero.");
        }
        setNombre(nuevoNombre);
        setCorreo(nuevoCorreo);
        anioCarrera = anio;
        recibeHorasBeca = beca;
        tarifaHora = tarifa;
    }
    public void setAprobado(boolean aprobado) { this.aprobado = aprobado; }
    public double getRemuneracionAcumulada() { return remuneracionAcumulada; }
    public RegistroHorasBeca getRegistroHorasBeca() { return registroHorasBeca; }
    public ArrayList<Curso> getCursosAsignados() { return new ArrayList<Curso>(cursosAsignados); }
    public ArrayList<Horario> getDisponibilidad() { return new ArrayList<Horario>(disponibilidad); }
    public ArrayList<SesionTutoria> getHistorialTutorias() {
        return new ArrayList<SesionTutoria>(historialTutorias);
    }
    public void agregarCurso(Curso curso) {
        if (curso == null) { throw new IllegalArgumentException("Curso requerido."); }
        if (!cursosAsignados.contains(curso)) { cursosAsignados.add(curso); }
    }
    public void agregarHorario(Horario horario) {
        if (horario == null) { throw new IllegalArgumentException("Horario requerido."); }
        for (Horario existente : disponibilidad) {
            if (existente.seCruzaCon(horario)) {
                throw new IllegalArgumentException("El horario se cruza con otro bloque del tutor.");
            }
        }
        disponibilidad.add(horario);
    }
    public void agregarSesion(SesionTutoria sesion) {
        if (sesion.getSolicitud().getTutor() != this || !sesion.isFinalizada()
                || historialTutorias.contains(sesion)) {
            throw new IllegalArgumentException("La sesion no puede agregarse al historial.");
        }
        historialTutorias.add(sesion);
        if (sesion.getSolicitud().isRecibeHorasBeca()) {
            registroHorasBeca.registrarSesion(sesion);
        } else {
            remuneracionAcumulada += sesion.getSolicitud().getPrecio();
        }
    }
    public String toString() {
        return super.toString() + " | Anio " + anioCarrera
                + (recibeHorasBeca ? " | Horas beca, sin costo" : " | Q" + tarifaHora + " por hora")
                + (aprobado ? " | Aprobado" : " | Sin aprobar");
    }
}
