package cu.uclv.proteomas.dao.impl;

import cu.uclv.proteomas.dao.ProteomaDao;
import cu.uclv.proteomas.db.DatabaseManager;
import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.Proteoma;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProteomaDaoImpl implements ProteomaDao {

    @Override
    public Proteoma crear(Proteoma p) throws SQLException {
        String sql = "INSERT INTO proteoma (nombre_proyecto, especie, cepa, ruta_archivo, md5_fasta, " +
                "estado, visible_otros, id_usuario_publica) " +
                "VALUES (?, ?, ?, ?, ?, ?::estado_proteoma_t, ?, ?) " +
                "RETURNING id_proteoma, fecha_subida, total_secuencias";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getNombreProyecto());
            ps.setString(2, p.getEspecie());
            ps.setString(3, p.getCepa());
            ps.setString(4, p.getRutaArchivo());
            ps.setString(5, p.getMd5Fasta());
            ps.setString(6, p.getEstado().name());  // el cast convierte el texto al ENUM
            ps.setBoolean(7, p.isVisibleOtros());
            ps.setInt(8, p.getIdUsuarioPublica());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                p.setIdProteoma(rs.getInt("id_proteoma"));
                p.setFechaSubida(rs.getTimestamp("fecha_subida").toLocalDateTime());
                p.setTotalSecuencias(rs.getInt("total_secuencias"));
            }
            return p;
        }
    }

    @Override
    public Proteoma buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM proteoma WHERE id_proteoma = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public List<Proteoma> listarTodos() throws SQLException {
        return query("SELECT * FROM proteoma ORDER BY id_proteoma");
    }

    @Override
    public List<Proteoma> listarPorUsuario(int idUsuario) throws SQLException {
        String sql = "SELECT * FROM proteoma WHERE id_usuario_publica = ? ORDER BY id_proteoma";
        List<Proteoma> out = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    @Override
    public List<Proteoma> listarPendientes() throws SQLException {
        return listarPorEstado(Enums.EstadoProteoma.pendiente);
    }

    @Override
    public List<Proteoma> listarPorEstado(Enums.EstadoProteoma estado) throws SQLException {
        List<Proteoma> out = new ArrayList<>();
        String sql = "SELECT * FROM proteoma WHERE estado = ?::estado_proteoma_t ORDER BY fecha_subida";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    private List<Proteoma> query(String sql) throws SQLException {
        List<Proteoma> out = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    @Override
    public void actualizar(Proteoma p) throws SQLException {
        // También aquí se añade el cast para el estado
        String sql = "UPDATE proteoma SET nombre_proyecto=?, especie=?, cepa=?, " +
                "visible_otros=?, estado=?::estado_proteoma_t " +
                "WHERE id_proteoma=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getNombreProyecto());
            ps.setString(2, p.getEspecie());
            ps.setString(3, p.getCepa());
            ps.setBoolean(4, p.isVisibleOtros());
            ps.setString(5, p.getEstado().name());
            ps.setInt(6, p.getIdProteoma());
            ps.executeUpdate();
        }
    }

    @Override
    public void actualizarEstado(int idProteoma, Enums.EstadoProteoma estado) throws SQLException {
        String sql = "UPDATE proteoma SET estado = ?::estado_proteoma_t WHERE id_proteoma = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, idProteoma);
            ps.executeUpdate();
        }
    }

    @Override
    public void registrarModificacion(int idProteoma, int idUsuario) throws SQLException {
        String sql = "INSERT INTO proteoma_modifica (id_usuario, id_proteoma) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idProteoma);
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminarConTrazabilidad(int idProteoma, int idAdministrador) throws SQLException {
        String sql = "CALL sp_eliminar_proteoma(?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            cs.setInt(1, idProteoma);
            cs.setInt(2, idAdministrador);
            cs.execute();
        }
    }

    @Override
    public void eliminar(Integer id) throws SQLException {
        throw new UnsupportedOperationException(
                "Usa eliminarConTrazabilidad(idProteoma, idAdministrador): el esquema exige " +
                        "trazabilidad y restringe el borrado de proteomas a administradores.");
    }

    private Proteoma map(ResultSet rs) throws SQLException {
        Proteoma p = new Proteoma();
        p.setIdProteoma(rs.getInt("id_proteoma"));
        p.setNombreProyecto(rs.getString("nombre_proyecto"));
        p.setEspecie(rs.getString("especie"));
        p.setCepa(rs.getString("cepa"));
        p.setRutaArchivo(rs.getString("ruta_archivo"));
        p.setMd5Fasta(rs.getString("md5_fasta"));
        p.setFechaSubida(rs.getTimestamp("fecha_subida").toLocalDateTime());
        p.setEstado(Enums.EstadoProteoma.valueOf(rs.getString("estado")));
        p.setTotalSecuencias(rs.getInt("total_secuencias"));
        p.setVisibleOtros(rs.getBoolean("visible_otros"));
        p.setIdUsuarioPublica(rs.getInt("id_usuario_publica"));
        return p;
    }
}