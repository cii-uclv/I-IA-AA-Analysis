package cu.uclv.proteomas.dao;

import cu.uclv.proteomas.model.Secuencia;

import java.sql.SQLException;
import java.util.List;

public interface SecuenciaDao {

    /** Inserta una secuencia; 'longitud' la calcula el trigger, se ignora si viene seteada. */
    Secuencia crear(Secuencia s) throws SQLException;

    /** Insercion masiva eficiente (batch) para cargar un FASTA completo. */
    void crearLote(List<Secuencia> secuencias) throws SQLException;

    Secuencia buscarPorId(int idProteoma, long id) throws SQLException;

    List<Secuencia> listarPorProteoma(int idProteoma) throws SQLException;

    void actualizarSecuenciaAa(int idProteoma, long id, String nuevaSecuencia) throws SQLException;

    void eliminar(int idProteoma, long id) throws SQLException;
}
