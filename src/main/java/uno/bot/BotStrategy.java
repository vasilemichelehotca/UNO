package uno.bot;

import java.util.List;
import java.util.Random;
import uno.core.Card;
import uno.core.Player;

/** Chooses moves without changing the game state. */
public interface BotStrategy {
    /** Chooses one of the playable cards for this bot turn. */
    BotDecision chooseMove(Player player, List<Card> playable, Random random);

    /** Returns whether the bot challenges a pending Wild Draw Four. */
    boolean shouldChallenge(Random random);
}
