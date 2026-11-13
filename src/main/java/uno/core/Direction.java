package uno.core;

/** The current direction around the table. */
public enum Direction {
    CLOCKWISE(1, "Clockwise"),
    COUNTERCLOCKWISE(-1, "Counter-clockwise");

    private final int step;
    private final String displayName;

    Direction(int step, String displayName) {
        this.step = step;
        this.displayName = displayName;
    }

    /** Returns the signed index step represented by this direction. */
    public int step() {
        return step;
    }

    /** Returns the opposite direction. */
    public Direction reversed() {
        return this == CLOCKWISE ? COUNTERCLOCKWISE : CLOCKWISE;
    }

    /** Returns a user-facing English direction name. */
    public String displayName() {
        return displayName;
    }
}
