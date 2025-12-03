import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderProcessor {

    // Wyrażenie regularne do parsowania wzorca: [NAZWA] [LICZBA]szt
    private static final Pattern TITLE_PATTERN = Pattern.compile("(.+)\\s(\\d+)szt$");

    /**
     * Parsuje tytuł zlecenia i aktualizuje kolumny 'boxes' i 'amount' w bazie.
     * Metoda powinna być wywołana po wstawieniu nowego zlecenia do bazy danych.
     *
     * @param conn Aktywne połączenie z bazą danych.
     * @param orderId ID zlecenia, które ma zostać zaktualizowane.
     * @param title Tytuł zlecenia do sparsowania.
     */
    public static void processTitleAndSetBoxesAmount(Connection conn, int orderId, String title) {

        Matcher matcher = TITLE_PATTERN.matcher(title);

        if (!matcher.find()) {
            // Jeśli tytuł nie pasuje do wzorca, kończymy działanie (nie ma nic do przetworzenia)
            return;
        }

        try {
            // Wyodrębnienie danych z tytułu (Grupa 1: boxes, Grupa 2: amount)
            String boxes = matcher.group(1).trim();
            int amount = Integer.parseInt(matcher.group(2));

            // --- 1. Modyfikacja kolumny 'boxes' (osobna instrukcja) ---
            String updateBoxesSql = "UPDATE orders SET boxes = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateBoxesSql)) {
                pstmt.setString(1, boxes);
                pstmt.setInt(2, orderId);
                pstmt.executeUpdate();
            }

            // --- 2. Modyfikacja kolumny 'amount' (osobna instrukcja) ---
            String updateAmountSql = "UPDATE orders SET amount = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateAmountSql)) {
                pstmt.setInt(1, amount);
                pstmt.setInt(2, orderId);
                pstmt.executeUpdate();
            }

            System.out.println("Zlecenie ID " + orderId + " zostało przetworzone (boxes i amount uzupełnione).");

        } catch (SQLException e) {
            System.err.println("Błąd SQL podczas aktualizacji kolumn boxes/amount dla ID " + orderId + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Błąd parsowania ilości dla ID " + orderId + ": " + e.getMessage());
        }
    }
}