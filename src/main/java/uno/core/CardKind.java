package uno.core;

/** The symbol carried by an UNO card. */
public enum CardKind {
    NUMBER("Number"),
    SKIP("Skip"),
    REVERSE("Reverse"),
    DRAW_TWO("Draw Two"),
    WILD("Wild"),
    WILD_DRAW_FOUR("Wild Draw Four");

    private final String displayName;

    CardKind(String displayName) {
        this.displayName = displayName;
    }

    /** Returns whether this card has no printed colour. */
    public boolean isWild() {
        return this == WILD || this == WILD_DRAW_FOUR;
    }

    /** Returns the label used by the GUI. */
    public String displayName() {
        return displayName;
    }
}
