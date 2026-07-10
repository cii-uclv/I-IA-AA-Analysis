package cu.uclv.proteomas.ui;

import cu.uclv.proteomas.dao.UsuarioDao;
import cu.uclv.proteomas.dao.impl.UsuarioDaoImpl;
import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.security.MessageDigest;
import java.util.List;

public class UsuarioPanel extends JPanel {

    private final UsuarioDao usuarioDao = new UsuarioDaoImpl();
    private final Usuario usuario;

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Usuario", "Correo", "Institucion", "Tipo", "Activo"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    private final JTextField txtNombreUsuario = new JTextField(16);
    private final JTextField txtCorreo = new JTextField(16);
    private final JPasswordField txtPassword = new JPasswordField(16);
    private final JTextField txtInstitucion = new JTextField(16);
    private final JTextField txtRamaORol = new JTextField(16);
    private final JComboBox<String> comboTipo = new JComboBox<>(new String[]{"investigador", "administrador"});

    public UsuarioPanel(Usuario usuario) {
        this.usuario = usuario;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Si no es administrador, mostrar mensaje de acceso denegado
        if (usuario.getTipoUsuario() != Enums.TipoUsuario.administrador) {
            JLabel label = new JLabel("Acceso restringido: Solo administradores pueden ver la gestión de usuarios.",
                    SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 16));
            add(label, BorderLayout.CENTER);
            return;
        }

        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Nuevo usuario"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;
        addRow(form, c, row++, "Nombre de usuario:", txtNombreUsuario);
        addRow(form, c, row++, "Correo:", txtCorreo);
        addRow(form, c, row++, "Contraseña:", txtPassword);
        addRow(form, c, row++, "Institución:", txtInstitucion);
        addRow(form, c, row++, "Tipo:", comboTipo);
        addRow(form, c, row++, "Rama investigación / Rol:", txtRamaORol);

        JButton btnCrear = new JButton("Crear usuario");
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(btnCrear, c);

        add(form, BorderLayout.SOUTH);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefrescar = new JButton("Refrescar");
        topBar.add(btnRefrescar);
        add(topBar, BorderLayout.NORTH);

        btnRefrescar.addActionListener(e -> refrescar());
        btnCrear.addActionListener(e -> crear());

        refrescar();
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        form.add(field, c);
    }

    private void refrescar() {
        try {
            modelo.setRowCount(0);
            List<Usuario> usuarios = usuarioDao.listarTodos();
            for (Usuario u : usuarios) {
                modelo.addRow(new Object[]{u.getIdUsuario(), u.getNombreUsuario(), u.getCorreo(),
                        u.getInstitucion(), u.getTipoUsuario(), u.isActivo() ? "Si" : "No"});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al refrescar: " + ex.getMessage());
        }
    }

    private void crear() {
        if (txtNombreUsuario.getText().isBlank() || txtCorreo.getText().isBlank() || txtPassword.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "Completa usuario, correo y contraseña.");
            return;
        }
        try {
            Usuario u = new Usuario();
            u.setNombreUsuario(txtNombreUsuario.getText().trim());
            u.setCorreo(txtCorreo.getText().trim());
            u.setHashContrasena(sha256(new String(txtPassword.getPassword())));
            u.setInstitucion(txtInstitucion.getText().isBlank() ? null : txtInstitucion.getText().trim());

            String tipo = (String) comboTipo.getSelectedItem();
            if ("investigador".equals(tipo)) {
                usuarioDao.crearInvestigador(u, txtRamaORol.getText().isBlank() ? null : txtRamaORol.getText().trim());
            } else {
                usuarioDao.crearAdministrador(u, txtRamaORol.getText().isBlank() ? null : txtRamaORol.getText().trim());
            }

            JOptionPane.showMessageDialog(this, "Usuario #" + u.getIdUsuario() + " creado como " + tipo + ".");
            txtNombreUsuario.setText(""); txtCorreo.setText(""); txtPassword.setText("");
            txtInstitucion.setText(""); txtRamaORol.setText("");
            refrescar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al crear usuario: " + ex.getMessage());
        }
    }

    private String sha256(String texto) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(texto.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}