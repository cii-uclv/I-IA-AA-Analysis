package cu.uclv.proteomas.ui;

import cu.uclv.proteomas.dao.UsuarioDao;
import cu.uclv.proteomas.dao.impl.UsuarioDaoImpl;
import cu.uclv.proteomas.model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.security.MessageDigest;

public class LoginDialog extends JDialog {
    private final JTextField txtUsuario = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JButton btnLogin = new JButton("Iniciar sesión");
    private final JButton btnCancel = new JButton("Cancelar");
    private Usuario usuarioAutenticado = null;
    private boolean success = false;

    public LoginDialog(Frame parent) {
        super(parent, "Inicio de sesión", true);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Etiquetas y campos
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 1;
        add(txtUsuario, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1;
        add(txtPassword, gbc);

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelBotones.add(btnLogin);
        panelBotones.add(btnCancel);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        add(panelBotones, gbc);

        // Acciones
        btnLogin.addActionListener(e -> autenticar());
        btnCancel.addActionListener(e -> {
            success = false;
            dispose();
        });
        getRootPane().setDefaultButton(btnLogin);

        setSize(350, 200);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            }
        });
    }

    private void autenticar() {
        String nombreUsuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword());
        if (nombreUsuario.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese usuario y contraseña.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            String hash = sha256(password);
            UsuarioDao dao = new UsuarioDaoImpl();
            Usuario u = dao.autenticar(nombreUsuario, hash);
            if (u != null) {
                usuarioAutenticado = u;
                success = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al autenticar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String sha256(String texto) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(texto.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }

    public boolean isSuccess() {
        return success;
    }
}