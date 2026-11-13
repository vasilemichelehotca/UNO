package uno.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/** Credits for this adaptation. All wording, including the draft name, lives in HomeText. */
@SuppressWarnings("serial")
final class CreditsPanel extends JPanel {
    private final JButton backButton = UiTheme.button(HomeText.BACK_HOME_BUTTON, false);

    CreditsPanel(Runnable backAction) {
        setBackground(UiTheme.BACKGROUND);
        setLayout(new BorderLayout(0, 20));
        setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        getAccessibleContext().setAccessibleName(HomeText.CREDITS_BUTTON);
        add(createHeader(backAction), BorderLayout.NORTH);

        JPanel center = UiTheme.transparentPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1;
        constraints.weighty = 1;
        center.add(createCard(), constraints);
        add(center, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (getRootPane() != null) {
            getRootPane().setDefaultButton(backButton);
        }
    }

    private JPanel createHeader(Runnable backAction) {
        JPanel header = UiTheme.transparentPanel(new BorderLayout(14, 0));
        header.add(UiTheme.brand(94, 52), BorderLayout.WEST);
        JLabel caption = UiTheme.label(HomeText.CREDITS_HEADER, 11, Font.BOLD);
        caption.setForeground(UiTheme.MUTED);
        header.add(caption, BorderLayout.CENTER);
        backButton.addActionListener(event -> backAction.run());
        header.add(backButton, BorderLayout.EAST);
        return header;
    }

    private JPanel createCard() {
        JPanel card = UiTheme.surface(28, UiTheme.SURFACE);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(800, 500));
        card.setMinimumSize(new Dimension(650, 460));
        card.setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 22, 0);

        JPanel heading = UiTheme.transparentPanel(new BorderLayout(0, 7));
        heading.add(UiTheme.label(HomeText.CREDITS_TITLE, 31, Font.BOLD), BorderLayout.NORTH);
        heading.add(note(HomeText.CREDITS_SUBTITLE, 13, 2), BorderLayout.CENTER);
        card.add(heading, constraints);

        constraints.gridy++;
        card.add(createCreator(), constraints);
        constraints.gridy++;
        card.add(createProject(), constraints);
        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 0, 0);
        card.add(createOriginalGameNote(), constraints);
        return card;
    }

    private JPanel createCreator() {
        JPanel creator = UiTheme.surface(20, UiTheme.RAISED);
        creator.setLayout(new BorderLayout(20, 0));
        creator.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        creator.add(UiTheme.brand(90, 50), BorderLayout.WEST);
        JPanel words = UiTheme.transparentPanel(new GridLayout(3, 1, 0, 4));
        words.add(UiTheme.label(HomeText.CREATOR_NAME, 23, Font.BOLD));
        JLabel role = UiTheme.label(HomeText.CREATOR_ROLE, 12, Font.PLAIN);
        role.setForeground(UiTheme.MUTED);
        words.add(role);
        creator.add(words, BorderLayout.CENTER);
        return creator;
    }

    private JPanel createProject() {
        JPanel project = UiTheme.transparentPanel(new BorderLayout(0, 8));
        project.add(note(HomeText.PROJECT_NOTE, 13, 3), BorderLayout.CENTER);
        JLabel technology = UiTheme.label(HomeText.TECHNOLOGY_NOTE, 11, Font.BOLD);
        technology.setForeground(UiTheme.TEXT);
        project.add(technology, BorderLayout.SOUTH);
        return project;
    }

    private JPanel createOriginalGameNote() {
        JPanel original = UiTheme.transparentPanel(new BorderLayout(0, 7));
        JLabel heading = UiTheme.label(HomeText.ORIGINAL_GAME_LABEL, 9, Font.BOLD);
        heading.setForeground(UiTheme.MUTED);
        original.add(heading, BorderLayout.NORTH);
        original.add(note(HomeText.ORIGINAL_GAME_NOTE, 11, 2), BorderLayout.CENTER);
        return original;
    }

    private static JTextArea note(String text, float size, int rows) {
        JTextArea note = new JTextArea(text, rows, 0);
        note.setEditable(false);
        note.setFocusable(false);
        note.setOpaque(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setFont(UiTheme.font(Font.PLAIN, size));
        note.setForeground(UiTheme.MUTED);
        note.setBorder(null);
        return note;
    }
}
