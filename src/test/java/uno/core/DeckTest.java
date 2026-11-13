package uno.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.HashMap;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Tests the composition of the standard 108-card deck. */
class DeckTest {
    @Test
    void standardDeckHasExactClassicComposition() {
        Deck deck = Deck.standard(new Random(4));
        Map<CardKind, Integer> kindCounts = new EnumMap<>(CardKind.class);
        Map<CardColor, Integer> colourCounts = new EnumMap<>(CardColor.class);
        Map<Card, Integer> copies = new HashMap<>();

        for (int index = 0; index < 108; index++) {
            Card card = deck.draw();
            assertNotNull(card);
            kindCounts.merge(card.kind(), 1, Integer::sum);
            colourCounts.merge(card.color(), 1, Integer::sum);
            copies.merge(card, 1, Integer::sum);
        }

        assertEquals(76, kindCounts.get(CardKind.NUMBER));
        assertEquals(8, kindCounts.get(CardKind.SKIP));
        assertEquals(8, kindCounts.get(CardKind.REVERSE));
        assertEquals(8, kindCounts.get(CardKind.DRAW_TWO));
        assertEquals(4, kindCounts.get(CardKind.WILD));
        assertEquals(4, kindCounts.get(CardKind.WILD_DRAW_FOUR));
        for (CardColor color : CardColor.playableColors()) {
            assertEquals(25, colourCounts.get(color));
            assertEquals(1, copies.get(Card.number(color, 0)));
            for (int value = 1; value <= 9; value++) {
                assertEquals(2, copies.get(Card.number(color, value)));
            }
            for (CardKind kind : List.of(CardKind.SKIP, CardKind.REVERSE, CardKind.DRAW_TWO)) {
                assertEquals(2, copies.get(Card.action(color, kind)));
            }
        }
        assertEquals(8, colourCounts.get(CardColor.NONE));
        assertEquals(0, deck.drawPileSize());
        assertNull(deck.draw());
    }

    @Test
    void recyclingPreservesTheVisibleTopCard() {
        Card older = Card.number(CardColor.RED, 1);
        Card top = Card.number(CardColor.BLUE, 2);
        Deck deck = new Deck(List.of(), new Random(1));
        deck.discard(older);
        deck.discard(top);

        assertEquals(older, deck.draw());
        assertEquals(0, deck.drawPileSize());
        assertEquals(1, deck.discardPileSize());
        assertNull(deck.draw());
        assertEquals(1, deck.discardPileSize());
    }

    @Test
    void openingCardSearchKeepsDeferredActionsInTheDeck() {
        Card wild = Card.wild(CardKind.WILD);
        Card skip = Card.action(CardColor.RED, CardKind.SKIP);
        Card number = Card.number(CardColor.BLUE, 2);
        Card next = Card.number(CardColor.GREEN, 3);
        Deck deck = new Deck(List.of(wild, skip, number, next), new Random(1));

        assertEquals(number, deck.revealStartingNumberCard());

        assertEquals(1, deck.discardPileSize());
        assertEquals(3, deck.drawPileSize());
        assertEquals(next, deck.draw());
        assertEquals(wild, deck.draw());
        assertEquals(skip, deck.draw());
        assertNull(deck.draw());
    }

    @Test
    void openingCardSearchWithoutNumbersFailsWithoutLosingCards() {
        Card wild = Card.wild(CardKind.WILD);
        Deck deck = new Deck(List.of(wild), new Random(1));

        assertThrows(IllegalStateException.class, deck::revealStartingNumberCard);

        assertEquals(1, deck.drawPileSize());
        assertEquals(wild, deck.draw());
    }
}
