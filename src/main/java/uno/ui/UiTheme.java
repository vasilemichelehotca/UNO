package uno.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.JTextComponent;

/** Shared colours and a few painted controls; the rest of the UI is plain Swing. */
final class UiTheme {
    static final Color BACKGROUND = new Color(0x101A29);
    static final Color SURFACE = new Color(0x192638);
    static final Color RAISED = new Color(0x213148);
    static final Color TEXT = new Color(0xF4F3ED);
    static final Color MUTED = new Color(0xA7B5C5);
    static final Color ACCENT = new Color(0xEF6A54);
    static final Color GOLD = new Color(0xF2C76A);
    static final Color TABLE = new Color(0x17483F);

    private UiTheme() {
    }

    static void install() {
        UIManager.put("control", SURFACE);
        UIManager.put("info", SURFACE);
        UIManager.put("text", TEXT);
        for (String type : new String[]{"Panel", "OptionPane", "CheckBox", "Viewport"}) {
            UIManager.put(type + ".background", SURFACE);
            UIManager.put(type + ".foreground", TEXT);
        }
        for (String type : new String[]{"Label", "Button", "ComboBox", "TextField", "FormattedTextField",
                "Spinner", "CheckBox", "OptionPane", "ToolTip", "List"}) {
            UIManager.put(type + ".font", font(Font.PLAIN, 13));
        }
        for (String type : new String[]{"ComboBox", "TextField", "FormattedTextField", "Spinner", "List"}) {
            UIManager.put(type + ".background", RAISED);
            UIManager.put(type + ".foreground", TEXT);
            UIManager.put(type + ".selectionBackground", TABLE);
            UIManager.put(type + ".selectionForeground", TEXT);
        }
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.background", RAISED);
        UIManager.put("Button.disabledText", MUTED);
        UIManager.put("ComboBox.disabledBackground", SURFACE);
        UIManager.put("ComboBox.disabledForeground", MUTED);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("FormattedTextField.caretForeground", TEXT);
        UIManager.put("TextField.inactiveForeground", MUTED);
        UIManager.put("FormattedTextField.inactiveForeground", MUTED);
        UIManager.put("ScrollBar.background", SURFACE);
        UIManager.put("ScrollBar.thumb", new Color(0x40546C));
        UIManager.put("ScrollBar.thumbDarkShadow", SURFACE);
        UIManager.put("ScrollBar.thumbHighlight", new Color(0x40546C));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ToolTip.background", RAISED);
        UIManager.put("ToolTip.foreground", TEXT);
    }

    static Font font(int style, float size) {
        return new Font(Font.SANS_SERIF, style, Math.round(size)).deriveFont(size);
    }

    static JLabel label(String text, float size, int style) {
        JLabel label = new JLabel(text);
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setForeground(TEXT);
        label.setFont(font(style, size));
        return label;
    }

    static JPanel surface(int radius, Color fill) {
        return new RoundedPanel(radius, fill);
    }

    static JPanel transparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static JButton button(String text, boolean primary) {
        return new GameButton(text, primary);
    }

    static void styleInput(JComponent component) {
        if (component instanceof JComboBox<?> combo) {
            combo.setUI(new BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    JButton arrow = new BasicArrowButton(SwingConstants.SOUTH, RAISED, RAISED, MUTED, RAISED);
                    arrow.setBorder(BorderFactory.createEmptyBorder());
                    return arrow;
                }
            });
        }
        component.setFont(font(Font.PLAIN, 13));
        component.setBackground(RAISED);
        component.setForeground(TEXT);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3A4C63)),
                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        if (component instanceof JTextComponent text) {
            text.setCaretColor(TEXT);
            text.setSelectionColor(TABLE);
            text.setSelectedTextColor(TEXT);
        } else if (component instanceof JSpinner spinner && spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            JTextComponent field = editor.getTextField();
            field.setFont(font(Font.PLAIN, 13));
            field.setBackground(RAISED);
            field.setForeground(TEXT);
            field.setCaretColor(TEXT);
            field.setBorder(BorderFactory.createEmptyBorder());
            editor.setOpaque(false);
        }
    }

    static void styleScrollPane(JScrollPane pane) {
        for (JScrollBar bar : new JScrollBar[]{pane.getHorizontalScrollBar(), pane.getVerticalScrollBar()}) {
            bar.setUI(new BasicScrollBarUI() {
                @Override
                protected void configureScrollBarColors() {
                    trackColor = SURFACE;
                    thumbColor = new Color(0x51647B);
                }

                @Override
                protected JButton createDecreaseButton(int orientation) {
                    return emptyArrow();
                }

                @Override
                protected JButton createIncreaseButton(int orientation) {
                    return emptyArrow();
                }

                private JButton emptyArrow() {
                    JButton arrow = new JButton();
                    arrow.setPreferredSize(new Dimension(0, 0));
                    arrow.setFocusable(false);
                    return arrow;
                }

                @Override
                protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
                    Graphics2D painter = (Graphics2D) graphics.create();
                    try {
                        painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        painter.setColor(thumbColor);
                        painter.fillRoundRect(bounds.x + 1, bounds.y + 1,
                                Math.max(0, bounds.width - 2), Math.max(0, bounds.height - 2), 8, 8);
                    } finally {
                        painter.dispose();
                    }
                }
            });
        }
    }

    static JComponent brand(int width, int height) {
        JComponent brand = new JComponent() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D painter = (Graphics2D) graphics.create();
                try {
                    painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    painter.scale(getWidth() / 112.0, getHeight() / 62.0);
                    painter.setColor(ACCENT);
                    painter.fillRoundRect(2, 3, 108, 56, 20, 20);
                    painter.setColor(GOLD);
                    painter.fillOval(15, 8, 82, 44);
                    painter.rotate(-0.12, 56, 31);
                    painter.setFont(font(Font.BOLD | Font.ITALIC, 34));
                    painter.setColor(BACKGROUND);
                    painter.drawString("UNO", 19, 44);
                } finally {
                    painter.dispose();
                }
            }
        };
        brand.setPreferredSize(new Dimension(width, height));
        brand.setMinimumSize(brand.getPreferredSize());
        brand.setMaximumSize(brand.getPreferredSize());
        brand.setToolTipText("UNO");
        return brand;
    }

    @SuppressWarnings("serial")
    private static final class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;

        RoundedPanel(int radius, Color fill) {
            super(new BorderLayout());
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D painter = (Graphics2D) graphics.create();
            try {
                painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                painter.setColor(fill);
                painter.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            } finally {
                painter.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    @SuppressWarnings("serial")
    private static final class GameButton extends JButton {
        private final boolean primary;

        GameButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(font(Font.BOLD, 13));
            setForeground(primary ? BACKGROUND : TEXT);
            setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D painter = (Graphics2D) graphics.create();
            try {
                painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = primary ? ACCENT : RAISED;
                if (!isEnabled()) {
                    fill = SURFACE;
                } else if (getModel().isPressed()) {
                    fill = fill.darker();
                } else if (getModel().isRollover()) {
                    fill = fill.brighter();
                }
                painter.setColor(fill);
                painter.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 13, 13);
                if (hasFocus()) {
                    painter.setColor(GOLD);
                    painter.setStroke(new BasicStroke(2));
                    painter.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 12, 12);
                }
            } finally {
                painter.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
