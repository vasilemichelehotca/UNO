package uno.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import uno.bot.BotController;
import uno.core.ActionResult;
import uno.core.Card;
import uno.core.CardColor;
import uno.core.GameEngine;
import uno.core.GameMode;
import uno.core.GamePhase;
import uno.core.Player;
import uno.core.PlayerKind;
import uno.core.RoundResult;

/** The table and its controls share one engine; drawing never changes game state. */
@SuppressWarnings("serial")
final class GamePanel extends JPanel {
    private static final int BOT_DELAY_MS = 1_500;
    private final GameEngine engine;
    private final BotController botController = new BotController();
    private final JLabel roundLabel = label("", 12, Font.BOLD);
    private final JLabel modeLabel = label("", 12, Font.PLAIN);
    private final JLabel directionLabel = label("", 12, Font.PLAIN);
    private final JLabel turnLabel = label("", 16, Font.BOLD);
    private final JLabel colorLabel = label("", 12, Font.BOLD);
    private final JLabel drawCountLabel = label("", 12, Font.PLAIN);
    private final JLabel discardLabel = label("", 12, Font.PLAIN);
    private final JLabel handTitle = label("", 16, Font.BOLD);
    private final JLabel handCount = label("", 12, Font.PLAIN);
    private final JLabel handHint = label("", 11, Font.PLAIN);
    private final JLabel contextTitle = label("", 16, Font.BOLD);
    private final JTextArea resultText = textArea();
    private final List<PlayerStatus> playerStatuses = new ArrayList<>();
    private final JPanel handPanel = UiTheme.transparentPanel(null);
    private final JScrollPane handScroll = new JScrollPane(handPanel,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    private final JTextArea eventArea = textArea();
    private final JPanel contextActions = UiTheme.transparentPanel(new GridLayout(0, 1, 0, 8));
    private final JCheckBox declareUno = new JCheckBox("Declare UNO");
    private final JButton drawButton = UiTheme.button("Draw card", true);
    private final JButton passButton = UiTheme.button("Keep card and pass", false);
    private final JButton catchUnoButton = UiTheme.button("Catch missed UNO", false);
    private final JButton acceptFourButton = UiTheme.button("Accept +4", false);
    private final JButton challengeFourButton = UiTheme.button("Challenge +4", true);
    private final JButton nextRoundButton = UiTheme.button("Next round", true);
    private final CardStyle.DrawPileButton drawPile = new CardStyle.DrawPileButton();
    private final CardStyle.DiscardView discard = new CardStyle.DiscardView();
    private final ColorDot colorDot = new ColorDot();
    private final Timer botTimer;
    private boolean active;
    private Player displayedPlayer;
    private GamePhase displayedPhase;
    private String displayedLog = "";
    private boolean compactLayout;

    GamePanel(GameEngine engine, Runnable newGameAction) {
        this.engine = engine;
        botTimer = new Timer(BOT_DELAY_MS, event -> takeBotTurn());
        botTimer.setRepeats(false);
        setBackground(UiTheme.BACKGROUND);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        add(createHeader(newGameAction), BorderLayout.NORTH);
        JPanel body = UiTheme.transparentPanel(new BorderLayout(16, 0));
        JPanel game = UiTheme.transparentPanel(new BorderLayout(0, 14));
        game.add(createTable(), BorderLayout.CENTER);
        game.add(createHand(), BorderLayout.SOUTH);
        body.add(game, BorderLayout.CENTER);
        body.add(createSidebar(), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
        wireActions();
        refresh();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        active = true;
        scheduleBotIfNeeded();
    }

    @Override
    public void removeNotify() {
        active = false;
        botTimer.stop();
        super.removeNotify();
    }

    @Override
    public void doLayout() {
        boolean compact = getHeight() < 730;
        if (compact != compactLayout) {
            compactLayout = compact;
            handScroll.setPreferredSize(new Dimension(420, compact ? 138 : 158));
            for (java.awt.Component component : handPanel.getComponents()) {
                if (component instanceof CardStyle.CardButton button) {
                    button.setCompact(compact);
                }
            }
            for (PlayerStatus status : playerStatuses) {
                status.setCompact(compact && playerStatuses.size() > 3);
            }
        }
        super.doLayout();
    }

    private JPanel createHeader(Runnable newGameAction) {
        JPanel header = UiTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.insets = new Insets(0, 0, 0, 10);
        constraints.gridx = 0;
        header.add(UiTheme.brand(76, 43), constraints);
        JPanel title = UiTheme.transparentPanel(new GridLayout(2, 1, 0, 2));
        title.add(label("Game night", 17, Font.BOLD));
        JLabel subtitle = label("A little luck. Your next move.", 11, Font.PLAIN);
        subtitle.setForeground(UiTheme.MUTED);
        title.add(subtitle);
        constraints.gridx++;
        header.add(title, constraints);
        constraints.gridx++;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        header.add(Box.createHorizontalGlue(), constraints);
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        for (JLabel chip : List.of(roundLabel, modeLabel, directionLabel)) {
            constraints.gridx++;
            chip.setBorder(BorderFactory.createEmptyBorder(8, 9, 8, 9));
            JPanel surface = UiTheme.surface(13, UiTheme.SURFACE);
            surface.add(chip);
            header.add(surface, constraints);
        }
        JButton newGame = UiTheme.button("New game", false);
        newGame.addActionListener(event -> {
            active = false;
            botTimer.stop();
            newGameAction.run();
        });
        constraints.gridx++;
        constraints.insets = new Insets(0, 0, 0, 0);
        header.add(newGame, constraints);
        return header;
    }

    private JPanel createTable() {
        JPanel table = new FeltTable();
        table.setLayout(new BorderLayout(10, 10));
        table.setBorder(BorderFactory.createEmptyBorder(16, 16, 14, 16));
        int playerCount = engine.players().size();
        int columns = playerCount > 3 ? (playerCount + 1) / 2 : playerCount;
        JPanel players = UiTheme.transparentPanel(new GridLayout(0, columns, 8, 8));
        for (int index = 0; index < playerCount; index++) {
            PlayerStatus status = new PlayerStatus(engine.players().get(index), index);
            playerStatuses.add(status);
            players.add(status);
        }
        table.add(players, BorderLayout.NORTH);

        JPanel middle = UiTheme.transparentPanel(new GridBagLayout());
        JPanel piles = UiTheme.transparentPanel(new GridLayout(1, 2, 30, 0));
        piles.add(pile(drawPile, drawCountLabel));
        piles.add(pile(discard, discardLabel));
        piles.setPreferredSize(new Dimension(342, 236));
        piles.setMinimumSize(new Dimension(300, 105));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.VERTICAL;
        middle.add(piles, constraints);
        table.add(middle, BorderLayout.CENTER);

        JPanel status = UiTheme.transparentPanel(new BorderLayout(8, 0));
        status.add(turnLabel, BorderLayout.CENTER);
        colorLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        colorLabel.setIcon(colorDot);
        colorLabel.setIconTextGap(7);
        JPanel colorChip = UiTheme.surface(14, new Color(13, 48, 42));
        colorChip.add(colorLabel);
        status.add(colorChip, BorderLayout.EAST);
        table.add(status, BorderLayout.SOUTH);
        return table;
    }

    private static JPanel pile(java.awt.Component card, JLabel caption) {
        JPanel pile = UiTheme.transparentPanel(new BorderLayout(0, 5));
        pile.add(card, BorderLayout.CENTER);
        caption.setHorizontalAlignment(SwingConstants.CENTER);
        caption.setForeground(new Color(201, 222, 210));
        pile.add(caption, BorderLayout.SOUTH);
        return pile;
    }

    private JPanel createHand() {
        JPanel hand = UiTheme.surface(20, UiTheme.SURFACE);
        hand.setBorder(BorderFactory.createEmptyBorder(12, 14, 10, 14));
        hand.setLayout(new BorderLayout(0, 6));
        JPanel heading = UiTheme.transparentPanel(new BorderLayout(8, 0));
        heading.add(handTitle, BorderLayout.CENTER);
        handCount.setForeground(UiTheme.MUTED);
        heading.add(handCount, BorderLayout.EAST);
        hand.add(heading, BorderLayout.NORTH);

        handPanel.setLayout(new BoxLayout(handPanel, BoxLayout.X_AXIS));
        handPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        handScroll.setBorder(null);
        handScroll.setOpaque(false);
        handScroll.getViewport().setOpaque(false);
        handScroll.setPreferredSize(new Dimension(420, 158));
        handScroll.getHorizontalScrollBar().setUnitIncrement(40);
        UiTheme.styleScrollPane(handScroll);
        handScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(10, 10));
        hand.add(handScroll, BorderLayout.CENTER);

        JPanel footer = UiTheme.transparentPanel(new BorderLayout(0, 7));
        handHint.setForeground(UiTheme.MUTED);
        footer.add(handHint, BorderLayout.NORTH);
        JPanel controls = UiTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 0, 8);
        drawButton.setPreferredSize(new Dimension(105, 37));
        controls.add(drawButton, constraints);
        constraints.gridx = 1;
        passButton.setFont(UiTheme.font(Font.BOLD, 12));
        passButton.setPreferredSize(new Dimension(147, 37));
        controls.add(passButton, constraints);
        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.insets = new Insets(0, 0, 0, 0);
        declareUno.setOpaque(false);
        declareUno.setForeground(UiTheme.TEXT);
        declareUno.setFont(UiTheme.font(Font.BOLD, 12));
        declareUno.setFocusPainted(true);
        declareUno.setPreferredSize(new Dimension(112, 37));
        declareUno.setToolTipText("Select before playing your second-to-last card.");
        controls.add(declareUno, constraints);
        footer.add(controls, BorderLayout.CENTER);
        hand.add(footer, BorderLayout.SOUTH);
        return hand;
    }

    private JPanel createSidebar() {
        JPanel sidebar = UiTheme.surface(20, UiTheme.SURFACE);
        sidebar.setLayout(new BorderLayout(0, 16));
        sidebar.setPreferredSize(new Dimension(244, 400));
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 16, 16, 16));
        JPanel heading = UiTheme.transparentPanel(new GridLayout(2, 1, 0, 5));
        heading.add(label("Around the table", 17, Font.BOLD));
        JLabel caption = label("THE LATEST MOVES", 10, Font.BOLD);
        caption.setForeground(UiTheme.MUTED);
        heading.add(caption);
        sidebar.add(heading, BorderLayout.NORTH);

        eventArea.setFont(UiTheme.font(Font.PLAIN, 12));
        eventArea.setForeground(UiTheme.MUTED);
        eventArea.setBackground(UiTheme.SURFACE);
        eventArea.setOpaque(true);
        eventArea.setSelectionColor(UiTheme.RAISED);
        eventArea.setSelectedTextColor(UiTheme.TEXT);
        eventArea.getAccessibleContext().setAccessibleName("Game activity");
        JScrollPane events = new JScrollPane(eventArea);
        UiTheme.styleScrollPane(events);
        events.setBorder(null);
        events.getViewport().setBackground(UiTheme.SURFACE);
        events.getVerticalScrollBar().setPreferredSize(new Dimension(8, 10));
        sidebar.add(events, BorderLayout.CENTER);

        JPanel context = UiTheme.surface(16, UiTheme.RAISED);
        context.setLayout(new BorderLayout(0, 10));
        context.setBorder(BorderFactory.createEmptyBorder(14, 13, 14, 13));
        context.add(contextTitle, BorderLayout.NORTH);
        resultText.setRows(4);
        resultText.setFont(UiTheme.font(Font.PLAIN, 12));
        resultText.setForeground(UiTheme.MUTED);
        context.add(resultText, BorderLayout.CENTER);
        context.add(contextActions, BorderLayout.SOUTH);
        sidebar.add(context, BorderLayout.SOUTH);
        return sidebar;
    }

    private void wireActions() {
        drawButton.addActionListener(event -> submit(engine.drawCard()));
        drawPile.addActionListener(event -> drawButton.doClick());
        passButton.addActionListener(event -> submit(engine.passAfterDrawing()));
        catchUnoButton.addActionListener(event -> submit(engine.catchUno(humanUnoCaller())));
        acceptFourButton.addActionListener(event -> submit(engine.acceptWildDrawFour()));
        challengeFourButton.addActionListener(event -> submit(engine.challengeWildDrawFour()));
        nextRoundButton.addActionListener(event -> submit(engine.startNextRound()));
    }

    private void refresh() {
        GamePhase phase = engine.phase();
        Player current = engine.currentPlayer();
        boolean playerChanged = displayedPlayer != current;
        if (playerChanged || displayedPhase != phase) {
            declareUno.setSelected(false);
        }
        displayedPlayer = current;
        displayedPhase = phase;
        roundLabel.setText("Round " + engine.roundNumber());
        modeLabel.setText(engine.config().mode() == GameMode.POINTS
                ? "First to " + engine.config().victoryTarget() : "Single round");
        directionLabel.setText(engine.direction().displayName());
        turnLabel.setText(turnMessage(current, phase));
        discard.setCard(engine.topCard());
        discardLabel.setText("Discard pile");
        drawCountLabel.setText("Draw pile · " + engine.drawPileSize());
        colorLabel.setText(engine.currentColor().displayName());
        colorDot.color = CardStyle.background(engine.currentColor());
        for (int index = 0; index < playerStatuses.size(); index++) {
            playerStatuses.get(index).refresh(index == engine.currentPlayerIndex());
        }

        boolean humanTurn = current.kind() == PlayerKind.HUMAN;
        drawButton.setEnabled(humanTurn && phase == GamePhase.TURN);
        drawPile.setEnabled(drawButton.isEnabled());
        drawPile.setToolTipText(drawButton.isEnabled() ? "Click to draw a card" : "Wait for your turn to draw.");
        passButton.setEnabled(humanTurn && phase == GamePhase.DRAWN_CARD_AVAILABLE);
        passButton.setToolTipText("Keep the playable card you just drew and end your turn.");
        declareUno.setEnabled(humanTurn && current.handSize() == 2
                && (phase == GamePhase.TURN || phase == GamePhase.DRAWN_CARD_AVAILABLE));
        if (!declareUno.isEnabled()) {
            declareUno.setSelected(false);
        }
        rebuildHand(current, phase);
        if (playerChanged) {
            handScroll.getHorizontalScrollBar().setValue(0);
        }
        updateContext(current, phase);
        updateLog();
        scheduleBotIfNeeded();
    }

    private String turnMessage(Player current, GamePhase phase) {
        return switch (phase) {
            case AWAITING_WILD_DRAW_FOUR_RESPONSE -> current.name() + " is facing a +4";
            case ROUND_OVER -> "A round well played.";
            case GAME_OVER -> engine.gameWinner().name() + " wins!";
            case DRAWN_CARD_AVAILABLE -> current.name() + " drew a playable card";
            case TURN -> current.name() + "'s turn";
        };
    }

    private void updateContext(Player current, GamePhase phase) {
        contextActions.removeAll();
        boolean human = current.kind() == PlayerKind.HUMAN;
        boolean finished = phase == GamePhase.ROUND_OVER || phase == GamePhase.GAME_OVER;
        RoundResult result = engine.lastRoundResult();
        if (finished && result != null) {
            contextTitle.setText(phase == GamePhase.GAME_OVER ? "We have a winner!" : "Round complete");
            contextTitle.setForeground(UiTheme.GOLD);
            resultText.setText(result.winnerName() + " earned " + result.awardedPoints()
                    + " points this round.\nTotal score: " + result.winnerTotalScore() + "."
                    + (phase == GamePhase.GAME_OVER ? "\nReady for another game?" : "\nNext round, new possibilities."));
            if (phase == GamePhase.ROUND_OVER) {
                contextActions.add(nextRoundButton);
            }
        } else if (phase == GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE) {
            contextTitle.setText(human ? "A Wild +4. Your call." : "Wild +4 response");
            contextTitle.setForeground(UiTheme.GOLD);
            resultText.setText(human ? "Accept four cards, or challenge the play. A failed challenge costs six cards."
                    : current.name() + " is deciding whether to accept or challenge the Wild +4.");
            if (human) {
                contextActions.add(acceptFourButton);
                contextActions.add(challengeFourButton);
            }
        } else {
            contextTitle.setText(human ? "Your move" : "Thinking it through…");
            contextTitle.setForeground(UiTheme.TEXT);
            resultText.setText(human ? phase == GamePhase.DRAWN_CARD_AVAILABLE
                    ? "Play the highlighted card you just drew, or keep it and pass."
                    : "Match the colour or symbol. Gold outlines show the cards you can play."
                    : current.name() + " will play in a moment. Sit back and watch the table.");
        }
        nextRoundButton.setEnabled(phase == GamePhase.ROUND_OVER);
        acceptFourButton.setEnabled(human && phase == GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE);
        challengeFourButton.setEnabled(acceptFourButton.isEnabled());
        int caller = humanUnoCaller();
        catchUnoButton.setEnabled(caller >= 0);
        if (caller >= 0) {
            catchUnoButton.setToolTipText("Player: " + engine.players().get(caller).name()
                    + " can catch " + engine.unoTarget().name() + " for missing UNO");
            contextActions.add(catchUnoButton);
        }
        contextActions.setVisible(contextActions.getComponentCount() > 0);
        contextActions.revalidate();
        contextActions.repaint();
    }

    private void rebuildHand(Player current, GamePhase phase) {
        handPanel.removeAll();
        boolean showHand = current.kind() == PlayerKind.HUMAN
                && (phase == GamePhase.TURN || phase == GamePhase.DRAWN_CARD_AVAILABLE);
        handTitle.setText(showHand ? current.name() + "'s hand" : "The next move");
        Set<Card> playable = new HashSet<>(engine.playableCards());
        long playableCount = current.hand().stream().filter(playable::contains).count();
        handCount.setText(showHand ? current.handSize() + " cards · " + playableCount + " playable" : "");
        if (showHand) {
            for (Card card : current.hand()) {
                CardStyle.CardButton button = new CardStyle.CardButton(card);
                button.setCompact(compactLayout);
                boolean enabled = playable.contains(card);
                button.setEnabled(enabled);
                button.setToolTipText(card.kind() == uno.core.CardKind.WILD_DRAW_FOUR
                        ? "Wild Draw Four · Only legal if you hold no " + engine.currentColor().displayName()
                                + " cards. Opponents may challenge."
                        : card.label() + (enabled ? " · Click to play" : " · Cannot be played this turn"));
                button.getAccessibleContext().setAccessibleDescription(enabled ? "Playable card. Press to play."
                        : "This card cannot be played this turn.");
                button.addActionListener(event -> playHumanCard(card));
                handPanel.add(button);
                handPanel.add(Box.createHorizontalStrut(5));
            }
            handHint.setText(phase == GamePhase.DRAWN_CARD_AVAILABLE ? "Only your newly drawn card can be played."
                    : current.handSize() == 2 ? "One card left after this move? Remember to declare UNO."
                    : "Play a highlighted card, or draw from the pile.");
        } else {
            JPanel waiting = UiTheme.transparentPanel(new BorderLayout(12, 0));
            waiting.add(UiTheme.brand(100, 58), BorderLayout.WEST);
            JLabel message = label(phase == GamePhase.GAME_OVER ? "Nice game. See you at the next table."
                    : phase == GamePhase.ROUND_OVER ? "A fresh hand is just around the corner."
                    : "The table is in good hands. Your cards will appear here.", 12, Font.PLAIN);
            message.setForeground(UiTheme.MUTED);
            waiting.add(message, BorderLayout.CENTER);
            waiting.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
            handPanel.add(waiting);
            handHint.setText(phase == GamePhase.GAME_OVER || phase == GamePhase.ROUND_OVER
                    ? "The result is shown on the right." : "Follow the latest moves in the activity panel.");
        }
        handPanel.revalidate();
        handPanel.repaint();
    }

    private void updateLog() {
        String text = String.join("\n\n", engine.events());
        if (!text.equals(displayedLog)) {
            displayedLog = text;
            eventArea.setText(text);
            eventArea.setCaretPosition(eventArea.getDocument().getLength());
        }
    }

    private void playHumanCard(Card card) {
        CardColor selectedColor = card.kind().isWild() ? chooseColor(card) : null;
        if (card.kind().isWild() && selectedColor == null) {
            return;
        }
        boolean declaresUno = declareUno.isSelected();
        declareUno.setSelected(false);
        submit(engine.playCard(card, selectedColor, declaresUno));
    }

    private CardColor chooseColor(Card card) {
        JPanel message = UiTheme.transparentPanel(new BorderLayout(0, 10));
        message.add(label("What colour comes next?", 18, Font.BOLD), BorderLayout.NORTH);
        if (card.kind() == uno.core.CardKind.WILD_DRAW_FOUR) {
            JTextArea reminder = textArea();
            reminder.setFont(UiTheme.font(Font.PLAIN, 12));
            reminder.setForeground(UiTheme.MUTED);
            reminder.setText("A +4 is only legal if you hold no " + engine.currentColor().displayName()
                    + " cards. Your opponent can challenge the play.");
            reminder.setColumns(37);
            reminder.setRows(3);
            message.add(reminder, BorderLayout.CENTER);
        }
        JButton[] choices = CardColor.playableColors().stream().map(color -> {
            JButton choice = new JButton(color.displayName());
            choice.setBackground(CardStyle.background(color));
            choice.setForeground(CardStyle.foreground(color));
            choice.setFont(UiTheme.font(Font.BOLD, 13));
            choice.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            return choice;
        }).toArray(JButton[]::new);
        JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, choices, choices[0]);
        for (int index = 0; index < choices.length; index++) {
            CardColor color = CardColor.playableColors().get(index);
            choices[index].addActionListener(event -> pane.setValue(color));
        }
        JDialog dialog = pane.createDialog(this, "Choose a colour");
        dialog.setVisible(true);
        dialog.dispose();
        return pane.getValue() instanceof CardColor color ? color : null;
    }

    private void submit(ActionResult result) {
        if (!result.accepted()) {
            JOptionPane.showMessageDialog(this, result.message(), "UNO", JOptionPane.WARNING_MESSAGE);
        }
        refresh();
    }

    private int humanUnoCaller() {
        int current = engine.currentPlayerIndex();
        if (engine.currentPlayer().kind() == PlayerKind.HUMAN && engine.canCatchUno(current)) {
            return current;
        }
        for (int index = 0; index < engine.players().size(); index++) {
            if (engine.players().get(index).kind() == PlayerKind.HUMAN && engine.canCatchUno(index)) {
                return index;
            }
        }
        return -1;
    }

    private void scheduleBotIfNeeded() {
        if (!active || !botController.hasPendingAction(engine)) {
            botTimer.stop();
        } else if (!botTimer.isRunning()) {
            botTimer.start();
        }
    }

    private void takeBotTurn() {
        // A callback may already be queued when its table is removed.
        if (active && botController.hasPendingAction(engine)) {
            botController.takeTurn(engine);
            refresh();
        }
    }

    private static JLabel label(String text, float size, int style) {
        return UiTheme.label(text, size, style);
    }

    private static JTextArea textArea() {
        JTextArea text = new JTextArea();
        text.setEditable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setBorder(null);
        return text;
    }

    private static final class FeltTable extends JPanel {
        FeltTable() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D painter = (Graphics2D) graphics.create();
            try {
                painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                painter.setPaint(new GradientPaint(0, 0, UiTheme.TABLE, getWidth(), getHeight(), new Color(0x123D35)));
                painter.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                painter.setColor(new Color(240, 231, 199, 30));
                painter.setStroke(new BasicStroke(1f));
                painter.drawRoundRect(7, 7, getWidth() - 15, getHeight() - 15, 23, 23);
                painter.setColor(new Color(255, 255, 255, 12));
                painter.drawOval(getWidth() / 2 - 160, getHeight() / 2 - 110, 320, 220);
                painter.drawOval(getWidth() / 2 - 168, getHeight() / 2 - 118, 336, 236);
            } finally {
                painter.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static final class PlayerStatus extends JPanel {
        private final Player player;
        private final Color avatarColor;
        private final Color avatarText;
        private final String initial;
        private final JLabel name = label("", 12, Font.BOLD);
        private final JLabel cards = label("", 11, Font.PLAIN);
        private final JLabel controller = label("", 9, Font.BOLD);
        private boolean current;

        private PlayerStatus(Player player, int index) {
            this.player = player;
            avatarColor = CardStyle.background(CardColor.playableColors().get(index % 4));
            avatarText = CardStyle.foreground(CardColor.playableColors().get(index % 4));
            initial = new String(Character.toChars(player.name().codePointAt(0))).toUpperCase(Locale.ROOT);
            setOpaque(false);
            setLayout(new GridLayout(3, 1, 0, 2));
            setBorder(BorderFactory.createEmptyBorder(9, 48, 9, 9));
            name.setText(player.name());
            name.setToolTipText("Player: " + player.name());
            cards.setForeground(UiTheme.MUTED);
            add(name);
            add(cards);
            add(controller);
        }

        private void refresh(boolean current) {
            this.current = current;
            cards.setText(player.handSize() + " cards · " + player.score() + " pts");
            controller.setText(current ? "CURRENT TURN" : player.kind() == PlayerKind.HUMAN
                    ? "HUMAN" : player.botProfile().displayName().toUpperCase(Locale.ROOT) + " BOT");
            controller.setForeground(current ? UiTheme.GOLD : UiTheme.MUTED);
            repaint();
        }

        private void setCompact(boolean compact) {
            if (compact) {
                remove(controller);
            } else if (controller.getParent() != this) {
                add(controller);
            }
            setLayout(new GridLayout(compact ? 2 : 3, 1, 0, 2));
            setBorder(BorderFactory.createEmptyBorder(compact ? 6 : 9, 48, compact ? 6 : 9, 9));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D painter = (Graphics2D) graphics.create();
            try {
                painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                painter.setColor(current ? UiTheme.RAISED : new Color(12, 37, 35, 185));
                painter.fillRoundRect(0, 0, getWidth(), getHeight(), 17, 17);
                if (current) {
                    painter.setColor(UiTheme.GOLD);
                    painter.setStroke(new BasicStroke(1.5f));
                    painter.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
                }
                painter.setColor(avatarColor);
                painter.fillOval(11, (getHeight() - 29) / 2, 29, 29);
                painter.setColor(avatarText);
                painter.setFont(UiTheme.font(Font.BOLD, 13));
                painter.drawString(initial, 25 - painter.getFontMetrics().stringWidth(initial) / 2,
                        getHeight() / 2 + 5);
            } finally {
                painter.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    private static final class ColorDot implements Icon {
        private Color color = UiTheme.GOLD;

        @Override
        public int getIconWidth() {
            return 9;
        }

        @Override
        public int getIconHeight() {
            return 9;
        }

        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            graphics.setColor(color);
            graphics.fillOval(x, y, getIconWidth(), getIconHeight());
        }
    }
}
