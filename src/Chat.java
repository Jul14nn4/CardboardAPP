import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    // ZMIANA TUTAJ: Klasa musi być publiczna, aby NewThreadDialog mogło jej użyć.
    public static class ChatThread {
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

        // 1. Nagłówek
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
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Centrum Komunikacji (Czat)", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(SIDEBAR_COLOR);

        headerPanel.add(titleLabel, BorderLayout.CENTER);

        return headerPanel;
    }

    // Metoda tworząca główną zawartość (zawiera przycisk "Nowa konwersacja")
    private JSplitPane createMainContentPanel() {

        // --- Lewy panel: Historia rozmów (threadsPanel) ---
        JPanel threadsPanel = new JPanel(new BorderLayout());
        threadsPanel.setBorder(BorderFactory.createLineBorder(SIDEBAR_COLOR));

        // NOWA SEKCJA: Nagłówek dla listy wątków (zawierający przycisk "Nowa konwersacja")
        JPanel threadsHeaderPanel = new JPanel(new BorderLayout());
        threadsHeaderPanel.setBackground(Color.WHITE);
        threadsHeaderPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Marginesy

        // Tytuł listy
        JLabel threadsTitle = new JLabel("Historia rozmów");
        threadsTitle.setFont(new Font("Arial", Font.BOLD, 16));
        threadsTitle.setForeground(SIDEBAR_COLOR);
        threadsHeaderPanel.add(threadsTitle, BorderLayout.WEST);

        // Przycisk "Nowa konwersacja"
        JButton newConversationButton = new JButton("Nowa konwersacja");
        newConversationButton.setFont(new Font("Arial", Font.PLAIN, 12));
        newConversationButton.setBackground(new Color(150, 150, 250));
        newConversationButton.setForeground(Color.WHITE);
        newConversationButton.addActionListener(e -> showNewThreadDialog());

        threadsHeaderPanel.add(newConversationButton, BorderLayout.EAST);

        threadsPanel.add(threadsHeaderPanel, BorderLayout.NORTH); // Dodanie nagłówka na górę

        // Inicjalizacja listy wątków
        threadsListModel = new DefaultListModel<>();
        threadsList = new JList<>(threadsListModel);
        threadsList.setFont(new Font("Arial", Font.PLAIN, 14));
        threadsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        threadsList.setCellRenderer(new ThreadListRenderer());

        threadsPanel.add(new JScrollPane(threadsList), BorderLayout.CENTER);

        // --- Prawy panel: Widok Czatu (chatPanel) ---
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createLineBorder(SIDEBAR_COLOR));

        // Panel na nagłówek czatu (wybrany topic)
        chatViewPanel = new JPanel(new BorderLayout());
        chatViewPanel.setBackground(Color.LIGHT_GRAY);
        chatViewPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        chatPanel.add(chatViewPanel, BorderLayout.NORTH);

        // Obszar na wiadomości
        chatScrollPane = new JScrollPane();
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Panel do wpisywania wiadomości
        JPanel inputPanel = createInputPanel(); // inicjalizuje messageInput i sendButton
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // PRAWIDŁOWE MIEJSCE: Wywołanie aktualizacji panelu po zainicjalizowaniu wszystkich komponentów
        updateChatViewPanelTitle();

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

        if (sendButton != null) {
            sendButton.setEnabled(currentThreadId != -1);
        }
        if (messageInput != null) {
            messageInput.setEnabled(currentThreadId != -1);
        }
    }

    // NOWA METODA: Pokazuje okno dialogowe do tworzenia nowego wątku
    private void showNewThreadDialog() {
        NewThreadDialog dialog = new NewThreadDialog(
                SwingUtilities.getWindowAncestor(this),
                dbConnection,
                loggedWorkerId,
                loggedWorkerUsername
        );
        dialog.setVisible(true);

        if (dialog.isThreadCreated()) {
            ChatThread newThread = dialog.getCreatedThread();
            if (newThread != null) {
                currentThreadId = newThread.id;
                currentThreadTopic = newThread.topic;

                loadThreadsList();

                for (int i = 0; i < threadsListModel.getSize(); i++) {
                    if (threadsListModel.getElementAt(i).id == currentThreadId) {
                        threadsList.setSelectedIndex(i);
                        break;
                    }
                }

                updateChatViewPanelTitle();
                loadChatMessages();
                messageInput.requestFocusInWindow();
            }
        }
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
                    messagesContainer.add(Box.createVerticalStrut(5));
                }
            }
        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas ładowania wiadomości: " + e.getMessage());
        }

        chatScrollPane.setViewportView(messagesContainer);

        JScrollBar verticalScrollBar = chatScrollPane.getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> verticalScrollBar.setValue(verticalScrollBar.getMaximum()));
    }

    // Metoda do tworzenia panelu pojedynczej wiadomości
    private JPanel createMessagePanel(String message, String timestamp, boolean isOwnMessage) {
        JPanel messageWrapper = new JPanel();
        messageWrapper.setLayout(new BoxLayout(messageWrapper, BoxLayout.X_AXIS));
        messageWrapper.setOpaque(false);

        JPanel messageContent = new JPanel(new BorderLayout());
        messageContent.setBackground(isOwnMessage ? MESSAGE_BG_OWN : MESSAGE_BG_OTHER);
        messageContent.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(5, 10, 5, 10),
                BorderFactory.createLineBorder(isOwnMessage ? SIDEBAR_COLOR : Color.LIGHT_GRAY, 1)
        ));

        messageContent.setMaximumSize(messageContent.getPreferredSize());


        JTextArea messageArea = new JTextArea(message);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setFont(new Font("Arial", Font.PLAIN, 14));
        messageArea.setForeground(Color.BLACK);
        messageArea.setBorder(new EmptyBorder(0, 0, 5, 0));

        JLabel timestampLabel = new JLabel(timestamp, isOwnMessage ? SwingConstants.RIGHT : SwingConstants.LEFT);
        timestampLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        timestampLabel.setForeground(Color.GRAY);

        messageContent.add(messageArea, BorderLayout.CENTER);
        messageContent.add(timestampLabel, BorderLayout.SOUTH);

        if (isOwnMessage) {
            messageWrapper.add(Box.createHorizontalGlue());
            messageWrapper.add(messageContent);
            messageWrapper.add(Box.createHorizontalStrut(10));
            messageWrapper.setAlignmentX(Component.RIGHT_ALIGNMENT);
        } else {
            messageWrapper.add(Box.createHorizontalStrut(10));
            messageWrapper.add(messageContent);
            messageWrapper.add(Box.createHorizontalGlue());
            messageWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

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

            messageInput.setText("");

            loadChatMessages();

        } catch (SQLException ex) {
            System.err.println("Błąd SQL podczas wysyłania wiadomości: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Błąd podczas wysyłania wiadomości.", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 4. Implementacja automatycznego odświeżania (wywoływana co 3 sekundy)
    @Override
    public void refreshData() {
        loadThreadsList();

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

            String htmlText = String.format("<html><b>%s</b><br><font size=\"-1\" color=\"gray\">Utworzono: %s</font></html>",
                    thread.topic, thread.createdAt);
            label.setText(htmlText);
            label.setBorder(border);

            if (isSelected) {
                label.setBackground(SIDEBAR_COLOR.brighter());
                label.setForeground(Color.WHITE);
            } else {
                if (index % 2 == 0) {
                    label.setBackground(Color.WHITE);
                } else {
                    label.setBackground(new Color(230, 230, 230));
                }
                label.setForeground(Color.BLACK);
            }

            return label;
        }
    }
}