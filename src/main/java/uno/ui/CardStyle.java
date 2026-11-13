package uno.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.JPanel;
import uno.core.Card;
import uno.core.CardColor;

/** The same card artwork is used on the table and in the player's hand. */
final class CardStyle {
    private static final Color RED = new Color(226, 75, 72);
    private static final Color GREEN = new Color(55, 161, 115);
    private static final Color BLUE = new Color(66, 129, 207);
    private static final Color YELLOW = new Color(242, 195, 72);
    private static final Color WILD = new Color(35, 43, 57);
    private static final Color PAPER = new Color(249, 246, 235);

    private CardStyle() {
    }

    static Color background(CardColor color) {
        return switch (color) {
            case RED -> RED;
            case GREEN -> GREEN;
            case BLUE -> BLUE;
            case YELLOW -> YELLOW;
            case NONE -> WILD;
        };
    }

    static Color foreground(CardColor color) {
        return color == CardColor.YELLOW ? WILD : PAPER;
    }

    private static String symbol(Card card) {
        return switch (card.kind()) {
            case NUMBER -> card.number().toString();
            case SKIP -> "⊘";
            case REVERSE -> "⇄";
            case DRAW_TWO -> "+2";
            case WILD -> "W";
            case WILD_DRAW_FOUR -> "+4";
        };
    }

    private static void prepare(Graphics2D painter) {
        painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        painter.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static void paintCard(Graphics2D painter, Card card, int x, int y, int width, int height) {
        Graphics2D art = (Graphics2D) painter.create();
        try {
            art.translate(x, y);
            art.scale(width / 100.0, height / 144.0);
            art.setColor(new Color(0, 0, 0, 45));
            art.fillRoundRect(3, 5, 97, 140, 14, 14);
            art.setColor(PAPER);
            art.fillRoundRect(0, 0, 98, 140, 13, 13);
            art.setColor(background(card.color()));
            art.fillRoundRect(5, 5, 88, 130, 10, 10);
            AffineTransform original = art.getTransform();
            art.rotate(Math.toRadians(23), 49, 70);
            art.setColor(PAPER);
            art.fill(new Ellipse2D.Double(16, 20, 66, 101));
            art.setTransform(original);
            if (card.kind().isWild()) {
                art.setColor(RED);
                art.fillArc(25, 41, 48, 58, 90, 90);
                art.setColor(BLUE);
                art.fillArc(25, 41, 48, 58, 0, 90);
                art.setColor(GREEN);
                art.fillArc(25, 41, 48, 58, 180, 90);
                art.setColor(YELLOW);
                art.fillArc(25, 41, 48, 58, 270, 90);
            }
            String symbol = symbol(card);
            art.setFont(UiTheme.font(Font.BOLD, symbol.length() > 1 ? 35f : 50f));
            FontMetrics metrics = art.getFontMetrics();
            int symbolX = (98 - metrics.stringWidth(symbol)) / 2;
            int symbolY = (140 + metrics.getAscent() - metrics.getDescent()) / 2;
            art.setColor(card.kind().isWild() ? WILD : background(card.color()));
            if (card.kind().isWild()) {
                art.setColor(new Color(249, 246, 235, 232));
                art.fillRoundRect(symbolX - 3, symbolY - metrics.getAscent() + 4,
                        metrics.stringWidth(symbol) + 6, metrics.getHeight() - 7, 5, 5);
                art.setColor(WILD);
            }
            art.drawString(symbol, symbolX, symbolY);
            art.setFont(UiTheme.font(Font.BOLD, 15));
            art.setColor(foreground(card.color()));
            art.drawString(symbol, 10, 23);
            art.rotate(Math.PI, 49, 70);
            art.drawString(symbol, 10, 23);
        } finally {
            art.dispose();
        }
    }

    private static void paintBack(Graphics2D painter, int x, int y, int width, int height) {
        Graphics2D art = (Graphics2D) painter.create();
        try {
            art.translate(x, y);
            art.scale(width / 100.0, height / 144.0);
            art.setColor(PAPER);
            art.fillRoundRect(0, 0, 98, 140, 13, 13);
            art.setColor(WILD);
            art.fillRoundRect(5, 5, 88, 130, 10, 10);
            art.rotate(Math.toRadians(-22), 49, 70);
            art.setColor(RED);
            art.fill(new Ellipse2D.Double(9, 29, 80, 83));
            art.setColor(YELLOW);
            art.setFont(UiTheme.font(Font.BOLD | Font.ITALIC, 29));
            FontMetrics metrics = art.getFontMetrics();
            art.drawString("UNO", (98 - metrics.stringWidth("UNO")) / 2, 80);
        } finally {
            art.dispose();
        }
    }

    @SuppressWarnings("serial")
    static final class CardButton extends JButton {
        private final Card card;

        CardButton(Card card) {
            this.card = card;
            setCompact(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(card.label());
            getAccessibleContext().setAccessibleName(card.label());
        }

        void setCompact(boolean compact) {
            setPreferredSize(compact ? new Dimension(84, 118) : new Dimension(96, 138));
            setMinimumSize(getPreferredSize());
            setMaximumSize(getPreferredSize());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D painter = (Graphics2D) graphics.create();
            try {
                prepare(painter);
                boolean raised = isEnabled() && (getModel().isRollover() || hasFocus());
                int top = raised ? 1 : 6;
                if (isEnabled()) {
                    painter.setColor(raised ? UiTheme.GOLD : new Color(242, 199, 106, 130));
                    painter.setStroke(new BasicStroke(raised ? 2.5f : 1.5f));
                    painter.drawRoundRect(2, top, getWidth() - 5, getHeight() - top - 3, 15, 15);
                } else {
                    painter.setComposite(AlphaComposite.SrcOver.derive(0.42f));
                }
                paintCard(painter, card, 7, top + 4, getWidth() - 14, getHeight() - 17);
            } finally {
                painter.dispose();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class DiscardView extends JPanel {
        private Card card;

        DiscardView() {
            setOpaque(false);
            setPreferredSize(new Dimension(150, 206));
            setMinimumSize(new Dimension(70, 96));
        }

        void setCard(Card card) {
            this.card = card;
            setToolTipText("Discard pile: " + card.label());
            getAccessibleContext().setAccessibleName("Discard pile: " + card.label());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (card == null) {
                return;
            }
            Graphics2D painter = (Graphics2D) graphics.create();
            try {
                prepare(painter);
                int height = Math.min(getHeight() - 8, (int) ((getWidth() - 12) * 1.44));
                int width = (int) (height / 1.44);
                paintCard(painter, card, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height);
            } finally {
                painter.dispose();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class DrawPileButton extends JButton {
        DrawPileButton() {
            setPreferredSize(new Dimension(150, 206));
            setMinimumSize(new Dimension(70, 96));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            getAccessibleContext().setAccessibleName("Draw a card from the pile");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D painter = (Graphics2D) graphics.create();
            try {
                prepare(painter);
                int height = Math.min(getHeight() - 17, (int) ((getWidth() - 20) * 1.44));
                int width = (int) (height / 1.44);
                int x = (getWidth() - width) / 2;
                int y = (getHeight() - height) / 2;
                painter.setColor(new Color(245, 242, 232, 100));
                painter.fillRoundRect(x + 7, y + 8, width - 2, height - 3, 12, 12);
                painter.setColor(new Color(245, 242, 232, 155));
                painter.fillRoundRect(x + 4, y + 4, width - 1, height - 2, 12, 12);
                paintBack(painter, x, y, width, height);
                if (isEnabled() && (getModel().isRollover() || hasFocus())) {
                    painter.setColor(UiTheme.GOLD);
                    painter.setStroke(new BasicStroke(2f));
                    painter.draw(new RoundRectangle2D.Double(x - 4, y - 4, width + 7, height + 7, 17, 17));
                }
            } finally {
                painter.dispose();
            }
        }
    }
}
