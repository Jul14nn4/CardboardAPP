import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Chat extends JPanel implements Refreshable {

    private final Connection dbConnection;
    private final int loggedWorkerId;
    private final String loggedWorkerUsername;

    private static final Color SIDEBAR_COLOR    = new Color(87, 50, 135);
    private static final Color MESSAGE_BG_OWN   = new Color(175, 238, 238);
    private static final Color MESSAGE_BG_OTHER = new Color(240, 240, 240);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss dd-MM-yyyy");

    private JList<ChatThread> threadsList;
    private DefaultListModel<ChatThread> threadsListModel;
    private JTextArea messageInput;
    private JButton sendButton;
    private JPanel chatViewPanel;
    private JScrollPane chatScrollPane;

    private int currentThreadId       = -1;
    private String currentThreadTopic = "Brak wybranej konwersacji";
    private int currentOtherWorkerId  = -1;

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

        add(createHeader(), BorderLayout.NORTH);

        JSplitPane splitPane = createMainContentPanel();
        add(splitPane, BorderLayout.CENTER);

        loadThreadsList();

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

        sendButton.addActionListener(this::sendMessage);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel("Centrum komunikacji (czat)", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(SIDEBAR_COLOR);

        panel.add(titleLabel, BorderLayout.CENTER);
        return panel;
    }

    private JSplitPane createMainContentPanel() {
        // LEWY PANEL – lista wątków
        JPanel threadsPanel = new JPanel(new BorderLayout());
        threadsPanel.setBorder(BorderFactory.createLineBorder(SIDEBAR_COLOR));

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

        threadsListModel = new DefaultListModel<>();
        threadsList = new JList<>(threadsListModel);
        threadsList.setFont(new Font("Arial", Font.PLAIN, 14));
        threadsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        threadsList.setCellRenderer(new ThreadListRenderer());

        threadsPanel.add(new JScrollPane(threadsList), BorderLayout.CENTER);

        // PRAWY PANEL – czat
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createLineBorder(SIDEBAR_COLOR));

        chatViewPanel = new JPanel(new BorderLayout());
        chatViewPanel.setBackground(new Color(245, 245, 245));
        chatViewPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        chatPanel.add(chatViewPanel, BorderLayout.NORTH);
        // UWAGA: tutaj sendButton jest jeszcze null, więc updateChatViewPanelTitle musi to obsłużyć
        updateChatViewPanelTitle();

        chatScrollPane = new JScrollPane();
        chatScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        messageInput = new JTextArea(3, 30);
        messageInput.setLineWrap(true);
        messageInput.setWrapStyleWord(true);
        messageInput.setEnabled(false);
        JScrollPane inputScroll = new JScrollPane(messageInput);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        sendButton = new JButton("Wyślij");
        sendButton.setEnabled(false);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, threadsPanel, chatPanel);
        split.setResizeWeight(0.3);
        split.setDividerLocation(350);
        return split;
    }

    private void updateChatViewPanelTitle() {
        chatViewPanel.removeAll();
        JLabel label = new JLabel("Konwersacja: " + currentThreadTopic, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        chatViewPanel.add(label, BorderLayout.CENTER);
        chatViewPanel.revalidate();
        chatViewPanel.repaint();

        boolean enabled = currentThreadId != -1;
        // 🔥 NAPRAWA: null-checki, bo ta metoda jest wołana zanim przyciski powstaną
        if (sendButton != null) {
            sendButton.setEnabled(enabled);
        }
        if (messageInput != null) {
            messageInput.setEnabled(enabled);
        }
    }

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

                loadThreadsList();
                for (int i = 0; i < threadsListModel.getSize(); i++) {
                    if (threadsListModel.get(i).id == currentThreadId) {
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

    private void loadThreadsList() {
        threadsListModel.clear();

        if (dbConnection == null) {
            return;
        }

        String sql =
                "SELECT id, topic, created_at, worker_id, worker_id2 " +
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

        String sql =
                "SELECT sender_type, sender_id, message, timestamp " +
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
                    messagesContainer.add(Box.createVerticalStrut(4));
                }
            }
        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas ładowania wiadomości: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Błąd podczas wczytywania wiadomości:\n" + e.getMessage(),
                    "Błąd", JOptionPane.ERROR_MESSAGE);
        }

        chatScrollPane.setViewportView(messagesContainer);

        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = chatScrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private JPanel createMessagePanel(String message, String timestamp, boolean isOwn) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setOpaque(false);

        JPanel bubble = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getMaximumSize() {
                Dimension d = super.getPreferredSize();
                int maxWidth = 420; // maks szerokość dymka
                if (d.width > maxWidth) {
                    d.width = maxWidth;
                }
                return d;
            }
        };

        bubble.setBackground(isOwn ? MESSAGE_BG_OWN : MESSAGE_BG_OTHER);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(5, 10, 5, 10),
                BorderFactory.createLineBorder(isOwn ? SIDEBAR_COLOR : Color.GRAY, 1)
        ));

        JTextArea text = new JTextArea(message);
        text.setEditable(false);
        text.setOpaque(false);
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.setFont(new Font("Arial", Font.PLAIN, 14));
        text.setColumns(30);

        JLabel time = new JLabel(timestamp);
        time.setFont(new Font("Arial", Font.ITALIC, 10));
        time.setForeground(Color.GRAY);
        time.setHorizontalAlignment(SwingConstants.RIGHT);

        bubble.add(text, BorderLayout.CENTER);
        bubble.add(time, BorderLayout.SOUTH);

        if (isOwn) {
            wrapper.add(Box.createHorizontalGlue());
            wrapper.add(bubble);
        } else {
            wrapper.add(bubble);
            wrapper.add(Box.createHorizontalGlue());
        }

        wrapper.setBorder(new EmptyBorder(2, 10, 2, 10));
        return wrapper;
    }

    private void sendMessage(ActionEvent e) {
        if (currentThreadId == -1) return;

        String msg = messageInput.getText().trim();
        if (msg.isEmpty()) return;

        String sql =
                "INSERT INTO chat_messages " +
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

    @Override
    public void refreshData() {
        loadThreadsList();
        if (currentThreadId != -1) {
            loadChatMessages();
        }
    }

    private static class ThreadListRenderer extends DefaultListCellRenderer {
        private final Border border = new EmptyBorder(5, 10, 5, 10);

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            ChatThread t = (ChatThread) value;

            String text =
                    "<html><b>" + t.topic + "</b><br>" +
                            "<font size='-1' color='gray'>" +
                            t.createdAt + "</font></html>";

            label.setText(text);
            label.setBorder(border);
            label.setOpaque(true);

            if (isSelected) {
                label.setBackground(SIDEBAR_COLOR.brighter());
                label.setForeground(Color.WHITE);
            } else {
                label.setForeground(Color.BLACK);
                label.setBackground(
                        index % 2 == 0 ? Color.WHITE : new Color(230, 230, 230)
                );
            }
            return label;
        }
    }
}
