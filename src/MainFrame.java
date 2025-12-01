// MainFrame.java (Modyfikacja)

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement; // Import potrzebny do zapytań SQL
import java.sql.ResultSet;       // Import potrzebny do wyników SQL
import java.sql.SQLException;    // Import potrzebny do obsługi błędów SQL
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

        // Używamy BorderLayout dla głównego kontenera
        setLayout(new BorderLayout());

        // 1. Lewy Panel Nawigacyjny (Sidebar)
        JPanel sideBar = createSideBar();
        add(sideBar, BorderLayout.WEST);

        // 2. Główny Panel Roboczy (Dashboard)
        JPanel mainContent = createDashboardPanel();
        add(mainContent, BorderLayout.CENTER);
    }

    // --- Metoda tworząca Panel Nawigacyjny ---
    private JPanel createSideBar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(200, 800));
        panel.setBackground(new Color(50, 50, 50));

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
        button.setBackground(new Color(60, 60, 60));
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setMargin(new Insets(10, 10, 10, 10));
        return button;
    }

    // ----------------------------------------------------------------------
    // NOWA METODA: Pobiera liczbę zleceń gotowych do przyjęcia (stage = 'Gotowe do magazynu')
    // ----------------------------------------------------------------------
    private int fetchReadyOrdersCount() {
        if (dbConnection == null) {
            System.err.println("Błąd: Obiekt połączenia z bazą danych jest null.");
            return 0;
        }

        // Zapytanie SQL zliczające zlecenia, które ukończyły produkcję i czekają na przyjęcie przez logistykę
        String sql = "SELECT COUNT(*) FROM orders WHERE stage = 'W magazynie'";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1); // Zwraca wartość z pierwszej kolumny (COUNT(*))
            }

        } catch (SQLException e) {
            System.err.println("Błąd pobierania liczby gotowych zleceń: " + e.getMessage());
        }
        return 0;
    }
    // ----------------------------------------------------------------------


    // --- Metoda tworząca Główny Panel Dashboardu ---
    // MainFrame.java (Modyfikacja metody createDashboardPanel)

    // --- Metoda tworząca Główny Panel Dashboardu ---
    // --- Metoda tworząca Główny Panel Dashboardu ---
    private JPanel createDashboardPanel() {
        // Główny kontener Dashboardu
        JPanel dashboard = new JPanel(new BorderLayout(10, 10));
        dashboard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dashboard.setBackground(new Color(240, 240, 240));

        // 1. Nagłówek (Witaj, Imię i Nazwisko)
        // Zakładamy, że ta metoda zwraca panel z JLabel "Witaj, [fullName] ([role])"
        JPanel headerPanel = createHeaderPanel();
        dashboard.add(headerPanel, BorderLayout.NORTH);

        // Wrapper dla sekcji środkowej (Wskaźniki + Wykres + Lista Akcji)
        JPanel centerWrapper = new JPanel(new BorderLayout(10, 10));
        centerWrapper.setBackground(new Color(240, 240, 240));

        // 2. Siatka dla Kluczowych Wskaźników i Diagramu (Górny rząd)
        JPanel topPanel = new JPanel(new GridLayout(1, 3, 15, 15));
        topPanel.setBackground(new Color(240, 240, 240));

        // Pobranie faktycznej liczby z bazy (metoda musi istnieć w MainFrame)
        int readyOrdersCount = fetchReadyOrdersCount();

        // A. DIAGRAM KOŁOWY
        // Użycie nowej, dedykowanej klasy OrderPieChartPanel.java
        JPanel chartPanel = new OrderPieChartPanel(dbConnection);
        topPanel.add(chartPanel);

        // B. Kluczowe Wskaźniki
        // Zlecenia gotowe do przyjęcia (liczba z bazy)
        topPanel.add(createMetricPanel("Zlecenia gotowe do przyjęcia", String.valueOf(readyOrdersCount), new Color(255, 100, 100)));
        // Zlecenia do wysyłki dzisiaj (przykład stałej wartości)
        topPanel.add(createMetricPanel("Zlecenia do wysyłki dzisiaj", "5", new Color(100, 255, 100)));

        centerWrapper.add(topPanel, BorderLayout.NORTH); // Wskaźniki i Diagram na górze

        // 3. Lista Oczekujących Akcji (Tabela)
        JScrollPane actionList = createActionList();
        centerWrapper.add(actionList, BorderLayout.CENTER); // Akcje pod wskaźnikami

        // Dodanie wrapper'a do głównego panelu dashboardu
        dashboard.add(centerWrapper, BorderLayout.CENTER);

        return dashboard;
    }

    // MainFrame.java (Dodaj do klasy MainFrame)

    // --- Metoda tworząca nagłówek z powitaniem ---
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(240, 240, 240)); // To samo tło co dashboard

        // Tytuł - Witaj, [Imię i Nazwisko]
        JLabel title = new JLabel("Witaj, " + fullName);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0)); // Dodaj mały margines dolny

        header.add(title, BorderLayout.NORTH);

        return header;
    }

    // --- Metody pomocnicze dla dashboardu ---

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

    // ZMIANA: Placeholder na panel z diagramem
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

        // Tutaj powinna być klasa rysująca faktyczny diagram (np. MyPieChartPanel)
        JLabel chartArea = new JLabel("<html><div style='text-align: center;'>[Miejsce na Diagram Kołowy JFreeChart]<br/>(Etapy z tabeli ORDERS)</div></html>", SwingConstants.CENTER);
        chartArea.setForeground(Color.GRAY);
        panel.add(chartArea, BorderLayout.CENTER);

        // Logika pobierania danych powinna być tutaj (lub w osobnym DataFetcherze)

        return panel;
    }

    private JScrollPane createActionList() {
        // Użycie JTable do wyświetlania
        String[] columnNames = {"ID Zlecenia", "Tytuł", "Etap", "Priorytet", "Akcja"};
        Object[][] data = {
                {101, "Pudełka 30x30", "Gotowe do magazynu", "Wysoka", "Przyjmij"},
                {102, "Opakowania prezentowe", "Gotowe do magazynu", "Normalna", "Przyjmij"},
                {103, "Kartony archiwizacyjne", "W magazynie", "Wysoka", "Wysyłaj"}
        };

        JTable table = new JTable(data, columnNames);
        table.setFillsViewportHeight(true);

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setBorder(BorderFactory.createTitledBorder("Oczekujące Akcje i Najpilniejsze Zlecenia"));
        tableWrapper.add(new JScrollPane(table), BorderLayout.CENTER);

        return new JScrollPane(tableWrapper);
    }
}