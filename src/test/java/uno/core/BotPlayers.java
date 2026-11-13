package uno.core;

import java.util.List;

/** Test hands for strategy tests, without exposing player mutators to the bots. */
public final class BotPlayers {
    private BotPlayers() {
    }

    public static Player withHand(BotProfile profile, List<Card> cards) {
        Player player = new Player(new GameConfig.PlayerConfig("Bot", PlayerKind.BOT, profile));
        cards.forEach(player::addCard);
        return player;
    }

    public static GameEngine game(BotProfile profile, List<Card> hand, List<Card> opponent,
                                  Card topCard, Card... nextDraws) {
        GameConfig config = new GameConfig(GameMode.SINGLE_ROUND, List.of(
                new GameConfig.PlayerConfig("Bot", PlayerKind.BOT, profile),
                new GameConfig.PlayerConfig("Human", PlayerKind.HUMAN, null)), 500);
        return GameFixture.game(config, List.of(hand, opponent), topCard, topCard.color(), 0,
                List.of(nextDraws));
    }
}
