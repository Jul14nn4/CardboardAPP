import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class MyFrame extends JFrame {

    JPanel downPanel;
    javax.swing.JPanel leftPanel;
    public RightPanel rightPanel;

    public MyFrame() {
        setTitle("System Cardboard - Logowanie");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setResizable(false);

        ImageIcon icon = new ImageIcon("package-variant.png");
        setIconImage(icon.getImage());

        // Gradient tła
        GradientPanel background = new GradientPanel();
        background.setLayout(new BorderLayout());   // << TU MA BYĆ BorderLayout
        setContentPane(background);

        // Panel dolny
        downPanel = new DarkerGradientPanel();
        downPanel.setLayout(new BoxLayout(downPanel, BoxLayout.X_AXIS));
        downPanel.setPreferredSize(new Dimension(0, 29)); // wysokość stopki

        Border topBorder   = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.CYAN);
        Border sideMargins = BorderFactory.createEmptyBorder(0, 50, 0, 50);
        downPanel.setBorder(BorderFactory.createCompoundBorder(topBorder, sideMargins));

        JLabel downLabel1 = new JLabel("Ⓒ Czterech znudzonych życiem studentów");
        downLabel1.setFont(new Font("", Font.PLAIN, 12));
        downLabel1.setForeground(Color.WHITE);

        ImageIcon downIcon = new ImageIcon("shield-check.png");
        Image scaledImage = downIcon.getImage().getScaledInstance(12, 12, Image.SCALE_SMOOTH);
        JLabel downLabel2 = new JLabel("Połączenie kiedyś będzie bezpieczne", new ImageIcon(scaledImage), JLabel.LEFT);
        downLabel2.setFont(new Font("", Font.PLAIN, 12));
        downLabel2.setForeground(Color.WHITE);

        downPanel.add(downLabel1);
        downPanel.add(Box.createHorizontalGlue());
        downPanel.add(downLabel2);

        // Panel lewy
        leftPanel = new LeftPanel();

        // Panel prawy
        rightPanel = new RightPanel();

        leftPanel.setOpaque(false);
        rightPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(550, 0));
        rightPanel.setPreferredSize(new Dimension(400, 0));

        // UTWORZENIE PANELU WRAPPERA DLA LEWEGO I PRAWEGO PANELU
        JPanel centerWrapper = new JPanel();
        // Używamy BoxLayout, aby umożliwić elastyczny odstęp
        centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.X_AXIS));
        centerWrapper.setOpaque(false); // Zachowanie tła gradientowego

        // 1. Lewy Panel
        centerWrapper.add(leftPanel);

        // 2. Sztywny odstęp między LeftPanel a RightPanel (np. 50 pikseli)
        centerWrapper.add(Box.createRigidArea(new Dimension(50, 0)));

        // 3. Prawy Panel (logowanie)
        centerWrapper.add(rightPanel);

        // 4. Horizontal Glue: Wypycha wolną przestrzeń na prawo, przesuwając oba panele w lewo
        centerWrapper.add(Box.createHorizontalGlue());

        // Dodanie wrapper'a do głównego tła w pozycji CENTER
        background.add(centerWrapper, BorderLayout.CENTER);
        background.add(downPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    // Gradient
    class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth();
            int h = getHeight();

            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(36, 0, 70),
                    w, h, new Color(97, 61, 193)
            );
            g2.setPaint(gradient);
            g2.fillRect(0, 0, w, h);
        }
    }

    class DarkerGradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth();
            int h = getHeight();

            GradientPaint darkerGradient = new GradientPaint(
                    0, 0, new Color(16, 0, 50),
                    w, h, new Color(77, 41, 173)
            );
            g2.setPaint(darkerGradient);
            g2.fillRect(0, 0, w, h);
        }
    }
}
