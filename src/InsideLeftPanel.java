import java.awt.*;
import java.awt.RenderingHints;
import javax.swing.ImageIcon;
import javax.swing.JPanel; // Dodanie jawnego importu dla JPanel

public class InsideLeftPanel extends JPanel { // Używamy JPanel jako bazowej klasy

    private ImageIcon BoxIcon;
    // Ustawienie stałych wymiarów ikony zgodnie z prośbą
    private static final int TARGET_IMAGE_WIDTH = 140;
    private static final int TARGET_IMAGE_HEIGHT = 140;

    public InsideLeftPanel() {
        // Ustawienie panelu jako przezroczystego, aby nie malował prostokątnego tła
        setOpaque(false);

        // Ładowanie obrazu (zakładając, że "package-variant.png" jest w katalogu roboczym)
        BoxIcon = new ImageIcon("package-variant.png");

        // Wymuszenie ładowania obrazu, aby wymiary były dostępne, jeśli to konieczne
        if (BoxIcon != null && BoxIcon.getImage() != null) {
            prepareImage(BoxIcon.getImage(), this);
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        // WŁĄCZENIE ANTI-ALIASINGU DLA GEOMETRII (OKREGU)
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        // WŁĄCZENIE ANTI-ALIASINGU DLA SKALOWANIA OBRAZU (ZDJĘCIA)
        g2.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        // 1. Rysowanie białego okręgu (tło)
        g2.setColor(Color.WHITE);
        g2.fillOval(0, 0, getWidth(), getHeight());

        // 2. Rysowanie Obrazu wewnątrz okręgu (skalowanie do 190x190)
        if (BoxIcon != null && BoxIcon.getImage() != null) {

            Image imageToDraw = BoxIcon.getImage();

            int panelW = getWidth();
            int panelH = getHeight();

            // Obliczenie pozycji, aby wyśrodkować skalowany obraz
            int x = (panelW - TARGET_IMAGE_WIDTH) / 2;
            int y = (panelH - TARGET_IMAGE_HEIGHT) / 2;

            // Narysowanie skalowanego obrazu (wymuszenie 190x190)
            g2.drawImage(imageToDraw, x, y, TARGET_IMAGE_WIDTH, TARGET_IMAGE_HEIGHT, this);
        }
    }
}