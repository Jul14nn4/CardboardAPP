// OrderPieChartPanel.java

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.plot.PiePlot;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.chart.plot.PieLabelLinkStyle;

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
    private final String chartTitle = "Status Zleceń";

    public OrderPieChartPanel(Connection conn) {
        this.dbConnection = conn;
        setLayout(new BorderLayout());

        JPanel chartComponent = createChartPanel();
        add(chartComponent, BorderLayout.CENTER);
    }

    /**
     * Główna metoda tworząca panel z wykresem.
     */
    private JPanel createChartPanel() {
        if (dbConnection == null) {
            return createErrorPanel("Brak aktywnego połączenia z bazą danych.");
        }

        DefaultPieDataset dataset = fetchChartData();

        if (dataset.getItemCount() == 0) {
            return createErrorPanel("Brak aktywnych danych o etapach zleceń.");
        }

        // 1. Utworzenie obiektu wykresu JFreeChart
        JFreeChart chart = ChartFactory.createPieChart(
                chartTitle,
                dataset,
                true,             // Pokaż legendę
                true,             // Pokaż tooltipy
                false             // Bez URL
        );

        // 2. DOSTOSOWANIE: Wizualne i zapobieganie łamaniu słów

        PiePlot plot = (PiePlot) chart.getPlot();

        // Opcje kontrolujące łamanie tekstu w etykietach
        chart.getLegend().setHorizontalAlignment(HorizontalAlignment.LEFT);
        plot.setLabelLinkStyle(PieLabelLinkStyle.STANDARD); // Włączenie standardowego mechanizmu łamania
        plot.setLabelFont(new Font("Arial", Font.PLAIN, 10));
        plot.setLabelLinkMargin(0.1);

        // Ustawienia wizualne
        chart.setBackgroundPaint(Color.white);
        chart.getPlot().setBackgroundPaint(Color.white);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 14));

        // 3. Utworzenie ChartPanelu
        ChartPanel chartPanel = new ChartPanel(chart);
        // ZWIĘKSZONA SZEROKOŚĆ OKNA WYKRESU (600 x 300)
        chartPanel.setPreferredSize(new Dimension(600, 300));

        return chartPanel;
    }

    /**
     * Pobiera dane z bazy danych dla wykresu kołowego.
     */
    private DefaultPieDataset fetchChartData() {
        DefaultPieDataset dataset = new DefaultPieDataset();

        // Zapytanie SQL zlicza i grupuje zlecenia wg etapu (stage).
        // Wykluczono tylko 'Anulowane' (status 'Zakończone' jest teraz widoczny).
        String sql = "SELECT stage, COUNT(*) AS count FROM orders WHERE stage IS NOT NULL AND stage NOT IN ('Anulowane') GROUP BY stage ORDER BY count DESC";

        try (Statement stmt = dbConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String stageName = rs.getString("stage");
                int count = rs.getInt("count");
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