package cu.uclv.proteomas.db;

import cu.uclv.proteomas.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Permite la conexion JDBC a PostgreSQL.
 */
public final class DatabaseManager {

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("No se encontro el driver de PostgreSQL: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(
                AppConfig.dbUrl(), AppConfig.dbUser(), AppConfig.dbPassword());
        return conn;
    }

    private DatabaseManager() {}
}
