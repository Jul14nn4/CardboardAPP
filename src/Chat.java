import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Interfejs Refreshable musi być zaimplementowany w MainFrame
public class Chat extends JPanel implements Refreshable {

    private final Connection dbConnection;
    private final int loggedWorkerId;
    private final String loggedWorkerUsername;
    private static final Color SIDEBAR_COLOR = new Color(87, 50, 135);
    private static final Color MESSAGE_BG_OWN = new Color(175, 238, 238); // Jasny błękit/cyjan dla własnych wiadomości
    private static final Color MESSAGE_BG_OTHER = new Color(240, 240, 240); // Jasny szary dla innych wiadomości
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

    private JList<ChatThread> threadsList;
    private DefaultListModel<ChatThread> threadsListModel;
    private JTextArea messageInput;
    private JButton sendButton;
    private JPanel chatViewPanel;
    private JScrollPane chatScrollPane;

    private int currentThreadId = -1;
    private String currentThreadTopic = "Brak wybranej konwersacji";

    // Klasa do przechowywania danych o wątku czatu
    private static class ChatThread {
        int id;
        String topic;
        String createdAt;

        public ChatThread(int id, String topic, String createdAt) {
            this.id = id;
            this.topic = topic;
            this.createdAt = createdAt;
        }

        @Override
        public String toString() {
            return topic;
        }
    }

    public Chat(Connection conn, int workerId, String workerUsername) {
        this.dbConnection = conn;
        this.loggedWorkerId = workerId;
        this.loggedWorkerUsername = workerUsername;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(240, 240, 240));

        // 1. Nagłówek (możesz użyć metody z AcceptedCommissionsFrame)
        add(createHeader(), BorderLayout.NORTH);

        // 2. Główny kontener - podział na listę wątków i widok czatu
        JSplitPane mainSplitPane = createMainContentPanel();
        add(mainSplitPane, BorderLayout.CENTER);

        // 3. Uruchomienie ładowania danych
        loadThreadsList();

        // Listener wyboru wątku
        threadsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && threadsList.getSelectedValue() != null) {
                ChatThread selectedThread = threadsList.getSelectedValue();
                if (selectedThread.id != currentThreadId) {
                    currentThreadId = selectedThread.id;
                    currentThreadTopic = selectedThread.topic;
                    updateChatViewPanelTitle();
                    loadChatMessages();
                }
            }
        });

        // Listener przycisku Wyslij
        sendButton.addActionListener(this::sendMessage);
    }

    // Metoda tworząca nagłówek panelu
    private JPanel createHeader() {
        // Wzorując się na innych plikach, tworzymy prosty nagłówek
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Centrum Komunikacji (Czat)", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(SIDEBAR_COLOR);

        headerPanel.add(titleLabel, BorderLayout.CENTER);

        return headerPanel;
    }

    // Metoda tworząca główną zawartość
    private JSplitPane createMainContentPanel() {

        // --- Lewy panel: Historia rozmów ---
        JPanel threadsPanel = new JPanel(new BorderLayout());
        threadsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(SIDEBAR_COLOR),
                "Historia rozmów",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                new Font("Arial", Font.BOLD, 16),
                SIDEBAR_COLOR
        ));

        threadsListModel = new DefaultListModel<>();
        threadsList = new JList<>(threadsListModel);
        threadsList.setFont(new Font("Arial", Font.PLAIN, 14));
        threadsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        threadsList.setCellRenderer(new ThreadListRenderer()); // Niestandardowy renderer

        threadsPanel.add(new JScrollPane(threadsList), BorderLayout.CENTER);

        // --- Prawy panel: Widok Czatu ---
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createLineBorder(SIDEBAR_COLOR));

        // Panel na nagłówek czatu (wybrany topic)
        chatViewPanel = new JPanel(new BorderLayout());
        chatViewPanel.setBackground(Color.LIGHT_GRAY);
        chatViewPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        updateChatViewPanelTitle();
        chatPanel.add(chatViewPanel, BorderLayout.NORTH);

        // Obszar na wiadomości
        chatScrollPane = new JScrollPane();
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Panel do wpisywania wiadomości
        JPanel inputPanel = createInputPanel();
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // Używamy JSplitPane do podziału
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, threadsPanel, chatPanel);
        splitPane.setResizeWeight(0.3); // 30% dla listy wątków
        splitPane.setDividerSize(5);

        return splitPane;
    }

    // Metoda tworząca panel wprowadzania wiadomości
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        messageInput = new JTextArea(3, 20);
        messageInput.setWrapStyleWord(true);
        messageInput.setLineWrap(true);
        messageInput.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane inputScrollPane = new JScrollPane(messageInput);

        sendButton = new JButton("Wyślij");
        sendButton.setBackground(SIDEBAR_COLOR);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));

        inputPanel.add(inputScrollPane, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        return inputPanel;
    }

    // Metoda aktualizująca tytuł głównego okna czatu
    private void updateChatViewPanelTitle() {
        chatViewPanel.removeAll();
        JLabel titleLabel = new JLabel("Konwersacja: " + currentThreadTopic, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        chatViewPanel.add(titleLabel, BorderLayout.CENTER);
        chatViewPanel.revalidate();
        chatViewPanel.repaint();
        sendButton.setEnabled(currentThreadId != -1);
        messageInput.setEnabled(currentThreadId != -1);
    }

    // ----------------------------------------------------------------------
    // LOGIKA BAZY DANYCH
    // ----------------------------------------------------------------------

    // 1. Ładowanie listy wątków (Historia rozmów)
    private void loadThreadsList() {
        threadsListModel.clear();
        String sql = "SELECT id, topic, created_at FROM chat_threads WHERE worker_id = ? ORDER BY created_at DESC";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, loggedWorkerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    threadsListModel.addElement(new ChatThread(
                            rs.getInt("id"),
                            rs.getString("topic"),
                            rs.getTimestamp("created_at").toLocalDateTime().format(DATE_FORMATTER)
                    ));
                }

                // Zachowanie wybranego wątku
                if (currentThreadId != -1 && threadsListModel.getSize() > 0) {
                    for (int i = 0; i < threadsListModel.getSize(); i++) {
                        if (threadsListModel.getElementAt(i).id == currentThreadId) {
                            threadsList.setSelectedIndex(i);
                            break;
                        }
                    }
                }

            }
        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas ładowania listy wątków: " + e.getMessage());
        }
    }

    // 2. Ładowanie wiadomości dla wybranego wątku
    private void loadChatMessages() {
        if (currentThreadId == -1) return;

        // Używamy JEditorPane lub prostego JPanel z BoxLayout.Y_AXIS
        JPanel messagesContainer = new JPanel();
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(Color.WHITE);

        String sql = "SELECT sender_type, sender_id, message, timestamp FROM chat_messages WHERE thread_id = ? ORDER BY timestamp ASC";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, currentThreadId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String senderType = rs.getString("sender_type");
                    int senderId = rs.getInt("sender_id");
                    String message = rs.getString("message");
                    LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();

                    boolean isOwnMessage = senderType.equals("worker") && senderId == loggedWorkerId;

                    messagesContainer.add(createMessagePanel(
                            message,
                            timestamp.format(DATE_FORMATTER),
                            isOwnMessage
                    ));
                    messagesContainer.add(Box.createVerticalStrut(5)); // Dodaj mały odstęp między wiadomościami
                }
            }
        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas ładowania wiadomości: " + e.getMessage());
        }

        chatScrollPane.setViewportView(messagesContainer);

        // Automatyczne przewijanie na dół
        JScrollBar verticalScrollBar = chatScrollPane.getVerticalScrollBar();
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }

    // Metoda do tworzenia panelu pojedynczej wiadomości
    private JPanel createMessagePanel(String message, String timestamp, boolean isOwnMessage) {
        JPanel messageWrapper = new JPanel();
        messageWrapper.setLayout(new BoxLayout(messageWrapper, BoxLayout.X_AXIS));
        messageWrapper.setOpaque(false);

        // Główny panel wiadomości
        JPanel messageContent = new JPanel(new BorderLayout());
        messageContent.setBackground(isOwnMessage ? MESSAGE_BG_OWN : MESSAGE_BG_OTHER);
        messageContent.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(5, 10, 5, 10), // Wewnętrzne marginesy
                BorderFactory.createLineBorder(isOwnMessage ? SIDEBAR_COLOR : Color.LIGHT_GRAY, 1) // Cienki border
        ));

        // Tekst wiadomości
        JTextArea messageArea = new JTextArea(message);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 14));
        messageArea.setForeground(Color.BLACK);

        // Etykieta czasowa
        JLabel timestampLabel = new JLabel(timestamp, isOwnMessage ? SwingConstants.RIGHT : SwingConstants.LEFT);
        timestampLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        timestampLabel.setForeground(Color.GRAY);

        messageContent.add(messageArea, BorderLayout.CENTER);
        messageContent.add(timestampLabel, BorderLayout.SOUTH);

        // Ustawienie wyrównania
        if (isOwnMessage) {
            messageWrapper.add(Box.createHorizontalGlue()); // Wypycha wiadomość na prawo
            messageWrapper.add(messageContent);
            messageWrapper.add(Box.createHorizontalStrut(10)); // Prawy margines
        } else {
            messageWrapper.add(Box.createHorizontalStrut(10)); // Lewy margines
            messageWrapper.add(messageContent);
            messageWrapper.add(Box.createHorizontalGlue()); // Wypycha wiadomość na lewo
        }

        messageContent.setMaximumSize(new Dimension(messageContent.getPreferredSize().width + 10, messageContent.getPreferredSize().height + 10));

        return messageWrapper;
    }

    // 3. Wysyłanie wiadomości
    private void sendMessage(ActionEvent e) {
        String message = messageInput.getText().trim();

        if (message.isEmpty() || currentThreadId == -1) {
            return;
        }

        String sql = "INSERT INTO chat_messages (thread_id, sender_type, sender_id, message, timestamp) VALUES (?, 'worker', ?, ?, NOW())";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, currentThreadId);
            pstmt.setInt(2, loggedWorkerId);
            pstmt.setString(3, message);

            pstmt.executeUpdate();

            messageInput.setText(""); // Wyczyść pole po wysłaniu

            // Natychmiastowe odświeżenie czatu
            loadChatMessages();

        } catch (SQLException ex) {
            System.err.println("Błąd SQL podczas wysyłania wiadomości: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Błąd podczas wysyłania wiadomości.", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 4. Implementacja automatycznego odświeżania
    @Override
    public void refreshData() {
        // Odśwież listę wątków (nowe wiadomości lub wątki)
        loadThreadsList();

        // Odśwież wiadomości w aktualnie wybranym wątku
        if (currentThreadId != -1) {
            loadChatMessages();
        }
    }

    // ----------------------------------------------------------------------
    // KLASA WEWNĘTRZNA – Niestandardowy Renderer dla listy wątków
    // ----------------------------------------------------------------------
    private class ThreadListRenderer extends DefaultListCellRenderer {
        private final Border border = new EmptyBorder(5, 10, 5, 10);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            ChatThread thread = (ChatThread) value;

            // HTML dla formatowania
            String htmlText = String.format("<html><b>%s</b><br><font size=\"-1\" color=\"gray\">Utworzono: %s</font></html>",
                    thread.topic, thread.createdAt);
            label.setText(htmlText);
            label.setBorder(border);

            if (isSelected) {
                label.setBackground(SIDEBAR_COLOR.brighter());
                label.setForeground(Color.WHITE);
            } else {
                // Naprzemienne kolory wierszy
                if (index % 2 == 0) {
                    label.setBackground(Color.WHITE);
                } else {
                    label.setBackground(new Color(230, 230, 230)); // Jasnoszary
                }
                label.setForeground(Color.BLACK);
            }

            return label;
        }
    }
}