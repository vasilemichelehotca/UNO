package uno.bot;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import uno.core.Card;
import uno.core.Player;

/**
 * An advanced bot that prefers costly action cards, sheds high-value number
 * cards, preserves an ordinary Wild while coloured options exist, and changes
 * to the colour represented most often in its remaining hand.
 */
public final class GreedyBotStrategy implements BotStrategy {
    private static final Comparator<Card> PREFERENCE = Comparator.comparingInt(GreedyBotStrategy::priority);

    @Override
    public BotDecision chooseMove(Player player, List<Card> playable, Random random) {
        Card card = playable.stream().max(PREFERENCE).orElseThrow();
        return new BotDecision(card, card.kind().isWild() ? BotColors.mostCommon(player) : null,
                player.handSize() == 2);
    }

    @Override
    public boolean shouldChallenge(Random random) {
        return random.nextDouble() < 0.35;
    }

    private static int priority(Card card) {
        return switch (card.kind()) {
            case WILD_DRAW_FOUR -> 120;
            case DRAW_TWO -> 105;
            case SKIP -> 95;
            case REVERSE -> 90;
            case WILD -> -1;
            case NUMBER -> card.number() * 8;
        };
    }
}
