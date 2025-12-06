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
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

    // Do wyświetlania pracowników w ComboBox
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

        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 25, 10, 25));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15));
        JButton cancelButton = new JButton("Anuluj");
        cancelButton.addActionListener(e -> dispose());
        JButton createButton = new JButton("Utwórz konwersację");
        createButton.setBackground(MAIN_COLOR);
        createButton.setForeground(Color.WHITE);
        createButton.setFont(new Font("Arial", Font.BOLD, 14));
        createButton.addActionListener(e -> createThread());
        buttonPanel.add(cancelButton);
        buttonPanel.add(createButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 5, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // 1. Pracownik (odbiorca)
        JLabel workerLabel = new JLabel("Adresat (pracownik):");
        workerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(workerLabel, gbc);

        workerComboBox = new JComboBox<>();
        workerComboBox.setPreferredSize(new Dimension(300, 30));
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(workerComboBox, gbc);

        // 2. Temat
        JLabel topicLabel = new JLabel("Temat konwersacji:");
        topicLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(topicLabel, gbc);

        topicField = new JTextField(25);
        topicField.setPreferredSize(new Dimension(300, 30));
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(topicField, gbc);

        // 3. Wiadomość początkowa
        JLabel messageLabel = new JLabel("Wiadomość początkowa:");
        messageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(messageLabel, gbc);

        initialMessageArea = new JTextArea(8, 25);
        initialMessageArea.setLineWrap(true);
        initialMessageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(initialMessageArea);
        scrollPane.setPreferredSize(new Dimension(300, 150));
        gbc.gridx = 0; gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(scrollPane, gbc);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // załaduj listę pracowników (bez siebie)
        loadWorkers();

        pack();
        setLocationRelativeTo(owner);
    }

    // ---------- Załadowanie listy pracowników ----------
    private void loadWorkers() {
        Vector<WorkerInfo> workers = new Vector<>();
        String sql = "SELECT id, full_name FROM users_workers WHERE id <> ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, senderWorkerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    workers.add(new WorkerInfo(
                            rs.getInt("id"),
                            rs.getString("full_name")
                    ));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Błąd ładowania pracowników:\n" + e.getMessage(),
                    "Błąd SQL", JOptionPane.ERROR_MESSAGE);
        }

        workerComboBox.setModel(new DefaultComboBoxModel<>(workers));
    }

    // ---------- Utworzenie wątku + pierwszej wiadomości ----------
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

        try {
            dbConnection.setAutoCommit(false);

            int newThreadId;

            // 1. wpis do chat_threads – zapamiętujemy DWÓCH pracowników
            String threadSql =
                    "INSERT INTO chat_threads (topic, worker_id, worker_id2, created_at) " +
                            "VALUES (?, ?, ?, NOW())";

            try (PreparedStatement threadPstmt =
                         dbConnection.prepareStatement(threadSql, Statement.RETURN_GENERATED_KEYS)) {

                threadPstmt.setString(1, topic);
                threadPstmt.setInt(2, senderWorkerId); // zalogowany
                threadPstmt.setInt(3, recipientId);    // wybrany pracownik

                threadPstmt.executeUpdate();

                try (ResultSet rs = threadPstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Nie udało się pobrać ID nowo utworzonego wątku.");
                    }
                    newThreadId = rs.getInt(1);
                }
            }

            // 2. pierwsza wiadomość – od zalogowanego pracownika
            String msgSql =
                    "INSERT INTO chat_messages (thread_id, sender_type, sender_id, message, timestamp) " +
                            "VALUES (?, 'worker', ?, ?, NOW())";

            try (PreparedStatement msgPstmt = dbConnection.prepareStatement(msgSql)) {
                msgPstmt.setInt(1, newThreadId);
                msgPstmt.setInt(2, senderWorkerId);
                msgPstmt.setString(3, initialMessage);
                msgPstmt.executeUpdate();
            }

            dbConnection.commit();
            dbConnection.setAutoCommit(true);

            threadCreated = true;

            String createdAt = LocalDateTime.now().format(DATE_FORMATTER);
            createdThread = new Chat.ChatThread(newThreadId, topic, createdAt, recipientId);

            dispose();

        } catch (SQLException ex) {
            try {
                dbConnection.rollback();
                dbConnection.setAutoCommit(true);
            } catch (SQLException rollbackEx) {
                System.err.println("Błąd podczas wycofywania transakcji: " + rollbackEx.getMessage());
            }

            JOptionPane.showMessageDialog(this,
                    "Błąd podczas tworzenia konwersacji:\n" + ex.getMessage(),
                    "Błąd SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- Gettery dla Chat.java ----------
    public boolean isThreadCreated() {
        return threadCreated;
    }

    public Chat.ChatThread getCreatedThread() {
        return createdThread;
    }
}
