package cu.uclv.proteomas.model;

/**
 * Espejo en Java de los tipos ENUM definidos en proteomasBD_comentada.sql.
 * Se mantienen como enums anidados en una sola clase por simplicidad,
 * pero cada uno corresponde 1:1 con su tipo homonimo en PostgreSQL.
 */
public class Enums {

    /** estado_proteoma_t */
    public enum EstadoProteoma {
        pendiente, procesando, completado, fallido
    }

    /** tipo_analisis_t */
    public enum TipoAnalisis {
        analisis_completo, reclasificar
    }

    /** estado_analisis_t */
    public enum EstadoAnalisis {
        en_cola, ejecutando, completado, fallido
    }

    /** familia_t */
    public enum Familia {
        GH18, GH19, AMP, otra
    }

    /** tipo_usuario (CHECK sobre VARCHAR, no ENUM real, pero se modela igual) */
    public enum TipoUsuario {
        investigador, administrador
    }

    private Enums() {}
}
