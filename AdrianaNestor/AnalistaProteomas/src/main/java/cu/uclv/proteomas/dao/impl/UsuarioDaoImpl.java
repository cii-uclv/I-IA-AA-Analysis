package cu.uclv.proteomas.dao.impl;

import cu.uclv.proteomas.dao.UsuarioDao;
import cu.uclv.proteomas.db.DatabaseManager;
import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDaoImpl implements UsuarioDao {

    @Override
    public Usuario crearInvestigador(Usuario u, String ramaInvestigacion) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                insertarUsuarioBase(conn, u);
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO investigador (id_usuario, rama_investigacion) VALUES (?, ?)")) {
                    ps.setInt(1, u.getIdUsuario());
                    ps.setString(2, ramaInvestigacion);
                    ps.executeUpdate();
                }
                u.setTipoUsuario(Enums.TipoUsuario.investigador);
                conn.commit();
                return u;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @Override
    public Usuario crearAdministrador(Usuario u, String rol) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                insertarUsuarioBase(conn, u);
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO administrador (id_usuario, rol) VALUES (?, ?)")) {
                    ps.setInt(1, u.getIdUsuario());
                    ps.setString(2, rol);
                    ps.executeUpdate();
                }
                u.setTipoUsuario(Enums.TipoUsuario.administrador);
                conn.commit();
                return u;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    @Override
    public Usuario autenticar(String nombreUsuario, String hashContrasena) throws SQLException {
        String selectSql = "SELECT * FROM usuario WHERE nombre_usuario = ? AND hash_contrasena = ? AND activo = true";
        String updateSql = "UPDATE usuario SET ultimo_acceso = CURRENT_TIMESTAMP WHERE id_usuario = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Iniciar transacción manual

            Usuario usuario = null;
            try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                psSelect.setString(1, nombreUsuario);
                psSelect.setString(2, hashContrasena);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        usuario = map(rs);
                    }
                }
            }

            if (usuario != null) {
                // Actualizar último acceso
                try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                    psUpdate.setInt(1, usuario.getIdUsuario());
                    int filasActualizadas = psUpdate.executeUpdate();
                    if (filasActualizadas == 0) {
                        // No debería ocurrir, pero por si acaso se lanza una excepción
                        throw new SQLException("No se pudo actualizar el último acceso para el usuario ID " + usuario.getIdUsuario());
                    }
                }
                conn.commit(); // Confirmar la transacción
            } else {
                conn.rollback();
            }
            return usuario;
        } catch (SQLException e) {
            // En caso de error, hacer rollback y relanzar
            throw e;
        }
    }

    /**
     * Inserta la fila base en 'usuario'. El valor de tipo_usuario que se manda
     * aqui es provisional: los triggers trg_validar_investigador /
     * trg_validar_administrador lo sobrescriben al insertar en la subtabla
     * correspondiente, pero la columna es NOT NULL asi que hay que mandar algo.
     */
    private void insertarUsuarioBase(Connection conn, Usuario u) throws SQLException {
        String sql = "INSERT INTO usuario (nombre_usuario, correo, hash_contrasena, institucion, activo, tipo_usuario) " +
                     "VALUES (?, ?, ?, ?, ?, 'investigador') RETURNING id_usuario, fecha_creacion";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNombreUsuario());
            ps.setString(2, u.getCorreo());
            ps.setString(3, u.getHashContrasena());
            ps.setString(4, u.getInstitucion());
            ps.setBoolean(5, u.isActivo());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                u.setIdUsuario(rs.getInt("id_usuario"));
                u.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
            }
        }
    }

    @Override
    public Usuario crear(Usuario entidad) throws SQLException {
        throw new UnsupportedOperationException(
            "Usa crearInvestigador(...) o crearAdministrador(...): la herencia exclusiva/total " +
            "del esquema exige indicar el subtipo al crear un usuario.");
    }

    @Override
    public Usuario buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM usuario WHERE id_usuario = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    @Override
    public List<Usuario> listarTodos() throws SQLException {
        List<Usuario> out = new ArrayList<>();
        String sql = "SELECT * FROM usuario ORDER BY id_usuario";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    @Override
    public List<Usuario> listarActivos() throws SQLException {
        List<Usuario> out = new ArrayList<>();
        String sql = "SELECT * FROM usuario WHERE activo = TRUE ORDER BY id_usuario";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        }
        return out;
    }

    @Override
    public void actualizar(Usuario u) throws SQLException {
        String sql = "UPDATE usuario SET nombre_usuario=?, correo=?, institucion=?, activo=?, ultimo_acceso=? " +
                     "WHERE id_usuario=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNombreUsuario());
            ps.setString(2, u.getCorreo());
            ps.setString(3, u.getInstitucion());
            ps.setBoolean(4, u.isActivo());
            ps.setTimestamp(5, u.getUltimoAcceso() == null ? null : Timestamp.valueOf(u.getUltimoAcceso()));
            ps.setInt(6, u.getIdUsuario());
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminar(Integer id) throws SQLException {
        // ON DELETE CASCADE se encarga de investigador/administrador y de las
        // filas dependientes (proteoma_modifica, trabajo_analisis via usuario, etc.)
        String sql = "DELETE FROM usuario WHERE id_usuario = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean esAdministrador(int idUsuario) throws SQLException {
        return existsIn(idUsuario, "administrador");
    }

    @Override
    public boolean esInvestigador(int idUsuario) throws SQLException {
        return existsIn(idUsuario, "investigador");
    }

    private boolean existsIn(int idUsuario, String tabla) throws SQLException {
        String sql = "SELECT 1 FROM " + tabla + " WHERE id_usuario = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Usuario map(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombreUsuario(rs.getString("nombre_usuario"));
        u.setCorreo(rs.getString("correo"));
        u.setHashContrasena(rs.getString("hash_contrasena"));
        u.setInstitucion(rs.getString("institucion"));
        Timestamp fc = rs.getTimestamp("fecha_creacion");
        if (fc != null) u.setFechaCreacion(fc.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("ultimo_acceso");
        if (ua != null) u.setUltimoAcceso(ua.toLocalDateTime());
        u.setActivo(rs.getBoolean("activo"));
        u.setTipoUsuario(Enums.TipoUsuario.valueOf(rs.getString("tipo_usuario")));
        return u;
    }
}
