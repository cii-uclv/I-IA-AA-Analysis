package cu.uclv.proteomas.ui;

import cu.uclv.proteomas.dao.ProteomaDao;
import cu.uclv.proteomas.dao.RespuestaDao;
import cu.uclv.proteomas.dao.impl.ProteomaDaoImpl;
import cu.uclv.proteomas.dao.impl.RespuestaDaoImpl;
import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.Proteoma;
import cu.uclv.proteomas.model.Respuesta;
import cu.uclv.proteomas.model.Usuario;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class RespuestaPanel extends JPanel {

    private final ProteomaDao proteomaDao = new ProteomaDaoImpl();
    private final RespuestaDao respuestaDao = new RespuestaDaoImpl();
    private final Usuario usuario;

    private final JComboBox<Proteoma> comboProteoma = new JComboBox<>();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"Trabajo", "Secuencia", "Es enzima", "Conf.", "Hidrolasa", "Familia", "Conf. familia", "EC"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JTextArea txtRazonamiento = new JTextArea();
    private List<Respuesta> respuestasActuales;

    public RespuestaPanel(Usuario usuario) {
        this.usuario = usuario;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Proteoma:"));
        top.add(comboProteoma);
        JButton btnRefrescar = new JButton("Refrescar");
        top.add(btnRefrescar);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setTopComponent(new JScrollPane(tabla));

        txtRazonamiento.setLineWrap(true);
        txtRazonamiento.setWrapStyleWord(true);
        txtRazonamiento.setEditable(false);
        JScrollPane scrollRazonamiento = new JScrollPane(txtRazonamiento);
        scrollRazonamiento.setBorder(BorderFactory.createTitledBorder("Razonamiento del LLM (columna 'respuesta.razonamiento')"));
        split.setBottomComponent(scrollRazonamiento);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);

        comboProteoma.addActionListener(e -> refrescarRespuestas());
        btnRefrescar.addActionListener(e -> refrescarProteomas());
        tabla.getSelectionModel().addListSelectionListener(this::seleccionCambiada);

        refrescarProteomas();
    }

    private void refrescarProteomas() {
        try {
            comboProteoma.removeAllItems();
            List<Proteoma> lista;
            if (usuario.getTipoUsuario() == Enums.TipoUsuario.administrador) {
                lista = proteomaDao.listarTodos();
            } else {
                lista = proteomaDao.listarPorUsuario(usuario.getIdUsuario());
            }
            for (Proteoma p : lista) comboProteoma.addItem(p);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void refrescarRespuestas() {
        Proteoma p = (Proteoma) comboProteoma.getSelectedItem();
        modelo.setRowCount(0);
        txtRazonamiento.setText("");
        if (p == null) return;
        try {
            respuestasActuales = respuestaDao.listarPorProteoma(p.getIdProteoma());
            for (Respuesta r : respuestasActuales) {
                modelo.addRow(new Object[]{r.getIdTrabajo(), r.getIdSecuencia(), r.isEsEnzima(),
                        String.format("%.2f", r.getConfianzaEnzima()), r.getEsHidrolasa(), r.getFamiliaSugerida(),
                        r.getConfianzaFamilia() == null ? "" : String.format("%.2f", r.getConfianzaFamilia()),
                        r.getNumeroEc()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al listar respuestas: " + ex.getMessage());
        }
    }

    private void seleccionCambiada(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int fila = tabla.getSelectedRow();

        if (fila < 0 || respuestasActuales == null || fila >= respuestasActuales.size()) {
            // Limpiar el texto si la selección no es válida
            txtRazonamiento.setText("");
            return;
        }

        txtRazonamiento.setText(respuestasActuales.get(fila).getRazonamiento());
        txtRazonamiento.setCaretPosition(0);
    }
}