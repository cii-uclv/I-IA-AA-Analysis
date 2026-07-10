package cu.uclv.proteomas.orquestador;

import cu.uclv.proteomas.aaontology.DescriptorCalculator;
import cu.uclv.proteomas.aaontology.DescriptorProteina;
import cu.uclv.proteomas.dao.*;
import cu.uclv.proteomas.dao.impl.*;
import cu.uclv.proteomas.llm.ClasificacionParser;
import cu.uclv.proteomas.llm.HuggingFaceClient;
import cu.uclv.proteomas.llm.PromptBuilder;
import cu.uclv.proteomas.model.*;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Orquesta el flujo hibrido completo:
 *   1) lee proteomas 'pendiente' desde la BD, filtrados según el rol del usuario operador
 *   2) por cada proteoma crea/usa un trabajo_analisis y lo marca 'ejecutando'
 *      (la BD, via trigger, impide dos trabajos ejecutando a la vez sobre el mismo proteoma)
 *   3) por cada secuencia calcula descriptores AAontology
 *   4) construye el prompt y llama al LLM
 *   5) parsea la clasificacion y la persiste en 'respuesta'
 *   6) marca el trabajo como completado/fallido y el proteoma como completado/fallido
 *
 * Cada paso respeta las reglas de negocio impuestas por la BD; el orquestador
 * no las duplica, simplemente reacciona a las excepciones que la BD lanza.
 */
public class OrquestadorAnalisis {

    private final ProteomaDao proteomaDao = new ProteomaDaoImpl();
    private final SecuenciaDao secuenciaDao = new SecuenciaDaoImpl();
    private final TrabajoAnalisisDao trabajoDao = new TrabajoAnalisisDaoImpl();
    private final RespuestaDao respuestaDao = new RespuestaDaoImpl();
    private final ConfiguracionModeloDao modeloDao = new ConfiguracionModeloDaoImpl();

    private final DescriptorCalculator descriptorCalculator = new DescriptorCalculator();
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final ClasificacionParser parser = new ClasificacionParser();
    private final HuggingFaceClient huggingFaceClient = new HuggingFaceClient();

    /**
     * Procesa TODOS los proteomas pendientes a los que el operador tiene acceso.
     * @param operador Usuario que ejecuta la operación (investigador o administrador)
     * @param log callback para reportar progreso a la UI
     */
    public void procesarPendientes(Usuario operador, Consumer<String> log) {
        List<Proteoma> pendientes;
        try {
            if (operador.getTipoUsuario() == Enums.TipoUsuario.administrador) {
                // Administrador: ve todos los proteomas pendientes
                pendientes = proteomaDao.listarPendientes();
            } else {
                // Investigador: solo los proteomas que él mismo creó y que están pendientes
                pendientes = proteomaDao.listarPorUsuario(operador.getIdUsuario()).stream()
                        .filter(p -> p.getEstado() == Enums.EstadoProteoma.pendiente)
                        .collect(Collectors.toList());
            }
        } catch (SQLException e) {
            log.accept("ERROR al listar proteomas pendientes: " + e.getMessage());
            return;
        }

        if (pendientes.isEmpty()) {
            log.accept("No hay proteomas en estado 'pendiente' para este usuario.");
            return;
        }

        for (Proteoma p : pendientes) {
            procesarProteoma(p, operador, log);
        }
    }

    public void procesarProteoma(Proteoma proteoma, Usuario operador, Consumer<String> log) {
        // --- VALIDACIÓN DE PERMISOS ---
        if (operador.getTipoUsuario() != Enums.TipoUsuario.administrador &&
                proteoma.getIdUsuarioPublica() != operador.getIdUsuario()) {
            log.accept("  Usuario " + operador.getNombreUsuario() +
                    " no tiene permiso para procesar el proteoma #" + proteoma.getIdProteoma());
            return;
        }

        log.accept("== Proteoma #" + proteoma.getIdProteoma() + " (" + proteoma.getNombreProyecto() + ") ==");

        TrabajoAnalisis trabajo = new TrabajoAnalisis();
        trabajo.setTipo(Enums.TipoAnalisis.analisis_completo);
        trabajo.setIdProteoma(proteoma.getIdProteoma());
        trabajo.setIdUsuario(operador.getIdUsuario());

        try {
            Integer idModelo = obtenerOCrearModelo();
            trabajo.setIdModelo(idModelo);
            trabajoDao.crear(trabajo);
            trabajoDao.marcarEjecutando(trabajo.getIdTrabajo());

            // Cambio de estado a 'procesando' + trazabilidad
            proteomaDao.actualizarEstado(proteoma.getIdProteoma(), Enums.EstadoProteoma.procesando);
            proteomaDao.registrarModificacion(proteoma.getIdProteoma(), operador.getIdUsuario());

        } catch (SQLException e) {
            log.accept("  No se pudo iniciar el trabajo: " + e.getMessage());
            return;
        }

        try {
            List<Secuencia> secuencias = secuenciaDao.listarPorProteoma(proteoma.getIdProteoma());
            log.accept("  " + secuencias.size() + " secuencia(s) a clasificar.");

            for (Secuencia s : secuencias) {
                clasificarSecuencia(trabajo, s, log);
            }

            trabajoDao.marcarCompletado(trabajo.getIdTrabajo());
            // Cambio de estado a 'completado' + trazabilidad
            proteomaDao.actualizarEstado(proteoma.getIdProteoma(), Enums.EstadoProteoma.completado);
            proteomaDao.registrarModificacion(proteoma.getIdProteoma(), operador.getIdUsuario());

            log.accept("  Proteoma #" + proteoma.getIdProteoma() + " completado.");

        } catch (Exception e) {
            log.accept("  ERROR durante el analisis: " + e.getMessage());
            e.printStackTrace();
            try {
                trabajoDao.marcarFallido(trabajo.getIdTrabajo(), acortar(e.getMessage()));
                // Cambio de estado a 'fallido' + trazabilidad
                proteomaDao.actualizarEstado(proteoma.getIdProteoma(), Enums.EstadoProteoma.fallido);
                proteomaDao.registrarModificacion(proteoma.getIdProteoma(), operador.getIdUsuario());
            } catch (SQLException inner) {
                log.accept("  ERROR adicional al marcar fallo: " + inner.getMessage());
            }
        }
    }

    private void clasificarSecuencia(TrabajoAnalisis trabajo, Secuencia s, Consumer<String> log) throws Exception {
        DescriptorProteina descriptor = descriptorCalculator.calcular(s.getId(), s.getSecuenciaAa());

        String textoLlm = huggingFaceClient.generarRazonamiento(
                promptBuilder.promptSistema(),
                promptBuilder.promptUsuario(s, descriptor));

        Respuesta r = parser.parsear(textoLlm);
        r.setIdTrabajo(trabajo.getIdTrabajo());
        r.setIdProteoma(s.getIdProteoma());
        r.setIdSecuencia(s.getId());

        respuestaDao.crear(r);
        log.accept("    Secuencia " + s.getId() + " -> es_enzima=" + r.isEsEnzima()
                + " familia=" + r.getFamiliaSugerida() + " (conf=" + r.getConfianzaFamilia() + ")");
    }

    /** Se asegura de que exista una fila en configuracion_modelo para nuestro LLM, y devuelve su id. */
    private Integer obtenerOCrearModelo() throws SQLException {
        ConfiguracionModelo existente = modeloDao.buscarPorNombre("Llama-3.1-8B-Instruct");
        if (existente != null) return existente.getIdModelo();

        ConfiguracionModelo m = new ConfiguracionModelo();
        m.setNombre("Llama-3.1-8B-Instruct");
        m.setDescripcion("Clasificador basado en descriptores AAontology + razonamiento generado por Llama");
        m.setMetadatosJson("{\"proveedor\":\"Llama\",\"tipo\":\"LLM\"}");
        modeloDao.crear(m);
        return m.getIdModelo();
    }

    private String acortar(String msg) {
        if (msg == null) return null;
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}