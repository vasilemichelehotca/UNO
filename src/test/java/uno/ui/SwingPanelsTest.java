package uno.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JRootPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uno.core.BotProfile;
import uno.core.Card;
import uno.core.GameConfig;
import uno.core.GameEngine;
import uno.core.GameMode;
import uno.core.GamePhase;
import uno.core.PlayerKind;

class SwingPanelsTest {
    @BeforeAll
    static void installTheme() throws Exception {
        SwingUtilities.invokeAndWait(UiTheme::install);
    }

    @Test
    void changingPlayerCountKeepsNamesAndControllers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicReference<GameConfig> started = new AtomicReference<>();
            GameSetupPanel panel = new GameSetupPanel(started::set);
            JSpinner count = named(panel, JSpinner.class, HomeText.PLAYER_COUNT_ACCESSIBLE_NAME);
            count.setValue(3);
            JTextField thirdName = named(panel, JTextField.class, HomeText.PLAYER_NAME_ACCESSIBLE_NAME.formatted(3));
            thirdName.setText("Charlie");
            JComboBox<?> thirdKind = named(panel, JComboBox.class, HomeText.PLAYER_CONTROLLER_ACCESSIBLE_NAME.formatted(3));
            thirdKind.setSelectedItem(PlayerKind.BOT);
            JComboBox<?> thirdProfile = named(panel, JComboBox.class, HomeText.PLAYER_PROFILE_ACCESSIBLE_NAME.formatted(3));
            assertTrue(thirdProfile.isEnabled());
            thirdProfile.setSelectedItem(BotProfile.GREEDY);
            count.setValue(2);
            count.setValue(3);
            button(panel, HomeText.START_BUTTON).doClick();
            assertEquals("Charlie", started.get().players().get(2).name());
            assertEquals(PlayerKind.BOT, started.get().players().get(2).kind());
            assertEquals(BotProfile.GREEDY, started.get().players().get(2).botProfile());
        });
    }

    @Test
    void startCommitsTypedSpinnerValuesIncludingOnePointTarget() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicReference<GameConfig> started = new AtomicReference<>();
            GameSetupPanel panel = new GameSetupPanel(started::set);
            named(panel, JComboBox.class, HomeText.GAME_MODE_ACCESSIBLE_NAME).setSelectedItem(GameMode.POINTS);
            JSpinner count = named(panel, JSpinner.class, HomeText.PLAYER_COUNT_ACCESSIBLE_NAME);
            JSpinner target = named(panel, JSpinner.class, HomeText.VICTORY_TARGET_ACCESSIBLE_NAME);
            assertTrue(target.isEnabled());
            ((JSpinner.DefaultEditor) count.getEditor()).getTextField().setText("4");
            ((JSpinner.DefaultEditor) target.getEditor()).getTextField().setText("1");
            button(panel, HomeText.START_BUTTON).doClick();
            assertNotNull(started.get());
            assertEquals(4, started.get().players().size());
            assertEquals(1, started.get().victoryTarget());
        });
    }

    @Test
    void setupValidationShowsAnInlineMessageAndKeepsTheFormEditable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicReference<GameConfig> started = new AtomicReference<>();
            GameSetupPanel panel = new GameSetupPanel(started::set);
            JTextField firstName = named(panel, JTextField.class, HomeText.PLAYER_NAME_ACCESSIBLE_NAME.formatted(1));
            JTextField secondName = named(panel, JTextField.class, HomeText.PLAYER_NAME_ACCESSIBLE_NAME.formatted(2));
            firstName.setText("Alice");
            secondName.setText("Alice");

            button(panel, HomeText.START_BUTTON).doClick();

            assertNull(started.get());
            assertTrue(components(panel, JTextArea.class).stream()
                    .anyMatch(text -> text.getText().contains("unique") && text.isVisible()));
            assertEquals("Alice", firstName.getText());
            secondName.setText("Bob");
            button(panel, HomeText.START_BUTTON).doClick();
            assertNotNull(started.get());
            assertEquals("Bob", started.get().players().get(1).name());
        });
    }

    @Test
    void sixPlayerSetupFitsBothSupportedWindowSizes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameSetupPanel panel = new GameSetupPanel(config -> { });
            named(panel, JSpinner.class, HomeText.PLAYER_COUNT_ACCESSIBLE_NAME).setValue(6);
            JScrollPane form = components(panel, JScrollPane.class).stream()
                    .filter(scroll -> components(scroll, JTextField.class).stream()
                            .anyMatch(field -> HomeText.DEFAULT_PLAYER_NAME.formatted(6).equals(field.getText())))
                    .findFirst().orElseThrow();
            for (Dimension size : windowSizes()) {
                panel.setSize(size);
                layoutTree(panel);
                assertTrue(form.getViewport().getExtentSize().width > 0);
                assertTrue(form.getViewport().getViewSize().width <= form.getViewport().getExtentSize().width,
                        "Setup fields must not be clipped horizontally at " + size + "; view="
                                + form.getViewport().getViewSize() + "; viewport="
                                + form.getViewport().getExtentSize());
                assertInside(panel, button(panel, HomeText.START_BUTTON));
                assertInside(panel, button(panel, HomeText.CREDITS_BUTTON));
                paint(panel, size);
            }
        });
    }

    @Test
    void detachedTableStopsItsTimerAndIgnoresAnAlreadyQueuedCallback() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = engine(PlayerKind.BOT);
            GamePanel panel = new GamePanel(engine, () -> { });
            Timer timer = field(panel, "botTimer", Timer.class);
            assertEquals(1_500, timer.getInitialDelay());
            assertEquals(1_500, timer.getDelay());
            assertFalse(timer.isRepeats());
            assertFalse(timer.isRunning());
            panel.addNotify();
            try {
                assertTrue(timer.isRunning());
                List<String> before = List.copyOf(engine.events());
                panel.removeNotify();
                assertFalse(timer.isRunning());
                timer.getActionListeners()[0].actionPerformed(new ActionEvent(timer, 0, "queued"));
                assertEquals(before, engine.events());
                panel.addNotify();
                assertSame(timer, field(panel, "botTimer", Timer.class));
                assertTrue(timer.isRunning());
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void newGameStopsBotBeforeCallingSetup() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean stoppedWhenCalled = new AtomicBoolean();
            AtomicReference<GamePanel> table = new AtomicReference<>();
            GamePanel panel = new GamePanel(engine(PlayerKind.BOT), () -> stoppedWhenCalled.set(
                    !field(table.get(), "botTimer", Timer.class).isRunning()));
            table.set(panel);
            panel.addNotify();
            try {
                button(panel, "New game").doClick();
                assertTrue(stoppedWhenCalled.get());
                assertFalse(field(panel, "botTimer", Timer.class).isRunning());
            } finally {
                panel.removeNotify();
            }
        });
    }

    @Test
    void largeHandsScrollAndNamesRemainPlainText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            GameEngine engine = engine(PlayerKind.HUMAN);
            for (int turn = 0; turn < 36; turn++) {
                assertTrue(engine.drawCard().accepted());
                if (engine.phase() == GamePhase.DRAWN_CARD_AVAILABLE) {
                    assertTrue(engine.passAfterDrawing().accepted());
                }
            }
            GamePanel panel = new GamePanel(engine, () -> { });
            JScrollPane hand = field(panel, "handScroll", JScrollPane.class);
            for (Dimension size : windowSizes()) {
                panel.setSize(size);
                layoutTree(panel);
                assertTrue(hand.getHorizontalScrollBar().isVisible());
                assertTrue(hand.getViewport().getViewSize().width > hand.getViewport().getExtentSize().width);
                assertTrue(hand.getViewport().getViewSize().height <= hand.getViewport().getExtentSize().height);
                for (JButton button : components(panel, JButton.class)) {
                    if (!(button instanceof CardStyle.CardButton) && visibleInside(panel, button)
                            && button.getText() != null && !button.getText().isBlank()) {
                        assertTrue(button.getWidth() >= button.getPreferredSize().width, button.getText());
                        assertInside(panel, button);
                    }
                }
                paint(panel, size);
            }
            for (CardStyle.CardButton card : components(panel, CardStyle.CardButton.class)) {
                assertTrue(card.isFocusable());
                assertNotNull(card.getAccessibleContext().getAccessibleName());
                assertFalse(card.getAccessibleContext().getAccessibleName().isBlank());
            }
            JLabel name = components(panel, JLabel.class).stream()
                    .filter(label -> label.getText().equals("<html>Alice")).findFirst().orElseThrow();
            assertEquals(Boolean.TRUE, name.getClientProperty("html.disable"));
            Card playable = engine.playableCards().stream().filter(card -> !card.kind().isWild())
                    .findFirst().orElseThrow();
            CardStyle.CardButton cardButton = named(panel, CardStyle.CardButton.class, playable.label());
            int handSize = engine.currentPlayer().handSize();
            int player = engine.currentPlayerIndex();
            cardButton.doClick();
            assertEquals(playable, engine.topCard());
            assertEquals(handSize - 1, engine.players().get(player).handSize());
        });
    }

    @Test
    void creditsNavigationRestoresTheSameConfiguredHomeAndItsDefaultButton() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JRootPane root = new JRootPane();
            JPanel host = new JPanel(new java.awt.BorderLayout());
            host.add(root);
            AtomicReference<GameSetupPanel> home = new AtomicReference<>();
            AtomicReference<GameConfig> started = new AtomicReference<>();
            CreditsPanel credits = new CreditsPanel(() -> root.setContentPane(home.get()));
            GameSetupPanel setup = new GameSetupPanel(started::set, () -> root.setContentPane(credits));
            home.set(setup);
            root.setContentPane(setup);
            named(setup, JSpinner.class, HomeText.PLAYER_COUNT_ACCESSIBLE_NAME).setValue(4);
            named(setup, JComboBox.class, HomeText.GAME_MODE_ACCESSIBLE_NAME).setSelectedItem(GameMode.POINTS);
            named(setup, JSpinner.class, HomeText.VICTORY_TARGET_ACCESSIBLE_NAME).setValue(42);
            named(setup, JTextField.class, HomeText.PLAYER_NAME_ACCESSIBLE_NAME.formatted(1)).setText("Alice");
            JButton start = button(setup, HomeText.START_BUTTON);
            host.addNotify();
            try {
                assertSame(start, root.getDefaultButton());

                button(setup, HomeText.CREDITS_BUTTON).doClick();

                assertSame(credits, root.getContentPane());
                assertSame(button(credits, HomeText.BACK_HOME_BUTTON), root.getDefaultButton());
                button(credits, HomeText.BACK_HOME_BUTTON).doClick();
                assertSame(setup, root.getContentPane());
                assertSame(start, root.getDefaultButton());
                start.doClick();
                assertEquals(4, started.get().players().size());
                assertEquals("Alice", started.get().players().get(0).name());
                assertEquals(GameMode.POINTS, started.get().mode());
                assertEquals(42, started.get().victoryTarget());
            } finally {
                host.removeNotify();
            }
        });
    }

    @Test
    void creditsShowEditableDraftTextAndFitBothWindowSizes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean returned = new AtomicBoolean();
            CreditsPanel panel = new CreditsPanel(() -> returned.set(true));
            JLabel creator = components(panel, JLabel.class).stream()
                    .filter(label -> HomeText.CREATOR_NAME.equals(label.getText())).findFirst().orElseThrow();
            assertEquals(Boolean.TRUE, creator.getClientProperty("html.disable"));
            assertTrue(components(panel, JLabel.class).stream()
                    .anyMatch(label -> HomeText.CREATOR_ROLE.equals(label.getText())));
            assertTrue(components(panel, JTextArea.class).stream()
                    .anyMatch(note -> HomeText.PROJECT_NOTE.equals(note.getText())));
            assertTrue(components(panel, JTextArea.class).stream()
                    .anyMatch(note -> HomeText.ORIGINAL_GAME_NOTE.equals(note.getText())));
            for (Dimension size : windowSizes()) {
                panel.setSize(size);
                layoutTree(panel);
                assertInside(panel, creator);
                assertInside(panel, button(panel, HomeText.BACK_HOME_BUTTON));
                paint(panel, size);
            }
            button(panel, HomeText.BACK_HOME_BUTTON).doClick();
            assertTrue(returned.get());
        });
    }

    @Test
    void eachBotProfileCanBeSelectedFromTheHomeScreen() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicReference<GameConfig> started = new AtomicReference<>();
            GameSetupPanel panel = new GameSetupPanel(started::set);
            JComboBox<?> profile = named(panel, JComboBox.class, HomeText.PLAYER_PROFILE_ACCESSIBLE_NAME.formatted(2));
            assertEquals(4, profile.getItemCount());
            assertTrue(profile.isEnabled());
            for (BotProfile choice : BotProfile.values()) {
                profile.setSelectedItem(choice);
                assertEquals(choice, profile.getSelectedItem());
                assertTrue(profile.getToolTipText().contains(choice.displayName()));
                button(panel, HomeText.START_BUTTON).doClick();
                assertEquals(choice, started.get().players().get(1).botProfile());
            }
        });
    }

    private static List<Dimension> windowSizes() {
        return List.of(new Dimension(780, 650), new Dimension(1280, 820));
    }

    private static void assertInside(Container panel, Component component) {
        Rectangle bounds = SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), panel);
        assertTrue(bounds.width > 0 && bounds.height > 0);
        assertTrue(new Rectangle(panel.getSize()).contains(bounds), component.toString());
    }

    private static boolean visibleInside(Container panel, Component component) {
        for (Component current = component; current != null && current != panel; current = current.getParent()) {
            if (!current.isVisible()) {
                return false;
            }
        }
        return SwingUtilities.isDescendingFrom(component, panel);
    }

    private static void paint(Container panel, Dimension size) {
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        try {
            panel.printAll(graphics);
        } finally {
            graphics.dispose();
        }
    }

    private static GameEngine engine(PlayerKind kind) {
        return new GameEngine(new GameConfig(GameMode.SINGLE_ROUND, List.of(
                new GameConfig.PlayerConfig("<html>Alice", kind, kind == PlayerKind.BOT ? BotProfile.RANDOM : null),
                new GameConfig.PlayerConfig("Bob", kind, kind == PlayerKind.BOT ? BotProfile.RANDOM : null)), 500),
                new Random(42));
    }

    private static JButton button(Container container, String text) {
        return components(container, JButton.class).stream().filter(button -> text.equals(button.getText()))
                .findFirst().orElseThrow();
    }

    private static <T extends Component> T named(Container container, Class<T> type, String name) {
        return components(container, type).stream()
                .filter(component -> component.getAccessibleContext() != null
                        && name.equals(component.getAccessibleContext().getAccessibleName()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing control: " + name));
    }

    private static <T extends Component> List<T> components(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component component : parent.getComponents()) {
            if (type.isInstance(component)) {
                result.add(type.cast(component));
            }
            if (component instanceof Container container) {
                result.addAll(components(container, type));
            }
        }
        return result;
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutTree(nested);
            }
        }
    }

    private static <T> T field(Object target, String name, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(target));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
