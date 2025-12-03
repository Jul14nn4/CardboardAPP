public interface Refreshable {
    /**
     * Metoda wywoływana cyklicznie przez globalny timer do odświeżenia danych w widoku.
     */
    void refreshData();
}