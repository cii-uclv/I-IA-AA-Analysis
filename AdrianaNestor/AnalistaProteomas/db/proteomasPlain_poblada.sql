--
-- PostgreSQL database dump
--

-- Dumped from database version 15.1
-- Dumped by pg_dump version 15.1

-- Started on 2026-07-10 14:31:30

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 2 (class 3079 OID 16384)
-- Name: adminpack; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS adminpack WITH SCHEMA pg_catalog;


--
-- TOC entry 3505 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION adminpack; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION adminpack IS 'administrative functions for PostgreSQL';


--
-- TOC entry 3 (class 3079 OID 16643)
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- TOC entry 3506 (class 0 OID 0)
-- Dependencies: 3
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- TOC entry 895 (class 1247 OID 16399)
-- Name: ec_number; Type: DOMAIN; Schema: public; Owner: postgres
--

CREATE DOMAIN public.ec_number AS character varying(20)
	CONSTRAINT ck CHECK (((VALUE IS NULL) OR ((VALUE)::text ~ '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'::text)));


ALTER DOMAIN public.ec_number OWNER TO postgres;

--
-- TOC entry 899 (class 1247 OID 16402)
-- Name: estado_analisis_t; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.estado_analisis_t AS ENUM (
    'en_cola',
    'ejecutando',
    'completado',
    'fallido'
);


ALTER TYPE public.estado_analisis_t OWNER TO postgres;

--
-- TOC entry 902 (class 1247 OID 16412)
-- Name: estado_proteoma_t; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.estado_proteoma_t AS ENUM (
    'pendiente',
    'procesando',
    'completado',
    'fallido'
);


ALTER TYPE public.estado_proteoma_t OWNER TO postgres;

--
-- TOC entry 905 (class 1247 OID 16422)
-- Name: familia_t; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.familia_t AS ENUM (
    'GH18',
    'GH19',
    'AMP',
    'otra'
);


ALTER TYPE public.familia_t OWNER TO postgres;

--
-- TOC entry 908 (class 1247 OID 16432)
-- Name: tipo_analisis_t; Type: TYPE; Schema: public; Owner: postgres
--

CREATE TYPE public.tipo_analisis_t AS ENUM (
    'analisis_completo',
    'reclasificar'
);


ALTER TYPE public.tipo_analisis_t OWNER TO postgres;

--
-- TOC entry 238 (class 1255 OID 16437)
-- Name: fn_actualizar_total_secuencias(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_actualizar_total_secuencias() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.fn_actualizar_total_secuencias() OWNER TO postgres;

--
-- TOC entry 239 (class 1255 OID 16438)
-- Name: fn_calcular_longitud_secuencia(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_calcular_longitud_secuencia() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.longitud := LENGTH(NEW.secuencia_aa);
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.fn_calcular_longitud_secuencia() OWNER TO postgres;

--
-- TOC entry 242 (class 1255 OID 16439)
-- Name: fn_validar_tipo_administrador(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_validar_tipo_administrador() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM investigador WHERE id_usuario = NEW.id_usuario) THEN --Hacemos el procedimiento análogo para los administradores
        RAISE EXCEPTION 'El usuario % ya es investigador, no puede ser administrador', NEW.id_usuario;
    END IF;
    UPDATE usuario SET tipo_usuario = 'administrador' WHERE id_usuario = NEW.id_usuario;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.fn_validar_tipo_administrador() OWNER TO postgres;

--
-- TOC entry 244 (class 1255 OID 16440)
-- Name: fn_validar_tipo_investigador(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_validar_tipo_investigador() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM administrador WHERE id_usuario = NEW.id_usuario) THEN --Si encontramos el investigador entre los administradores lanzamos una excepción
        RAISE EXCEPTION 'El usuario % ya es administrador, no puede ser investigador', NEW.id_usuario;
    END IF;
    UPDATE usuario SET tipo_usuario = 'investigador' WHERE id_usuario = NEW.id_usuario;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.fn_validar_tipo_investigador() OWNER TO postgres;

--
-- TOC entry 253 (class 1255 OID 16441)
-- Name: fn_validar_trabajo_unico_ejecutando(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.fn_validar_trabajo_unico_ejecutando() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.fn_validar_trabajo_unico_ejecutando() OWNER TO postgres;

--
-- TOC entry 283 (class 1255 OID 16442)
-- Name: sp_eliminar_proteoma(integer, integer); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.sp_eliminar_proteoma(IN p_id_proteoma integer, IN p_id_admin integer)
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


ALTER PROCEDURE public.sp_eliminar_proteoma(IN p_id_proteoma integer, IN p_id_admin integer) OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 216 (class 1259 OID 16443)
-- Name: administrador; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.administrador (
    id_usuario integer NOT NULL,
    rol character varying(80)
);


ALTER TABLE public.administrador OWNER TO postgres;

--
-- TOC entry 217 (class 1259 OID 16446)
-- Name: configuracion_modelo; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.configuracion_modelo (
    id_modelo integer NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion text,
    metadatos jsonb
);


ALTER TABLE public.configuracion_modelo OWNER TO postgres;

--
-- TOC entry 218 (class 1259 OID 16451)
-- Name: configuracion_modelo_id_modelo_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.configuracion_modelo_id_modelo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.configuracion_modelo_id_modelo_seq OWNER TO postgres;

--
-- TOC entry 3507 (class 0 OID 0)
-- Dependencies: 218
-- Name: configuracion_modelo_id_modelo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.configuracion_modelo_id_modelo_seq OWNED BY public.configuracion_modelo.id_modelo;


--
-- TOC entry 219 (class 1259 OID 16452)
-- Name: investigador; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.investigador (
    id_usuario integer NOT NULL,
    rama_investigacion character varying(150)
);


ALTER TABLE public.investigador OWNER TO postgres;

--
-- TOC entry 220 (class 1259 OID 16455)
-- Name: proteoma; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.proteoma (
    id_proteoma integer NOT NULL,
    nombre_proyecto character varying(150) NOT NULL,
    especie character varying(120) NOT NULL,
    cepa character varying(120),
    ruta_archivo character varying(255) NOT NULL,
    md5_fasta character varying(64) NOT NULL,
    fecha_subida timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    estado public.estado_proteoma_t DEFAULT 'pendiente'::public.estado_proteoma_t NOT NULL,
    total_secuencias integer DEFAULT 0 NOT NULL,
    visible_otros boolean DEFAULT false NOT NULL,
    id_usuario_publica integer NOT NULL,
    CONSTRAINT proteoma_total_secuencias_check CHECK ((total_secuencias >= 0))
);


ALTER TABLE public.proteoma OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 16465)
-- Name: proteoma_elimina; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.proteoma_elimina (
    id_proteoma integer NOT NULL,
    id_administrador integer NOT NULL,
    fecha_eliminacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    nombre_proyecto character varying(150),
    especie character varying(120)
);


ALTER TABLE public.proteoma_elimina OWNER TO postgres;

--
-- TOC entry 222 (class 1259 OID 16469)
-- Name: proteoma_id_proteoma_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.proteoma_id_proteoma_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.proteoma_id_proteoma_seq OWNER TO postgres;

--
-- TOC entry 3508 (class 0 OID 0)
-- Dependencies: 222
-- Name: proteoma_id_proteoma_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.proteoma_id_proteoma_seq OWNED BY public.proteoma.id_proteoma;


--
-- TOC entry 223 (class 1259 OID 16470)
-- Name: proteoma_modifica; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.proteoma_modifica (
    id_usuario integer NOT NULL,
    id_proteoma integer NOT NULL,
    fecha_modificacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.proteoma_modifica OWNER TO postgres;

--
-- TOC entry 224 (class 1259 OID 16474)
-- Name: respuesta; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.respuesta (
    id_trabajo integer NOT NULL,
    id_proteoma integer NOT NULL,
    id_secuencia bigint NOT NULL,
    es_enzima boolean NOT NULL,
    confianza_enzima real NOT NULL,
    es_hidrolasa boolean,
    familia_sugerida public.familia_t,
    confianza_familia real,
    numero_ec public.ec_number,
    razonamiento text,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_ec_formato CHECK ((((numero_ec)::text ~ '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'::text) OR (numero_ec IS NULL))),
    CONSTRAINT respuesta_confianza_enzima_check CHECK (((confianza_enzima >= (0)::double precision) AND (confianza_enzima <= (1)::double precision))),
    CONSTRAINT respuesta_confianza_familia_check CHECK (((confianza_familia >= (0)::double precision) AND (confianza_familia <= (1)::double precision)))
);


ALTER TABLE public.respuesta OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 16483)
-- Name: secuencia; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.secuencia (
    id_proteoma integer NOT NULL,
    id bigint NOT NULL,
    longitud integer,
    secuencia_aa text NOT NULL,
    CONSTRAINT secuencia_longitud_check CHECK ((longitud >= 0))
);


ALTER TABLE public.secuencia OWNER TO postgres;

--
-- TOC entry 226 (class 1259 OID 16489)
-- Name: secuencia_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.secuencia_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.secuencia_id_seq OWNER TO postgres;

--
-- TOC entry 3509 (class 0 OID 0)
-- Dependencies: 226
-- Name: secuencia_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.secuencia_id_seq OWNED BY public.secuencia.id;


--
-- TOC entry 227 (class 1259 OID 16490)
-- Name: trabajo_analisis; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.trabajo_analisis (
    id_trabajo integer NOT NULL,
    tipo public.tipo_analisis_t NOT NULL,
    estado public.estado_analisis_t DEFAULT 'en_cola'::public.estado_analisis_t NOT NULL,
    fecha_inicio timestamp without time zone,
    fecha_fin timestamp without time zone,
    mensaje_error text,
    id_proteoma integer NOT NULL,
    id_usuario integer NOT NULL,
    id_modelo integer NOT NULL
);


ALTER TABLE public.trabajo_analisis OWNER TO postgres;

--
-- TOC entry 228 (class 1259 OID 16496)
-- Name: trabajo_analisis_id_trabajo_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.trabajo_analisis_id_trabajo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.trabajo_analisis_id_trabajo_seq OWNER TO postgres;

--
-- TOC entry 3510 (class 0 OID 0)
-- Dependencies: 228
-- Name: trabajo_analisis_id_trabajo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.trabajo_analisis_id_trabajo_seq OWNED BY public.trabajo_analisis.id_trabajo;


--
-- TOC entry 229 (class 1259 OID 16497)
-- Name: usuario; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.usuario (
    id_usuario integer NOT NULL,
    nombre_usuario character varying(50) NOT NULL,
    correo character varying(120) NOT NULL,
    hash_contrasena character varying(255) NOT NULL,
    institucion character varying(150),
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ultimo_acceso timestamp without time zone,
    activo boolean DEFAULT true NOT NULL,
    tipo_usuario character varying(20) NOT NULL,
    CONSTRAINT usuario_tipo_usuario_check CHECK (((tipo_usuario)::text = ANY (ARRAY[('investigador'::character varying)::text, ('administrador'::character varying)::text])))
);


ALTER TABLE public.usuario OWNER TO postgres;

--
-- TOC entry 230 (class 1259 OID 16505)
-- Name: usuario_id_usuario_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.usuario_id_usuario_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.usuario_id_usuario_seq OWNER TO postgres;

--
-- TOC entry 3511 (class 0 OID 0)
-- Dependencies: 230
-- Name: usuario_id_usuario_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.usuario_id_usuario_seq OWNED BY public.usuario.id_usuario;


--
-- TOC entry 3273 (class 2604 OID 16506)
-- Name: configuracion_modelo id_modelo; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuracion_modelo ALTER COLUMN id_modelo SET DEFAULT nextval('public.configuracion_modelo_id_modelo_seq'::regclass);


--
-- TOC entry 3274 (class 2604 OID 16507)
-- Name: proteoma id_proteoma; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.proteoma ALTER COLUMN id_proteoma SET DEFAULT nextval('public.proteoma_id_proteoma_seq'::regclass);


--
-- TOC entry 3282 (class 2604 OID 16508)
-- Name: secuencia id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.secuencia ALTER COLUMN id SET DEFAULT nextval('public.secuencia_id_seq'::regclass);


--
-- TOC entry 3283 (class 2604 OID 16509)
-- Name: trabajo_analisis id_trabajo; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trabajo_analisis ALTER COLUMN id_trabajo SET DEFAULT nextval('public.trabajo_analisis_id_trabajo_seq'::regclass);


--
-- TOC entry 3285 (class 2604 OID 16510)
-- Name: usuario id_usuario; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario ALTER COLUMN id_usuario SET DEFAULT nextval('public.usuario_id_usuario_seq'::regclass);


--
-- TOC entry 3485 (class 0 OID 16443)
-- Dependencies: 216
-- Data for Name: administrador; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.administrador (id_usuario, rol) FROM stdin;
55	Super Administrador
56	Administrador de Datos
\.


--
-- TOC entry 3486 (class 0 OID 16446)
-- Dependencies: 217
-- Data for Name: configuracion_modelo; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.configuracion_modelo (id_modelo, nombre, descripcion, metadatos) FROM stdin;
19	CNN v1.0	Red neuronal convolucional para clasificación de familias	{"tipo": "CNN", "capas": 5, "dataset": "uniprot"}
20	RandomForest v2	Ensemble de árboles de decisión	{"tipo": "RF", "estimadores": 100, "profundidad": 20}
21	ProtBERT	Modelo de lenguaje pre-entrenado para proteínas	{"tipo": "Transformer", "parametros": "420M"}
22	Llama-3.1-8B-Instruct	Clasificador basado en descriptores AAontology + razonamiento generado por Llama	{"tipo": "LLM", "proveedor": "Llama"}
\.


--
-- TOC entry 3488 (class 0 OID 16452)
-- Dependencies: 219
-- Data for Name: investigador; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.investigador (id_usuario, rama_investigacion) FROM stdin;
57	Bioinformática
58	Microbiología
59	Genómica
60	Alumno Ayudante
\.


--
-- TOC entry 3489 (class 0 OID 16455)
-- Dependencies: 220
-- Data for Name: proteoma; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.proteoma (id_proteoma, nombre_proyecto, especie, cepa, ruta_archivo, md5_fasta, fecha_subida, estado, total_secuencias, visible_otros, id_usuario_publica) FROM stdin;
52	Proyecto Quitina	Aspergillus niger	ATCC 1015	/data/fasta/aspergillus.fasta	81dc9bdb52d04dc20036dbd8313ed055	2026-07-10 00:24:33.689204	completado	5	t	57
53	Proyecto Antimicrobiano	Bacillus subtilis	168	/data/fasta/bacillus.fasta	674f3c2c1a8a6f90461e8a66fb5550ba	2026-07-10 00:24:33.689204	procesando	3	f	58
51	Proyecto Humano	Homo sapiens	HeLa	/data/fasta/human.fasta	c5c53759e4dd1bfe8b3dcfec37d0ea72	2026-07-10 00:24:33.689204	pendiente	4	t	55
54	Proteoma Base	Proteoma E. coli K12	K12	/data/proteomas/ecoli_k12.fasta	a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0u1v2w3x4y5z6	2026-07-10 14:11:21.797096	completado	0	t	60
55	Cerevisiae	Proteoma S. cerevisiae	S.	/data/proteomas/scerevisiae.fasta	z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4j3i2h1g0f9e8d7c6b5a4	2026-07-10 14:12:14.134833	pendiente	3	t	60
56	Proteoma humano (muestra)	Homo sapiens	HeLa	/data/proteomas/human_hela.fasta	f1e2d3c4b5a6f7e8d9c0b1a2f3e4d5c6b7a8f9e0d1c2b3a4f5e6d7c8b9a0	2026-07-10 14:16:57.150621	completado	5	t	60
57	Proyecto Quitinasas	Candida albicans	SC5314	/data/proteomas/quitinasas.fasta	a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6	2026-07-10 14:23:25.401563	completado	4	t	60
58	Análisis de AMPs	Homo sapiens	332	/data/proteomas/amps_humanos.fasta	z9y8x7w6v5u4t3s2r1q0p9o8n7m6l5k4	2026-07-10 14:28:42.569078	pendiente	3	t	60
\.


--
-- TOC entry 3490 (class 0 OID 16465)
-- Dependencies: 221
-- Data for Name: proteoma_elimina; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.proteoma_elimina (id_proteoma, id_administrador, fecha_eliminacion, nombre_proyecto, especie) FROM stdin;
51	55	2026-06-30 00:24:33.689204	Proyecto Humano	Homo sapiens
\.


--
-- TOC entry 3492 (class 0 OID 16470)
-- Dependencies: 223
-- Data for Name: proteoma_modifica; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.proteoma_modifica (id_usuario, id_proteoma, fecha_modificacion) FROM stdin;
57	52	2026-07-09 00:24:33.689204
55	53	2026-07-09 22:24:33.689204
\.


--
-- TOC entry 3493 (class 0 OID 16474)
-- Dependencies: 224
-- Data for Name: respuesta; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.respuesta (id_trabajo, id_proteoma, id_secuencia, es_enzima, confianza_enzima, es_hidrolasa, familia_sugerida, confianza_familia, numero_ec, razonamiento, fecha) FROM stdin;
107	52	47	t	0.98	t	GH18	0.95	3.2.1.14	Dominios conservados GH18 detectados	2026-07-10 00:24:33.689204
107	52	48	t	0.85	t	GH19	0.8	3.2.1.14	Plegamiento similar a lisozima	2026-07-10 00:24:33.689204
107	52	49	f	0.92	\N	\N	\N	\N	No se detectaron dominios enzimáticos	2026-07-10 00:24:33.689204
107	52	50	t	0.75	f	AMP	0.88	\N	Péptido antimicrobiano candidato	2026-07-10 00:24:33.689204
107	52	51	t	0.99	t	otra	0.7	3.4.21.89	Familia no GH, posible serín proteasa	2026-07-10 00:24:33.689204
110	56	62	f	0	\N	otra	\N	\N	No se detectan características típicas de una enzima en la secuencia proporcionada.	2026-07-10 14:18:48.953684
110	56	63	f	0	\N	otra	\N	\N	No se detectan características típicas de enzimas en la secuencia	2026-07-10 14:18:50.266461
110	56	64	f	0	\N	\N	\N	\N	No se detectan características típicas de enzimas en la secuencia	2026-07-10 14:18:51.481568
110	56	65	f	0	\N	\N	\N	\N	No se detectan características típicas de enzimas en la secuencia	2026-07-10 14:18:53.24049
110	56	66	f	0	\N	\N	\N	\N	No se detectan características típicas de una enzima en la secuencia proporcionada.	2026-07-10 14:18:54.478648
111	57	67	f	0	\N	\N	\N	\N	La secuencia no muestra características típicas de una enzima	2026-07-10 14:25:16.078334
111	57	68	f	0	\N	otra	\N	\N	La secuencia no muestra características típicas de enzimas ni de hidrolasas.	2026-07-10 14:25:17.608791
111	57	69	f	0	\N	otra	\N	\N	La secuencia no muestra características típicas de una enzima y no se puede asignar a una familia específica.	2026-07-10 14:25:19.765872
111	57	70	f	0	\N	\N	\N	\N	La secuencia no tiene características típicas de una enzima	2026-07-10 14:25:24.086236
\.


--
-- TOC entry 3494 (class 0 OID 16483)
-- Dependencies: 225
-- Data for Name: secuencia; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.secuencia (id_proteoma, id, longitud, secuencia_aa) FROM stdin;
52	47	20	MKTLLLTAVLLLVVPLVAGD
52	48	54	MALWMRLLPLLALLALWGPDPAAAFVNQHLCGSHLVEALYLVCGERGFFYTPKT
52	49	21	MRVLVLLGLFLVAGVAAGDPG
52	50	9	MAVLLLILW
52	51	18	MVKVYAPASSANMSVGFD
53	52	15	MKKLLVLVLLLALAG
53	53	12	MTVLLALLAVVG
53	54	10	MLVLVLLLAG
51	55	11	MAAVLLLVLVV
51	56	11	MVDVLVLLLVV
51	57	12	MRSVLVLLVLVV
51	58	12	MYKVLVLLVLVV
55	59	110	MALWMRLLPLLALLALWGPDPAAAFVNQHLCGSHLVEALYLVCGERGFFYTPKTRREAEDLQVGQVELGGGPGAGSLQPLALEGSLQKRGIVEQCCTSICSLYQLENYCN
55	60	38	MKKLLLAVLLLAAVAGSSAQAEEQKLAALEKAGQIVPN
55	61	238	MSKGEELFTGVVPILVELDGDVNGHKFSVSGEGEGDATYGKLTLKFICTTGKLPVPWPTLVTTLTYGVQCFSRYPDHMKQHDFFKSAMPEGYVQERTIFFKDDGNYKTRAEVKFEGDTLVNRIELKGIDFKEDGNILGHKLEYNYNSHNVYIMADKQKNGIKVNFKIRHNIEDGSVQLADHYQQNTPIGDGPVLLPDNHYLSTQSALSKDPNEKRDHMVLLEFVTAAGITHGMDELYK
56	62	223	IVGGYTCGANTVPYQVSLNSGYHFCGGSLINSQWVVSAAHCYKSGIQVRLGEDNINVVEGNEQFISASKSIVHPSYNSNTLNNDIMLIKLKSAASLNSRVASISLPTSCASAGTQCLISGWGNTKSSGTSYPDVLKCLKAPILSDSSCKSAYPGQITSNMFCAGYLEGGKDSCQGDSGGPVVCSGKLQGIVSWGSGCAQKNKPGVYTKVCNYVSWIKQTIASN
56	63	129	KVFGRCELAAAMKRHGLDNYRGYSLGNWVCAAKFESNFNTQATNRNTDGSTDYGILQINSRWWCNDGRTPGSRNLCNIPCSALLSSDITASVNCAKKIVSDGNGMNAWVAWRNRCKGTDVQAWIRGCRL
56	64	209	MKQSTIALALLPLLFTPVTKARTPVEMVQQAQVLSKDTFTLAPVTGKLALVTRHLYDDTDIPVVATIAGGTASTPVTAGTSRGTAVTTGQSGSSATGDADTGVAPATASGQSGPSEGQTTVVASTTTVSDTMLTGSVGPGTTAGQSQLKGSGTAAISGTKGMPDNVSGQLLDSPDTSGKALNIALQPNSGMTVLGGAVVGSGTSTATGP
56	65	605	MLEICLKLVGCKSKKGLSSSSSCYLEEALQRPVASDFEPQGLSEAARWNSKENLLAGPSENDPNLFVALYDFVASGDNTLSITKGEKLRVLGYNHNGEWCEAQTKNGQGWVPSNYITPVNSLEKHSWYHGPVSRNAAEYLLSSGINGSFLVRESESSPGQRSISLRYEGRVYHYRINTASDGKLYVSSESRFNTLAELVHHHSTVADGLITTLHYPAPKRNKPTVYGVSPNYDKWEMERTDITMKHKLGGGQYGEVYEGVWKKYSLTVAVKTLKEDTMEVEEFLKEAAVMKEIKHPNLVQLLGVCTREPPFYIITEFMTYGNLLDYLRECNRQEVNAVVLLYMATQISAMEYLEKKNFIHRDLAARNCLVGENHLVKVADFGLSRLMTGDTYTAHAGAKFPIKWTAPESLAYNKFSIKSDVWAFGVLLWEIATYGMSPYPGIDLSQVYELLEKDYRMERPEGCPEKVYELMRACWQWNPSDRPSFAEIHQAFETMFQESSISDEVEKELGKQGVRGAVSTLLQAPELPTKTRTSRRAAEHRDTTDVPEMPHSKGQGESDPLDHEPAVSPLLPRKERGPPEGGLNEDERLLPKDKKTNLFSALIKK
56	66	147	MVHLTPEEKSAVTALWGKVNVDEVGGEALGRLLVVYPWTQRFFESFGDLSTPDAVMGNPKVKAHGKKVLGAFSDGLAHLDNLKGTFATLSELHCDKLHVDPENFRLLGNVLVCVLAHHFGKEFTPPVQAAYQKVVAGVANALAHKYH
57	67	273	MASSRARLFVAAVLLLALAVASAQAPGQAGGAAVPGSTGPGATGDYVATLDAGKFRDYVQYFANMVRDRNGSFGADYSRAWNPAAQAAAAAAGAVSFAGGVVQYGNHLPAGQHVGFDGLDMDWEYPAGAQYRVGPGQPRGFGATVNYNGLCNQKQTYCDAVAHPAALAPGVTPGGRVGASGSPAAGGVDLAGGVVQYGNVLPAGQHVGFDGLDMDWEYPAGAQYRVGPGQPRGFGATVNYNGLCNQKQTYCDAVAHPAALAPGVTPGGRVGAS
57	68	355	MKKLLGIALAGLAAVAAPAMAADGSGKPIAVYIWNQAYDPSKPFNDGATLEAGYAVTYDAAGNVAATAAKAFTAGKPKVGVTALVDATKVYQDLVRYWGQNGKTAWADAVSAWQKVTGKPATYDLVGPWANNGAGGVGYKGAAAIAGVADYLKQAGYQAGKVIVGIPYAATGGASVAGSDTAYTFGGAAGVANYGQYVRASVWNPYQADYAAYQQVVDTLKAYGKDLRVLDVPGYVATGATDASGATYGVTADYADQIKAAGGVKGKLVVMSWDDATAGTAQYKAYAGTIKADGKTATVTYPGTNTGAVKAFATPATGVGGWTATGDGAGDGPFGYGTVGDTAGVTGGAGYTVGP
57	69	273	MASSRARLFVAAVLLLALAVASAQAPGQAGGAAVPGSTGPGATGDYVATLDAGKFRDYVQYFANMVRDRNGSFGADYSRAWNPAAQAAAAAAGAVSFAGGVVQYGNHLPAGQHVGFDGLDMDWEYPAGAQYRVGPGQPRGFGATVNYNGLCNQKQTYCDAVAHPAALAPGVTPGGRVGASGSPAAGGVDLAGGVVQYGNVLPAGQHVGFDGLDMDWEYPAGAQYRVGPGQPRGFGATVNYNGLCNQKQTYCDAVAHPAALAPGVTPGGRVGAS
57	70	37	LLGDFFRKSKEKIGKEFKRIVQRIKDFLRNLVPRTES
58	71	126	MKKPLAVLAAVAAAATAAAGQAGPAVPAAPAGYADGKDYGIVIGWTGVNVGAPNGALGWNGGNDKQYAIATAVSVDFLHPDKCVWDKVYISQNLKGFQESMMTPAMKQAGKLVLGVPSVDAITGIK
58	72	119	MAKLLLVLFLVLTLSLAHSAQCSVPGKGAFSASADYATKIVLAKVGSAASDFQSAFVLAWNAGEGVKQKRIAVDKFAKLTTYGPAFLKTTGEPSWQYREMLAAVRDKKLPGFTGAVWGK
58	73	122	MVSAFTSFLLFAAAVTRAAPTVGYYTAPGQAVWAVGADTGNELRACANTCNAKFAVFDFANGGTGLTQTTPAALGLWDARLMAHWDGSAQGLSALVDEYVAFYSAAKRTGLDGLDIDWEYPA
\.


--
-- TOC entry 3496 (class 0 OID 16490)
-- Dependencies: 227
-- Data for Name: trabajo_analisis; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.trabajo_analisis (id_trabajo, tipo, estado, fecha_inicio, fecha_fin, mensaje_error, id_proteoma, id_usuario, id_modelo) FROM stdin;
111	analisis_completo	completado	2026-07-10 14:25:14.151141	2026-07-10 14:25:24.108742	\N	57	60	22
105	analisis_completo	fallido	2026-07-09 00:24:33.689204	2026-07-09 00:24:33.689204	Falta de memoria en GPU	51	56	19
106	reclasificar	en_cola	\N	\N	\N	52	57	20
107	analisis_completo	completado	2026-07-08 00:24:33.689204	2026-07-09 00:24:33.689204	\N	52	57	19
108	analisis_completo	ejecutando	2026-07-09 19:24:33.689204	\N	\N	53	58	21
109	analisis_completo	completado	2026-07-10 14:14:13.005362	2026-07-10 14:14:13.069161	\N	54	60	22
110	analisis_completo	completado	2026-07-10 14:18:32.455321	2026-07-10 14:18:54.504658	\N	56	60	22
\.


--
-- TOC entry 3498 (class 0 OID 16497)
-- Dependencies: 229
-- Data for Name: usuario; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.usuario (id_usuario, nombre_usuario, correo, hash_contrasena, institucion, fecha_creacion, ultimo_acceso, activo, tipo_usuario) FROM stdin;
57	jperez	juan.perez@ibp.cu	807f8f65b860ece5afc86b7ce5bfc6bc3eec67c1fbdf90368e3349b0a88d8a6c	IBP	2026-07-10 00:24:33.689204	\N	t	investigador
58	mgomez	maria.gomez@lab.cu	807f8f65b860ece5afc86b7ce5bfc6bc3eec67c1fbdf90368e3349b0a88d8a6c	LAB	2026-07-10 00:24:33.689204	\N	t	investigador
59	cruiz	carlos.ruiz@univ.cu	170b00da0d752f0eef5fa3608ea2e6c0bd751a9bf539dc101ebe9425f5003c53	UNIV	2026-07-10 00:24:33.689204	\N	f	investigador
55	nnunez	nestor@ibp.cu	8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918	IBP	2026-07-10 00:24:33.689204	2026-07-10 00:24:33.689204	t	administrador
56	aacosta	adriana@ibp.cu	8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918	UCLV	2026-07-10 00:24:33.689204	2026-07-10 00:24:33.689204	t	administrador
60	cjgarcia	cj@uclv.cu	89270a50341bb8e83734ee062603c63c219320ed4a40fb9fb8c2792358548e8a	Facultad MFC	2026-07-10 13:55:38.032183	\N	t	investigador
\.


--
-- TOC entry 3512 (class 0 OID 0)
-- Dependencies: 218
-- Name: configuracion_modelo_id_modelo_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.configuracion_modelo_id_modelo_seq', 22, true);


--
-- TOC entry 3513 (class 0 OID 0)
-- Dependencies: 222
-- Name: proteoma_id_proteoma_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.proteoma_id_proteoma_seq', 58, true);


--
-- TOC entry 3514 (class 0 OID 0)
-- Dependencies: 226
-- Name: secuencia_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.secuencia_id_seq', 73, true);


--
-- TOC entry 3515 (class 0 OID 0)
-- Dependencies: 228
-- Name: trabajo_analisis_id_trabajo_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.trabajo_analisis_id_trabajo_seq', 111, true);


--
-- TOC entry 3516 (class 0 OID 0)
-- Dependencies: 230
-- Name: usuario_id_usuario_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.usuario_id_usuario_seq', 61, true);


--
-- TOC entry 3295 (class 2606 OID 16512)
-- Name: administrador administrador_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.administrador
    ADD CONSTRAINT administrador_pkey PRIMARY KEY (id_usuario);


--
-- TOC entry 3297 (class 2606 OID 16514)
-- Name: configuracion_modelo configuracion_modelo_nombre_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuracion_modelo
    ADD CONSTRAINT configuracion_modelo_nombre_key UNIQUE (nombre);


--
-- TOC entry 3299 (class 2606 OID 16516)
-- Name: configuracion_modelo configuracion_modelo_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuracion_modelo
    ADD CONSTRAINT configuracion_modelo_pkey PRIMARY KEY (id_modelo);


--
-- TOC entry 3301 (class 2606 OID 16518)
-- Name: investigador investigador_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.investigador
    ADD CONSTRAINT investigador_pkey PRIMARY KEY (id_usuario);


--
-- TOC entry 3306 (class 2606 OID 16520)
-- Name: proteoma_elimina proteoma_elimina_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.proteoma_elimina
    ADD CONSTRAINT proteoma_elimina_pkey PRIMARY KEY (id_proteoma);


--
-- TOC entry 3308 (class 2606 OID 16522)
-- Name: proteoma_modifica proteoma_modifica_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.proteoma_modifica
    ADD CONSTRAINT proteoma_modifica_pkey PRIMARY KEY (id_usuario, id_proteoma, fecha_modificacion);


--
-- TOC entry 3304 (class 2606 OID 16524)
-- Name: proteoma proteoma_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.proteoma
    ADD CONSTRAINT proteoma_pkey PRIMARY KEY (id_proteoma);


--
-- TOC entry 3311 (class 2606 OID 16526)
-- Name: respuesta respuesta_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.respuesta
    ADD CONSTRAINT respuesta_pkey PRIMARY KEY (id_trabajo, id_secuencia);


--
-- TOC entry 3314 (class 2606 OID 16528)
-- Name: secuencia secuencia_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.secuencia
    ADD CONSTRAINT secuencia_pkey PRIMARY KEY (id_proteoma, id);


--
-- TOC entry 3318 (class 2606 OID 16530)
-- Name: trabajo_analisis trabajo_analisis_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trabajo_analisis
    ADD CONSTRAINT trabajo_analisis_pkey PRIMARY KEY (id_trabajo);


--
-- TOC entry 3320 (class 2606 OID 16532)
-- Name: usuario usuario_correo_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_correo_key UNIQUE (correo);


--
-- TOC entry 3322 (class 2606 OID 16534)
-- Name: usuario usuario_nombre_usuario_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_nombre_usuario_key UNIQUE (nombre_usuario);


--
-- TOC entry 3324 (class 2606 OID 16536)
-- Name: usuario usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id_usuario);


--
-- TOC entry 3302 (class 1259 OID 16537)
-- Name: idx_proteoma_usuario; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_proteoma_usuario ON public.proteoma USING btree (id_usuario_publica);


--
-- TOC entry 3309 (class 1259 OID 16538)
-- Name: idx_respuesta_secuencia; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_respuesta_secuencia ON public.respuesta USING btree (id_proteoma, id_secuencia);


--
-- TOC entry 3312 (class 1259 OID 16539)
-- Name: idx_secuencia_proteoma; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_secuencia_proteoma ON public.secuencia USING btree (id_proteoma);


--
-- TOC entry 3315 (class 1259 OID 16540)
-- Name: idx_trabajo_proteoma; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_trabajo_proteoma ON public.trabajo_analisis USING btree (id_proteoma);


--
-- TOC entry 3316 (class 1259 OID 16541)
-- Name: idx_trabajo_usuario; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_trabajo_usuario ON public.trabajo_analisis USING btree (id_usuario);


--
-- TOC entry 3339 (class 2620 OID 16542)
-- Name: secuencia trg_calcular_longitud; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_calcular_longitud BEFORE INSERT OR UPDATE ON public.secuencia FOR EACH ROW EXECUTE FUNCTION public.fn_calcular_longitud_secuencia();


--
-- TOC entry 3340 (class 2620 OID 16543)
-- Name: secuencia trg_total_secuencias_delete; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_total_secuencias_delete AFTER DELETE ON public.secuencia FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_total_secuencias();


--
-- TOC entry 3341 (class 2620 OID 16544)
-- Name: secuencia trg_total_secuencias_insert; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_total_secuencias_insert AFTER INSERT ON public.secuencia FOR EACH ROW EXECUTE FUNCTION public.fn_actualizar_total_secuencias();


--
-- TOC entry 3342 (class 2620 OID 16545)
-- Name: trabajo_analisis trg_trabajo_unico_ejecutando; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_trabajo_unico_ejecutando BEFORE INSERT OR UPDATE ON public.trabajo_analisis FOR EACH ROW EXECUTE FUNCTION public.fn_validar_trabajo_unico_ejecutando();


--
-- TOC entry 3337 (class 2620 OID 16546)
-- Name: administrador trg_validar_administrador; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validar_administrador BEFORE INSERT ON public.administrador FOR EACH ROW EXECUTE FUNCTION public.fn_validar_tipo_administrador();


--
-- TOC entry 3338 (class 2620 OID 16547)
-- Name: investigador trg_validar_investigador; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validar_investigador BEFORE INSERT ON public.investigador FOR EACH ROW EXECUTE FUNCTION public.fn_validar_tipo_investigador();


--
-- TOC entry 3325 (class 2606 OID 16548)
-- Name: administrador administrador_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.administrador
    ADD CONSTRAINT administrador_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuario(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 3326 (class 2606 OID 16553)
-- Name: investigador investigador_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.investigador
    ADD CONSTRAINT investigador_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuario(id_usuario) ON DELETE CASCADE;


--
-- TOC entry 3328 (class 2606 OID 16558)
-- Name: proteoma_elimina proteoma_elimina_id_administrador_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.proteoma_elimina
    ADD CONSTRAINT proteoma_elimina_id_administrador_fkey FOREIGN KEY (id_administrador) REFERENCES public.administrador(id_usuario);


--
-- TOC entry 3327 (class 2606 OID 16563)
-- Name: proteoma proteoma_id_usuario_publica_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.proteoma
    ADD CONSTRAINT proteoma_id_usuario_publica_fkey FOREIGN KEY (id_usuario_publica) REFERENCES public.usuario(id_usuario);


--
-- TOC entry 3329 (class 2606 OID 16568)
-- Name: proteoma_modifica proteoma_modifica_id_proteoma_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.proteoma_modifica
    ADD CONSTRAINT proteoma_modifica_id_proteoma_fkey FOREIGN KEY (id_proteoma) REFERENCES public.proteoma(id_proteoma) ON DELETE CASCADE;


--
-- TOC entry 3330 (class 2606 OID 16573)
-- Name: proteoma_modifica proteoma_modifica_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.proteoma_modifica
    ADD CONSTRAINT proteoma_modifica_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuario(id_usuario);


--
-- TOC entry 3331 (class 2606 OID 16578)
-- Name: respuesta respuesta_id_proteoma_id_secuencia_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.respuesta
    ADD CONSTRAINT respuesta_id_proteoma_id_secuencia_fkey FOREIGN KEY (id_proteoma, id_secuencia) REFERENCES public.secuencia(id_proteoma, id) ON DELETE CASCADE;


--
-- TOC entry 3332 (class 2606 OID 16583)
-- Name: respuesta respuesta_id_trabajo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.respuesta
    ADD CONSTRAINT respuesta_id_trabajo_fkey FOREIGN KEY (id_trabajo) REFERENCES public.trabajo_analisis(id_trabajo) ON DELETE CASCADE;


--
-- TOC entry 3333 (class 2606 OID 16588)
-- Name: secuencia secuencia_id_proteoma_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.secuencia
    ADD CONSTRAINT secuencia_id_proteoma_fkey FOREIGN KEY (id_proteoma) REFERENCES public.proteoma(id_proteoma) ON DELETE CASCADE;


--
-- TOC entry 3334 (class 2606 OID 16593)
-- Name: trabajo_analisis trabajo_analisis_id_modelo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trabajo_analisis
    ADD CONSTRAINT trabajo_analisis_id_modelo_fkey FOREIGN KEY (id_modelo) REFERENCES public.configuracion_modelo(id_modelo);


--
-- TOC entry 3335 (class 2606 OID 16598)
-- Name: trabajo_analisis trabajo_analisis_id_proteoma_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trabajo_analisis
    ADD CONSTRAINT trabajo_analisis_id_proteoma_fkey FOREIGN KEY (id_proteoma) REFERENCES public.proteoma(id_proteoma) ON DELETE CASCADE;


--
-- TOC entry 3336 (class 2606 OID 16603)
-- Name: trabajo_analisis trabajo_analisis_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.trabajo_analisis
    ADD CONSTRAINT trabajo_analisis_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuario(id_usuario);


-- Completed on 2026-07-10 14:31:30

--
-- PostgreSQL database dump complete
--

