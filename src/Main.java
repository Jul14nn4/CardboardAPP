// Main.java (Zmodyfikowany)

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {

    // ZMIANA: FLAGA DEBUGOWANIA
    private static final boolean SKIP_LOGIN = true;

    public static void main(String[] args) {

        String url = "jdbc:mysql://130.61.37.178:3306/projekt";
        String user = "project";
        String pass = "ZAQ!2wsx";

        Connection conn = null;

        try {
            conn = DriverManager.getConnection(url, user, pass);
            if(conn != null) {
                System.out.println("Połączono z bazą.");
            }

            Connection finalConn = conn;

            SwingUtilities.invokeLater(() -> {

                if (SKIP_LOGIN) {
                    // --- ŚCIEŻKA DEBUGOWANIA (OMINIĘCIE LOGOWANIA) ---
                    String testUser = "testLogistyk";
                    String testRole = "magazyn";
                    String testFullName = "Testowy Logistyk";

                    MainFrame mainFrame = new MainFrame(testUser, testRole, testFullName, finalConn);
                    mainFrame.setVisible(true);

                } else {
                    // --- ŚCIEŻKA PRODUKCYJNA (EKRAN LOGOWANIA) ---
                    MyFrame frame = new MyFrame();
                    if (frame.rightPanel != null) {
                        frame.rightPanel.setDbConnection(finalConn);
                    }
                    frame.setVisible(true);
                }
            });

        } catch (SQLException e) {
            System.err.println("Błąd połączenia z bazą danych: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Błąd połączenia z bazą danych:\n" + e.getMessage(), "Błąd Krytyczny", JOptionPane.ERROR_MESSAGE);
        }
    }
}