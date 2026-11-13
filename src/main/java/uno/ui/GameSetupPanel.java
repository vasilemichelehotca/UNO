package uno.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.Scrollable;
import uno.core.BotProfile;
import uno.core.GameConfig;
import uno.core.GameMode;
import uno.core.PlayerKind;

/** The welcome screen and the settings needed for a new local match. */
@SuppressWarnings("serial")
final class GameSetupPanel extends JPanel {
    private final Consumer<GameConfig> startAction;
    private final Runnable creditsAction;
    private final JComboBox<GameMode> modeBox = new JComboBox<>(GameMode.values());
    private final JSpinner playerCount = new JSpinner(new SpinnerNumberModel(2, 2, 6, 1));
    private final JSpinner targetScore = new JSpinner(new SpinnerNumberModel(
            GameConfig.DEFAULT_VICTORY_TARGET, 1, Integer.MAX_VALUE, 1));
    private final JPanel playerRows = UiTheme.transparentPanel(new GridLayout(0, 1, 0, 8));
    private final List<PlayerRow> rows = new ArrayList<>();
    private final JTextArea modeHint = note("");
    private final JTextArea errorText = note("");
    private final JButton startButton = UiTheme.button(HomeText.START_BUTTON, true);

    GameSetupPanel(Consumer<GameConfig> startAction) {
        this(startAction, () -> { });
    }

    GameSetupPanel(Consumer<GameConfig> startAction, Runnable creditsAction) {
        this.startAction = startAction;
        this.creditsAction = creditsAction;
        setLayout(new BorderLayout(20, 20));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        add(createHeader(), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        modeBox.addActionListener(event -> updateTargetAvailability());
        playerCount.addChangeListener(event -> rebuildPlayerRows());
        rebuildPlayerRows();
        updateTargetAvailability();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (getRootPane() != null) {
            getRootPane().setDefaultButton(startButton);
        }
    }

    private JPanel createHeader() {
        JPanel header = UiTheme.transparentPanel(new BorderLayout(14, 0));
        header.add(UiTheme.brand(94, 52), BorderLayout.WEST);
        JLabel local = UiTheme.label(HomeText.LOCAL_PLAY_HINT, 12, Font.PLAIN);
        local.setForeground(UiTheme.MUTED);
        JPanel links = UiTheme.transparentPanel(new BorderLayout(16, 0));
        links.add(local, BorderLayout.CENTER);
        JButton credits = UiTheme.button(HomeText.CREDITS_BUTTON, false);
        credits.addActionListener(event -> creditsAction.run());
        links.add(credits, BorderLayout.EAST);
        header.add(links, BorderLayout.EAST);
        return header;
    }

    private JPanel createContent() {
        JPanel content = UiTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.35;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 22);
        content.add(createWelcome(), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.65;
        constraints.insets = new Insets(0, 0, 0, 0);
        content.add(createForm(), constraints);
        return content;
    }

    private JPanel createWelcome() {
        JPanel welcome = UiTheme.surface(26, UiTheme.TABLE);
        welcome.setBorder(BorderFactory.createEmptyBorder(24, 22, 24, 22));
        welcome.setMinimumSize(new Dimension(210, 250));
        welcome.setPreferredSize(new Dimension(325, 500));
        JPanel words = UiTheme.transparentPanel(new GridLayout(0, 1, 0, 8));
        words.add(UiTheme.label(HomeText.WELCOME_TITLE_FIRST_LINE, 33, Font.BOLD));
        words.add(UiTheme.label(HomeText.WELCOME_TITLE_SECOND_LINE, 33, Font.BOLD));
        JTextArea invitation = note(HomeText.WELCOME_DESCRIPTION);
        invitation.setForeground(new Color(0xD6E7DE));
        words.add(invitation);
        welcome.add(words, BorderLayout.NORTH);
        welcome.add(new WelcomeCards(), BorderLayout.CENTER);
        JPanel rules = UiTheme.transparentPanel(new GridLayout(0, 1, 0, 9));
        welcome.add(rules, BorderLayout.SOUTH);
        return welcome;
    }

    private JPanel createForm() {
        JPanel form = UiTheme.transparentPanel(new BorderLayout(0, 14));
        JPanel heading = UiTheme.transparentPanel(new GridLayout(0, 1, 0, 5));
        heading.add(UiTheme.label(HomeText.SETUP_TITLE, 25, Font.BOLD));
        form.add(heading, BorderLayout.NORTH);

        JPanel stack = new FormContents();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 14, 0);
        stack.add(createOptions(), constraints);
        constraints.gridy++;
        stack.add(createPlayers(), constraints);
        constraints.gridy++;
        constraints.weighty = 1;
        stack.add(UiTheme.transparentPanel(new BorderLayout()), constraints);
        JScrollPane scroll = new JScrollPane(stack, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        UiTheme.styleScrollPane(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        form.add(scroll, BorderLayout.CENTER);
        form.setMinimumSize(new Dimension(430, 280));
        return form;
    }

    private JPanel createOptions() {
        JPanel options = UiTheme.surface(20, UiTheme.SURFACE);
        options.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));
        options.add(UiTheme.label(HomeText.MATCH_SETTINGS_TITLE, 15, Font.BOLD), BorderLayout.NORTH);
        JPanel fields = UiTheme.transparentPanel(new GridLayout(1, 3, 12, 0));
        fields.setBorder(BorderFactory.createEmptyBorder(16, 0, 12, 0));
        UiTheme.styleInput(modeBox);
        UiTheme.styleInput(playerCount);
        UiTheme.styleInput(targetScore);
        ((JSpinner.DefaultEditor) playerCount.getEditor()).getTextField().setColumns(2);
        ((JSpinner.DefaultEditor) targetScore.getEditor()).getTextField().setColumns(6);
        modeBox.getAccessibleContext().setAccessibleName(HomeText.GAME_MODE_ACCESSIBLE_NAME);
        playerCount.getAccessibleContext().setAccessibleName(HomeText.PLAYER_COUNT_ACCESSIBLE_NAME);
        targetScore.getAccessibleContext().setAccessibleName(HomeText.VICTORY_TARGET_ACCESSIBLE_NAME);
        targetScore.setToolTipText(HomeText.VICTORY_TARGET_TOOLTIP);
        fields.add(field(HomeText.MODE_LABEL, modeBox));
        fields.add(field(HomeText.PLAYER_COUNT_LABEL, playerCount));
        fields.add(field(HomeText.POINTS_TARGET_LABEL, targetScore));
        options.add(fields, BorderLayout.CENTER);
        modeHint.setRows(2);
        options.add(modeHint, BorderLayout.SOUTH);
        return options;
    }

    private JPanel createPlayers() {
        JPanel players = UiTheme.surface(20, UiTheme.SURFACE);
        players.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JPanel heading = UiTheme.transparentPanel(new BorderLayout());
        heading.add(UiTheme.label(HomeText.PLAYERS_TITLE, 15, Font.BOLD), BorderLayout.WEST);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        players.add(heading, BorderLayout.NORTH);
        players.add(playerRows, BorderLayout.CENTER);
        return players;
    }

    private JPanel createFooter() {
        JPanel footer = UiTheme.transparentPanel(new BorderLayout(20, 0));
        errorText.setText(HomeText.FOOTER_NOTE);
        errorText.setForeground(UiTheme.MUTED);
        footer.add(errorText, BorderLayout.CENTER);
        startButton.addActionListener(event -> startGame());
        startButton.setPreferredSize(new Dimension(170, 44));
        footer.add(startButton, BorderLayout.EAST);
        return footer;
    }

    private void rebuildPlayerRows() {
        int count = (Integer) playerCount.getValue();
        while (rows.size() < count) {
            rows.add(new PlayerRow(rows.size() + 1));
        }
        playerRows.removeAll();
        for (int index = 0; index < count; index++) {
            playerRows.add(rows.get(index).panel);
        }
        playerRows.revalidate();
        playerRows.repaint();
    }

    private void updateTargetAvailability() {
        boolean pointsMatch = modeBox.getSelectedItem() == GameMode.POINTS;
        targetScore.setEnabled(pointsMatch);
        if (targetScore.getEditor() instanceof JSpinner.DefaultEditor editor) {
            editor.getTextField().setEnabled(pointsMatch);
        }
        modeHint.setText(pointsMatch ? HomeText.POINTS_MODE_HELP : HomeText.SINGLE_ROUND_HELP);
    }

    private void startGame() {
        try {
            playerCount.commitEdit();
            if (modeBox.getSelectedItem() == GameMode.POINTS) {
                targetScore.commitEdit();
            }
            int count = (Integer) playerCount.getValue();
            List<GameConfig.PlayerConfig> players = rows.subList(0, count).stream().map(PlayerRow::toConfig).toList();
            startAction.accept(new GameConfig((GameMode) modeBox.getSelectedItem(), players,
                    (Integer) targetScore.getValue()));
        } catch (ParseException exception) {
            showError(HomeText.INVALID_NUMBER_MESSAGE);
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void showError(String message) {
        errorText.setForeground(UiTheme.ACCENT);
        errorText.setText(message);
    }

    private static JPanel field(String title, javax.swing.JComponent input) {
        JPanel panel = UiTheme.transparentPanel(new BorderLayout(0, 6));
        JLabel label = UiTheme.label(title, 10, Font.BOLD);
        label.setForeground(UiTheme.MUTED);
        label.setLabelFor(input);
        panel.add(label, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private static JTextArea note(String text) {
        JTextArea note = new JTextArea(text);
        note.setEditable(false);
        note.setFocusable(false);
        note.setOpaque(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setFont(UiTheme.font(Font.PLAIN, 12));
        note.setForeground(UiTheme.MUTED);
        return note;
    }

    private static final class FormContents extends JPanel implements Scrollable {
        FormContents() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(20, visible.height - 20);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static final class PlayerRow {
        private final JTextField name = new JTextField(8);
        private final JComboBox<PlayerKind> kind = new JComboBox<>(PlayerKind.values());
        private final JComboBox<BotProfile> profile = new JComboBox<>(BotProfile.values());
        private final JPanel panel = UiTheme.transparentPanel(new GridBagLayout());

        private PlayerRow(int number) {
            name.setText(HomeText.DEFAULT_PLAYER_NAME.formatted(number));
            UiTheme.styleInput(name);
            UiTheme.styleInput(kind);
            UiTheme.styleInput(profile);
            name.getAccessibleContext().setAccessibleName(HomeText.PLAYER_NAME_ACCESSIBLE_NAME.formatted(number));
            kind.getAccessibleContext().setAccessibleName(HomeText.PLAYER_CONTROLLER_ACCESSIBLE_NAME.formatted(number));
            profile.getAccessibleContext().setAccessibleName(HomeText.PLAYER_PROFILE_ACCESSIBLE_NAME.formatted(number));
            name.setToolTipText(HomeText.PLAYER_NAME_TOOLTIP);
            kind.setToolTipText(HomeText.PLAYER_CONTROLLER_TOOLTIP);
            profile.setToolTipText(HomeText.BOT_PROFILE_TOOLTIP);
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(0, 0, 0, 8);
            constraints.anchor = GridBagConstraints.CENTER;
            constraints.gridx = 0;
            JLabel seat = UiTheme.label(String.format("%02d", number), 12, Font.BOLD);
            seat.setForeground(UiTheme.GOLD);
            panel.add(seat, constraints);
            constraints.gridx++;
            constraints.weightx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            panel.add(name, constraints);
            constraints.weightx = 0;
            constraints.gridx++;
            panel.add(kind, constraints);
            constraints.gridx++;
            constraints.insets = new Insets(0, 0, 0, 0);
            panel.add(profile, constraints);
            kind.addActionListener(event -> profile.setEnabled(kind.getSelectedItem() == PlayerKind.BOT));
            kind.setSelectedItem(number == 2 ? PlayerKind.BOT : PlayerKind.HUMAN);
            profile.setEnabled(kind.getSelectedItem() == PlayerKind.BOT);
        }

        private GameConfig.PlayerConfig toConfig() {
            PlayerKind selectedKind = (PlayerKind) kind.getSelectedItem();
            return new GameConfig.PlayerConfig(name.getText(), selectedKind,
                    selectedKind == PlayerKind.BOT ? (BotProfile) profile.getSelectedItem() : null);
        }
    }

    private static final class WelcomeCards extends JPanel {
        WelcomeCards() {
            setOpaque(false);
            setPreferredSize(new Dimension(250, 235));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D painter = (Graphics2D) graphics.create();
            try {
                painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double scale = Math.min(getWidth() / 270.0, getHeight() / 235.0);
                painter.translate(getWidth() / 2.0, getHeight() / 2.0);
                painter.scale(scale, scale);
                drawCard(painter, -64, 0, -0.22, new Color(0xE2AE37), "7");
                drawCard(painter, 64, 0, 0.22, new Color(0x4086C6), "+2");
                drawCard(painter, 0, -10, 0.02, UiTheme.ACCENT, "8");
            } finally {
                painter.dispose();
            }
        }

        private void drawCard(Graphics2D painter, int x, int y, double angle, Color color, String value) {
            AffineTransform original = painter.getTransform();
            painter.translate(x, y);
            painter.rotate(angle);
            painter.setColor(new Color(0, 0, 0, 40));
            painter.fillRoundRect(-49, -69, 106, 150, 15, 15);
            painter.setColor(UiTheme.TEXT);
            painter.fillRoundRect(-53, -75, 106, 150, 15, 15);
            painter.setColor(color);
            painter.fillRoundRect(-47, -69, 94, 138, 12, 12);
            painter.setColor(UiTheme.TEXT);
            painter.fillOval(-37, -53, 74, 106);
            painter.setColor(color.darker());
            painter.setFont(UiTheme.font(Font.BOLD | Font.ITALIC, 48));
            painter.drawString(value, -painter.getFontMetrics().stringWidth(value) / 2, 18);
            painter.setTransform(original);
        }
    }
}
