package cu.uclv.proteomas.dao;

import cu.uclv.proteomas.model.Usuario;

import java.sql.SQLException;
import java.util.List;

/**
 * La creacion de un usuario SIEMPRE debe ir acompañada de su subtipo
 * (investigador o administrador), porque la herencia en el esquema es
 * exclusiva y total: fn_validar_tipo_investigador / fn_validar_tipo_administrador
 * son las que realmente fijan usuario.tipo_usuario, y ambas rechazan que
 * un mismo id_usuario aparezca en las dos subtablas.
 */
public interface UsuarioDao extends Dao<Usuario, Integer> {

    /** Inserta en usuario + investigador dentro de una misma transaccion. */
    Usuario crearInvestigador(Usuario usuario, String ramaInvestigacion) throws SQLException;

    /** Inserta en usuario + administrador dentro de una misma transaccion. */
    Usuario crearAdministrador(Usuario usuario, String rol) throws SQLException;

    boolean esAdministrador(int idUsuario) throws SQLException;

    boolean esInvestigador(int idUsuario) throws SQLException;

    List<Usuario> listarActivos() throws SQLException;

    Usuario autenticar(String nombreUsuario, String hashContrasena) throws SQLException;
}
