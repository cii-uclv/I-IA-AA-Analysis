package cu.uclv.proteomas.dao;

import java.sql.SQLException;
import java.util.List;

/** Contrato CRUD minimo comun a todos los DAO del sistema. */
public interface Dao<T, ID> {
    T crear(T entidad) throws SQLException;
    T buscarPorId(ID id) throws SQLException;
    List<T> listarTodos() throws SQLException;
    void actualizar(T entidad) throws SQLException;
    void eliminar(ID id) throws SQLException;
}
