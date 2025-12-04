import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AcceptedCommissionsFrame extends JPanel implements Refreshable{

    private final Connection dbConnection;
    private final Color SIDEBAR_COLOR = new Color(87, 50, 135);
    private final Color ALT_ROW_COLOR = new Color(212, 191, 255); // #bbadff
    private JTable acceptanceTable;
    private DefaultTableModel tableModel;

    public AcceptedCommissionsFrame(Connection conn) {
        this.dbConnection = conn;
        // Panel przyjmuje BorderLayout, aby dodać zawartość i marginesy
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(new Color(240, 240, 240));

        // 1. Nagłówek
        add(createHeader(), BorderLayout.NORTH);

        // 2. Tabela Akcji i Przycisk
        add(createContentPanel(), BorderLayout.CENTER);
    }

    /**
     * Tworzy kontener na tabelę i przycisk.
     */
    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(createAcceptanceTable(), BorderLayout.CENTER);
        contentPanel.add(createActionButton(), BorderLayout.SOUTH);
        contentPanel.setBackground(new Color(240, 240, 240));
        return contentPanel;
    }

    /**
     * Tworzy nagłówek panelu.
     */
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(240, 240, 240));

        JLabel title = new JLabel("Twoje zlecenia");
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
        // Dodano nową kolumnę "Zrealizowano" typu Boolean
        String[] columnNames = {"ID Zlecenia", "Tytuł", "Etap", "Priorytet", "Data Zlecenia", "Zrealizowano"};

        // Zmieniono model, aby obsłużyć typ Boolean dla nowej kolumny
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Pozwalamy na edycję tylko ostatniej kolumny (Zrealizowano)
                return column == 5;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Ustawiamy typ dla kolumny "Zrealizowano" na Boolean
                if (columnIndex == 5) {
                    return Boolean.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };

        acceptanceTable = new JTable(tableModel);
        loadOrdersToTable(); // Ładowanie danych przeniesione do osobnej metody

        acceptanceTable.setFillsViewportHeight(true);
        acceptanceTable.setRowHeight(25);
        acceptanceTable.setGridColor(new Color(220, 220, 220));

        // 1. Zmiana wyglądu nagłówka
        javax.swing.table.JTableHeader header = acceptanceTable.getTableHeader();
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

        // AcceptedCommissionsFrame.java (w metodzie createAcceptanceTable, ok. linii 139)

// 2. Naprzemienne kolory wierszy i wyrównanie dla wszystkich kolumn
        acceptanceTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                // Pobiera domyślny komponent (JLabel dla większości, JCheckBox dla Boolean.class)
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // ZMIANA 1: Ustawienie komponentu jako nieprzezroczystego.
                // Jest to kluczowe, aby malowanie tła zadziałało dla JCheckBox.
                if (c instanceof JComponent) {
                    ((JComponent) c).setOpaque(true);
                }

                // Sprawdzenie, czy wiersz nie jest zaznaczony
                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        // Ustawienie nowego fioletowego koloru
                        c.setBackground(ALT_ROW_COLOR); // Fioletowy #bbadff
                    }
                }

                // ZMIANA 2: Bezpieczne wyrównanie tylko dla komponentów, które są JLabelami.
                if (c instanceof JLabel) {
                    // Wyrównanie dla kolumny ID (0)
                    if (column == 0) {
                        ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                    } else {
                        ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
                    }
                }
                // Wyrównanie dla JCheckBox jest opcjonalne, ponieważ już domyślnie jest często wyśrodkowany.
                else if (c instanceof JCheckBox) {
                    ((JCheckBox) c).setHorizontalAlignment(SwingConstants.CENTER);
                }

                c.setForeground(Color.BLACK);
                return c;
            }
        });

        // Renderer i Editor dla Checkboxa - nie są wymagane dla typu Boolean, ale dla customizacji
        TableColumnModel tcm = acceptanceTable.getColumnModel();
        // Ustawienie szerokości kolumny Zrealizowano
        tcm.getColumn(5).setPreferredWidth(100);
        tcm.getColumn(5).setMaxWidth(100);

        // 3. Włączenie i konfiguracja sortowania
        TableRowSorter<javax.swing.table.TableModel> sorter = new TableRowSorter<>(tableModel);
        acceptanceTable.setRowSorter(sorter);

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

        JScrollPane scrollPane = new JScrollPane(acceptanceTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Lista Oczekujących Zleceń do Wykonania"));

        return scrollPane;
    }

    /**
     * Wczytuje dane z bazy i wypełnia nimi model tabeli.
     */
    private void loadOrdersToTable() {
        tableModel.setRowCount(0); // Wyczyść tabelę przed ponownym wczytaniem
        String sql = "SELECT id, title, stage, priority, order_date FROM orders WHERE stage = 'W magazynie' ORDER BY CASE priority WHEN 'Wysoka' THEN 1 WHEN 'Normalna' THEN 2 WHEN 'Niska' THEN 3 ELSE 4 END, order_date ASC";

        if (dbConnection != null) {
            try (PreparedStatement pstmt = dbConnection.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("stage"),
                            rs.getString("priority"),
                            rs.getTimestamp("order_date").toLocalDateTime().toLocalDate().toString(),
                            // Domyślnie checkbox niezaznaczony
                            false
                    });
                }
            } catch (SQLException e) {
                System.err.println("Błąd SQL podczas ładowania listy zleceń do przyjęcia: " + e.getMessage());
            }
        }
    }


    /**
     * Tworzy przycisk do aktualizacji statusu zleceń.
     */
    private JButton createActionButton() {
        JButton button = new JButton("Oznacz zaznaczone jako Wysłane");
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(SIDEBAR_COLOR.brighter());
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        button.addActionListener(e -> updateSelectedOrders());

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(240, 240, 240));
        buttonPanel.add(button);
        return button;
    }

    /**
     * Aktualizuje status zaznaczonych zleceń w bazie danych.
     */
    private void updateSelectedOrders() {
        List<Integer> selectedOrderIds = new ArrayList<>();
        int idColumnIndex = 0; // Kolumna z ID Zlecenia
        int checkboxColumnIndex = 5; // Kolumna z checkboxem

        // 1. Zbieranie ID zaznaczonych zleceń
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            // Konwersja indeksu widoku na indeks modelu, aby prawidłowo pobrać dane
            int modelRow = acceptanceTable.convertRowIndexToModel(i);
            Boolean isSelected = (Boolean) tableModel.getValueAt(modelRow, checkboxColumnIndex);

            if (isSelected != null && isSelected) {
                Integer orderId = (Integer) tableModel.getValueAt(modelRow, idColumnIndex);
                selectedOrderIds.add(orderId);
            }
        }

        if (selectedOrderIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nie wybrano żadnego zlecenia do aktualizacji.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 2. Aktualizacja bazy danych
        String sql = "UPDATE orders SET stage = 'Wysłane' WHERE id = ?";
        int updatedCount = 0;

        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            dbConnection.setAutoCommit(false); // Rozpocznij transakcję

            for (Integer id : selectedOrderIds) {
                pstmt.setInt(1, id);
                pstmt.addBatch();
            }

            int[] batchResults = pstmt.executeBatch();
            for (int result : batchResults) {
                if (result > 0) {
                    updatedCount += result;
                }
            }

            dbConnection.commit(); // Zatwierdź transakcję
            JOptionPane.showMessageDialog(this, String.format("Pomyślnie zaktualizowano status dla %d zleceń na 'Wysłane'.", updatedCount), "Sukces", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            try {
                if (dbConnection != null) {
                    dbConnection.rollback(); // Wycofaj transakcję w razie błędu
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Błąd podczas wycofywania transakcji: " + rollbackEx.getMessage());
            }
            System.err.println("Błąd SQL podczas aktualizacji statusu zleceń: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Wystąpił błąd podczas aktualizacji zleceń.", "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            try {
                if (dbConnection != null) {
                    dbConnection.setAutoCommit(true); // Przywróć domyślny tryb
                }
            } catch (SQLException autoCommitEx) {
                System.err.println("Błąd podczas przywracania auto-commit: " + autoCommitEx.getMessage());
            }
        }

        // 3. Odświeżenie tabeli
        loadOrdersToTable();
    }

    @Override
    public void refreshData() {

    }
}