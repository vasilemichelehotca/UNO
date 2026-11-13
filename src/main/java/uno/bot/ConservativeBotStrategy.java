package uno.bot;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import uno.core.Card;
import uno.core.Player;

/** Saves special cards early, then sheds them when three or fewer cards remain. */
public final class ConservativeBotStrategy implements BotStrategy {
    private static final Comparator<Card> EARLY_PREFERENCE =
            Comparator.comparingInt(ConservativeBotStrategy::earlyPriority);
    private static final Comparator<Card> LATE_PREFERENCE =
            Comparator.comparingInt(ConservativeBotStrategy::latePriority);

    @Override
    public BotDecision chooseMove(Player player, List<Card> playable, Random random) {
        Comparator<Card> preference = player.handSize() <= 3 ? LATE_PREFERENCE : EARLY_PREFERENCE;
        Card card = playable.stream().max(preference).orElseThrow();
        return new BotDecision(card, card.kind().isWild() ? BotColors.mostCommon(player) : null,
                player.handSize() == 2);
    }

    @Override
    public boolean shouldChallenge(Random random) {
        return random.nextDouble() < 0.10;
    }

    private static int earlyPriority(Card card) {
        return switch (card.kind()) {
            case NUMBER -> 100 + card.number();
            case DRAW_TWO, SKIP, REVERSE -> 50;
            case WILD -> 10;
            case WILD_DRAW_FOUR -> 0;
        };
    }

    private static int latePriority(Card card) {
        return switch (card.kind()) {
            case WILD_DRAW_FOUR -> 70;
            case WILD -> 60;
            case DRAW_TWO -> 50;
            case SKIP -> 40;
            case REVERSE -> 30;
            case NUMBER -> card.number();
        };
    }
}
