import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

public class NewThreadDialog extends JDialog {

    private final Connection dbConnection;
    private final int senderWorkerId;
    private final String senderUsername;

    private JComboBox<WorkerInfo> workerComboBox;
    private JTextField topicField;
    private JTextArea initialMessageArea;

    private boolean threadCreated = false;
    private Chat.ChatThread createdThread;

    private static final Color MAIN_COLOR = new Color(87, 50, 135);

    // Wewnętrzna klasa do przechowywania informacji o pracowniku
    private static class WorkerInfo {
        int id;
        String fullName;

        public WorkerInfo(int id, String fullName) {
            this.id = id;
            this.fullName = fullName;
        }

        @Override
        public String toString() {
            return fullName;
        }
    }

    public NewThreadDialog(Window owner, Connection conn, int senderId, String senderName) {
        super(owner, "Nowa konwersacja", ModalityType.APPLICATION_MODAL);
        this.dbConnection = conn;
        this.senderWorkerId = senderId;
        this.senderUsername = senderName;

        // 1. Zwiększenie rozmiaru okna
        // Usunięto setSize(), użyjemy pack() z lepszymi preferowanymi rozmiarami komponentów.
        // setSize(450, 500); // Nowe, sugerowane wymiary, jeśli konieczne
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // 2. Ustawienie głównego panelu formularza (Użycie GridBagLayout)
        JPanel formPanel = new JPanel(new GridBagLayout());
        // Zwiększenie marginesów wewnętrznych dla paneli
        formPanel.setBorder(new EmptyBorder(20, 25, 10, 25));

        // 3. Panel przycisków (z większymi marginesami zewnętrznymi)
        JPanel buttonWrapperPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15)); // Zwiększenie odstępu przycisków od krawędzi
        JButton createButton = new JButton("Utwórz konwersację");
        createButton.setBackground(MAIN_COLOR);
        createButton.setForeground(Color.WHITE);
        createButton.setFont(new Font("Arial", Font.BOLD, 14));
        createButton.addActionListener(e -> createThread());

        JButton cancelButton = new JButton("Anuluj");
        cancelButton.addActionListener(e -> dispose());

        buttonWrapperPanel.add(cancelButton);
        buttonWrapperPanel.add(createButton);

        // --- KONFIGURACJA GridBagLayout ---
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 5, 0); // Odstępy między wierszami
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // 1. Wybór odbiorcy
        JLabel workerLabel = new JLabel("Odbiorca (Pracownik):");
        workerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0;
        formPanel.add(workerLabel, gbc);

        workerComboBox = new JComboBox<>();
        loadWorkers(); // Ładowanie danych
        workerComboBox.setPreferredSize(new Dimension(300, 30)); // Ustawienie preferowanej szerokości/wysokości
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.anchor = GridBagConstraints.CENTER; gbc.weightx = 1.0;
        formPanel.add(workerComboBox, gbc);


        // 2. Tytuł wątku
        JLabel topicLabel = new JLabel("Temat konwersacji:");
        topicLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0;
        formPanel.add(topicLabel, gbc);

        topicField = new JTextField(25); // Zwiększenie domyślnej szerokości
        topicField.setPreferredSize(new Dimension(300, 30));
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.anchor = GridBagConstraints.CENTER; gbc.weightx = 1.0;
        formPanel.add(topicField, gbc);

        // 3. Wiadomość początkowa
        JLabel messageLabel = new JLabel("Wiadomość początkowa:");
        messageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0;
        formPanel.add(messageLabel, gbc);

        initialMessageArea = new JTextArea(8, 25); // Zwiększenie wysokości JTextArea
        initialMessageArea.setLineWrap(true);
        initialMessageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(initialMessageArea);
        scrollPane.setPreferredSize(new Dimension(300, 150)); // Ustawienie preferowanego rozmiaru

        gbc.gridx = 0; gbc.gridy = row++;
        gbc.weighty = 1.0; // Rozciągnięcie w pionie
        gbc.fill = GridBagConstraints.BOTH; // Wypełnienie w obu kierunkach
        formPanel.add(scrollPane, gbc);


        // DODANIE WSZYSTKIEGO DO GŁÓWNEGO JDialogu
        add(formPanel, BorderLayout.CENTER);
        add(buttonWrapperPanel, BorderLayout.SOUTH);
        pack(); // Dopasowanie rozmiaru do zawartości
    }

    // Ładowanie listy pracowników (oprócz siebie)
    private void loadWorkers() {
        Vector<WorkerInfo> workers = new Vector<>();
        // Wybieramy wszystkich workerów OPRÓCZ zalogowanego
        String sql = "SELECT id, full_name FROM users_workers WHERE id != ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, senderWorkerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    workers.add(new WorkerInfo(rs.getInt("id"), rs.getString("full_name")));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Błąd ładowania pracowników: " + e.getMessage(), "Błąd SQL", JOptionPane.ERROR_MESSAGE);
        }

        workerComboBox.setModel(new DefaultComboBoxModel<>(workers));
    }

    // Logika tworzenia wątku i pierwszej wiadomości
    private void createThread() {
        WorkerInfo recipient = (WorkerInfo) workerComboBox.getSelectedItem();
        String topic = topicField.getText().trim();
        String initialMessage = initialMessageArea.getText().trim();

        if (recipient == null) {
            JOptionPane.showMessageDialog(this, "Wybierz odbiorcę.", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (topic.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Wprowadź temat konwersacji.", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (initialMessage.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Wprowadź wiadomość początkową.", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int recipientId = recipient.id;

        // Zaczynamy transakcję (tworzenie wątku i wiadomości)
        try {
            dbConnection.setAutoCommit(false);
            int newThreadId = -1;

            // 1. Utwórz nowy wątek
            // UWAGA: Taki SQL zakłada, że konwersacje między pracownikami są przypisane do ID odbiorcy (recipientId)
            // w kolumnie `worker_id` w tabeli `chat_threads`
            String threadSql = "INSERT INTO chat_threads (topic, worker_id, created_at) VALUES (?, ?, NOW())";
            try (PreparedStatement threadPstmt = dbConnection.prepareStatement(threadSql, Statement.RETURN_GENERATED_KEYS)) {
                threadPstmt.setString(1, topic);
                threadPstmt.setInt(2, recipientId); // Przypisujemy do odbiorcy
                threadPstmt.executeUpdate();

                try (ResultSet rs = threadPstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        newThreadId = rs.getInt(1);
                    }
                }
            }

            if (newThreadId == -1) {
                throw new SQLException("Nie udało się pobrać ID nowo utworzonego wątku.");
            }

            // 2. Dodaj pierwszą wiadomość (od zalogowanego użytkownika)
            String messageSql = "INSERT INTO chat_messages (thread_id, sender_type, sender_id, message, timestamp) VALUES (?, 'worker', ?, ?, NOW())";
            try (PreparedStatement messagePstmt = dbConnection.prepareStatement(messageSql)) {
                messagePstmt.setInt(1, newThreadId);
                messagePstmt.setInt(2, senderWorkerId);
                messagePstmt.setString(3, initialMessage);
                messagePstmt.executeUpdate();
            }

            dbConnection.commit(); // Zatwierdzenie transakcji
            dbConnection.setAutoCommit(true);

            // Pomyślnie utworzono
            threadCreated = true;

            // Pobieramy sformatowaną datę
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy"));
            createdThread = new Chat.ChatThread(newThreadId, topic, now);

            dispose();

        } catch (SQLException ex) {
            try {
                dbConnection.rollback();
                dbConnection.setAutoCommit(true);
            } catch (SQLException rollbackEx) {
                System.err.println("Błąd podczas wycofywania transakcji: " + rollbackEx.getMessage());
            }
            JOptionPane.showMessageDialog(this, "Błąd podczas tworzenia konwersacji: " + ex.getMessage(), "Błąd SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isThreadCreated() {
        return threadCreated;
    }

    public Chat.ChatThread getCreatedThread() {
        return createdThread;
    }
}