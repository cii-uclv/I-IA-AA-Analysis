package cu.uclv.proteomas.dao;

import cu.uclv.proteomas.model.ConfiguracionModelo;

import java.sql.SQLException;
import java.util.List;

public interface ConfiguracionModeloDao extends Dao<ConfiguracionModelo, Integer> {
    ConfiguracionModelo buscarPorNombre(String nombre) throws SQLException;
}
