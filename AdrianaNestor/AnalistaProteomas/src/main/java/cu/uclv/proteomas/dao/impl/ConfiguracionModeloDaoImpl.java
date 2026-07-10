package cu.uclv.proteomas.dao.impl;

import cu.uclv.proteomas.dao.ConfiguracionModeloDao;
import cu.uclv.proteomas.db.DatabaseManager;
import cu.uclv.proteomas.model.ConfiguracionModelo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConfiguracionModeloDaoImpl implements ConfiguracionModeloDao {

    @Override
    public ConfiguracionModelo crear(ConfiguracionModelo m) throws SQLException {
        String sql = "INSERT INTO configuracion_modelo (nombre, descripcion, metadatos) VALUES (?, ?, ?::jsonb) RETURNING id_modelo";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getNombre());
            ps.setString(2, m.getDescripcion());
            ps.setString(3, m.getMetadatosJson() == null ? "{}" : m.getMetadatosJson());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                m.setIdModelo(rs.getInt("id_modelo"));
            }
            return m;
        }
    }

    @Override
    public ConfiguracionModelo buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM configuracion_modelo WHERE id_modelo = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public ConfiguracionModelo buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT * FROM configuracion_modelo WHERE nombre = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public List<ConfiguracionModelo> listarTodos() throws SQLException {
        List<ConfiguracionModelo> out = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM configuracion_modelo ORDER BY id_modelo")) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    @Override
    public void actualizar(ConfiguracionModelo m) throws SQLException {
        String sql = "UPDATE configuracion_modelo SET nombre=?, descripcion=?, metadatos=?::jsonb WHERE id_modelo=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getNombre());
            ps.setString(2, m.getDescripcion());
            ps.setString(3, m.getMetadatosJson() == null ? "{}" : m.getMetadatosJson());
            ps.setInt(4, m.getIdModelo());
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminar(Integer id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM configuracion_modelo WHERE id_modelo = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private ConfiguracionModelo map(ResultSet rs) throws SQLException {
        ConfiguracionModelo m = new ConfiguracionModelo();
        m.setIdModelo(rs.getInt("id_modelo"));
        m.setNombre(rs.getString("nombre"));
        m.setDescripcion(rs.getString("descripcion"));
        m.setMetadatosJson(rs.getString("metadatos"));
        return m;
    }
}
