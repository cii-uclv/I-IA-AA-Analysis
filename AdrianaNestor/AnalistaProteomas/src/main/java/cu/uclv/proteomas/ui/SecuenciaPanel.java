package cu.uclv.proteomas.ui;

import cu.uclv.proteomas.dao.ProteomaDao;
import cu.uclv.proteomas.dao.SecuenciaDao;
import cu.uclv.proteomas.dao.impl.ProteomaDaoImpl;
import cu.uclv.proteomas.dao.impl.SecuenciaDaoImpl;
import cu.uclv.proteomas.model.Proteoma;
import cu.uclv.proteomas.model.Secuencia;
import cu.uclv.proteomas.model.Enums;
import cu.uclv.proteomas.model.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class SecuenciaPanel extends JPanel {

    private static final String AMINOACIDOS_VALIDOS = "ARNDCQEGHILKMFPSTWYV";
    private final ProteomaDao proteomaDao = new ProteomaDaoImpl();
    private final SecuenciaDao secuenciaDao = new SecuenciaDaoImpl();
    private final Usuario usuario;

    private final JComboBox<Proteoma> comboProteoma = new JComboBox<>();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Longitud", "Secuencia (primeros 60 aa)"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JTextArea txtNuevaSecuencia = new JTextArea(4, 40);

    public SecuenciaPanel(Usuario usuario) {
        this.usuario = usuario;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Proteoma:"));
        top.add(comboProteoma);
        JButton btnRefrescar = new JButton("Refrescar");
        JButton btnCargarFasta = new JButton("Cargar FASTA...");
        top.add(btnRefrescar);
        top.add(btnCargarFasta);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.setBorder(BorderFactory.createTitledBorder("Añadir secuencia manualmente"));
        bottom.add(new JScrollPane(txtNuevaSecuencia), BorderLayout.CENTER);
        JButton btnAgregar = new JButton("Agregar secuencia");
        JPanel botonesBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnEliminarSel = new JButton("Eliminar seleccionada");
        // Si es investigador, puede eliminar secuencias de sus propios proteomas, pero no debería poder eliminar de proteomas de otros
        // Controlamos en la acción.
        botonesBottom.add(btnEliminarSel);
        botonesBottom.add(btnAgregar);
        bottom.add(botonesBottom, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        comboProteoma.addActionListener(e -> refrescarSecuencias());
        btnRefrescar.addActionListener(e -> refrescarProteomas());
        btnCargarFasta.addActionListener(e -> cargarFasta());
        btnAgregar.addActionListener(e -> agregarManual());
        btnEliminarSel.addActionListener(e -> eliminarSeleccionada());

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

    private void refrescarSecuencias() {
        Proteoma p = (Proteoma) comboProteoma.getSelectedItem();
        modelo.setRowCount(0);
        if (p == null) return;
        try {
            for (Secuencia s : secuenciaDao.listarPorProteoma(p.getIdProteoma())) {
                String preview = s.getSecuenciaAa().length() > 60 ? s.getSecuenciaAa().substring(0, 60) + "..." : s.getSecuenciaAa();
                modelo.addRow(new Object[]{s.getId(), s.getLongitud(), preview});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al listar secuencias: " + ex.getMessage());
        }
    }

    private void agregarManual() {
        Proteoma p = (Proteoma) comboProteoma.getSelectedItem();
        String seq = txtNuevaSecuencia.getText().trim();
        if (p == null || seq.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona un proteoma y escribe una secuencia.");
            return;
        }
        // --- VALIDACIÓN DE SECUENCIA ---
        if (!secuenciaValida(seq)) {
            JOptionPane.showMessageDialog(this,
                    "La secuencia contiene caracteres no válidos.\n" +
                            "Solo se permiten los 20 aminoácidos estándar: A, R, N, D, C, Q, E, G, H, I, L, K, M, F, P, S, T, W, Y, V.",
                    "Secuencia inválida", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Verificar propiedad ...
        try {
            Secuencia s = new Secuencia();
            s.setIdProteoma(p.getIdProteoma());
            s.setSecuenciaAa(seq.toUpperCase());
            secuenciaDao.crear(s);
            txtNuevaSecuencia.setText("");
            refrescarSecuencias();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al agregar secuencia: " + ex.getMessage());
        }
    }

    private void eliminarSeleccionada() {
        Proteoma p = (Proteoma) comboProteoma.getSelectedItem();
        int fila = tabla.getSelectedRow();
        if (p == null || fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una secuencia en la tabla.");
            return;
        }
        // Verificar propiedad si es investigador
        if (usuario.getTipoUsuario() != Enums.TipoUsuario.administrador && p.getIdUsuarioPublica() != usuario.getIdUsuario()) {
            JOptionPane.showMessageDialog(this, "No puedes eliminar secuencias de un proteoma que no te pertenece.");
            return;
        }
        long id = (long) modelo.getValueAt(fila, 0);
        try {
            secuenciaDao.eliminar(p.getIdProteoma(), id);
            refrescarSecuencias();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + ex.getMessage());
        }
    }

    private void cargarFasta() {
        Proteoma p = (Proteoma) comboProteoma.getSelectedItem();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un proteoma primero.");
            return;
        }
        if (usuario.getTipoUsuario() != Enums.TipoUsuario.administrador && p.getIdUsuarioPublica() != usuario.getIdUsuario()) {
            JOptionPane.showMessageDialog(this, "No puedes cargar secuencias a un proteoma que no te pertenece.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File archivo = chooser.getSelectedFile();

        List<Secuencia> lote = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        int numSecuencia = 0;
        List<Integer> secuenciasInvalidas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;
                if (linea.startsWith(">")) {
                    // Si hay una secuencia acumulada, validarla antes de agregar
                    if (!actual.isEmpty()) {
                        numSecuencia++;
                        String seq = actual.toString().toUpperCase();
                        if (!secuenciaValida(seq)) {
                            secuenciasInvalidas.add(numSecuencia);
                        } else {
                            agregarSecuenciaAlLote(lote, p.getIdProteoma(), seq);
                        }
                        actual = new StringBuilder();
                    }
                    // Reiniciar el contador de secuencia? Mejor contar al final
                } else {
                    actual.append(linea);
                }
            }
            // Procesar la última secuencia
            if (!actual.isEmpty()) {
                numSecuencia++;
                String seq = actual.toString().toUpperCase();
                if (!secuenciaValida(seq)) {
                    secuenciasInvalidas.add(numSecuencia);
                } else {
                    agregarSecuenciaAlLote(lote, p.getIdProteoma(), seq);
                }
            }

            // Si hay secuencias inválidas, mostrar error y no guardar nada
            if (!secuenciasInvalidas.isEmpty()) {
                StringBuilder msg = new StringBuilder("Las siguientes secuencias contienen caracteres no válidos:\n");
                for (int idx : secuenciasInvalidas) {
                    msg.append("  - Secuencia #").append(idx).append("\n");
                }
                msg.append("\nSolo se permiten los 20 aminoácidos estándar.");
                JOptionPane.showMessageDialog(this, msg.toString(), "Error en FASTA", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (lote.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No se encontraron secuencias válidas en el archivo.");
                return;
            }
            secuenciaDao.crearLote(lote);
            JOptionPane.showMessageDialog(this, lote.size() + " secuencia(s) cargada(s) para el proteoma #" + p.getIdProteoma() + ".");
            refrescarSecuencias();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al leer/cargar el FASTA: " + ex.getMessage());
        }
    }

    private void agregarSiNoVacia(List<Secuencia> lote, int idProteoma, StringBuilder sb) {
        if (sb.length() == 0) return;
        Secuencia s = new Secuencia();
        s.setIdProteoma(idProteoma);
        s.setSecuenciaAa(sb.toString().toUpperCase());
        lote.add(s);
    }

    private boolean secuenciaValida(String secuencia) {
        if (secuencia == null || secuencia.isEmpty()) {
            return false;
        }
        for (char c : secuencia.toUpperCase().toCharArray()) {
            if (AMINOACIDOS_VALIDOS.indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }

    private void agregarSecuenciaAlLote(List<Secuencia> lote, int idProteoma, String secuencia) {
        Secuencia s = new Secuencia();
        s.setIdProteoma(idProteoma);
        s.setSecuenciaAa(secuencia); // ya viene en mayúsculas y validada
        lote.add(s);
    }
}