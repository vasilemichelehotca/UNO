package uno.bot;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import uno.core.Card;
import uno.core.Player;

/** Favours penalties and skips, then number cards before a non-penalty Reverse. */
public final class AggressiveBotStrategy implements BotStrategy {
    private static final Comparator<Card> PREFERENCE = Comparator.comparingInt(AggressiveBotStrategy::priority);

    @Override
    public BotDecision chooseMove(Player player, List<Card> playable, Random random) {
        Card card = playable.stream().max(PREFERENCE).orElseThrow();
        return new BotDecision(card, card.kind().isWild() ? BotColors.mostCommon(player) : null,
                player.handSize() == 2);
    }

    @Override
    public boolean shouldChallenge(Random random) {
        return random.nextDouble() < 0.50;
    }

    private static int priority(Card card) {
        return switch (card.kind()) {
            case WILD_DRAW_FOUR -> 500;
            case DRAW_TWO -> 400;
            case SKIP -> 300;
            case REVERSE -> -1;
            case NUMBER -> card.number() * 10;
            case WILD -> -2;
        };
    }
}
