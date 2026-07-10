package cu.uclv.proteomas.dao;

import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.TrabajoAnalisis;

import java.sql.SQLException;
import java.util.List;

public interface TrabajoAnalisisDao extends Dao<TrabajoAnalisis, Integer> {

    List<TrabajoAnalisis> listarEnCola() throws SQLException;

    /**
     * Marca el trabajo como 'ejecutando'. Puede lanzar SQLException si
     * trg_trabajo_unico_ejecutando detecta que ya hay otro trabajo
     * ejecutando para el mismo proteoma (regla de negocio de la BD).
     */
    void marcarEjecutando(int idTrabajo) throws SQLException;

    void marcarCompletado(int idTrabajo) throws SQLException;

    void marcarFallido(int idTrabajo, String mensajeError) throws SQLException;
}
