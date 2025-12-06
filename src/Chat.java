import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Interfejs Refreshable musi być zaimplementowany w MainFrame
public class Chat extends JPanel implements Refreshable {

    private final Connection dbConnection;
    private final int loggedWorkerId;
    private final String loggedWorkerUsername;

    private static final Color SIDEBAR_COLOR    = new Color(87, 50, 135);
    private static final Color MESSAGE_BG_OWN   = new Color(175, 238, 238);
    private static final Color MESSAGE_BG_OTHER = new Color(240, 240, 240);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

    // UI
    private JList<ChatThread> threadsList;
    private DefaultListModel<ChatThread> threadsListModel;
    private JTextArea messageInput;
    private JButton sendButton;
    private JPanel chatViewPanel;
    private JScrollPane chatScrollPane;

    // aktualnie wybrany wątek
    private int currentThreadId    = -1;
    private String currentThreadTopic = "Brak wybranej konwersacji";
    private int currentOtherWorkerId  = -1;

    // Reprezentacja wątku (lewa lista)
    public static class ChatThread {
        public int id;
        public String topic;
        public String createdAt;
        public int otherWorkerId;

        public ChatThread(int id, String topic, String createdAt, int otherWorkerId) {
            this.id = id;
            this.topic = topic;
            this.createdAt = createdAt;
            this.otherWorkerId = otherWorkerId;
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

        // 2. Główna część (lewa: lista wątków, prawa: czat)
        JSplitPane splitPane = createMainContentPanel();
        add(splitPane, BorderLayout.CENTER);

        // 3. Załaduj wątki
        loadThreadsList();

        // Reakcja na wybór wątku
        threadsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && threadsList.getSelectedValue() != null) {
                ChatThread selected = threadsList.getSelectedValue();
                if (selected.id != currentThreadId) {
                    currentThreadId = selected.id;
                    currentThreadTopic = selected.topic;
                    currentOtherWorkerId = selected.otherWorkerId;
                    updateChatViewPanelTitle();
                    loadChatMessages();
                }
            }
        });

        // Reakcja na przycisk "Wyślij"
        sendButton.addActionListener(this::sendMessage);
    }

    // ------------------- NAGŁÓWEK -------------------
    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel("Centrum komunikacji (czat)", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(SIDEBAR_COLOR);

        panel.add(titleLabel, BorderLayout.CENTER);
        return panel;
    }

    // ------------------- GŁÓWNA ZAWARTOŚĆ -------------------
    private JSplitPane createMainContentPanel() {

        // ---- LEWY PANEL: lista wątków ----
        JPanel threadsPanel = new JPanel(new BorderLayout());
        threadsPanel.setBorder(BorderFactory.createLineBorder(SIDEBAR_COLOR));

        // nagłówek z przyciskiem "Nowa konwersacja"
        JPanel threadsHeader = new JPanel(new BorderLayout());
        threadsHeader.setBackground(Color.WHITE);
        threadsHeader.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel threadsTitle = new JLabel("Historia rozmów");
        threadsTitle.setFont(new Font("Arial", Font.BOLD, 16));
        threadsTitle.setForeground(SIDEBAR_COLOR);
        threadsHeader.add(threadsTitle, BorderLayout.WEST);

        JButton newThreadButton = new JButton("Nowa konwersacja");
        newThreadButton.setFont(new Font("Arial", Font.PLAIN, 12));
        newThreadButton.setBackground(new Color(150, 150, 250));
        newThreadButton.setForeground(Color.WHITE);
        newThreadButton.addActionListener(e -> showNewThreadDialog());
        threadsHeader.add(newThreadButton, BorderLayout.EAST);

        threadsPanel.add(threadsHeader, BorderLayout.NORTH);

        // lista wątków
        threadsListModel = new DefaultListModel<>();
        threadsList = new JList<>(threadsListModel);
        threadsList.setFont(new Font("Arial", Font.PLAIN, 14));
        threadsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        threadsList.setCellRenderer(new ThreadListRenderer());

        threadsPanel.add(new JScrollPane(threadsList), BorderLayout.CENTER);

        // ---- PRAWY PANEL: czat ----
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createLineBorder(SIDEBAR_COLOR));

        // nagłówek czatu
        chatViewPanel = new JPanel(new BorderLayout());
        chatViewPanel.setBackground(new Color(245, 245, 245));
        chatViewPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        chatPanel.add(chatViewPanel, BorderLayout.NORTH);
        updateChatViewPanelTitle();

        // obszar wiadomości
        chatScrollPane = new JScrollPane();
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        // panel wejściowy
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        messageInput = new JTextArea(3, 30);
        messageInput.setLineWrap(true);
        messageInput.setWrapStyleWord(true);
        messageInput.setEnabled(false);
        JScrollPane inputScroll = new JScrollPane(messageInput);

        sendButton = new JButton("Wyślij");
        sendButton.setEnabled(false);

        inputPanel.add(inputScroll, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // ---- SPLITPANE ----
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, threadsPanel, chatPanel);
        split.setResizeWeight(0.3);
        split.setDividerLocation(350);
        return split;
    }

    // ------------------- AKTUALIZACJA TYTUŁU CZATU -------------------
    private void updateChatViewPanelTitle() {
        chatViewPanel.removeAll();
        JLabel label = new JLabel("Konwersacja: " + currentThreadTopic, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        chatViewPanel.add(label, BorderLayout.CENTER);
        chatViewPanel.revalidate();
        chatViewPanel.repaint();

        boolean enabled = currentThreadId != -1;
        if (sendButton != null)   sendButton.setEnabled(enabled);
        if (messageInput != null) messageInput.setEnabled(enabled);
    }

    // ------------------- NOWY WĄTEK -------------------
    private void showNewThreadDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        NewThreadDialog dialog = new NewThreadDialog(owner, dbConnection, loggedWorkerId, loggedWorkerUsername);
        dialog.setVisible(true);

        if (dialog.isThreadCreated()) {
            ChatThread newThread = dialog.getCreatedThread();
            if (newThread != null) {
                currentThreadId = newThread.id;
                currentThreadTopic = newThread.topic;
                currentOtherWorkerId = newThread.otherWorkerId;

                // przeładuj listę, zaznacz nowy wątek
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

    // ------------------- BAZA: WĄTKI -------------------
    private void loadThreadsList() {
        threadsListModel.clear();

        if (dbConnection == null) {
            return;
        }

        String sql = "SELECT id, topic, created_at, worker_id, worker_id2 " +
                "FROM chat_threads " +
                "WHERE worker_id = ? OR worker_id2 = ? " +
                "ORDER BY created_at DESC";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, loggedWorkerId);
            pstmt.setInt(2, loggedWorkerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String topic = rs.getString("topic");
                    Timestamp ts = rs.getTimestamp("created_at");
                    String createdAt = ts != null
                            ? ts.toLocalDateTime().format(DATE_FORMATTER)
                            : "";

                    int w1 = rs.getInt("worker_id");
                    int w2 = rs.getInt("worker_id2");
                    int other = (w1 == loggedWorkerId) ? w2 : w1;

                    threadsListModel.addElement(
                            new ChatThread(id, topic, createdAt, other)
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas ładowania listy wątków: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Błąd podczas wczytywania listy rozmów:\n" + e.getMessage(),
                    "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------- BAZA: WIADOMOŚCI -------------------
    private void loadChatMessages() {
        JPanel messagesContainer = new JPanel();
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(Color.WHITE);

        if (currentThreadId == -1) {
            JLabel info = new JLabel("Wybierz konwersację z listy po lewej.", SwingConstants.CENTER);
            info.setBorder(new EmptyBorder(20, 10, 20, 10));
            messagesContainer.add(info);
            chatScrollPane.setViewportView(messagesContainer);
            return;
        }

        String sql = "SELECT sender_type, sender_id, message, timestamp " +
                "FROM chat_messages " +
                "WHERE thread_id = ? " +
                "ORDER BY timestamp ASC";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, currentThreadId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String senderType = rs.getString("sender_type");
                    int senderId      = rs.getInt("sender_id");
                    String msg        = rs.getString("message");
                    LocalDateTime ts  = rs.getTimestamp("timestamp").toLocalDateTime();

                    boolean isOwn = "worker".equals(senderType) && senderId == loggedWorkerId;

                    messagesContainer.add(
                            createMessagePanel(msg, ts.format(DATE_FORMATTER), isOwn)
                    );
                    messagesContainer.add(Box.createVerticalStrut(5));
                }
            }
        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas ładowania wiadomości: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Błąd podczas wczytywania wiadomości:\n" + e.getMessage(),
                    "Błąd", JOptionPane.ERROR_MESSAGE);
        }

        chatScrollPane.setViewportView(messagesContainer);

        // przewinięcie na dół
        JScrollBar bar = chatScrollPane.getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum()));
    }

    // Tworzenie pojedynczego "dymka" wiadomości
    private JPanel createMessagePanel(String message, String timestamp, boolean isOwnMessage) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setOpaque(false);

        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setBackground(isOwnMessage ? MESSAGE_BG_OWN : MESSAGE_BG_OTHER);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(5, 10, 5, 10),
                BorderFactory.createLineBorder(isOwnMessage ? SIDEBAR_COLOR : Color.LIGHT_GRAY, 1)
        ));

        JTextArea msgArea = new JTextArea(message);
        msgArea.setWrapStyleWord(true);
        msgArea.setLineWrap(true);
        msgArea.setEditable(false);
        msgArea.setOpaque(false);
        msgArea.setFont(new Font("Arial", Font.PLAIN, 14));
        msgArea.setBorder(new EmptyBorder(0, 0, 5, 0));

        JLabel timeLabel = new JLabel(timestamp,
                isOwnMessage ? SwingConstants.RIGHT : SwingConstants.LEFT);
        timeLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        timeLabel.setForeground(Color.GRAY);

        bubble.add(msgArea, BorderLayout.CENTER);
        bubble.add(timeLabel, BorderLayout.SOUTH);

        if (isOwnMessage) {
            wrapper.add(Box.createHorizontalGlue());
            wrapper.add(bubble);
            wrapper.add(Box.createHorizontalStrut(10));
            wrapper.setAlignmentX(Component.RIGHT_ALIGNMENT);
        } else {
            wrapper.add(Box.createHorizontalStrut(10));
            wrapper.add(bubble);
            wrapper.add(Box.createHorizontalGlue());
            wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        return wrapper;
    }

    // ------------------- WYSYŁANIE WIADOMOŚCI -------------------
    private void sendMessage(ActionEvent e) {
        String msg = messageInput.getText().trim();
        if (msg.isEmpty() || currentThreadId == -1) {
            return;
        }

        String sql = "INSERT INTO chat_messages " +
                "(thread_id, sender_type, sender_id, message, timestamp) " +
                "VALUES (?, 'worker', ?, ?, NOW())";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setInt(1, currentThreadId);
            pstmt.setInt(2, loggedWorkerId);
            pstmt.setString(3, msg);
            pstmt.executeUpdate();

            messageInput.setText("");
            loadChatMessages();

        } catch (SQLException ex) {
            System.err.println("Błąd SQL podczas wysyłania wiadomości: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Błąd podczas wysyłania wiadomości:\n" + ex.getMessage(),
                    "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------- AUTO-REFRESH -------------------
    @Override
    public void refreshData() {
        loadThreadsList();
        if (currentThreadId != -1) {
            loadChatMessages();
        }
    }

    // ------------------- RENDERER LISTY WĄTKÓW -------------------
    private static class ThreadListRenderer extends DefaultListCellRenderer {
        private final Border border = new EmptyBorder(5, 10, 5, 10);

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            ChatThread thread = (ChatThread) value;
            String text = String.format(
                    "<html><b>%s</b><br><font size='-1' color='gray'>Utworzono: %s</font></html>",
                    thread.topic, thread.createdAt
            );

            label.setText(text);
            label.setBorder(border);

            if (isSelected) {
                label.setBackground(SIDEBAR_COLOR.brighter());
                label.setForeground(Color.WHITE);
            } else {
                label.setForeground(Color.BLACK);
                label.setBackground(index % 2 == 0 ? Color.WHITE : new Color(230, 230, 230));
            }

            return label;
        }
    }
}
