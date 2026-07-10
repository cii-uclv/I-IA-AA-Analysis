package cu.uclv.proteomas.ui;

import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.Usuario;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final Usuario usuarioAutenticado;

    public MainFrame(Usuario usuarioAutenticado) {
        super("Orquestador de Clasificacion de Proteomas - AAontology + Llama");
        this.usuarioAutenticado = usuarioAutenticado;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        // Siempre visible
        tabs.addTab("Orquestador", new OrquestadorPanel(usuarioAutenticado));
        tabs.addTab("Proteomas", new ProteomaPanel(usuarioAutenticado));
        tabs.addTab("Secuencias", new SecuenciaPanel(usuarioAutenticado));
        tabs.addTab("Respuestas del LLM", new RespuestaPanel(usuarioAutenticado));

        // Solo administradores ven la pestaña de Usuarios
        if (usuarioAutenticado.getTipoUsuario() == Enums.TipoUsuario.administrador) {
            tabs.addTab("Usuarios", new UsuarioPanel(usuarioAutenticado));
        }

        add(tabs, BorderLayout.CENTER);
    }
}