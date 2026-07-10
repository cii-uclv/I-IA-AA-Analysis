package cu.uclv.proteomas.dao.impl;

import cu.uclv.proteomas.dao.SecuenciaDao;
import cu.uclv.proteomas.db.DatabaseManager;
import cu.uclv.proteomas.model.Secuencia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SecuenciaDaoImpl implements SecuenciaDao {

    @Override
    public Secuencia crear(Secuencia s) throws SQLException {
        // No enviamos 'longitud': lo calcula trg_calcular_longitud BEFORE INSERT.
        // El contador proteoma.total_secuencias tambien se actualiza solo (trigger AFTER INSERT).
        String sql = "INSERT INTO secuencia (id_proteoma, secuencia_aa) VALUES (?, ?) RETURNING id, longitud";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, s.getIdProteoma());
            ps.setString(2, s.getSecuenciaAa());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                s.setId(rs.getLong("id"));
                s.setLongitud(rs.getInt("longitud"));
            }
            return s;
        }
    }

    @Override
    public void crearLote(List<Secuencia> secuencias) throws SQLException {
        String sql = "INSERT INTO secuencia (id_proteoma, secuencia_aa) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int count = 0;
                for (Secuencia s : secuencias) {
                    ps.setInt(1, s.getIdProteoma());
                    ps.setString(2, s.getSecuenciaAa());
                    ps.addBatch();
                    if (++count % 500 == 0) ps.executeBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @Override
    public Secuencia buscarPorId(int idProteoma, long id) throws SQLException {
        String sql = "SELECT * FROM secuencia WHERE id_proteoma = ? AND id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProteoma);
            ps.setLong(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public List<Secuencia> listarPorProteoma(int idProteoma) throws SQLException {
        List<Secuencia> out = new ArrayList<>();
        String sql = "SELECT * FROM secuencia WHERE id_proteoma = ? ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProteoma);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    @Override
    public void actualizarSecuenciaAa(int idProteoma, long id, String nuevaSecuencia) throws SQLException {
        // Al actualizar secuencia_aa, trg_calcular_longitud recalcula 'longitud' automaticamente.
        String sql = "UPDATE secuencia SET secuencia_aa = ? WHERE id_proteoma = ? AND id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevaSecuencia);
            ps.setInt(2, idProteoma);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminar(int idProteoma, long id) throws SQLException {
        String sql = "DELETE FROM secuencia WHERE id_proteoma = ? AND id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProteoma);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private Secuencia map(ResultSet rs) throws SQLException {
        Secuencia s = new Secuencia();
        s.setIdProteoma(rs.getInt("id_proteoma"));
        s.setId(rs.getLong("id"));
        s.setLongitud((Integer) rs.getObject("longitud"));
        s.setSecuenciaAa(rs.getString("secuencia_aa"));
        return s;
    }
}
