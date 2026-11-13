package uno.core;

/** Identifies whether a seat is controlled by a person or the computer. */
public enum PlayerKind {
    HUMAN("Human"),
    BOT("Computer");

    private final String displayName;

    PlayerKind(String displayName) {
        this.displayName = displayName;
    }

    /** Returns the label used by the setup screen. */
    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
