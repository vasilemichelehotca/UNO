package uno.core;

import java.util.List;

/** Colours that can be active in an UNO game. */
public enum CardColor {
    RED("Red"),
    YELLOW("Yellow"),
    GREEN("Green"),
    BLUE("Blue"),
    NONE("None");

    private static final List<CardColor> PLAYABLE_COLORS = List.of(RED, YELLOW, GREEN, BLUE);
    private final String displayName;

    CardColor(String displayName) {
        this.displayName = displayName;
    }

    /** Returns the four colours that may be selected after a Wild card. */
    public static List<CardColor> playableColors() {
        return PLAYABLE_COLORS;
    }

    /** Returns a user-facing English colour name. */
    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
