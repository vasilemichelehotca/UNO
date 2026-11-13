package uno.bot;

import java.util.List;
import java.util.Random;
import uno.core.Card;
import uno.core.CardColor;
import uno.core.Player;

/** A baseline bot that chooses a legal move and a wild colour uniformly at random. */
public final class RandomBotStrategy implements BotStrategy {
    @Override
    public BotDecision chooseMove(Player player, List<Card> playable, Random random) {
        Card card = playable.get(random.nextInt(playable.size()));
        CardColor color = card.kind().isWild()
                ? CardColor.playableColors().get(random.nextInt(CardColor.playableColors().size()))
                : null;
        return new BotDecision(card, color, player.handSize() == 2);
    }

    @Override
    public boolean shouldChallenge(Random random) {
        return random.nextDouble() < 0.15;
    }
}
