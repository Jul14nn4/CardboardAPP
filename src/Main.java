// Main.java (Zmodyfikowany)

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) { // Usunięto rzucanie wyjątku SQLException

        String url = "jdbc:mysql://130.61.37.178:3306/projekt";
        String user = "project";
        String pass = "ZAQ!2wsx";

        try {
            Connection conn = DriverManager.getConnection(url, user, pass);
            if(conn != null) {
                System.out.println("Połączono!");
            }
            else {
                System.out.println("Brak połączenia");
                JOptionPane.showMessageDialog(null, "Nie udało się połączyć z bazą danych.", "Błąd Krytyczny", JOptionPane.ERROR_MESSAGE);
                return; // Zakończ, jeśli nie ma połączenia
            }

            // --- Uruchomienie GUI ---
            Connection finalConn = conn; // Potrzebne dla lambda
            SwingUtilities.invokeLater(() -> {
                MyFrame frame = new MyFrame();

                // Kluczowy krok: Ustawienie połączenia w RightPanel
                if (frame.rightPanel instanceof RightPanel) {
                    ((RightPanel) frame.rightPanel).setDbConnection(finalConn);
                }

                frame.setVisible(true);
            });

        } catch (SQLException e) {
            System.err.println("Błąd połączenia z bazą danych: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Błąd połączenia z bazą danych:\n" + e.getMessage(), "Błąd Krytyczny", JOptionPane.ERROR_MESSAGE);
        }
    }
}