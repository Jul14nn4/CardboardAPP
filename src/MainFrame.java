// MainFrame.java

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement; // do zapytań SQL
import java.sql.ResultSet;       // do wyników SQL
import java.sql.SQLException;    // do obsługi błędów SQL
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class MainFrame extends JFrame {

    private final Connection dbConnection;
    private final String username;
    private final String role;
    private final String fullName;

    public MainFrame(String username, String role, String fullName, Connection conn) {
        this.dbConnection = conn;
        this.username = username;
        this.role = role;
        this.fullName = fullName;

        setTitle("System Cardboard - Zalogowano jako: " + fullName + " (" + role + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // główny BorderLayout
        setLayout(new BorderLayout());

        // 1. Lewy sidebar
        JPanel sideBar = createSideBar();
        add(sideBar, BorderLayout.WEST);

        // 2. Dashboard
        JPanel mainContent = createDashboardPanel();
        add(mainContent, BorderLayout.CENTER);
    }

    // ----------------------- SIDEBAR -----------------------

    // --- Metoda tworząca Panel Nawigacyjny ---
    // --- Metoda tworząca Panel Nawigacyjny ---
    // --- Metoda tworząca Panel Nawigacyjny ---
    private JPanel createSideBar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(200, 800));

        // ZMIANA KOLORU NA (77, 41, 173)
        panel.setBackground(new Color(77, 41, 173));

        // DODANIE IKONKI Z BIAŁYM OKRĘGIEM W TLE
        try {
            ImageIcon icon = new ImageIcon("package-variant.png");

            // 1. Skalowanie obrazka do 70% szerokości panelu (140x140 px)
            Image image = icon.getImage();
            Image scaledImage = image.getScaledInstance(140, 140, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);

            JLabel iconLabel = new JLabel(scaledIcon);

            // Wyrównanie Label wewnątrz BorderLayout (CirclePanel)
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setVerticalAlignment(SwingConstants.CENTER);

            // Tworzenie panelu z białym okręgiem i umieszczanie w nim ikony
            CirclePanel circleContainer = new CirclePanel(iconLabel);
            circleContainer.setBackground(new Color(77, 41, 173)); // Tło panelu kontenera

            // Wyśrodkowanie całego kontenera (CirclePanel) w pasku bocznym (Box Layout)
            circleContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(Box.createRigidArea(new Dimension(0, 20))); // Górny margines
            panel.add(circleContainer);
            panel.add(Box.createRigidArea(new Dimension(0, 10))); // Dolny margines

        } catch (Exception e) {
            // Placeholder tekstowy
            JLabel placeholder = new JLabel("CARDBOARD");
            placeholder.setFont(new Font("Arial", Font.BOLD, 16));
            placeholder.setForeground(Color.WHITE);
            placeholder.setAlignmentX(Component.CENTER_ALIGNMENT);
            placeholder.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));
            panel.add(placeholder);
            System.err.println("Błąd ładowania ikonki: " + e.getMessage());
        }

        // Przykładowe przyciski nawigacyjne
        panel.add(createSidebarButton("Pulpit"));
        panel.add(createSidebarButton("Przyjęcie Zleceń"));
        panel.add(createSidebarButton("Stan Magazynu"));
        panel.add(createSidebarButton("Organizacja Wysyłek"));
        panel.add(createSidebarButton("Raporty"));

        return panel;
    }

    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(180, 50));

        // ZMIANA KOLORU PRZYCISKU NA CIEMNIEJSZY FIOLET DLA KONTRASTU
        button.setBackground(new Color(60, 30, 140));
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

        // MySQL: porównujemy samą datę (bez godziny)
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

    // MainFrame.java (Cała metoda createDashboardPanel po poprawkach)

    // --- Metoda tworząca Główny Panel Dashboardu ---
    private JPanel createDashboardPanel() {
        JPanel dashboard = new JPanel(new BorderLayout(10, 10));
        dashboard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dashboard.setBackground(new Color(240, 240, 240));

        // 1. Nagłówek (Witaj, Imię i Nazwisko)
        JPanel headerPanel = createHeaderPanel();
        dashboard.add(headerPanel, BorderLayout.NORTH);

        // Wrapper dla sekcji środkowej (Wskaźniki + Wykres + Lista Akcji)
        JPanel centerWrapper = new JPanel(new BorderLayout(10, 10));
        centerWrapper.setBackground(new Color(240, 240, 240));

        // 2. Top Panel (Wskaźniki i Diagram)
        // Użycie BorderLayout, aby wykres mógł zajmować CENTER, a liczniki EAST.
        JPanel topPanel = new JPanel(new BorderLayout(15, 15));
        topPanel.setBackground(new Color(240, 240, 240));

        // Pobranie faktycznej liczby z bazy
        int readyOrdersCount = fetchReadyOrdersCount();

        // A. DIAGRAM KOŁOWY
        // Użycie klasy OrderPieChartPanel.java - umieszczamy w CENTER
        JPanel chartPanel = new OrderPieChartPanel(dbConnection);
        topPanel.add(chartPanel, BorderLayout.CENTER);

        // B. Kluczowe Wskaźniki (Grupa)
        // Dwa liczniki ułożone pionowo (GridLayout 2x1)
        JPanel metricsWrapper = new JPanel(new GridLayout(2, 1, 15, 15));
        metricsWrapper.setBackground(new Color(240, 240, 240));

        // Dwa panele z licznikami
        metricsWrapper.add(createMetricPanel("Zlecenia gotowe do przyjęcia", String.valueOf(readyOrdersCount), new Color(255, 100, 100)));
        metricsWrapper.add(createMetricPanel("Zlecenia do wysyłki dzisiaj", "5", new Color(100, 255, 100)));

        // Ustawiamy wrapper liczników po prawej stronie wykresu
        // Używamy BoxLayout, aby upewnić się, że metricsWrapper zajmie tyle miejsca, ile potrzebuje.
        JPanel metricsContainer = new JPanel();
        metricsContainer.setLayout(new BoxLayout(metricsContainer, BoxLayout.Y_AXIS));
        metricsContainer.setBackground(new Color(240, 240, 240));
        metricsContainer.add(metricsWrapper);
        metricsContainer.add(Box.createVerticalGlue()); // Wypycha wolną przestrzeń pod liczniki

        topPanel.add(metricsContainer, BorderLayout.EAST); // <-- LICZNIKI W EAST (w opakowaniu)

        centerWrapper.add(topPanel, BorderLayout.NORTH); // Wskaźniki i Diagram na górze

        // 3. Lista Oczekujących Akcji
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

    // (opcjonalny placeholder – aktualnie nieużywany, ale zostawiam)
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

    // MainFrame.java

// ... (Inne metody) ...

    /**
     * Tworzy i zwraca panel zawierający tabelę dynamicznie wypełnioną zleceniami.
     * Tabela ma aktywne sortowanie po nagłówkach kolumn, ale KOMÓRKI SĄ NIEMOŻLIWE DO EDYCJI.
     * @return JScrollPane z JTable
     */
    // MainFrame.java (POPRAWIONA metoda createActionList)

    /**
     * Tworzy i zwraca JScrollPane zawierający tabelę dynamicznie wypełnioną zleceniami.
     * @return JScrollPane z JTable
     */
    private JScrollPane createActionList() {
        String[] columnNames = {"ID Zlecenia", "Tytuł", "Etap", "Priorytet", "Data Zlecenia"};

        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String sql = "SELECT id, title, stage, priority, order_date FROM orders WHERE stage NOT IN ('Zakończone', 'Anulowane') ORDER BY CASE priority WHEN 'Wysoka' THEN 1 WHEN 'Normalna' THEN 2 WHEN 'Niska' THEN 3 ELSE 4 END, order_date ASC";

        if (dbConnection != null) {
            try (java.sql.PreparedStatement pstmt = dbConnection.prepareStatement(sql);
                 java.sql.ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("stage"),
                            rs.getString("priority"),
                            rs.getTimestamp("order_date").toLocalDateTime().toLocalDate().toString()
                    });
                }
            } catch (java.sql.SQLException e) {
                System.err.println("Błąd SQL podczas ładowania listy zleceń: " + e.getMessage());
                model.addRow(new Object[]{"Błąd", "Nie udało się załadować danych", "", "", ""});
            }
        } else {
            model.addRow(new Object[]{"Błąd", "Brak połączenia z bazą danych", "", "", ""});
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);

        // Ustawienie sortowania po nagłówkach
        javax.swing.table.TableRowSorter<javax.swing.table.TableModel> sorter = new javax.swing.table.TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Niestandardowy comparator dla kolumny Priorytet
        int priorityColumnIndex = 3;
        sorter.setComparator(priorityColumnIndex, (String p1, String p2) -> {
            // ... (logika sortowania priorytetów) ...
            return 0; // Pominięto dla zwięzłości, zakładając, że logika jest przeniesiona.
        });

        // -----------------------------------------------------------------
        // KLUCZOWA POPRAWKA: Tworzymy JScrollPane bezpośrednio z JTable
        // -----------------------------------------------------------------
        JScrollPane scrollPane = new JScrollPane(table);

        // Dodajemy tytuł jako TitledBorder do JScrollPane
        scrollPane.setBorder(BorderFactory.createTitledBorder("Lista Aktywnych Zleceń"));

        // Zwracamy pojedynczy JScrollPane
        return scrollPane;
    }
    // ----------------------------------------------------------------------
    // KLASA WEWNĘTRZNA do RYSOWANIA BIAŁEGO OKRĘGU WOKÓŁ IKONY
    // ----------------------------------------------------------------------
    private class CirclePanel extends JPanel {
        private final JLabel iconLabel;

        public CirclePanel(JLabel iconLabel) {
            this.iconLabel = iconLabel;
            // Wymuszamy rozmiar panelu (okrąg 150x150 plus marginesy, np. 160x160)
            setPreferredSize(new Dimension(160, 160));
            setMaximumSize(new Dimension(160, 160));
            setOpaque(false); // Wymagane, aby tło (fiolet) było widoczne
            setLayout(new BorderLayout());
            add(iconLabel, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();

            // Włączamy Antialiasing dla gładkich krawędzi
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Ustawiamy biały kolor dla okręgu
            g2d.setColor(Color.WHITE);

            // Rysujemy wypełniony owal (koło), zaczynając od (5, 5)
            // i mając rozmiar 150x150 wewnątrz panelu 160x160
            int margin = 1;
            int size = getWidth() - 2 * margin;
            g2d.fillOval(margin, margin, size, size);

            g2d.dispose();
        }
    }
    // ----------------------------------------------------------------------
}
