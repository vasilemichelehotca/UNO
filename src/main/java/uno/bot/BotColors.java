package uno.bot;

import java.util.EnumMap;
import uno.core.Card;
import uno.core.CardColor;
import uno.core.Player;

/** Colour selection shared by the strategies that follow a fixed policy. */
final class BotColors {
    private BotColors() {
    }

    static CardColor mostCommon(Player player) {
        EnumMap<CardColor, Integer> counts = new EnumMap<>(CardColor.class);
        for (Card card : player.hand()) {
            if (card.color() != CardColor.NONE) {
                counts.merge(card.color(), 1, Integer::sum);
            }
        }
        CardColor best = CardColor.RED;
        // Keeping the first colour on a tie also gives an all-Wild hand a default.
        for (CardColor color : CardColor.playableColors()) {
            if (counts.getOrDefault(color, 0) > counts.getOrDefault(best, 0)) {
                best = color;
            }
        }
        return best;
    }
}
