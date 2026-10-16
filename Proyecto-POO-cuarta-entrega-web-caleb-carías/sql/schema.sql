-- Compatible con PostgreSQL y H2. Las migraciones no eliminan datos existentes.
CREATE TABLE IF NOT EXISTS app_version (id INTEGER PRIMARY KEY, revision BIGINT NOT NULL);
CREATE TABLE IF NOT EXISTS coordinadores (carnet VARCHAR(80) PRIMARY KEY, nombre VARCHAR(200) NOT NULL, correo VARCHAR(200) NOT NULL);
CREATE TABLE IF NOT EXISTS estudiantes (carnet VARCHAR(80) PRIMARY KEY, nombre VARCHAR(200) NOT NULL, correo VARCHAR(200) NOT NULL, anio INTEGER NOT NULL CHECK(anio > 0));
CREATE TABLE IF NOT EXISTS tutores (carnet VARCHAR(80) PRIMARY KEY, nombre VARCHAR(200) NOT NULL, correo VARCHAR(200) NOT NULL, anio INTEGER NOT NULL CHECK(anio > 0), beca BOOLEAN NOT NULL, tarifa DOUBLE PRECISION NOT NULL CHECK(tarifa >= 0), aprobado BOOLEAN NOT NULL, CHECK(NOT beca OR tarifa = 0));
CREATE TABLE IF NOT EXISTS cursos (codigo VARCHAR(80) PRIMARY KEY, nombre VARCHAR(200) NOT NULL);
CREATE TABLE IF NOT EXISTS aprobaciones (tutor VARCHAR(80) REFERENCES tutores(carnet), curso VARCHAR(80) REFERENCES cursos(codigo), PRIMARY KEY(tutor, curso));
CREATE TABLE IF NOT EXISTS horarios (id INTEGER PRIMARY KEY, tutor VARCHAR(80) NOT NULL REFERENCES tutores(carnet), fecha VARCHAR(10) NOT NULL, inicio VARCHAR(5) NOT NULL, fin VARCHAR(5) NOT NULL, UNIQUE(tutor, fecha, inicio, fin));
CREATE TABLE IF NOT EXISTS solicitudes (id INTEGER PRIMARY KEY, estudiante VARCHAR(80) NOT NULL REFERENCES estudiantes(carnet), tutor VARCHAR(80) NOT NULL REFERENCES tutores(carnet), curso VARCHAR(80) NOT NULL REFERENCES cursos(codigo), horario INTEGER NOT NULL REFERENCES horarios(id), tema VARCHAR(2000) NOT NULL, estado VARCHAR(20) NOT NULL CHECK(estado IN ('PENDIENTE','CONFIRMADA','CANCELADA','FINALIZADA')), precio DOUBLE PRECISION NOT NULL CHECK(precio >= 0));
CREATE TABLE IF NOT EXISTS sesiones (id INTEGER PRIMARY KEY, solicitud INTEGER NOT NULL UNIQUE REFERENCES solicitudes(id), temas VARCHAR(2000) NOT NULL, notas VARCHAR(2000) NOT NULL, proximo VARCHAR(2000) NOT NULL, finalizada BOOLEAN NOT NULL);
CREATE TABLE IF NOT EXISTS preguntas (id INTEGER PRIMARY KEY, solicitud INTEGER NOT NULL REFERENCES solicitudes(id), estudiante VARCHAR(80) NOT NULL REFERENCES estudiantes(carnet), contenido VARCHAR(2000) NOT NULL);
CREATE TABLE IF NOT EXISTS cuentas (rol VARCHAR(20) NOT NULL CHECK(rol IN ('estudiante','tutor','coordinador')), carnet VARCHAR(80) NOT NULL, salt VARCHAR(64) NOT NULL, hash VARCHAR(128) NOT NULL, PRIMARY KEY(rol,carnet));
CREATE INDEX IF NOT EXISTS solicitudes_estudiante ON solicitudes(estudiante);
CREATE INDEX IF NOT EXISTS solicitudes_tutor ON solicitudes(tutor);
