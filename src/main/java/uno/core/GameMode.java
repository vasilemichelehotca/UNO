package uno.core;

/** The supported ways of ending a game. */
public enum GameMode {
    SINGLE_ROUND("Single round"),
    POINTS("Points match");

    private final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    /** Returns the label used in the setup screen. */
    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
