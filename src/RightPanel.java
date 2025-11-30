// RightPanel.java (Zmodyfikowany)

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.GroupLayout;
import javax.swing.border.Border;
import java.sql.Connection; // IMPORT
import java.sql.PreparedStatement; // IMPORT
import java.sql.ResultSet; // IMPORT
import java.sql.SQLException; // IMPORT
import java.util.Arrays; // IMPORT
import javax.swing.JPasswordField; // IMPORT
import javax.swing.text.JTextComponent; // IMPORT do metody createInputField

/**
 *
 * @author Julianna
 */
public class RightPanel extends javax.swing.JPanel {

    // --- Klasa rysująca panel z zaokrąglonymi rogami ---
    class RoundedPanel extends JPanel {
        private int cornerRadius = 25;

        public RoundedPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            g2.dispose();
            super.paintComponent(g);
        }

        // Specjalna metoda do rysowania obramowania, używana przez statusPanel
        public void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(220, 220, 220)); // Jasnoszary
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
            g2.dispose();
            super.paintBorder(g);
        }
    }
    // --------------------------------------------------

    // Zmienne dla pól tekstowych i przycisków
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel loginFieldPanel;
    private javax.swing.JPanel passwordFieldPanel;
    private javax.swing.JTextField jTextField1;
    // ZMIANA 1: Zmieniono z JTextField na JPasswordField
    private javax.swing.JPasswordField jPasswordField2;
    private javax.swing.JPanel statusPanel;

    // NOWE POLE: Połączenie z bazą danych
    private Connection dbConnection;

    // --- METODA: Ustawianie połączenia z bazy danych ---
    public void setDbConnection(Connection conn) {
        this.dbConnection = conn;
        // Opcjonalnie: zaktualizuj statusPanel, jeśli chcesz
        if (conn == null) {
            System.out.println("Błąd: Connection jest null.");
        }
    }

    // --- GŁÓWNY KONSTRUKTOR ---
    public RightPanel() {
        initComponents();
        setOpaque(false);
        addPlaceholderListeners();
        // Połączenie zostanie ustawione przez Main.java
    }

    // --- METODA: Obsługa placeholderów ---
    private void addPlaceholderListeners() {
        Color defaultTextColor = Color.GRAY;
        Color activeTextColor = Color.BLACK;

        jTextField1.setForeground(defaultTextColor);
        // ZMIANA 2: Obsługa JPasswordField
        jPasswordField2.setForeground(defaultTextColor);

        // --- Listener dla Nazwy użytkownika (jTextField1) ---
        jTextField1.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (jTextField1.getText().equals("Nazwa użytkownika")) {
                    jTextField1.setText("");
                    jTextField1.setForeground(activeTextColor);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (jTextField1.getText().isEmpty()) {
                    jTextField1.setForeground(defaultTextColor);
                    jTextField1.setText("Nazwa użytkownika");
                }
            }
        });

        // --- Listener dla Hasła (jPasswordField2) ---
        jPasswordField2.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                // Konwersja hasła do stringa dla porównania z placeholderem
                String passwordText = new String(jPasswordField2.getPassword());

                if (passwordText.equals("Hasło")) {
                    jPasswordField2.setText("");
                    jPasswordField2.setForeground(activeTextColor);
                    jPasswordField2.setEchoChar('*'); // Pokaż gwiazdki dla hasła
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (jPasswordField2.getPassword().length == 0) {
                    jPasswordField2.setEchoChar((char) 0); // Ukryj gwiazdki dla placeholdera
                    jPasswordField2.setForeground(defaultTextColor);
                    jPasswordField2.setText("Hasło");
                }
            }
        });
    }

    // Metoda pomocnicza do tworzenia panelu z ikoną (imitacja pola tekstowego)
    // ZMIANA 3: Akceptuje JTextComponent (JTextField lub JPasswordField)
    private JPanel createInputField(String placeholder, String iconPath, JTextComponent textField) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(Color.WHITE);

        // Lewa ikona (ładowana z podanej ścieżki)
        ImageIcon originalIcon = new ImageIcon(iconPath);
        Image scaledImage = originalIcon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
        JLabel iconLabel = new JLabel(new ImageIcon(scaledImage));

        iconLabel.setBorder(new EmptyBorder(0, 15, 0, 0));

        // Pole tekstowe
        if (textField != null) {
            textField.setText(placeholder);
            textField.setBorder(new EmptyBorder(0, 0, 0, 0));
            textField.setFont(new Font("Arial", Font.PLAIN, 16));
            textField.setForeground(Color.GRAY);
            panel.add(textField, BorderLayout.CENTER);

            // DLA JPasswordField - ukrycie gwiazdek, gdy wyświetlany jest placeholder
            if (textField instanceof JPasswordField) {
                ((JPasswordField) textField).setEchoChar((char) 0);
            }
        }

        panel.add(iconLabel, BorderLayout.WEST);
        panel.setPreferredSize(new Dimension(300, 50));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                new EmptyBorder(0, 0, 0, 0)
        ));

        return panel;
    }

    private void initComponents() {

        // Prawidłowa inicjalizacja jPanel1 z niestandardową klasą
        jPanel1 = new RoundedPanel();

        // Tworzenie pól tekstowych
        jTextField1 = new JTextField("Nazwa użytkownika");
        // ZMIANA 4: Użycie JPasswordField
        jPasswordField2 = new JPasswordField("Hasło");

        // Użycie account.png i lock.png (argumenty muszą być dostosowane)
        loginFieldPanel = createInputField("Nazwa użytkownika", "account.png", jTextField1);
        passwordFieldPanel = createInputField("Hasło", "lock.png", jPasswordField2);

        // --- Stylizowanie jPanel1 (Główny biały kontener) ---
        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        // --- Stylizowanie nagłówków ---
        jLabel1 = new javax.swing.JLabel("Witaj ponownie!");
        jLabel1.setFont(new Font("Arial", Font.BOLD, 28));

        jLabel3 = new javax.swing.JLabel("Zaloguj się, aby kontynuować");
        jLabel3.setFont(new Font("Arial", Font.PLAIN, 16));
        jLabel3.setForeground(new Color(100, 100, 100));

        // --- Stylizowanie Przycisku (ZMIANA) ---
        jButton1 = new javax.swing.JButton("ZALOGUJ SIĘ") {
            // Nadpisanie metody paintComponent, by rysować zaokrąglone tło
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                // Rysowanie zaokrąglonego prostokąta o promieniu 25
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2.dispose();
                super.paintComponent(g); // Rysowanie tekstu
            }
        };
        jButton1.setBackground(new java.awt.Color(59, 130, 246));
        jButton1.setForeground(Color.WHITE);
        jButton1.setFont(new Font("Arial", Font.BOLD, 18));
        jButton1.setPreferredSize(new Dimension(300, 50));
        jButton1.setFocusPainted(false);
        jButton1.setContentAreaFilled(false); // Wyłącza domyślne rysowanie tła
        jButton1.setOpaque(false);
        // Ustawienie marginesów tekstu wewnątrz przycisku
        jButton1.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));

        // ZMIANA 5: Dodanie Akcji do Przycisku
        jButton1.addActionListener(this::jButton1ActionPerformed);


        // --- Stylizowanie Panelu Statusu (dolny prostokąt) ---
        statusPanel = new RoundedPanel(); // Używa RoundedPanel
        statusPanel.setOpaque(true);
        statusPanel.setBackground(Color.WHITE); // ZMIANA: Tło na białe
        statusPanel.setPreferredSize(new Dimension(300, 90));

        // Dodanie przezroczystego marginesu, aby obramowanie było wewnątrz
        statusPanel.setBorder(new EmptyBorder(10, 20, 10, 1));

        statusPanel.setLayout(new BorderLayout(10, 5));


        // --- IKONA STATUSU ---
        String statusIconPath = "database-check.png";
        ImageIcon originalStatusIcon = new ImageIcon(statusIconPath);
        Image scaledStatusImage = originalStatusIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JLabel statusIconLabel = new JLabel(new ImageIcon(scaledStatusImage));
        statusIconLabel.setVerticalAlignment(SwingConstants.CENTER);

        // --- TEKST STATUSU ---
        JLabel statusText = new JLabel("Status połączenia");
        statusText.setFont(new Font("Arial", Font.PLAIN, 12));
        statusText.setForeground(new Color(100, 100, 100));

        JLabel connectionStatus = new JLabel("Połączono z bazą danych");
        connectionStatus.setFont(new Font("Arial", Font.BOLD, 14));
        connectionStatus.setForeground(new Color(34, 197, 94));

        JLabel databaseDetails = new JLabel("Oracle Database - 130.61.37.178");
        databaseDetails.setFont(new Font("Arial", Font.PLAIN, 12));
        databaseDetails.setForeground(new Color(150, 150, 150));

        // Panel do grupowania tekstu
        JPanel statusTextPanel = new JPanel(new GridLayout(3, 1));
        statusTextPanel.setOpaque(false);
        statusTextPanel.add(statusText);
        statusTextPanel.add(connectionStatus);
        statusTextPanel.add(databaseDetails);

        // Dodanie ikony i tekstu do statusPanel
        statusPanel.add(statusIconLabel, BorderLayout.WEST);
        statusPanel.add(statusTextPanel, BorderLayout.CENTER);

        // Prawidłowe użycie GroupLayout
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);

        // --- UKŁAD jPanel1 (Wewnętrzny) ---
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(loginFieldPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(passwordFieldPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel1)
                                        .addComponent(jLabel3))
                                .addContainerGap(50, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel3)
                                .addGap(50, 50, 50)
                                .addComponent(loginFieldPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(20, 20, 20)
                                .addComponent(passwordFieldPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(40, 40, 40)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(40, 40, 40)
                                .addComponent(statusPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(50, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);

        // --- UKŁAD RightPanel (zewnętrzny, PRZESUNIĘCIE W LEWO) ---
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
        );
    }

    // ZMIANA 6: Nowa metoda logiki logowania
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        // Walidacja pól placeholdera
        // Sprawdzamy czy username to placeholder LUB czy hasło to placeholder LUB czy pole hasła jest puste
        if (jTextField1.getText().equals("Nazwa użytkownika") || new String(jPasswordField2.getPassword()).equals("Hasło") || jPasswordField2.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Proszę podać nazwę użytkownika i hasło.",
                    "Błąd Logowania",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Sprawdzenie, czy połączenie z bazą jest dostępne
        if (dbConnection == null) {
            JOptionPane.showMessageDialog(this,
                    "Brak aktywnego połączenia z bazą danych. Sprawdź plik Main.java.",
                    "Błąd Logowania",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String username = jTextField1.getText();
        String password = new String(jPasswordField2.getPassword());

        // Zapytanie SELECT pobierające 'role' ORAZ 'full_name'
        String sql = "SELECT role, full_name FROM users_workers WHERE username = ? AND password = ? AND role IN ('magazyn', 'admin')";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Zalogowano pomyślnie
                String role = rs.getString("role");
                String fullName = rs.getString("full_name");

                // 1. Ukrycie bieżącego okna logowania (MyFrame)
                JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
                parentFrame.dispose();

                // 2. Wyświetlenie nowego okna MainFrame
                SwingUtilities.invokeLater(() -> {
                    // Przekazujemy Connection dbConnection, fullName, role i username
                    MainFrame mainFrame = new MainFrame(username, role, fullName, dbConnection);
                    mainFrame.setVisible(true);
                });

            } else {
                // Błędny login/hasło, lub nieprawidłowa rola (np. 'produkcja' zamiast 'magazyn')
                JOptionPane.showMessageDialog(this,
                        "Błędna nazwa użytkownika, hasło lub brak uprawnień (dozwolone: magazyn/admin).",
                        "Błąd Logowania",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Błąd zapytania do bazy danych: " + e.getMessage(),
                    "Błąd Logowania",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}