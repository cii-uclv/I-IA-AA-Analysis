package cu.uclv.proteomas.ui;

import cu.uclv.proteomas.dao.ProteomaDao;
import cu.uclv.proteomas.dao.UsuarioDao;
import cu.uclv.proteomas.dao.impl.ProteomaDaoImpl;
import cu.uclv.proteomas.dao.impl.UsuarioDaoImpl;
import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.Proteoma;
import cu.uclv.proteomas.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ProteomaPanel extends JPanel {

    private final ProteomaDao proteomaDao = new ProteomaDaoImpl();
    private final UsuarioDao usuarioDao = new UsuarioDaoImpl();
    private final Usuario usuarioAutenticado;

    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Proyecto", "Especie", "Cepa", "Estado", "Secuencias", "Publica"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    private final JTextField txtProyecto = new JTextField(18);
    private final JTextField txtEspecie = new JTextField(18);
    private final JTextField txtCepa = new JTextField(18);
    private final JTextField txtRuta = new JTextField(18);
    private final JTextField txtMd5 = new JTextField(18);
    private final JCheckBox chkVisible = new JCheckBox("Visible para otros investigadores");
    private final JButton btnEliminar = new JButton("Eliminar");

    public ProteomaPanel(Usuario usuarioAutenticado) {
        this.usuarioAutenticado = usuarioAutenticado;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Nuevo proteoma"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;
        addRow(form, c, row++, "Nombre proyecto:", txtProyecto);
        addRow(form, c, row++, "Especie:", txtEspecie);
        addRow(form, c, row++, "Cepa:", txtCepa);
        addRow(form, c, row++, "Ruta archivo FASTA:", txtRuta);
        addRow(form, c, row++, "MD5 del FASTA:", txtMd5);

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(chkVisible, c);
        row++;

        JButton btnCrear = new JButton("Crear proteoma (pendiente)");
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(btnCrear, c);
        row++;

        // Panel de eliminación (sin combo)
        JPanel eliminarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        eliminarPanel.setBorder(BorderFactory.createTitledBorder("Eliminar seleccionado (solo administradores)"));
        eliminarPanel.add(btnEliminar);
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(eliminarPanel, c);

        add(form, BorderLayout.SOUTH);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefrescar = new JButton("Refrescar");
        topBar.add(btnRefrescar);
        add(topBar, BorderLayout.NORTH);

        btnRefrescar.addActionListener(e -> refrescar());
        btnCrear.addActionListener(e -> crear());
        btnEliminar.addActionListener(e -> eliminar());

        // Si el usuario no es administrador, deshabilitar el botón de eliminar
        try {
            if (!usuarioDao.esAdministrador(usuarioAutenticado.getIdUsuario())) {
                btnEliminar.setEnabled(false);
            }
        } catch (Exception ex) {
            btnEliminar.setEnabled(false);
        }

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
            List<Proteoma> proteomas;
            if (usuarioDao.esAdministrador(usuarioAutenticado.getIdUsuario())) {
                proteomas = proteomaDao.listarTodos();
            } else {
                proteomas = proteomaDao.listarPorUsuario(usuarioAutenticado.getIdUsuario());
            }
            for (Proteoma p : proteomas) {
                modelo.addRow(new Object[]{p.getIdProteoma(), p.getNombreProyecto(), p.getEspecie(), p.getCepa(),
                        p.getEstado(), p.getTotalSecuencias(), p.isVisibleOtros() ? "Si" : "No"});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al refrescar: " + ex.getMessage());
        }
    }

    private void crear() {
        if (txtProyecto.getText().isBlank() || txtEspecie.getText().isBlank()
                || txtRuta.getText().isBlank() || txtMd5.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Completa proyecto, especie, ruta y MD5.");
            return;
        }
        try {
            Proteoma p = new Proteoma();
            p.setNombreProyecto(txtProyecto.getText().trim());
            p.setEspecie(txtEspecie.getText().trim());
            p.setCepa(txtCepa.getText().isBlank() ? null : txtCepa.getText().trim());
            p.setRutaArchivo(txtRuta.getText().trim());
            p.setMd5Fasta(txtMd5.getText().trim());
            p.setEstado(Enums.EstadoProteoma.pendiente);
            p.setVisibleOtros(chkVisible.isSelected());
            p.setIdUsuarioPublica(usuarioAutenticado.getIdUsuario());

            proteomaDao.crear(p);

            // --- TRAZABILIDAD: registrar la creación como modificación ---
            proteomaDao.registrarModificacion(p.getIdProteoma(), usuarioAutenticado.getIdUsuario());

            JOptionPane.showMessageDialog(this, "Proteoma #" + p.getIdProteoma() + " creado como 'pendiente'.");
            txtProyecto.setText(""); txtEspecie.setText(""); txtCepa.setText("");
            txtRuta.setText(""); txtMd5.setText("");
            refrescar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al crear proteoma: " + ex.getMessage());
        }
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un proteoma en la tabla.");
            return;
        }
        // Verificar que el usuario autenticado sea administrador (por si se habilitó por error)
        try {
            if (!usuarioDao.esAdministrador(usuarioAutenticado.getIdUsuario())) {
                JOptionPane.showMessageDialog(this, "No tienes permisos de administrador para eliminar.");
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al verificar permisos: " + ex.getMessage());
            return;
        }

        int idProteoma = (int) modelo.getValueAt(fila, 0);
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Esto eliminará el proteoma #" + idProteoma + " dejando trazabilidad en proteoma_elimina. Continuar?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) return;

        try {
            // Usamos el id del administrador autenticado
            proteomaDao.eliminarConTrazabilidad(idProteoma, usuarioAutenticado.getIdUsuario());
            refrescar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + ex.getMessage());
        }
    }
}