import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;

public class AcceptedCommissionsFrame extends JPanel {

    private final Connection dbConnection;
    private final Color SIDEBAR_COLOR = new Color(50, 50, 50);
    private final Color ALT_ROW_COLOR = new Color(212, 191, 255); // #bbadff

    public AcceptedCommissionsFrame(Connection conn) {
        this.dbConnection = conn;
        // Panel przyjmuje BorderLayout, aby dodać zawartość i marginesy
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(240, 240, 240));

        // 1. Nagłówek
        add(createHeader(), BorderLayout.NORTH);

        // 2. Tabela Akcji
        add(createAcceptanceTable(), BorderLayout.CENTER);
    }

    /**
     * Tworzy nagłówek panelu.
     */
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(240, 240, 240));

        JLabel title = new JLabel("Przyjęcie Zleceń");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        header.add(title, BorderLayout.NORTH);
        return header;
    }

    /**
     * Tworzy i zwraca tabelę aktywnych zleceń do przyjęcia.
     * Logika tabeli jest przeniesiona z MainFrame.
     */
    private JScrollPane createAcceptanceTable() {
        String[] columnNames = {"ID Zlecenia", "Tytuł", "Etap", "Priorytet", "Data Zlecenia"};

        // Niestandardowy model, który blokuje edycję komórek
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Zapytanie SQL: Wyświetla te same aktywne zlecenia co na dashboardzie
        String sql = "SELECT id, title, stage, priority, order_date FROM orders WHERE stage NOT IN ('Zakończone', 'Anulowane') ORDER BY CASE priority WHEN 'Wysoka' THEN 1 WHEN 'Normalna' THEN 2 WHEN 'Niska' THEN 3 ELSE 4 END, order_date ASC";

        if (dbConnection != null) {
            try (PreparedStatement pstmt = dbConnection.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("stage"),
                            rs.getString("priority"),
                            rs.getTimestamp("order_date").toLocalDateTime().toLocalDate().toString()
                    });
                }
            } catch (SQLException e) {
                System.err.println("Błąd SQL podczas ładowania listy zleceń do przyjęcia: " + e.getMessage());
            }
        }

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(25);
        table.setGridColor(new Color(220, 220, 220));

        // 1. Zmiana wyglądu nagłówka
        javax.swing.table.JTableHeader header = table.getTableHeader();
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

        // 2. Naprzemienne kolory wierszy
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        c.setBackground(ALT_ROW_COLOR); // Fioletowy #bbadff
                    }
                }

                if (column == 0) {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
                }

                c.setForeground(Color.BLACK);
                return c;
            }
        });

        // 3. Włączenie i konfiguracja sortowania
        TableRowSorter<javax.swing.table.TableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Niestandardowy comparator dla kolumny Priorytet (zachowany)
        int priorityColumnIndex = 3;
        Map<String, Integer> priorityOrder = new HashMap<>();
        priorityOrder.put("Wysoka", 1);
        priorityOrder.put("Normalna", 2);
        priorityOrder.put("Niska", 3);

        sorter.setComparator(priorityColumnIndex, (String p1, String p2) -> {
            Integer order1 = priorityOrder.getOrDefault(p1, 99);
            Integer order2 = priorityOrder.getOrDefault(p2, 99);
            return order1.compareTo(order2);
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Lista Oczekujących Zleceń do Przyjęcia"));

        return scrollPane;
    }
}
