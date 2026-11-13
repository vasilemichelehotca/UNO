package uno.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Owns both physical piles. When the draw pile becomes empty it reshuffles all
 * discarded cards except the visible top card back into the draw pile.
 */
public final class Deck {
    private final Deque<Card> drawPile = new ArrayDeque<>();
    private final List<Card> discardPile = new ArrayList<>();
    private final Random random;

    Deck(List<Card> cards, Random random) {
        this.random = Objects.requireNonNull(random, "random");
        drawPile.addAll(cards);
    }

    /** Creates and shuffles the exact 108-card classic UNO deck. */
    public static Deck standard(Random random) {
        List<Card> cards = new ArrayList<>(108);
        for (CardColor color : CardColor.playableColors()) {
            cards.add(Card.number(color, 0));
            for (int value = 1; value <= 9; value++) {
                cards.add(Card.number(color, value));
                cards.add(Card.number(color, value));
            }
            for (int copy = 0; copy < 2; copy++) {
                cards.add(Card.action(color, CardKind.SKIP));
                cards.add(Card.action(color, CardKind.REVERSE));
                cards.add(Card.action(color, CardKind.DRAW_TWO));
            }
        }
        for (int copy = 0; copy < 4; copy++) {
            cards.add(Card.wild(CardKind.WILD));
            cards.add(Card.wild(CardKind.WILD_DRAW_FOUR));
        }
        Collections.shuffle(cards, random);
        return new Deck(cards, random);
    }

    /** Draws the next card, recycling the discard pile when necessary. */
    public Card draw() {
        if (drawPile.isEmpty()) {
            recycleDiscards();
        }
        return drawPile.pollFirst();
    }

    /** Adds a played card to the discard pile. */
    public void discard(Card card) {
        discardPile.add(Objects.requireNonNull(card, "card"));
    }

    /**
     * Reveals a coloured number card to begin a round. Non-number cards drawn
     * while looking for it are returned to the bottom of the draw pile.
     */
    public Card revealStartingNumberCard() {
        List<Card> deferred = new ArrayList<>();
        Card card;
        while ((card = draw()) != null) {
            if (card.kind() == CardKind.NUMBER) {
                drawPile.addAll(deferred);
                discard(card);
                return card;
            }
            deferred.add(card);
        }
        drawPile.addAll(deferred);
        throw new IllegalStateException("A classic deck must contain a number card.");
    }

    /** Returns the number of cards that can currently be drawn without recycling. */
    public int drawPileSize() {
        return drawPile.size();
    }

    /** Returns the number of discarded cards, including the visible top card. */
    public int discardPileSize() {
        return discardPile.size();
    }

    private void recycleDiscards() {
        if (discardPile.size() <= 1) {
            return;
        }
        Card topCard = discardPile.remove(discardPile.size() - 1);
        Collections.shuffle(discardPile, random);
        drawPile.addAll(discardPile);
        discardPile.clear();
        discardPile.add(topCard);
    }
}
