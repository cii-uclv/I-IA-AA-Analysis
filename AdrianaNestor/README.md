# 🧬 Analista de Proteomas – Clasificación con AAontology + LLM

Sistema de escritorio para la gestión y clasificación jerárquica de proteomas, que combina:

- Un **esquema relacional robusto** en PostgreSQL con reglas de negocio (triggers, procedimientos almacenados, herencia de tablas).
- Un **orquestador Java** que procesa proteomas pendientes, calcula descriptores fisicoquímicos basados en la ontología **AAontology** (586 escalas agrupadas en 8 categorías) y consulta a un **LLM** (Llama 3.1) alojado en Hugging Face para generar un razonamiento experto y una clasificación en 3 niveles:
  1. ¿Es enzima?
  2. ¿Es hidrolasa?
  3. Familia sugerida (GH18, GH19, AMP u otra) + número EC (si aplica).

Los resultados se almacenan en la tabla `respuesta`, junto con el razonamiento en lenguaje natural generado por el LLM, permitiendo trazabilidad y auditoría.

**Dentro de la carpeta se encuentra adjuntado un diagrama de clases de la aplicación, así como un diagrama de actividad de su funcionalidad principal.**

---

## 📋 Características principales

- **Gestión de usuarios** con herencia exclusiva/total: `investigador` y `administrador`.
- **Control de acceso** por rol: los investigadores solo ven y modifican sus propios proteomas; los administradores tienen control total.
- **Proteomas**: registro, carga de secuencias (FASTA), estados (`pendiente`, `procesando`, `completado`, `fallido`) y trazabilidad de modificaciones/eliminaciones.
- **Análisis automático**: el orquestador detecta proteomas en estado `pendiente` y lanza el flujo de clasificación.
- **Descriptores AAontology**: cálculo de 8 categorías fisicoquímicas (composición, autocorrelación, energía, forma, estructura secundaria, polaridad, ASA, otras) utilizando una escala representativa por categoría (arquitectura extensible a las 586 escalas originales).
- **Integración con LLM** (Hugging Face): prompt estructurado con contexto fisicoquímico, generación de respuesta JSON con clasificación y razonamiento.
- **Interfaz Swing**: paneles para orquestador, listado de proteomas, secuencias, respuestas y gestión de usuarios (solo administradores).
- **Seguridad**: contraseñas hasheadas con SHA‑256, variables de entorno para credenciales sensibles.

---

## 🛠️ Requisitos previos

- **Java 17** o superior (JDK).
- **PostgreSQL 15** o superior (con extensión `pgcrypto` opcional).
- **Maven** (para compilar y empaquetar).
- Clave de API de Hugging Face (para acceder al modelo Llama 3.1).
- Opcional: IDE (IntelliJ, Eclipse, VS Code) para desarrollo.

---

### Crear la base de datos

`psql -U postgres -d proteomas_bd -f sql/proteomasPlain_poblada.sql`

La base de datos comentada es la misma que esta, pero con comentarios para mejorar la comprensión.

---

### Configurar variables de entorno

Crea un archivo `.env` en la raíz del proyecto (o define las variables en el sistema) con el siguiente contenido:

```
Conexión a PostgreSQL

DB_URL=jdbc:postgresql://localhost:5432/proteomas_bd
DB_USER=postgres
DB_PASSWORD=tu_contraseña

Hugging Face API

HF_API_KEY=hf_tu_clave_de_api
HF_API_URL=https://router.huggingface.co/v1/chat/completions
HF_MODEL=meta-llama/Llama-3.1-8B-Instruct
```

---

### Compilar y empaquetar

`mvn clean package`

---

## Uso

```bash
java -jar target/AnalistaProteomas.jar
```

La aplicación mostrará una ventana de inicio de sesión. Los datos de prueba de los usuarios incluyen:

| Usuario  | Contraseña   | Rol           |
| -------- | ------------ | ------------- |
| nnunez   | admin        | administrador |
| aacosta  | admin        | administrador |
| cjgarcia | investigador | investigador  |

*(Las contraseñas se almacenan hasheadas).*

### Paneles principales

- **Orquestador**: lista los proteomas pendientes (según el rol del usuario) y permite ejecutar el análisis completo o sobre un proteoma seleccionado. El log muestra el progreso.

- **Proteomas**: visualiza los proteomas, crea nuevos (en estado `pendiente`) y elimina (solo administradores, con trazabilidad en `proteoma_elimina`).

- **Secuencias**: permite cargar archivos FASTA o agregar secuencias manualmente a un proteoma. Solo el propietario (o administrador) puede modificar.

- **Respuestas del LLM**: muestra los resultados de clasificación almacenados en `respuesta`, con el razonamiento completo del modelo.

- **Usuarios** (solo administradores): creación de nuevos usuarios (investigador o administrador).

## Flujo de análisis (orquestador)

1. El usuario (investigador o administrador) inicia el proceso desde el panel **Orquestador**.

2. El sistema consulta los proteomas en estado `pendiente` (filtrados por rol).

3. Por cada proteoma:
   
   - Crea un registro en `trabajo_analisis` con estado `en_cola`, luego lo marca como `ejecutando` (trigger valida unicidad).
   
   - Cambia el estado del proteoma a `procesando` y registra la modificación en `proteoma_modifica`.
   
   - Para cada secuencia:
     
     - Calcula descriptores AAontology (8 categorías) usando `DescriptorCalculator`.
     
     - Construye un prompt con los descriptores y la secuencia.
     
     - Llama al LLM (Hugging Face) y obtiene un JSON con la clasificación.
     
     - Inserta el resultado en `respuesta`.
   
   - Al finalizar, marca el trabajo como `completado` y el proteoma como `completado` (o `fallido` en caso de error).

4. El log en la UI muestra el progreso de cada secuencia.

---

## Estructura del proyecto

```
src/main/java/cu/uclv/proteomas/
├── Main.java                         # Punto de entrada
├── aaontology/                       # Descriptores AAontology
│   ├── CategoriaAAontology.java
│   ├── DescriptorCalculator.java
│   ├── DescriptorProteina.java
│   ├── Escala.java
│   └── EscalasRegistry.java
├── config/                           # Configuración (variables de entorno)
│   └── AppConfig.java
├── dao/                              # Interfaces y DAOs
│   ├── ConfiguracionModeloDao.java
│   ├── Dao.java
│   ├── ProteomaDao.java
│   ├── RespuestaDao.java
│   ├── SecuenciaDao.java
│   ├── TrabajoAnalisisDao.java
│   ├── UsuarioDao.java
│   └── impl/                         # Implementaciones JDBC
├── db/                               # Gestión de conexión
│   └── DatabaseManager.java
├── llm/                              # Integración con Hugging Face
│   ├── ClasificacionParser.java
│   ├── HuggingFaceClient.java
│   └── PromptBuilder.java
├── model/                            # Modelos de dominio (POJOs)
│   ├── ConfiguracionModelo.java
│   ├── Enums.java
│   ├── Proteoma.java
│   ├── Respuesta.java
│   ├── Secuencia.java
│   ├── TrabajoAnalisis.java
│   └── Usuario.java
├── orquestador/                      # Lógica de orquestación
│   └── OrquestadorAnalisis.java
└── ui/                               # Interfaz Swing
    ├── LoginDialog.java
    ├── MainFrame.java
    ├── OrquestadorPanel.java
    ├── ProteomaPanel.java
    ├── RespuestaPanel.java
    ├── SecuenciaPanel.java
    └── UsuarioPanel.java

sql/                                  
├── proteomasBD_comentada.sql
└── proteomasPlain_poblada.sql

pom.xml                               # Configuración Maven
.env                                  # Variables de entorno
```

---

## Notas técnicas

- **AAontology**: el proyecto incluye una escala representativa por cada una de las 8 categorías. Para usar las 586 escalas completas, solo hay que extender `EscalasRegistry`.

- **Modelo LLM**: el prompt está optimizado para Llama 3.1 (8B Instruct), pero se puede cambiar en `.env` a cualquier modelo compatible con el endpoint de Chat Completions de Hugging Face, se recomienda hacer testeos con Qwen/Qwen2.5-7B-Instruct-1M, deepseek-ai/DeepSeek-R1 y Qwen/Qwen3-4B-Thinking-2507.

- **Seguridad**: las contraseñas se almacenan como hash SHA‑256.

- **Rendimiento**: el orquestador procesa las secuencias en serie; para grandes volúmenes se podría paralelizar (por ejemplo, con un pool de hilos).

---

## Autores

- **Néstor Nuñez Díaz** – [@nestor051214](https://github.com/nestor051214)

- **Adriana Isabel Acosta González** – [@acostaAdriana-cyber](https://github.com/acostaAdriana-cyber)

Trabajo de curso – Sistemas de Bases de Datos II  
CII - UCLV / IBP
