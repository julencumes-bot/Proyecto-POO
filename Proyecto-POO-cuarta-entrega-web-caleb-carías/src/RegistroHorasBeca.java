import java.util.ArrayList;

public class RegistroHorasBeca {
    private double horasAcumuladas;
    private ArrayList<SesionTutoria> sesionesRegistradas;

    public RegistroHorasBeca() { sesionesRegistradas = new ArrayList<SesionTutoria>(); }
    public double getHorasAcumuladas() { return horasAcumuladas; }
    public ArrayList<SesionTutoria> getSesionesRegistradas() {
        return new ArrayList<SesionTutoria>(sesionesRegistradas);
    }
    public void registrarSesion(SesionTutoria sesion) {
        if (sesion == null || !sesion.isFinalizada()
                || !sesion.getSolicitud().getTutor().isRecibeHorasBeca()
                || sesion.getSolicitud().getTutor().getRegistroHorasBeca() != this
                || sesionesRegistradas.contains(sesion)) {
            throw new IllegalArgumentException("La sesion no permite acreditar horas beca.");
        }
        sesionesRegistradas.add(sesion);
        horasAcumuladas += sesion.getDuracionHoras();
    }
}
