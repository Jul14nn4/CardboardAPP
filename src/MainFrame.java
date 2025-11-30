// MainFrame.java (Modyfikacja)

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
// Importy dla logiki SQL będą w osobnym module

public class MainFrame extends JFrame {

    private final Connection dbConnection;
    private final String username;
    private final String role;
    private final String fullName;

    // ZMIANA: Dodano argument Connection i fullName
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

    // --- Metoda tworząca Główny Panel Dashboardu ---
    private JPanel createDashboardPanel() {
        JPanel dashboard = new JPanel(new BorderLayout(10, 10));
        dashboard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dashboard.setBackground(new Color(240, 240, 240));

        // Tytuł
        JLabel title = new JLabel("Witaj, " + fullName );
        title.setFont(new Font("Arial", Font.BOLD, 24));
        dashboard.add(title, BorderLayout.NORTH);

        // Siatka dla Kluczowych Wskaźników i Diagramu
        JPanel topPanel = new JPanel(new GridLayout(1, 3, 15, 15));

        // A. DIAGRAM KOŁOWY (Zlicza statusy z orders)
        JPanel chartPanel = createChartPlaceholder("Raport Statusów Zleceń (Stage)", dbConnection);
        topPanel.add(chartPanel);

        // B. Kluczowe Wskaźniki (Przykład)
        topPanel.add(createMetricPanel("Zlecenia gotowe do przyjęcia", "25", new Color(255, 100, 100)));
        topPanel.add(createMetricPanel("Zlecenia do wysyłki dzisiaj", "5", new Color(100, 255, 100)));

        dashboard.add(topPanel, BorderLayout.CENTER); // Zmieniono na Center, aby lista Akcji była na dole

        // C. Lista Oczekujących Akcji
        JScrollPane actionList = createActionList();
        dashboard.add(actionList, BorderLayout.SOUTH);

        return dashboard;
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