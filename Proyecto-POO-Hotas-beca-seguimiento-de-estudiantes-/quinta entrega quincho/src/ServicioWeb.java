import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServicioWeb {
    private final RepositorioTutorias repo;

    public ServicioWeb(RepositorioTutorias repo) {
        this.repo = repo;
    }

    public synchronized void inicializar(boolean demo) throws SQLException {
        var estado = repo.cargar();

        if (estado.sistema() != null) {
            return;
        }

        SistemaTutorias s;

        if (demo) {
            ProfesorCoordinador p = new ProfesorCoordinador(
                    "Prof. Rivera", "P001", "rivera@example.com");

            s = new SistemaTutorias(p);

            Curso poo = new Curso(
                    "CC2006", "Programación Orientada a Objetos");
            Curso mate = new Curso("MM101", "Matemática");
            Curso avanzado = new Curso(
                    "CC5001", "Algoritmos avanzados");

            s.agregarCurso(poo);
            s.agregarCurso(mate);
            s.agregarCurso(avanzado);

            s.agregarEstudiante(new Estudiante(
                    "Ana López", "E001", "ana@example.com", 2));
            s.agregarEstudiante(new Estudiante(
                    "Luis Méndez", "E002", "luis@example.com", 5));

            Tutor maria = new Tutor(
                    "María Pérez", "T001", "maria@example.com",
                    3, true, 0);

            Tutor diego = new Tutor(
                    "Diego Ruiz", "T002", "diego@example.com",
                    5, false, 40);

            s.agregarTutor(maria);
            s.agregarTutor(diego);

            p.aprobarTutor(maria, poo);
            p.aprobarTutor(diego, mate);
            p.aprobarTutor(diego, avanzado);
            p.aprobarTutor(p.getPerfilTutor(), avanzado);

            LocalDate dia = LocalDate.now(
                    ZoneId.of("America/Guatemala")).plusDays(1);

            for (int i = 0; i < 5; i++) {
                String fecha = dia.plusDays(i).toString();

                maria.agregarHorario(
                        new Horario(fecha, "14:00", "15:00", ModalidadTutoria.PRESENCIAL, "Salón B-201 (demo)"));

                diego.agregarHorario(
                        new Horario(fecha, "09:00", "10:30", ModalidadTutoria.PRESENCIAL, "Biblioteca (demo)"));

                p.getPerfilTutor().agregarHorario(
                        new Horario(fecha, "16:00", "17:00", ModalidadTutoria.PRESENCIAL, "Salón A-101 (demo)"));
            }
        } else {
            s = new SistemaTutorias(new ProfesorCoordinador(
                    "Coordinación",
                    BaseDatos.env("ADMIN_ID", "P001"),
                    BaseDatos.env(
                            "ADMIN_EMAIL",
                            "coordinacion@example.com")));
        }

        repo.guardar(
                new RepositorioTutorias.Estado(s, estado.revision()),
                null);
    }

    public static Map<String, Object> obj(Object... kv) {
        Map<String, Object> resultado = new LinkedHashMap<>();

        for (int i = 0; i < kv.length; i += 2) {
            resultado.put((String) kv[i], kv[i + 1]);
        }

        return resultado;
    }

    static Estudiante estudiante(SistemaTutorias s, String id) {
        for (Estudiante e : s.getEstudiantes()) {
            if (e.getCarnet().equals(id)) {
                return e;
            }
        }

        throw new IllegalArgumentException(
                "Estudiante no encontrado.");
    }

    static Tutor tutor(SistemaTutorias s, String id) {
        for (Tutor t : s.getTutores()) {
            if (t.getCarnet().equals(id)) {
                return t;
            }
        }

        throw new IllegalArgumentException("Tutor no encontrado.");
    }

    static Curso curso(SistemaTutorias s, String id) {
        for (Curso c : s.getCursos()) {
            if (c.getCodigo().equals(id)) {
                return c;
            }
        }

        throw new IllegalArgumentException("Curso no encontrado.");
    }

    static SolicitudTutoria solicitud(SistemaTutorias s, String id) {
        for (SolicitudTutoria sol : s.getSolicitudes()) {
            if (Integer.toString(sol.getIdSolicitud()).equals(id)) {
                return sol;
            }
        }

        throw new IllegalArgumentException(
                "Solicitud no encontrada.");
    }

    static SesionTutoria sesion(SistemaTutorias s, String id) {
        for (SesionTutoria ses : s.getSesiones()) {
            if (Integer.toString(ses.getIdSesion()).equals(id)) {
                return ses;
            }
        }

        throw new IllegalArgumentException("Sesión no encontrada.");
    }

    static String horarioId(Horario h) {
        return h.getFecha()
                + "|" + h.getHoraInicio()
                + "|" + h.getHoraFin();
    }

    static Horario horario(Tutor t, String id) {
        for (Horario h : t.getDisponibilidad()) {
            if (horarioId(h).equals(id)) {
                return h;
            }
        }

        throw new IllegalArgumentException("Horario no encontrado.");
    }

    private static void rol(
            Seguridad.Identidad id, String requerido) {

        if (!id.rol().equals(requerido)) {
            throw new SecurityException(
                    "Esta acción no está disponible para tu perfil.");
        }
    }

    private static Tutor tutorActual(
            SistemaTutorias s, Seguridad.Identidad id) {

        if (!id.rol().equals("tutor")
                && !id.rol().equals("coordinador")) {
            throw new SecurityException(
                    "Se requiere un perfil de tutor.");
        }

        return tutor(s, id.carnet());
    }

    private static void verificarAcceso(
            SistemaTutorias s, Seguridad.Identidad id) {

        if (id == null
                || !List.of(
                        "estudiante",
                        "tutor",
                        "coordinador").contains(id.rol())) {

            throw new SecurityException(
                    "Inicia sesión con un perfil válido.");
        }

        if (id.rol().equals("tutor")
                && !tutor(s, id.carnet()).isActivo()) {

            throw new SecurityException(
                    "Tu perfil de tutor fue retirado. "
                            + "Comunícate con coordinación.");
        }
    }

    private static String texto(
            Map<String, String> f, String campo, int max) {

        String valor = Validacion.texto(f.get(campo), campo);

        if (valor.length() > max) {
            throw new IllegalArgumentException(
                    campo + " excede el máximo de "
                            + max + " caracteres.");
        }

        return valor;
    }

    private static String identificador(
            Map<String, String> f, String campo) {

        String valor = texto(f, campo, 80);

        if (!valor.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException(
                    "El identificador solo admite letras, números, "
                            + "guion y guion bajo.");
        }

        return valor;
    }

    private static String correo(Map<String, String> f) {
        String valor = texto(f, "correo", 200);

        if (!valor.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException(
                    "Escribe un correo válido.");
        }

        return valor;
    }

    private void guardarPerfil(
            SistemaTutorias s,
            Tutor t,
            Map<String, String> f) {

        if (!t.isActivo()) {
            throw new IllegalArgumentException(
                    "Perfil no disponible.");
        }

        String carrera = f.getOrDefault(
                "carrera", t.getCarrera());

        String descripcion = f.getOrDefault(
                "descripcion", t.getDescripcion());

        List<Curso> cursados = t.getCursosCursados();

        if (f.containsKey("cursados")) {
            String codigos = f.get("cursados");

            if (codigos.length() > 8000) {
                throw new IllegalArgumentException(
                        "Demasiados cursos cursados.");
            }

            cursados = new ArrayList<>();

            if (!codigos.isBlank()) {
                for (String codigo : codigos.split(",", -1)) {
                    cursados.add(curso(s, codigo.trim()));
                }
            }
        }

        t.actualizarPerfilAcademico(
                carrera, descripcion, cursados);

        if (t == s.getCoordinador().getPerfilTutor()
                && f.containsKey("formacion")) {

            s.getCoordinador().setFormacionAcademica(
                    f.get("formacion"));
        }
    }

    public synchronized void registrarEstudiante(
            Map<String, String> f) throws SQLException {

        String carnet = identificador(f, "carnet");
        String nombre = texto(f, "nombre", 200);
        String email = correo(f);

        int anio = Integer.parseInt(texto(f, "anio", 2));

        if (anio < 1 || anio > 10) {
            throw new IllegalArgumentException(
                    "El año de carrera debe estar entre 1 y 10.");
        }

        String clave = f.get("clave");

        if (clave == null
                || !clave.equals(f.get("confirmacion"))) {

            throw new IllegalArgumentException(
                    "Las contraseñas no coinciden.");
        }

        var estado = repo.cargar();

        new CoordinadorController(estado.sistema())
                .registrarEstudiante(
                        nombre, carnet, email, anio);

        Seguridad.Cuenta cuenta = Seguridad.crear(
                "estudiante", carnet, clave);

        repo.guardar(estado, cuenta);
    }

    public synchronized void ejecutar(
            Seguridad.Identidad id,
            String accion,
            Map<String, String> f) throws SQLException {

        var estado = repo.cargar();
        SistemaTutorias s = estado.sistema();

        verificarAcceso(s, id);

        var ec = new EstudianteController(s);
        var tc = new TutorController(s);
        var cc = new CoordinadorController(s);

        Seguridad.Cuenta nueva = null;

        switch (accion) {
            case "solicitar" -> {
                rol(id, "estudiante");

                Tutor t = tutor(s, f.get("tutor"));

                Horario elegido = horario(t, f.get("horario"));
                if ((f.containsKey("modalidad")
                        && ModalidadTutoria.desdeTexto(f.get("modalidad")) != elegido.getModalidad())
                        || (f.containsKey("ubicacion")
                            && !f.get("ubicacion").equals(elegido.getUbicacion()))) {
                    throw new IllegalArgumentException("No puedes cambiar las condiciones del horario.");
                }
                ec.solicitarTutoria(
                        estudiante(s, id.carnet()),
                        curso(s, f.get("curso")),
                        t,
                        texto(f, "tema", 2000),
                        elegido);
            }

            case "cancelar" -> {
                var sol = solicitud(s, f.get("solicitud"));

                if (id.rol().equals("estudiante")) {
                    ec.cancelarTutoria(
                            estudiante(s, id.carnet()), sol);
                } else {
                    tc.cancelarTutoria(
                            tutorActual(s, id), sol);
                }
            }

            case "preguntar" -> {
                rol(id, "estudiante");

                ec.registrarPregunta(
                        estudiante(s, id.carnet()),
                        solicitud(s, f.get("solicitud")),
                        texto(f, "contenido", 2000));
            }

            case "aceptar" -> tc.aceptarTutoria(
                    tutorActual(s, id),
                    solicitud(s, f.get("solicitud")));

            case "finalizar" -> {
                String proximo = f.getOrDefault(
                        "proximo", "").trim();

                if (proximo.length() > 2000) {
                    throw new IllegalArgumentException(
                            "El próximo tema es demasiado largo.");
                }

                tc.finalizarSesion(
                        tutorActual(s, id),
                        sesion(s, f.get("sesion")),
                        texto(f, "temas", 2000),
                        texto(f, "notas", 2000),
                        proximo,
                        f.containsKey("duracion") ? Double.parseDouble(texto(f, "duracion", 20))
                                : sesion(s, f.get("sesion")).getSolicitud().getHorario().getDuracionHoras());
            }

            case "ausencia" -> tc.registrarAusencia(tutorActual(s, id), sesion(s, f.get("sesion")));

            case "enlace-permanente" -> tutorActual(s, id).setEnlacePermanente(f.get("enlace"));

            case "completar-horario" -> {
                Tutor t = tutorActual(s, id);
                tc.completarHorario(t, horario(t, f.get("horario")),
                        ModalidadTutoria.desdeTexto(f.get("modalidad")), f.get("ubicacion"));
            }

            case "comentario-academico" -> {
                Tutor autor = tutorActual(s, id);
                SesionTutoria ses = sesion(s, f.get("sesion"));
                if (ses.getSolicitud().getTutor() != autor) {
                    throw new SecurityException("Solo quien impartió la sesión puede comentar.");
                }
                int siguiente = 1;
                for (Estudiante est : s.getEstudiantes()) {
                    for (ComentarioAcademico comentario : est.getComentarios()) {
                        siguiente = Math.max(siguiente, comentario.getId() + 1);
                    }
                }
                ComentarioAcademico comentario = new ComentarioAcademico(siguiente, ses, autor,
                        texto(f, "contenido", 2000));
                comentario.getEstudiante().agregarComentario(comentario);
            }

            case "horario" -> tc.agregarHorario(
                    tutorActual(s, id),
                    texto(f, "fecha", 10),
                    texto(f, "inicio", 5),
                    texto(f, "fin", 5),
                    ModalidadTutoria.desdeTexto(f.get("modalidad")), f.get("ubicacion"));

            case "aprobar" -> {
                rol(id, "coordinador");

                cc.aprobarTutor(
                        tutor(s, f.get("tutor")),
                        curso(s, f.get("curso")));
            }

            case "curso" -> {
                rol(id, "coordinador");

                cc.registrarCurso(
                        identificador(f, "codigo"),
                        texto(f, "nombre", 200),
                        f.get("icono"));
            }

            case "eliminar-curso" -> {
                rol(id, "coordinador");

                cc.eliminarCurso(
                        curso(s, f.get("curso")));
            }

            case "eliminar-tutor" -> {
                rol(id, "coordinador");

                cc.eliminarTutor(
                        tutor(s, f.get("tutor")));
            }

            case "perfil-academico" -> {
                // Cada tutor modifica únicamente su perfil académico.
                // Esta acción no cambia autorizaciones ni pagos.
                guardarPerfil(s, tutorActual(s, id), f);
            }

            case "editar-tutor" -> {
                rol(id, "coordinador");

                int anio = Integer.parseInt(
                        texto(f, "anio", 2));

                if (anio < 1 || anio > 10) {
                    throw new IllegalArgumentException(
                            "El año de carrera debe estar entre 1 y 10.");
                }

                Tutor t = tutor(s, f.get("tutor"));

                cc.modificarTutor(
                        t,
                        texto(f, "nombre", 200),
                        correo(f),
                        anio,
                        "on".equals(f.get("beca")),
                        Double.parseDouble(
                                texto(f, "tarifa", 20)));

                if (f.containsKey("multiplicador")) {
                    t.setMultiplicadorBeca(Integer.parseInt(texto(f, "multiplicador", 2)));
                }
                guardarPerfil(s, t, f);
            }

            case "estudiante", "tutor" -> {
                rol(id, "coordinador");

                String carnet = identificador(f, "carnet");
                String nombre = texto(f, "nombre", 200);
                String email = correo(f);

                int anio = Integer.parseInt(
                        texto(f, "anio", 2));

                if (anio < 1 || anio > 10) {
                    throw new IllegalArgumentException(
                            "El año de carrera debe estar entre 1 y 10.");
                }

                nueva = Seguridad.crear(
                        accion, carnet, f.get("clave"));

                if (accion.equals("estudiante")) {
                    cc.registrarEstudiante(
                            nombre, carnet, email, anio);
                } else {
                    cc.registrarTutor(
                            nombre,
                            carnet,
                            email,
                            anio,
                            "on".equals(f.get("beca")),
                            Double.parseDouble(
                                    texto(f, "tarifa", 20)));

                    if (f.containsKey("multiplicador")) {
                        tutor(s, carnet).setMultiplicadorBeca(Integer.parseInt(texto(f, "multiplicador", 2)));
                    }
                    guardarPerfil(
                            s, tutor(s, carnet), f);
                }
            }

            default -> throw new IllegalArgumentException(
                    "Acción desconocida.");
        }

        repo.guardar(estado, nueva);
    }

    private Map<String, Object> solicitudJson(
            SolicitudTutoria sol) {

        return obj(
                "id", sol.getIdSolicitud(),
                "estudiante", sol.getEstudiante().getCarnet(),
                "nombreEstudiante", sol.getEstudiante().getNombre(),
                "tutor", sol.getTutor().getCarnet(),
                "nombreTutor", sol.getTutor().getNombre(),
                "curso", sol.getCurso().getCodigo(),
                "nombreCurso", sol.getCurso().getNombre(),
                "tema", sol.getTema(),
                "fecha", sol.getHorario().getFecha(),
                "inicio", sol.getHorario().getHoraInicio(),
                "fin", sol.getHorario().getHoraFin(),
                "estado", sol.getEstado().name(),
                "precio", sol.getPrecio(),
                "beca", sol.isRecibeHorasBeca(),
                "multiplicador", sol.getMultiplicadorBeca(),
                "ubicacion", sol.getUbicacion(),
                "modalidad", sol.getModalidad() == null
                        ? null
                        : sol.getModalidad().name());
    }

    private boolean visible(
            SolicitudTutoria sol,
            Seguridad.Identidad id) {

        return id.rol().equals("coordinador")
                || (id.rol().equals("estudiante")
                        ? sol.getEstudiante().getCarnet()
                                .equals(id.carnet())
                        : sol.getTutor().getCarnet()
                                .equals(id.carnet()));
    }

    private boolean puedeVerEstudiante(Estudiante estudiante, Seguridad.Identidad id) {
        if (id.rol().equals("coordinador")) { return true; }
        if (!id.rol().equals("tutor")) { return false; }
        for (SolicitudTutoria sol : estudiante.getSolicitudes()) {
            if (sol.getTutor().getCarnet().equals(id.carnet())) { return true; }
        }
        return false;
    }

    private Map<String, Object> resumenEstudiante(Estudiante estudiante) {
        int pendientes = 0, canceladas = 0, confirmadas = 0;
        for (SolicitudTutoria sol : estudiante.getSolicitudes()) {
            if (sol.getEstado() == EstadoSolicitud.PENDIENTE) { pendientes++; }
            if (sol.getEstado() == EstadoSolicitud.CANCELADA) { canceladas++; }
            if (sol.getEstado() == EstadoSolicitud.CONFIRMADA) { confirmadas++; }
        }
        return obj("carnet", estudiante.getCarnet(), "nombre", estudiante.getNombre(),
                "anio", estudiante.getAnioCarrera(), "finalizadas", estudiante.getTutoriasFinalizadas(),
                "pendientes", pendientes, "canceladas", canceladas, "confirmadas", confirmadas,
                "recibeTutorias", confirmadas > 0 || estudiante.getTutoriasFinalizadas() > 0);
    }

    public synchronized Map<String, Object> perfilEstudiante(Seguridad.Identidad id, String carnet)
            throws SQLException {
        SistemaTutorias s = repo.cargar().sistema();
        verificarAcceso(s, id);
        // Se comprueba la relación antes de entregar cualquier dato del alumno.
        Estudiante est = null;
        for (Estudiante candidato : s.getEstudiantes()) {
            if (candidato.getCarnet().equals(carnet) && puedeVerEstudiante(candidato, id)) {
                est = candidato;
            }
        }
        if (est == null) { throw new SecurityException("No tienes acceso a este perfil de estudiante."); }
        Map<String, Object> perfil = resumenEstudiante(est);
        List<Object> seguimiento = new ArrayList<>();
        for (SesionTutoria ses : est.getHistorialTutorias()) {
            SolicitudTutoria sol = ses.getSolicitud();
            seguimiento.add(obj("id", ses.getIdSesion(), "curso", sol.getCurso().getNombre(),
                    "tutor", sol.getTutor().getNombre(), "fecha", sol.getHorario().getFecha(),
                    "temas", ses.getTemasVistos(), "notas", ses.getNotas(),
                    "proximo", ses.getTemaProximaTutoria(), "duracion", ses.getDuracionHoras(),
                    "puedeComentar", sol.getTutor().getCarnet().equals(id.carnet())));
        }
        List<Object> comentarios = new ArrayList<>();
        for (ComentarioAcademico c : est.getComentarios()) {
            comentarios.add(obj("id", c.getId(), "estudiante", est.getCarnet(),
                    "sesion", c.getSesion().getIdSesion(), "autor", c.getAutor().getNombre(),
                    "autorCarnet", c.getAutor().getCarnet(), "fecha", c.getFecha(), "contenido", c.getContenido()));
        }
        perfil.put("seguimiento", seguimiento);
        perfil.put("comentarios", comentarios);
        // El paso 8 todavía no existe en este proyecto. NULL significa sin información, no cero strikes.
        perfil.put("strikesConfirmadosSemestre", null);
        return perfil;
    }

    public synchronized Map<String, Object> datos(
            Seguridad.Identidad id) throws SQLException {

        SistemaTutorias s = repo.cargar().sistema();

        verificarAcceso(s, id);

        var cc = new CoordinadorController(s);

        Usuario u = id.rol().equals("coordinador")
                ? s.getCoordinador()
                : id.rol().equals("estudiante")
                        ? estudiante(s, id.carnet())
                        : tutor(s, id.carnet());

        Estudiante e = id.rol().equals("estudiante")
                ? (Estudiante) u
                : null;

        List<Object> cursos = new ArrayList<>();
        List<Object> tutores = new ArrayList<>();
        List<Object> estudiantes = new ArrayList<>();
        List<Object> solicitudes = new ArrayList<>();
        List<Object> sesiones = new ArrayList<>();
        List<Object> preguntas = new ArrayList<>();
        List<Object> demanda = new ArrayList<>();
        List<Object> cursosEliminados = new ArrayList<>();

        for (Curso c : s.getCursos()) {
            if (!c.isActivo()) {
                if (id.rol().equals("coordinador")) {
                    cursosEliminados.add(obj(
                            "codigo", c.getCodigo(),
                            "nombre", c.getNombre()));
                }

                continue;
            }

            Tutor alternativa = e == null
                    ? null
                    : cc.buscarTutorAlternativo(c, e);

            cursos.add(obj(
                    "codigo", c.getCodigo(),
                    "nombre", c.getNombre(),
                    "icono", c.getIcono(),
                    "alternativa", alternativa == null
                            ? ""
                            : alternativa.getCarnet()));
        }

        for (Tutor t : s.getTutores()) {
            if (!t.isActivo()
                    && !id.rol().equals("coordinador")) {
                continue;
            }

            if (e != null
                    && (!t.isAprobado()
                            || t.getCarnet().equals(e.getCarnet()))) {
                continue;
            }

            if (id.rol().equals("tutor")
                    && !t.getCarnet().equals(id.carnet())) {
                continue;
            }

            List<Object> horarios = new ArrayList<>();

            for (Horario h : t.getDisponibilidad()) {
                boolean libre = h.isDisponible() && h.estaCompleto()
                        && !s.tieneConflicto(t.getCarnet(), h)
                        && (e == null
                                || !s.tieneConflicto(
                                        e.getCarnet(), h));

                if (e == null || libre) {
                    horarios.add(obj(
                            "id", horarioId(h),
                            "fecha", h.getFecha(),
                            "inicio", h.getHoraInicio(),
                            "fin", h.getHoraFin(),
                            "duracion", h.getDuracionHoras(),
                            "disponible", libre,
                            "editable", h.isDisponible() && !s.tieneConflicto(t.getCarnet(), h),
                            "completo", h.estaCompleto(),
                            "modalidad", h.getModalidad() == null ? null : h.getModalidad().name(),
                            // El directorio no entrega enlaces virtuales, ni siquiera ocultos en HTML.
                            "ubicacion", e == null || h.getModalidad() == ModalidadTutoria.PRESENCIAL
                                    ? h.getUbicacion() : null));
                }
            }

            List<String> asignados = new ArrayList<>();

            for (Curso c : t.getCursosAsignados()) {
                if (c.isActivo()) {
                    asignados.add(c.getCodigo());
                }
            }

            boolean esProfesor =
                    t == s.getCoordinador().getPerfilTutor();

            Map<String, Object> perfil = obj(
                    "carnet", t.getCarnet(),
                    "nombre", t.getNombre(),
                    "anio", t.getAnioCarrera(),
                    "beca", t.isRecibeHorasBeca(),
                    "tarifa", t.getTarifaHora(),
                    "aprobado", t.isAprobado(),
                    "profesor", esProfesor,
                    "cursos", asignados,
                    "horarios", horarios,
                    "activo", t.isActivo(),
                    "carrera", t.getCarrera(),
                    "descripcion", t.getDescripcion(),
                    "formacion", esProfesor
                            ? s.getCoordinador()
                                    .getFormacionAcademica()
                            : "");

            List<Object> cursados = new ArrayList<>();

            for (Curso c : t.getCursosCursados()) {
                cursados.add(obj(
                        "codigo", c.getCodigo(),
                        "nombre", c.getNombre()));
            }

            perfil.put("cursados", cursados);

            if (e == null) {
                perfil.put("correo", t.getCorreo());
                perfil.put("enlacePermanente", t.getEnlacePermanente());
                perfil.put("multiplicador", t.getMultiplicadorBeca());
                perfil.put(
                        "remuneracion",
                        t.getRemuneracionAcumulada());
                perfil.put(
                        "horasBeca",
                        t.getRegistroHorasBeca()
                                .getHorasAcumuladas());
            }

            tutores.add(perfil);
        }

        if (id.rol().equals("coordinador")) {
            for (DemandaCurso d : cc.consultarDemandaPorCurso()) {
                List<Object> temas = new ArrayList<>();

                for (var tema : d.temas().entrySet()) {
                    temas.add(obj(
                            "tema", tema.getKey(),
                            "cantidad", tema.getValue()));
                }

                demanda.add(obj(
                        "codigo", d.curso().getCodigo(),
                        "nombre", d.curso().getNombre(),
                        "icono", d.curso().getIcono(),
                        "activo", d.curso().isActivo(),
                        "cantidad", d.cantidad(),
                        "temas", temas));
            }

        }
        for (Estudiante est : s.getEstudiantes()) {
            if (puedeVerEstudiante(est, id)) {
                Map<String, Object> resumen = resumenEstudiante(est);
                if (id.rol().equals("coordinador")) { resumen.put("correo", est.getCorreo()); }
                estudiantes.add(resumen);
            }
        }

        for (SolicitudTutoria sol : s.getSolicitudes()) {
            if (visible(sol, id)) {
                solicitudes.add(solicitudJson(sol));
            }
        }

        for (SesionTutoria ses : s.getSesiones()) {
            if (visible(ses.getSolicitud(), id)) {
                sesiones.add(obj(
                        "id", ses.getIdSesion(),
                        "solicitud",
                        ses.getSolicitud().getIdSolicitud(),
                        "temas", ses.getTemasVistos(),
                        "notas", ses.getNotas(),
                        "proximo", ses.getTemaProximaTutoria(),
                        "finalizada", ses.isFinalizada(),
                        "duracion", ses.getDuracionHoras(),
                        "ausente", ses.isAusente(),
                        "horasAcreditadas", ses.getHorasAcreditadas()));
            }
        }

        for (Pregunta q : s.getPreguntas()) {
            if (visible(q.getSolicitud(), id)) {
                preguntas.add(obj(
                        "id", q.getIdPregunta(),
                        "solicitud",
                        q.getSolicitud().getIdSolicitud(),
                        "contenido", q.getContenido(),
                        "estudiante",
                        q.getEstudiante().getNombre()));
            }
        }

        return obj(
                "usuario", obj(
                        "nombre", u.getNombre(),
                        "carnet", u.getCarnet(),
                        "rol", id.rol()),
                "cursos", cursos,
                "cursosEliminados", cursosEliminados,
                "tutores", tutores,
                "estudiantes", estudiantes,
                "solicitudes", solicitudes,
                "sesiones", sesiones,
                "preguntas", preguntas,
                "demanda", demanda);
    }
}