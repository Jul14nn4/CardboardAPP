// MainFrame.java

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {

    private final Connection dbConnection;
    private final String username;
    private final String role;
    private final String fullName;
    private final Color MAIN_COLOR = new Color(87, 50, 135); //97\
    private final int workerId;
    Color ALT_ROW_COLOR = new Color(212, 191, 255);
    private static final int REFRESH_INTERVAL = 3000; // 3 sekundy
    private final List<Refreshable> refreshablePanels = new ArrayList<>();

    private final JPanel contentPanel;
    private final CardLayout cardLayout = new CardLayout();

    // Stałe identyfikatory dla widoków
    private static final String DASHBOARD_VIEW = "DASHBOARD_VIEW";
    private static final String ACCEPTANCE_VIEW = "ACCEPTANCE_VIEW";
    private static final String WAREHOUSE_VIEW = "WAREHOUSE_VIEW";
    private static final String CHAT_VIEW = "CHAT_VIEW";

    public MainFrame(String username, String role, String fullName, Connection conn) {
        this.dbConnection = conn;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.workerId = getWorkerId(conn, username);

        setTitle("System Cardboard - Zalogowano jako: " + fullName + " (" + role + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // główny BorderLayout
        setLayout(new BorderLayout());

        // MainFrame.java (Fragment konstruktora)

        // 1. Lewy sidebar
        JPanel sideBar = createSideBar();
        add(sideBar, BorderLayout.WEST);

        // 2. Dashboard - NOWA IMPLEMENTACJA Z CARDLAYOUT
        contentPanel = new JPanel(cardLayout);
        add(contentPanel, BorderLayout.CENTER);

        // Dodanie widoków do CardLayout
        contentPanel.add(createDashboardPanel(), DASHBOARD_VIEW);
        // UWAGA: Wymaga istnienia klasy AcceptancePanel.java w tym samym pakiecie
        contentPanel.add(new AcceptedCommissionsFrame(dbConnection), ACCEPTANCE_VIEW);

        contentPanel.add(new StanMagazynuFrame(dbConnection), WAREHOUSE_VIEW) ;
        contentPanel.add(new Chat(dbConnection, workerId, username), CHAT_VIEW) ;

        // Wyświetl domyślnie Dashboard
        cardLayout.show(contentPanel, DASHBOARD_VIEW);
    }

    // ... reszta klasy MainFrame ...

    // ----------------------- SIDEBAR -----------------------
    private JPanel createSideBar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(200, 800));

        // Fioletowe tło
        panel.setBackground(new Color(MAIN_COLOR.getRed(), MAIN_COLOR.getGreen(), MAIN_COLOR.getBlue()));

        // LOGO w białym okręgu
        try {
            ImageIcon icon = new ImageIcon("package-variant.png");

            Image image = icon.getImage();
            Image scaledImage = image.getScaledInstance(140, 140, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);

            JLabel iconLabel = new JLabel(scaledIcon);
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setVerticalAlignment(SwingConstants.CENTER);

            CirclePanel circleContainer = new CirclePanel(iconLabel);
            circleContainer.setBackground(MAIN_COLOR);
            circleContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(Box.createRigidArea(new Dimension(0, 20)));
            panel.add(circleContainer);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));

        } catch (Exception e) {
            JLabel placeholder = new JLabel("CARDBOARD");
            placeholder.setFont(new Font("Arial", Font.BOLD, 18));
            placeholder.setForeground(Color.WHITE);
            placeholder.setAlignmentX(Component.CENTER_ALIGNMENT);
            placeholder.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));
            panel.add(placeholder);
            System.err.println("Błąd ładowania ikonki: " + e.getMessage());
        }

        // MainFrame.java (Fragment metody createSideBar)

        // Przykładowe przyciski nawigacyjne

        // NOWA IMPLEMENTACJA Z OBSŁUGĄ KLIKNIĘĆ:
        JButton dashboardBtn = createSidebarButton("Pulpit");
        JButton acceptanceBtn = createSidebarButton("Przyjęcie Zleceń");
        JButton warehouseBtn = createSidebarButton("Stan Magazynu");
        JButton chatBtn = createSidebarButton("Czat");

        // Akcja dla Pulpitu
        dashboardBtn.addActionListener(e -> showPanel(DASHBOARD_VIEW));

        // Akcja dla Przyjęcia Zleceń
        acceptanceBtn.addActionListener(e -> showPanel(ACCEPTANCE_VIEW));

        warehouseBtn.addActionListener(e -> showPanel(WAREHOUSE_VIEW));
        chatBtn.addActionListener(e -> showPanel(CHAT_VIEW));

        panel.add(dashboardBtn);
        panel.add(acceptanceBtn);
        panel.add(warehouseBtn);
        panel.add(chatBtn);
        panel.add(createSidebarButton("Raporty"));

        return panel;
    }

    private void showPanel(String panelName) {
        cardLayout.show(contentPanel, panelName);
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(180, 50));

        button.setBackground(new Color(61, 35, 94));
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setMargin(new Insets(10, 10, 10, 10));
        return button;
    }

    // ------------------- ZAPYTANIA DO BAZY -------------------

    // Zlecenia gotowe do przyjęcia (stage = 'W magazynie')
    private int fetchReadyOrdersCount() {
        if (dbConnection == null) {
            System.err.println("Błąd: Obiekt połączenia z bazą danych jest null.");
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM orders WHERE stage = 'W magazynie'";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Błąd pobierania liczby gotowych zleceń: " + e.getMessage());
        }
        return 0;
    }

    // *** NOWA METODA ***
    // Zlecenia do wysyłki dzisiaj – predicted_delivery_date = dzisiejsza data
    private int fetchTodayShipmentCount() {
        if (dbConnection == null) {
            System.err.println("Błąd: Obiekt połączenia z bazą danych jest null.");
            return 0;
        }

        // MySQL – porównujemy samą datę (bez godziny)
        String sql = """
                SELECT COUNT(*) 
                FROM orders 
                WHERE predicted_delivery_date IS NOT NULL
                  AND DATE(predicted_delivery_date) = CURDATE()
                """;

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Błąd pobierania liczby dzisiejszych wysyłek: " + e.getMessage());
        }
        return 0;
    }

    // ------------------- DASHBOARD -------------------

    private JPanel createDashboardPanel() {
        JPanel dashboard = new JPanel(new BorderLayout(10, 10));
        dashboard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dashboard.setBackground(new Color(240, 240, 240));

        // 1. Nagłówek
        JPanel headerPanel = createHeaderPanel();
        dashboard.add(headerPanel, BorderLayout.NORTH);

        // Wrapper dla środka
        JPanel centerWrapper = new JPanel(new BorderLayout(10, 10));
        centerWrapper.setBackground(new Color(240, 240, 240));

        // 2. Górny panel (wykres + wskaźniki)
        JPanel topPanel = new JPanel(new BorderLayout(15, 15));
        topPanel.setBackground(new Color(240, 240, 240));

        // Dane z bazy
        int readyOrdersCount = fetchReadyOrdersCount();
        int todayShipmentCount = fetchTodayShipmentCount();

        // A. Diagram kołowy (po lewej / center)
        JPanel chartPanel = new OrderPieChartPanel(dbConnection);
        topPanel.add(chartPanel, BorderLayout.CENTER);

        // B. Kluczowe wskaźniki (po prawej)
        JPanel metricsWrapper = new JPanel(new GridLayout(2, 1, 15, 15));
        metricsWrapper.setBackground(new Color(240, 240, 240));

        metricsWrapper.add(createMetricPanel(
                "Zlecenia gotowe do przyjęcia",
                String.valueOf(readyOrdersCount),
                new Color(255, 100, 100)
        ));

        // TU JEST ZMIANA – zamiast "5" używamy wyniku z fetchTodayShipmentCount()
        metricsWrapper.add(createMetricPanel(
                "Zlecenia do wysyłki dzisiaj",
                String.valueOf(todayShipmentCount),
                new Color(100, 255, 100)
        ));

        JPanel metricsContainer = new JPanel();
        metricsContainer.setLayout(new BoxLayout(metricsContainer, BoxLayout.Y_AXIS));
        metricsContainer.setBackground(new Color(240, 240, 240));
        metricsContainer.add(metricsWrapper);
        metricsContainer.add(Box.createVerticalGlue());

        topPanel.add(metricsContainer, BorderLayout.EAST);
        centerWrapper.add(topPanel, BorderLayout.NORTH);

        // 3. Lista zleceń / akcje
        JScrollPane actionList = createActionList();
        centerWrapper.add(actionList, BorderLayout.CENTER);

        dashboard.add(centerWrapper, BorderLayout.CENTER);

        return dashboard;
    }

    // ------------------- HEADER -------------------

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(240, 240, 240));

        JLabel title = new JLabel("Witaj, " + fullName);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        header.add(title, BorderLayout.NORTH);
        return header;
    }

    // ------------------- POMOCNICZE PANELE -------------------

    private JPanel createMetricPanel(String title, String value, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 40));
        valueLabel.setForeground(color);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        return panel;
    }
    // Dodaj metodę pomocniczą do MainFrame
    private int getWorkerId(Connection conn, String username) {
        String sql = "SELECT id FROM users_workers WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas pobierania workerId: " + e.getMessage());
        }
        return -1; // Zwróć -1 w razie błędu/braku
    }
    // (opcjonalny placeholder – aktualnie nieużywany)
    private JPanel createChartPlaceholder(String title, Connection conn) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);

        JLabel chartArea = new JLabel(
                "<html><div style='text-align: center;'>[Miejsce na Diagram Kołowy JFreeChart]<br/>(Etapy z tabeli ORDERS)</div></html>",
                SwingConstants.CENTER
        );
        chartArea.setForeground(Color.GRAY);
        panel.add(chartArea, BorderLayout.CENTER);

        return panel;
    }

    // ------------------- TABELA ZLECEŃ -------------------

    private JScrollPane createActionList() {
        String[] columnNames = {"ID Zlecenia", "Tytuł", "Etap", "Priorytet", "Data Zlecenia"};

        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

        };

        String sql = "SELECT id, title, stage, priority, order_date " +
                "FROM orders " +
                "WHERE stage NOT IN ('Zakończone', 'Anulowane') " +
                "ORDER BY " +
                "CASE priority WHEN 'Wysoka' THEN 1 WHEN 'Normalna' THEN 2 WHEN 'Niska' THEN 3 ELSE 4 END, " +
                "order_date ASC";

        if (dbConnection != null) {
            try (PreparedStatement pstmt = dbConnection.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("stage"),
                            rs.getString("priority"),
                            rs.getTimestamp("order_date")
                                    .toLocalDateTime().toLocalDate().toString()
                    });
                }
            } catch (SQLException e) {
                System.err.println("Błąd SQL podczas ładowania listy zleceń: " + e.getMessage());
                model.addRow(new Object[]{"Błąd", "Nie udało się załadować danych", "", "", ""});
            }
        } else {
            model.addRow(new Object[]{"Błąd", "Brak połączenia z bazą danych", "", "", ""});
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);

        javax.swing.table.TableRowSorter<javax.swing.table.TableModel> sorter =
                new javax.swing.table.TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // (tu możesz dodać comparator dla priorytetu, jeśli chcesz)

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Lista Aktywnych Zleceń"));
        /*Jakby, chciała zmienić kolor obramowania
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(150, 100, 255)), // Kolor linii obramowania (opcjonalnie)
                "Lista Aktywnych Zleceń (Wymagających Akcji)",
                javax.swing.border.TitledBorder.LEFT, // Pozycja tekstu
                javax.swing.border.TitledBorder.TOP, // Pozycja tekstu
                new Font("Arial", Font.BOLD, 14), // Czcionka
                new Color(97, 61, 193) // <--- KOLOR TEKSTU TITLED BORDER (ciemniejszy fiolet)
        ));*/
        // 1. Zmiana wyglądu nagłówka
        // ----------------------------------------------------
        javax.swing.table.JTableHeader header = table.getTableHeader();
        // Tutaj jest ustawiane pogrubienie i kolor tekstu:
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setForeground(Color.WHITE);
        // Tutaj jest ustawiany kolor tła nagłówka (sidebar color):
        header.setBackground(MAIN_COLOR);
        header.setPreferredSize(new Dimension(0, 30));

        // Renderer dla nagłówka (to jest kluczowe, by kolor był poprawnie malowany)
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // TE DWIE LINIE USTAWIAJĄ TŁO I KOLOR TEKSTU DLA TYTUŁÓW KOLUMN:
                label.setBackground(MAIN_COLOR);
                label.setForeground(Color.WHITE);
                label.setFont(new Font("Arial", Font.BOLD, 12));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // Sprawdzenie, czy wiersz nie jest zaznaczony
                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        // Ustawienie nowego fioletowego koloru
                        c.setBackground(ALT_ROW_COLOR);
                    }
                }

                // ... (Kod do wyrównywania i koloru tekstu) ...
                c.setForeground(Color.BLACK);
                return c;
            }
        });
        return scrollPane;
    }
    private void startGlobalRefreshTimer() {
        // Użycie javax.swing.Timer jest kluczowe, ponieważ jego akcje są wykonywane w wątku EDT.
        Timer globalTimer = new Timer(REFRESH_INTERVAL, e -> {
            // Logika wykonywana co 3 sekundy
            for (Refreshable panel : refreshablePanels) {
                // Wywołaj metodę odświeżającą w każdym panelu z listy
                panel.refreshData();
            }
        });

    }
    // ----------------------------------------------------------------------
    // KLASA WEWNĘTRZNA – biały okrąg wokół ikony w sideBar
    // ----------------------------------------------------------------------
    private class CirclePanel extends JPanel {
        private final JLabel iconLabel;

        public CirclePanel(JLabel iconLabel) {
            this.iconLabel = iconLabel;
            setPreferredSize(new Dimension(160, 160));
            setMaximumSize(new Dimension(160, 160));
            setOpaque(false);
            setLayout(new BorderLayout());
            add(iconLabel, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.WHITE);
            int margin = 1;
            int size = getWidth() - 2 * margin;
            g2d.fillOval(margin, margin, size, size);

            g2d.dispose();
        }

    }

}
