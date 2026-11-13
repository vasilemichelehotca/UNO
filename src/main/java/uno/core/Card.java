package uno.core;

import java.util.Objects;

/**
 * An immutable UNO card. Number cards have a number from 0 to 9; action cards
 * have no number. Wild cards always use {@link CardColor#NONE}.
 */
public record Card(CardColor color, CardKind kind, Integer number) {
    /** Validates and creates an UNO card. */
    public Card {
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(kind, "kind");
        if (kind == CardKind.NUMBER && (number == null || number < 0 || number > 9)) {
            throw new IllegalArgumentException("Number cards need a value from 0 to 9.");
        }
        if (kind != CardKind.NUMBER && number != null) {
            throw new IllegalArgumentException("Only number cards may have a numeric value.");
        }
        if (kind.isWild() != (color == CardColor.NONE)) {
            throw new IllegalArgumentException("Wild cards have no colour; coloured cards need one.");
        }
    }

    /** Creates a coloured number card. */
    public static Card number(CardColor color, int value) {
        return new Card(color, CardKind.NUMBER, value);
    }

    /** Creates a coloured action card. */
    public static Card action(CardColor color, CardKind kind) {
        if (kind == CardKind.NUMBER || kind.isWild()) {
            throw new IllegalArgumentException("Use a specialised factory for this card kind.");
        }
        return new Card(color, kind, null);
    }

    /** Creates a Wild or Wild Draw Four card. */
    public static Card wild(CardKind kind) {
        if (!kind.isWild()) {
            throw new IllegalArgumentException("Only wild card kinds can be created here.");
        }
        return new Card(CardColor.NONE, kind, null);
    }

    /** Returns the classic end-of-round value of this card. */
    public int pointValue() {
        return switch (kind) {
            case NUMBER -> number;
            case SKIP, REVERSE, DRAW_TWO -> 20;
            case WILD, WILD_DRAW_FOUR -> 50;
        };
    }

    /** Returns a concise English label for logs and buttons. */
    public String label() {
        if (kind == CardKind.NUMBER) {
            return color.displayName() + " " + number;
        }
        return kind.isWild() ? kind.displayName() : color.displayName() + " " + kind.displayName();
    }
}
