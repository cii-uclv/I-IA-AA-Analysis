package cu.uclv.proteomas.dao.impl;

import cu.uclv.proteomas.dao.RespuestaDao;
import cu.uclv.proteomas.db.DatabaseManager;
import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.Respuesta;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RespuestaDaoImpl implements RespuestaDao {

    @Override
    public Respuesta crear(Respuesta r) throws SQLException {
        // El CHECK chk_ec_formato y el dominio ec_number validan numero_ec en la propia BD;
        // aqui no se repite esa validacion, se deja que la BD sea la fuente de verdad.
        String sql = "INSERT INTO respuesta (id_trabajo, id_proteoma, id_secuencia, es_enzima, confianza_enzima, " +
                     "es_hidrolasa, familia_sugerida, confianza_familia, numero_ec, razonamiento) " +
                     "VALUES (?,?,?,?,?,?,?::familia_t,?,?,?) RETURNING fecha";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getIdTrabajo());
            ps.setInt(2, r.getIdProteoma());
            ps.setLong(3, r.getIdSecuencia());
            ps.setBoolean(4, r.isEsEnzima());
            ps.setFloat(5, r.getConfianzaEnzima());
            setNullableBoolean(ps, 6, r.getEsHidrolasa());
            ps.setString(7, r.getFamiliaSugerida() == null ? null : r.getFamiliaSugerida().name());
            if (r.getConfianzaFamilia() == null) ps.setNull(8, Types.REAL); else ps.setFloat(8, r.getConfianzaFamilia());
            ps.setString(9, r.getNumeroEc());
            ps.setString(10, r.getRazonamiento());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                r.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
            }
            return r;
        }
    }

    private void setNullableBoolean(PreparedStatement ps, int idx, Boolean value) throws SQLException {
        if (value == null) ps.setNull(idx, Types.BOOLEAN); else ps.setBoolean(idx, value);
    }

    @Override
    public List<Respuesta> listarPorTrabajo(int idTrabajo) throws SQLException {
        return query("SELECT * FROM respuesta WHERE id_trabajo = ? ORDER BY id_secuencia", idTrabajo);
    }

    @Override
    public List<Respuesta> listarPorProteoma(int idProteoma) throws SQLException {
        return query("SELECT * FROM respuesta WHERE id_proteoma = ? ORDER BY id_trabajo, id_secuencia", idProteoma);
    }

    private List<Respuesta> query(String sql, int param) throws SQLException {
        List<Respuesta> out = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    @Override
    public Respuesta buscar(int idTrabajo, long idSecuencia) throws SQLException {
        String sql = "SELECT * FROM respuesta WHERE id_trabajo = ? AND id_secuencia = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTrabajo);
            ps.setLong(2, idSecuencia);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private Respuesta map(ResultSet rs) throws SQLException {
        Respuesta r = new Respuesta();
        r.setIdTrabajo(rs.getInt("id_trabajo"));
        r.setIdProteoma(rs.getInt("id_proteoma"));
        r.setIdSecuencia(rs.getLong("id_secuencia"));
        r.setEsEnzima(rs.getBoolean("es_enzima"));
        r.setConfianzaEnzima(rs.getFloat("confianza_enzima"));
        Object hidrolasa = rs.getObject("es_hidrolasa");
        r.setEsHidrolasa(hidrolasa == null ? null : (Boolean) hidrolasa);
        String familia = rs.getString("familia_sugerida");
        r.setFamiliaSugerida(familia == null ? null : Enums.Familia.valueOf(familia));
        Object confFamilia = rs.getObject("confianza_familia");
        r.setConfianzaFamilia(confFamilia == null ? null : rs.getFloat("confianza_familia"));
        r.setNumeroEc(rs.getString("numero_ec"));
        r.setRazonamiento(rs.getString("razonamiento"));
        r.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
        return r;
    }
}
