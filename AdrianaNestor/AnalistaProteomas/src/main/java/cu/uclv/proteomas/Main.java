package cu.uclv.proteomas;

import cu.uclv.proteomas.ui.LoginDialog;
import cu.uclv.proteomas.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            LoginDialog login = new LoginDialog(null);
            login.setVisible(true);
            if (login.isSuccess()) {
                new MainFrame(login.getUsuarioAutenticado()).setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}