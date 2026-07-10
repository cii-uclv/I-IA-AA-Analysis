package cu.uclv.proteomas.ui;

import cu.uclv.proteomas.dao.ProteomaDao;
import cu.uclv.proteomas.dao.impl.ProteomaDaoImpl;
import cu.uclv.proteomas.model.Proteoma;
import cu.uclv.proteomas.model.Usuario;
import cu.uclv.proteomas.orquestador.OrquestadorAnalisis;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class OrquestadorPanel extends JPanel {

    private final ProteomaDao proteomaDao = new ProteomaDaoImpl();
    private final OrquestadorAnalisis orchestrator = new OrquestadorAnalisis();
    private final Usuario usuario;

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Proyecto", "Especie", "Estado", "Secuencias"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tablaPendientes = new JTable(modeloTabla);
    private final JTextArea log = new JTextArea();
    private final JButton btnRefrescar = new JButton("Refrescar pendientes");
    private final JButton btnProcesar = new JButton("Procesar TODOS los pendientes");
    private final JButton btnProcesarSeleccionado = new JButton("Procesar seleccionado");

    public OrquestadorPanel(Usuario usuario) {
        this.usuario = usuario;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel superior sin combo de usuario
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Usuario operador: " + usuario.getNombreUsuario() + " (" + usuario.getTipoUsuario() + ")"));
        top.add(btnRefrescar);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(tablaPendientes), BorderLayout.CENTER);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollLog = new JScrollPane(log);
        scrollLog.setPreferredSize(new Dimension(100, 220));

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(btnProcesarSeleccionado);
        botones.add(btnProcesar);
        bottom.add(botones, BorderLayout.NORTH);
        bottom.add(scrollLog, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        btnRefrescar.addActionListener(e -> refrescar());
        btnProcesar.addActionListener(e -> ejecutar(null));
        btnProcesarSeleccionado.addActionListener(e -> {
            int fila = tablaPendientes.getSelectedRow();
            if (fila < 0) {
                JOptionPane.showMessageDialog(this, "Selecciona un proteoma de la tabla.");
                return;
            }
            int idProteoma = (int) modeloTabla.getValueAt(fila, 0);
            ejecutar(idProteoma);
        });

        refrescar();
    }

    private void refrescar() {
        try {
            modeloTabla.setRowCount(0);
            // Si es administrador, ve todos los pendientes; si investigador, solo los suyos.
            List<Proteoma> pendientes;
            if (usuario.getTipoUsuario() == cu.uclv.proteomas.model.Enums.TipoUsuario.administrador) {
                pendientes = proteomaDao.listarPendientes();
            } else {
                // Filtrar solo los del investigador (estado pendiente)
                pendientes = proteomaDao.listarPorUsuario(usuario.getIdUsuario()).stream()
                        .filter(p -> p.getEstado() == cu.uclv.proteomas.model.Enums.EstadoProteoma.pendiente)
                        .toList();
            }
            for (Proteoma p : pendientes) {
                modeloTabla.addRow(new Object[]{p.getIdProteoma(), p.getNombreProyecto(), p.getEspecie(),
                        p.getEstado(), p.getTotalSecuencias()});
            }
            appendLog(pendientes.size() + " proteoma(s) pendiente(s) cargado(s).");
        } catch (Exception ex) {
            appendLog("ERROR al refrescar: " + ex.getMessage());
        }
    }

    private void ejecutar(Integer idProteomaUnico) {
        setBotonesHabilitados(false);
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    if (idProteomaUnico == null) {
                        // Pasar el objeto usuario completo (no solo su ID)
                        orchestrator.procesarPendientes(usuario, this::publish);
                    } else {
                        Proteoma p = proteomaDao.buscarPorId(idProteomaUnico);
                        if (p != null) {
                            // Pasar el objeto usuario completo
                            orchestrator.procesarProteoma(p, usuario, this::publish);
                        } else {
                            publish("Proteoma #" + idProteomaUnico + " no encontrado.");
                        }
                    }
                } catch (Exception e) {
                    publish("ERROR fatal: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String s : chunks) appendLog(s);
            }

            @Override
            protected void done() {
                setBotonesHabilitados(true);
                refrescar();
            }
        };
        worker.execute();
    }

    private void setBotonesHabilitados(boolean habilitados) {
        btnProcesar.setEnabled(habilitados);
        btnProcesarSeleccionado.setEnabled(habilitados);
        btnRefrescar.setEnabled(habilitados);
    }

    private void appendLog(String s) {
        log.append(s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }
}