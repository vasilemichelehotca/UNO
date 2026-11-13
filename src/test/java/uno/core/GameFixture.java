package uno.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Deals controlled hands without adding cards to the standard deck. */
final class GameFixture {
    private GameFixture() {
    }

    static GameEngine game(List<List<Card>> hands, Card topCard, Card... nextDraws) {
        return game(config(GameMode.SINGLE_ROUND, 500, hands.size()), hands, topCard,
                topCard.color(), 0, List.of(nextDraws));
    }

    static GameEngine game(GameConfig config, List<List<Card>> hands, Card topCard,
                           CardColor activeColor, int activePlayer, List<Card> nextDraws) {
        List<Card> remaining = standardCards();
        for (List<Card> hand : hands) {
            hand.forEach(card -> remove(remaining, card));
        }
        remove(remaining, topCard);
        nextDraws.forEach(card -> remove(remaining, card));
        Collections.shuffle(remaining, new Random(7));
        List<Card> drawOrder = new ArrayList<>(nextDraws);
        drawOrder.addAll(remaining);
        Deck deck = new Deck(drawOrder, new Random(7));
        deck.discard(topCard);
        return GameEngine.scenario(config, hands, deck, topCard, activeColor, activePlayer);
    }

    static GameConfig config(GameMode mode, int target, int playerCount) {
        List<String> names = List.of("Alice", "Bob", "Carol", "Dan", "Eve", "Frank");
        List<GameConfig.PlayerConfig> seats = new ArrayList<>();
        for (int index = 0; index < playerCount; index++) {
            seats.add(new GameConfig.PlayerConfig(names.get(index), PlayerKind.HUMAN, null));
        }
        return new GameConfig(mode, seats, target);
    }

    static List<Card> standardCards() {
        Deck deck = Deck.standard(new Random(1));
        List<Card> cards = new ArrayList<>(108);
        Card card;
        while ((card = deck.draw()) != null) {
            cards.add(card);
        }
        return cards;
    }

    private static void remove(List<Card> cards, Card card) {
        if (!cards.remove(card)) {
            throw new IllegalArgumentException("The scenario uses too many copies of " + card.label());
        }
    }
}
