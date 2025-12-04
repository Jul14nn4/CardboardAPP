// StanMagazynuFrame.java

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StanMagazynuFrame extends JPanel implements Refreshable {

    private final Connection dbConnection;
    private final Color SIDEBAR_COLOR = new Color(87, 50, 135);
    private final Color ALT_ROW_COLOR = new Color(212, 191, 255);
    private JTable inventoryTable;
    private DefaultTableModel tableModel;

    public StanMagazynuFrame(Connection conn) {
        this.dbConnection = conn;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(240, 240, 240));

        add(createHeader(), BorderLayout.NORTH);
        add(createInventoryTablePanel(), BorderLayout.CENTER);

        refreshData(); // Załaduj dane przy starcie
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(240, 240, 240));

        JLabel title = new JLabel("Stan Magazynu");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        header.add(title, BorderLayout.NORTH);
        return header;
    }

    private JScrollPane createInventoryTablePanel() {
        // Kolumny: Nazwa kartonu (boxes) i Łączna ilość (SUM(amount))
        String[] columnNames = {"Nazwa Kartonu", "Łączna Ilość (szt.)"};

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        inventoryTable = new JTable(tableModel);
        inventoryTable.setFillsViewportHeight(true);
        inventoryTable.setRowHeight(25);
        inventoryTable.setGridColor(new Color(220, 220, 220));

        // Styling nagłówka tabeli (zgodny z innymi widokami)
        javax.swing.table.JTableHeader header = inventoryTable.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setForeground(Color.WHITE);
        header.setBackground(SIDEBAR_COLOR);
        header.setPreferredSize(new Dimension(0, 30));

        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setBackground(SIDEBAR_COLOR);
                label.setForeground(Color.WHITE);
                label.setFont(new Font("Arial", Font.BOLD, 12));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });

        // Styling komórek tabeli (zgodny z innymi widokami)
        inventoryTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        c.setBackground(ALT_ROW_COLOR);
                    }
                }
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                c.setForeground(Color.BLACK);
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Bieżące kartony w magazynie"));

        return scrollPane;
    }

    @Override
    public void refreshData() {
        tableModel.setRowCount(0);

        // KLUCZOWE ZAPYTANIE SQL:
        // 1. Wybierz nazwę kartonu (boxes) i sumę ilości (SUM(amount))
        // 2. Tylko dla zleceń o etapie 'W magazynie'
        // 3. Ignoruj wiersze, gdzie 'boxes' lub 'amount' są puste
        // 4. Grupuj po nazwie kartonu
        // 5. Pokazuj tylko te, gdzie suma jest większa niż 0
        String sql = "SELECT boxes, SUM(amount) AS total_amount FROM orders WHERE stage = 'W magazynie' AND boxes IS NOT NULL AND amount IS NOT NULL GROUP BY boxes HAVING SUM(amount) > 0 ORDER BY total_amount DESC";

        if (dbConnection != null) {
            try (PreparedStatement pstmt = dbConnection.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    String boxName = rs.getString("boxes");
                    int totalAmount = rs.getInt("total_amount");

                    tableModel.addRow(new Object[]{
                            boxName,
                            totalAmount
                    });
                }
            } catch (SQLException e) {
                System.err.println("Błąd SQL podczas ładowania stanu magazynu: " + e.getMessage());
            }
        }
    }
}