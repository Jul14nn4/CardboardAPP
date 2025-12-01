// OrderPieChartPanel.java

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class OrderPieChartPanel extends JPanel {

    private final Connection dbConnection;
    private final String chartTitle = "Status Zleceń (Podział na etapy)";

    public OrderPieChartPanel(Connection conn) {
        this.dbConnection = conn;
        setLayout(new BorderLayout());

        // Dodanie wykresu do panelu w konstruktorze
        JPanel chartComponent = createChartPanel();
        add(chartComponent, BorderLayout.CENTER);
    }

    /**
     * Główna metoda tworząca panel z wykresem.
     * @return JPanel zawierający wykres lub komunikat błędu.
     */
    private JPanel createChartPanel() {
        if (dbConnection == null) {
            return createErrorPanel("Brak aktywnego połączenia z bazą danych.");
        }

        DefaultPieDataset dataset = fetchChartData();

        if (dataset.getItemCount() == 0) {
            return createErrorPanel("Brak aktywnych danych o etapach zleceń.");
        }

        // Utworzenie obiektu wykresu JFreeChart
        JFreeChart chart = ChartFactory.createPieChart(
                chartTitle,
                dataset,
                true,             // Pokaż legendę
                true,             // Pokaż tooltipy
                false             // Bez URL
        );

        // Dostosowanie wyglądu
        chart.setBackgroundPaint(Color.white);
        chart.getPlot().setBackgroundPaint(Color.white);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 14));

        // Utworzenie i zwrócenie ChartPanelu (komponent Swing osadzający wykres)
        ChartPanel chartPanel = new ChartPanel(chart);
        // ZWIĘKSZONA SZEROKOŚĆ I WYSOKOŚĆ OKNA WYKRESU
        chartPanel.setPreferredSize(new Dimension(450, 300));

        return chartPanel;
    }

    /**
     * Pobiera dane z bazy danych dla wykresu kołowego.
     * @return Zbiór danych DefaultPieDataset.
     */
    private DefaultPieDataset fetchChartData() {
        DefaultPieDataset dataset = new DefaultPieDataset();

        // ZMIANA W ZAPYTANIU SQL: Usunięto 'Zakończone' z klauzuli NOT IN.
        // Zlecenia z etapem 'Zakończone' będą teraz widoczne na wykresie.
        String sql = "SELECT stage, COUNT(*) AS count FROM orders WHERE stage IS NOT NULL AND stage NOT IN ('Anulowane') GROUP BY stage ORDER BY count DESC";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String stageName = rs.getString("stage");
                int count = rs.getInt("count");
                // Dodajemy dane do Dataset: Etykieta (Nazwa etapu + liczba) oraz wartość
                dataset.setValue(stageName + " (" + count + ")", count);
            }

        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas ładowania danych wykresu: " + e.getMessage());
        }
        return dataset;
    }

    /**
     * Metoda pomocnicza do tworzenia panelu błędu.
     */
    private JPanel createErrorPanel(String message) {
        JLabel errorLabel = new JLabel(message, SwingConstants.CENTER);
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.add(errorLabel, BorderLayout.CENTER);
        errorPanel.setBackground(Color.WHITE);
        return errorPanel;
    }
}