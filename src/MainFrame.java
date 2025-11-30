// MainFrame.java

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame(String username, String role) {
        setTitle("System Cardboard - Zalogowano jako: " + username + " (" + role + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // --- Prosty interfejs po zalogowaniu ---
        JLabel welcomeLabel = new JLabel("Witaj, " + username + "! Twoja rola: " + role, SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 32));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(welcomeLabel, BorderLayout.CENTER);

        setContentPane(contentPanel);
    }
}