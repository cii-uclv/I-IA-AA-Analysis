-- =====================================================================
-- Trabajo de Curso - Sistemas de Bases de Datos II
-- Sistema de gestión de proteomas, análisis y clasificación
-- CII - UCLV / IBP
-- Autores: Nestor Nuñez Díaz y Adriana Isabel Acosta González
-- =====================================================================

-- NOTA: DECIDIMOS COMENTAR EL CÓDIGO ENTRE BARRAS DOBLES PARA DARLE MÁS CLARIDAD. CADA DECISIÓN ESTÁ BASADA EN EL INFORME SUBIDO ANTERIORMENTE AL MOODLE.

-- ---------------------------------------------------------------------
-- Esto es una medida de seguridad por si re-ejecuta el script
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS respuesta CASCADE;
DROP TABLE IF EXISTS trabajo_analisis CASCADE;
DROP TABLE IF EXISTS configuracion_modelo CASCADE;
DROP TABLE IF EXISTS secuencia CASCADE;
DROP TABLE IF EXISTS proteoma_modifica CASCADE;
DROP TABLE IF EXISTS proteoma_elimina CASCADE;
DROP TABLE IF EXISTS proteoma CASCADE;
DROP TABLE IF EXISTS administrador CASCADE;
DROP TABLE IF EXISTS investigador CASCADE;
DROP TABLE IF EXISTS usuario CASCADE;

DROP TYPE IF EXISTS estado_proteoma_t;
DROP TYPE IF EXISTS tipo_analisis_t;
DROP TYPE IF EXISTS estado_analisis_t;
DROP TYPE IF EXISTS familia_t;

-- ---------------------------------------------------------------------
-- Aquí encontramos los tipos ENUM, que consideramos necesarios para los casos en que hay una cantidad limitada de opciones
-- ---------------------------------------------------------------------
CREATE TYPE estado_proteoma_t AS ENUM ('pendiente', 'procesando', 'completado', 'fallido');
CREATE TYPE tipo_analisis_t   AS ENUM ('analisis_completo', 'reclasificar');
CREATE TYPE estado_analisis_t AS ENUM ('en_cola', 'ejecutando', 'completado', 'fallido');
CREATE TYPE familia_t         AS ENUM ('GH18', 'GH19', 'AMP', 'otra');

-- Dominio para número EC (Enzyme Commission)
-- Acepta el formato: número.número.número.número (ej. 3.2.1.4)
-- Permite valores NULL (por si no se ha determinado el EC)
CREATE DOMAIN ec_number AS VARCHAR(20)
CHECK (
    VALUE IS NULL
    OR VALUE ~ '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'
);
-- =====================================================================
-- USUARIO (supertipo) - herencia exclusiva y total: Investigador / Administrador, como se plantea en el informe
-- =====================================================================
CREATE TABLE usuario (
    id_usuario       SERIAL PRIMARY KEY,
    nombre_usuario   VARCHAR(50)  NOT NULL UNIQUE,
    correo           VARCHAR(120) NOT NULL UNIQUE,
    hash_contrasena  VARCHAR(255) NOT NULL,
    institucion      VARCHAR(150),
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso    TIMESTAMP,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    tipo_usuario     VARCHAR(20) NOT NULL CHECK (tipo_usuario IN ('investigador', 'administrador')) --atributo para mejorar el rendimiento en búsquedas
);

-- Subtipo: Investigador (puede o no tener rama de investigación)
CREATE TABLE investigador (
    id_usuario        INTEGER PRIMARY KEY REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    rama_investigacion VARCHAR(150)
);

-- Subtipo: Administrador (puede o no tener un rol determinado)
CREATE TABLE administrador (
    id_usuario INTEGER PRIMARY KEY REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    rol        VARCHAR(80)
);

-- =====================================================================
-- PROTEOMA
-- =====================================================================
CREATE TABLE proteoma (
    id_proteoma          SERIAL PRIMARY KEY,
    nombre_proyecto      VARCHAR(150) NOT NULL,
    especie              VARCHAR(120) NOT NULL,
    cepa                 VARCHAR(120),
    ruta_archivo         VARCHAR(255) NOT NULL,
    md5_fasta            VARCHAR(64) NOT NULL,
    fecha_subida         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado               estado_proteoma_t NOT NULL DEFAULT 'pendiente',
    total_secuencias     INTEGER NOT NULL DEFAULT 0 CHECK (total_secuencias >= 0),
    visible_otros        BOOLEAN NOT NULL DEFAULT FALSE,
    id_usuario_publica   INTEGER NOT NULL REFERENCES usuario(id_usuario)
);

-- Relación "Modifica" (M:N usuario-proteoma) con fecha de modificación
CREATE TABLE proteoma_modifica (
    id_usuario         INTEGER NOT NULL REFERENCES usuario(id_usuario),
    id_proteoma        INTEGER NOT NULL REFERENCES proteoma(id_proteoma) ON DELETE CASCADE,
    fecha_modificacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario, id_proteoma, fecha_modificacion)
);

-- Relación "Elimina" (solo administradores, queda registro de quién y cuándo)
CREATE TABLE proteoma_elimina (
    id_proteoma        INTEGER NOT NULL,
    id_administrador   INTEGER NOT NULL REFERENCES administrador(id_usuario),
    fecha_eliminacion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nombre_proyecto    VARCHAR(150),
    especie            VARCHAR(120),
    PRIMARY KEY (id_proteoma) --Como el id_proteoma es clave primaria de la entidad y un mismo proteoma no puede ser eliminado dos veces,
    --Entonces es suficiente para identificar cada instancia de proteoma_elimina con el id_proteoma.
);

-- =====================================================================
-- SECUENCIA (entidad débil por identificación, depende de proteoma)
-- =====================================================================
CREATE TABLE secuencia (
    id_proteoma   INTEGER NOT NULL REFERENCES proteoma(id_proteoma) ON DELETE CASCADE,
    id            BIGSERIAL, --id que identifica una secuencia dentro de un proteoma específico
    longitud      INTEGER CHECK (longitud >= 0),
    secuencia_aa  TEXT NOT NULL, --Secuencia de aminoácidos en forma de texto
    PRIMARY KEY (id_proteoma, id)
);

-- =====================================================================
-- CONFIGURACION_MODELO (catálogo de clasificadores entrenados)
-- =====================================================================
CREATE TABLE configuracion_modelo (
    id_modelo    SERIAL PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL UNIQUE,
    descripcion  TEXT,
    metadatos    JSONB
);

-- =====================================================================
-- TRABAJO_ANALISIS
-- =====================================================================
CREATE TABLE trabajo_analisis (
    id_trabajo      SERIAL PRIMARY KEY,
    tipo            tipo_analisis_t NOT NULL,
    estado          estado_analisis_t NOT NULL DEFAULT 'en_cola', --Al crear cada trabajo de análisis este recae por defecto en la cola de procesamiento
    fecha_inicio    TIMESTAMP,
    fecha_fin       TIMESTAMP,
    mensaje_error   TEXT,
    id_proteoma     INTEGER NOT NULL REFERENCES proteoma(id_proteoma) ON DELETE CASCADE,
    id_usuario      INTEGER NOT NULL REFERENCES usuario(id_usuario),
    id_modelo       INTEGER NOT NULL REFERENCES configuracion_modelo(id_modelo)
);

-- =====================================================================
-- RESPUESTA (resultado de clasificación jerárquica de 3 niveles)
-- =====================================================================
CREATE TABLE respuesta (
    id_trabajo           INTEGER NOT NULL REFERENCES trabajo_analisis(id_trabajo) ON DELETE CASCADE,
    id_proteoma           INTEGER NOT NULL,
    id_secuencia          BIGINT NOT NULL,
    -- Nivel 1: ¿es enzima?
    es_enzima             BOOLEAN NOT NULL,
    confianza_enzima      REAL NOT NULL CHECK (confianza_enzima BETWEEN 0 AND 1),
    -- Nivel 2: ¿es hidrolasa?
    es_hidrolasa          BOOLEAN,
    -- Nivel 3: familia (GH18, GH19, AMP/otra) + confianza
    familia_sugerida      familia_t,
    confianza_familia     REAL CHECK (confianza_familia BETWEEN 0 AND 1),
    numero_ec             ec_number,
    razonamiento          TEXT,
    fecha                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_trabajo, id_secuencia), --No es necesario incluir el id_proteoma porque cada trabajo de análisis ya está asociado a un único proteoma
    FOREIGN KEY (id_proteoma, id_secuencia) REFERENCES secuencia(id_proteoma, id) ON DELETE CASCADE,
    CONSTRAINT chk_ec_formato CHECK (numero_ec ~ '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$' OR numero_ec IS NULL)
);


-- =====================================================================
-- ÍNDICES
-- =====================================================================
CREATE INDEX idx_proteoma_usuario       ON proteoma(id_usuario_publica);
CREATE INDEX idx_trabajo_proteoma       ON trabajo_analisis(id_proteoma);
CREATE INDEX idx_trabajo_usuario        ON trabajo_analisis(id_usuario);
CREATE INDEX idx_respuesta_secuencia    ON respuesta(id_proteoma, id_secuencia);
CREATE INDEX idx_secuencia_proteoma     ON secuencia(id_proteoma);


-- =====================================================================
-- ROLES
-- =====================================================================
-- 1. Rol de solo lectura, para el usuario que visualice la base de datos sin ser investigador o administrador
CREATE ROLE role_read;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO role_read;--Solo puede leer los datos

-- 2. Rol de investigador (hereda de role_read)
CREATE ROLE role_investigador;
GRANT role_read TO role_investigador;
GRANT INSERT, UPDATE ON TABLE proteoma, secuencia, trabajo_analisis TO role_investigador;
-- Es crucial dar permiso sobre las secuencias de las claves primarias porque son seriales
GRANT USAGE, SELECT ON SEQUENCE proteoma_id_proteoma_seq, secuencia_id_seq TO role_investigador;

-- 3. Rol de administrador (hereda de role_investigador)
CREATE ROLE role_administrador;
GRANT role_investigador TO role_administrador;
-- Permisos adicionales: DELETE y control total
GRANT DELETE ON ALL TABLES IN SCHEMA public TO role_administrador;
-- Privilegios para crear y modificar objetos de la base de datos (si es necesario)
GRANT CREATE ON SCHEMA public TO role_administrador;
-- Los administradores pueden otorgar el rol de investigador
GRANT role_investigador TO role_administrador WITH ADMIN OPTION;
--El rol de administrador solo es otorgado por el superusuario postgres manualmente

-- =====================================================================
-- DISPARADORES Y FUNCIONES
-- =====================================================================

-- 1) Calculamos la longitud de la secuencia de aminoácidos y se la asignamos al atributo correspondiente de la entidad.
CREATE OR REPLACE FUNCTION fn_calcular_longitud_secuencia()
RETURNS TRIGGER AS $$
BEGIN
    NEW.longitud := LENGTH(NEW.secuencia_aa);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_calcular_longitud --Cada vez que insertamos o actualizamos una fila en la entidad secuencia llamamos a la función anterior.
BEFORE INSERT OR UPDATE ON secuencia
FOR EACH ROW
EXECUTE FUNCTION fn_calcular_longitud_secuencia();

-- 2) Actualizar el contador total_secuencias del proteoma al insertar/eliminar secuencias
CREATE OR REPLACE FUNCTION fn_actualizar_total_secuencias()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE proteoma
           SET total_secuencias = total_secuencias + 1 --Si agregamos un aminoácido sumamos uno al total.
         WHERE id_proteoma = NEW.id_proteoma;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE proteoma
           SET total_secuencias = total_secuencias - 1 --Si eliminamos un aminoácido restamos uno al total.
         WHERE id_proteoma = OLD.id_proteoma;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

--Pudiéramos agrupar ambas operaciones en un mismo trigger y el resultado sería el mismo
--Pero optamos por hacerlo por separado para mayor legibilidad en el código.

CREATE TRIGGER trg_total_secuencias_insert --Llamamos a la función para que actualice el proteoma correspondiente después de insertar una secuencia
AFTER INSERT ON secuencia
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_total_secuencias();

CREATE TRIGGER trg_total_secuencias_delete
AFTER DELETE ON secuencia
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_total_secuencias();


-- 3) No permitir dos trabajos en estado 'ejecutando' para el mismo proteoma
CREATE OR REPLACE FUNCTION fn_validar_trabajo_unico_ejecutando()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.estado = 'ejecutando' THEN
        IF EXISTS (
            SELECT 1 FROM trabajo_analisis
             WHERE id_proteoma = NEW.id_proteoma --Necesitamos verificar si existe un trabajo de análisis asociado al mismo proteoma
               AND estado = 'ejecutando' -- y que se esté ejecutando
               AND id_trabajo <> COALESCE(NEW.id_trabajo, -1) --Esta línea es porque el trigger necesita excluirse a sí mismo de la búsqueda, porque si se pudiera encontrar a sí mismo entonces siempre sería positivo el condicional
        ) THEN
            RAISE EXCEPTION 'Ya existe un trabajo en estado ejecutando para el proteoma %', NEW.id_proteoma;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_trabajo_unico_ejecutando --Llamamos a la función antes de la inserción o actualización de un trabajo
BEFORE INSERT OR UPDATE ON trabajo_analisis
FOR EACH ROW
EXECUTE FUNCTION fn_validar_trabajo_unico_ejecutando();

-- 4) Verificar que el tipo_usuario de 'usuario' sea consistente con la subtabla
--    (un usuario no puede estar en investigador y administrador a la vez)
CREATE OR REPLACE FUNCTION fn_validar_tipo_investigador()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM administrador WHERE id_usuario = NEW.id_usuario) THEN --Si encontramos el investigador entre los administradores lanzamos una excepción
        RAISE EXCEPTION 'El usuario % ya es administrador, no puede ser investigador', NEW.id_usuario;
    END IF;
    UPDATE usuario SET tipo_usuario = 'investigador' WHERE id_usuario = NEW.id_usuario;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_investigador
BEFORE INSERT ON investigador
FOR EACH ROW
EXECUTE FUNCTION fn_validar_tipo_investigador();
---------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_tipo_administrador()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM investigador WHERE id_usuario = NEW.id_usuario) THEN --Hacemos el procedimiento análogo para los administradores
        RAISE EXCEPTION 'El usuario % ya es investigador, no puede ser administrador', NEW.id_usuario;
    END IF;
    UPDATE usuario SET tipo_usuario = 'administrador' WHERE id_usuario = NEW.id_usuario;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_administrador
BEFORE INSERT ON administrador
FOR EACH ROW
EXECUTE FUNCTION fn_validar_tipo_administrador();

-- 5) Procedimiento almacenado para eliminar proteoma dejando trazabilidad (solo administradores)
CREATE OR REPLACE PROCEDURE sp_eliminar_proteoma(p_id_proteoma INTEGER, p_id_admin INTEGER)
LANGUAGE plpgsql
AS $$
DECLARE
    v_nombre VARCHAR(150);
    v_especie VARCHAR(120);
BEGIN
    IF NOT EXISTS (SELECT 1 FROM administrador WHERE id_usuario = p_id_admin) THEN --Nos aseguramos que sea un administrador el que lo elimine
        RAISE EXCEPTION 'El usuario % no es administrador y no puede eliminar proteomas', p_id_admin;
    END IF;

    SELECT nombre_proyecto, especie INTO v_nombre, v_especie
      FROM proteoma WHERE id_proteoma = p_id_proteoma;

    INSERT INTO proteoma_elimina (id_proteoma, id_administrador, nombre_proyecto, especie) --Garantizamos la trazabilidad de los datos
    VALUES (p_id_proteoma, p_id_admin, v_nombre, v_especie);

    DELETE FROM proteoma WHERE id_proteoma = p_id_proteoma; --Eliminamos el proteoma correspondiente
END;
$$;
