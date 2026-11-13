package uno.core;

/** Strategies available to computer players. */
public enum BotProfile {
    RANDOM("Random"),
    GREEDY("Greedy"),
    CONSERVATIVE("Conservative"),
    AGGRESSIVE("Aggressive");

    private final String displayName;

    BotProfile(String displayName) {
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
