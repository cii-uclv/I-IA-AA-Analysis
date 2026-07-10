package cu.uclv.proteomas.dao;

import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.Proteoma;

import java.sql.SQLException;
import java.util.List;

public interface ProteomaDao extends Dao<Proteoma, Integer> {

    /** Proteomas en estado 'pendiente', los que el orquestador debe procesar. */
    List<Proteoma> listarPendientes() throws SQLException;

    List<Proteoma> listarPorEstado(Enums.EstadoProteoma estado) throws SQLException;

    /** Registra en proteoma_modifica la fecha/usuario que modifico el proteoma. */
    void registrarModificacion(int idProteoma, int idUsuario) throws SQLException;

    /**
     * Elimina un proteoma dejando trazabilidad, llamando al procedimiento
     * sp_eliminar_proteoma(p_id_proteoma, p_id_admin) definido en la BD.
     * Solo un administrador puede ejecutar esta operacion (lo valida el
     * propio procedimiento almacenado).
     */
    void eliminarConTrazabilidad(int idProteoma, int idAdministrador) throws SQLException;

    void actualizarEstado(int idProteoma, Enums.EstadoProteoma estado) throws SQLException;

    List<Proteoma> listarPorUsuario(int idUsuario) throws SQLException;

}
