package cu.uclv.proteomas.dao.impl;

import cu.uclv.proteomas.dao.TrabajoAnalisisDao;
import cu.uclv.proteomas.db.DatabaseManager;
import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.TrabajoAnalisis;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrabajoAnalisisDaoImpl implements TrabajoAnalisisDao {

    @Override
    public TrabajoAnalisis crear(TrabajoAnalisis t) throws SQLException {
        String sql = "INSERT INTO trabajo_analisis (tipo, estado, id_proteoma, id_usuario, id_modelo) " +
                     "VALUES (?::tipo_analisis_t, 'en_cola', ?, ?, ?) RETURNING id_trabajo";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getTipo().name());
            ps.setInt(2, t.getIdProteoma());
            ps.setInt(3, t.getIdUsuario());
            ps.setInt(4, t.getIdModelo());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                t.setIdTrabajo(rs.getInt("id_trabajo"));
            }
            t.setEstado(Enums.EstadoAnalisis.en_cola);
            return t;
        }
    }

    @Override
    public TrabajoAnalisis buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM trabajo_analisis WHERE id_trabajo = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public List<TrabajoAnalisis> listarTodos() throws SQLException {
        return query("SELECT * FROM trabajo_analisis ORDER BY id_trabajo DESC");
    }

    @Override
    public List<TrabajoAnalisis> listarEnCola() throws SQLException {
        return query("SELECT * FROM trabajo_analisis WHERE estado = 'en_cola' ORDER BY id_trabajo");
    }

    private List<TrabajoAnalisis> query(String sql) throws SQLException {
        List<TrabajoAnalisis> out = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    @Override
    public void actualizar(TrabajoAnalisis t) throws SQLException {
        String sql = "UPDATE trabajo_analisis SET tipo=?::tipo_analisis_t, estado=?::estado_analisis_t, " +
                     "fecha_inicio=?, fecha_fin=?, mensaje_error=? WHERE id_trabajo=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getTipo().name());
            ps.setString(2, t.getEstado().name());
            ps.setTimestamp(3, t.getFechaInicio() == null ? null : Timestamp.valueOf(t.getFechaInicio()));
            ps.setTimestamp(4, t.getFechaFin() == null ? null : Timestamp.valueOf(t.getFechaFin()));
            ps.setString(5, t.getMensajeError());
            ps.setInt(6, t.getIdTrabajo());
            ps.executeUpdate();
        }
    }

    @Override
    public void marcarEjecutando(int idTrabajo) throws SQLException {
        // Si ya hay otro trabajo 'ejecutando' para el mismo proteoma,
        // trg_trabajo_unico_ejecutando lanza una excepcion que se propaga tal cual.
        String sql = "UPDATE trabajo_analisis SET estado='ejecutando', fecha_inicio=CURRENT_TIMESTAMP WHERE id_trabajo=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTrabajo);
            ps.executeUpdate();
        }
    }

    @Override
    public void marcarCompletado(int idTrabajo) throws SQLException {
        String sql = "UPDATE trabajo_analisis SET estado='completado', fecha_fin=CURRENT_TIMESTAMP WHERE id_trabajo=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTrabajo);
            ps.executeUpdate();
        }
    }

    @Override
    public void marcarFallido(int idTrabajo, String mensajeError) throws SQLException {
        String sql = "UPDATE trabajo_analisis SET estado='fallido', fecha_fin=CURRENT_TIMESTAMP, mensaje_error=? WHERE id_trabajo=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mensajeError);
            ps.setInt(2, idTrabajo);
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM trabajo_analisis WHERE id_trabajo = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * mapeador privado (típico en DAOs) cuya función es transformar una fila del
     * resultado de una consulta SQL (ResultSet) en un objeto Java de tipo TrabajoAnalisis
     */
    private TrabajoAnalisis map(ResultSet rs) throws SQLException {
        TrabajoAnalisis t = new TrabajoAnalisis();
        t.setIdTrabajo(rs.getInt("id_trabajo"));
        t.setTipo(Enums.TipoAnalisis.valueOf(rs.getString("tipo")));
        t.setEstado(Enums.EstadoAnalisis.valueOf(rs.getString("estado")));
        Timestamp fi = rs.getTimestamp("fecha_inicio");
        if (fi != null) t.setFechaInicio(fi.toLocalDateTime());
        Timestamp ff = rs.getTimestamp("fecha_fin");
        if (ff != null) t.setFechaFin(ff.toLocalDateTime());
        t.setMensajeError(rs.getString("mensaje_error"));
        t.setIdProteoma(rs.getInt("id_proteoma"));
        t.setIdUsuario(rs.getInt("id_usuario"));
        t.setIdModelo(rs.getInt("id_modelo"));
        return t;
    }
}
