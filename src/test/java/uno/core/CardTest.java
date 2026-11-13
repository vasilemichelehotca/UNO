package uno.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for immutable card invariants and scoring. */
class CardTest {
    @Test
    void pointsFollowClassicUnoValues() {
        for (CardColor color : CardColor.playableColors()) {
            for (int number = 0; number <= 9; number++) {
                assertEquals(number, Card.number(color, number).pointValue());
            }
            for (CardKind kind : List.of(CardKind.SKIP, CardKind.REVERSE, CardKind.DRAW_TWO)) {
                assertEquals(20, Card.action(color, kind).pointValue());
            }
        }
        assertEquals(50, Card.wild(CardKind.WILD).pointValue());
        assertEquals(50, Card.wild(CardKind.WILD_DRAW_FOUR).pointValue());
    }

    @Test
    void invalidCardShapesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Card.number(CardColor.NONE, 4));
        assertThrows(IllegalArgumentException.class, () -> new Card(CardColor.RED, CardKind.WILD, null));
        assertThrows(IllegalArgumentException.class, () -> new Card(CardColor.BLUE, CardKind.NUMBER, 10));
        assertThrows(IllegalArgumentException.class, () -> Card.number(CardColor.BLUE, -1));
        assertThrows(IllegalArgumentException.class, () -> new Card(CardColor.BLUE, CardKind.NUMBER, null));
        assertThrows(IllegalArgumentException.class, () -> new Card(CardColor.RED, CardKind.SKIP, 3));
        assertThrows(IllegalArgumentException.class, () -> Card.action(CardColor.RED, CardKind.NUMBER));
        assertThrows(IllegalArgumentException.class, () -> Card.action(CardColor.RED, CardKind.WILD));
        assertThrows(IllegalArgumentException.class, () -> Card.wild(CardKind.SKIP));
        assertThrows(NullPointerException.class, () -> Card.number(null, 3));
        assertThrows(NullPointerException.class, () -> new Card(CardColor.RED, null, null));
    }

    @Test
    void equalCardsHaveValueEqualityAndEnglishLabels() {
        assertEquals(Card.number(CardColor.RED, 3), Card.number(CardColor.RED, 3));
        assertEquals("Red 3", Card.number(CardColor.RED, 3).label());
        assertEquals("Blue Skip", Card.action(CardColor.BLUE, CardKind.SKIP).label());
        assertEquals("Wild Draw Four", Card.wild(CardKind.WILD_DRAW_FOUR).label());
    }
}
