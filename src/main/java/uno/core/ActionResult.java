package uno.core;

/** Result returned by every command sent to the rules engine. */
public record ActionResult(boolean accepted, String message) {
    /** Creates a successful command result. */
    public static ActionResult accepted(String message) {
        return new ActionResult(true, message);
    }

    /** Creates a rejected command result. */
    public static ActionResult rejected(String message) {
        return new ActionResult(false, message);
    }
}
