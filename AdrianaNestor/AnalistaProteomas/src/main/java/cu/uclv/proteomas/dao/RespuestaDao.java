package cu.uclv.proteomas.dao;

import cu.uclv.proteomas.model.Respuesta;

import java.sql.SQLException;
import java.util.List;

public interface RespuestaDao {

    Respuesta crear(Respuesta r) throws SQLException;

    List<Respuesta> listarPorTrabajo(int idTrabajo) throws SQLException;

    List<Respuesta> listarPorProteoma(int idProteoma) throws SQLException;

    Respuesta buscar(int idTrabajo, long idSecuencia) throws SQLException;
}
