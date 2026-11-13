package uno.ui;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import uno.core.GameConfig;
import uno.core.GameEngine;

/** Application entry point for the UNO Swing user interface. */
public final class UnoApplication {
    private final JFrame frame = new JFrame(HomeText.APPLICATION_TITLE);
    private GameSetupPanel setupPanel;

    private UnoApplication() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(820, 710));
        frame.setSize(1280, 850);
        frame.setLocationByPlatform(true);
    }

    /** Starts the graphical application on the Swing event-dispatch thread. */
    public static void main(String[] arguments) {
        SwingUtilities.invokeLater(() -> {
            setLookAndFeel();
            UiTheme.install();
            new UnoApplication().showSetup();
        });
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException exception) {
            // Keep Swing's default if the requested look and feel is unavailable.
        }
    }

    private void showSetup() {
        if (setupPanel == null) {
            setupPanel = new GameSetupPanel(this::startGame, this::showCredits);
        }
        frame.setContentPane(setupPanel);
        frame.revalidate();
        frame.repaint();
        frame.setVisible(true);
    }

    private void showCredits() {
        frame.getRootPane().setDefaultButton(null);
        frame.setContentPane(new CreditsPanel(this::showSetup));
        frame.revalidate();
        frame.repaint();
    }

    private void startGame(GameConfig config) {
        frame.setContentPane(new GamePanel(new GameEngine(config), this::showSetup));
        frame.getRootPane().setDefaultButton(null);
        frame.revalidate();
        frame.repaint();
    }
}
