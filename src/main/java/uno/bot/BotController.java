package uno.bot;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import uno.core.ActionResult;
import uno.core.BotProfile;
import uno.core.Card;
import uno.core.CardKind;
import uno.core.GameEngine;
import uno.core.GamePhase;
import uno.core.Player;
import uno.core.PlayerKind;

/** Executes one complete decision step for the computer player whose turn it is. */
public final class BotController {
    private static final BotStrategy RANDOM_STRATEGY = new RandomBotStrategy();
    private static final BotStrategy GREEDY_STRATEGY = new GreedyBotStrategy();
    private static final BotStrategy CONSERVATIVE_STRATEGY = new ConservativeBotStrategy();
    private static final BotStrategy AGGRESSIVE_STRATEGY = new AggressiveBotStrategy();
    private final Random random;

    /** Creates a controller for normal play. */
    public BotController() {
        this(new Random());
    }

    /** Uses the supplied random source, allowing repeatable bot decisions in tests. */
    public BotController(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    /** Includes an opponent bot's chance to catch UNO after a two-player Skip. */
    public boolean hasPendingAction(GameEngine engine) {
        return findUnoCaller(engine) >= 0 || engine.currentPlayer().kind() == PlayerKind.BOT
                && (engine.phase() == GamePhase.TURN || engine.phase() == GamePhase.DRAWN_CARD_AVAILABLE
                    || engine.phase() == GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE);
    }

    /** Handles a bot call-out, then the active bot's move if it has the turn. */
    public ActionResult takeTurn(GameEngine engine) {
        int caller = findUnoCaller(engine);
        boolean caughtUno = caller >= 0 && engine.catchUno(caller).accepted();
        Player player = engine.currentPlayer();
        if (player.kind() != PlayerKind.BOT) {
            return caughtUno ? ActionResult.accepted("UNO penalty applied.")
                    : ActionResult.rejected("The active player is human.");
        }
        BotStrategy strategy = strategyFor(player.botProfile());
        if (engine.phase() == GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE) {
            return strategy.shouldChallenge(random) ? engine.challengeWildDrawFour() : engine.acceptWildDrawFour();
        }
        if (engine.phase() != GamePhase.TURN && engine.phase() != GamePhase.DRAWN_CARD_AVAILABLE) {
            return ActionResult.rejected("The bot cannot act in this phase.");
        }
        // All supplied profiles play fairly. Human players can still bluff a +4.
        boolean holdsCurrentColor = player.hand().stream()
                .anyMatch(card -> card.color() == engine.currentColor());
        List<Card> playable = engine.playableCards().stream()
                .filter(card -> card.kind() != CardKind.WILD_DRAW_FOUR || !holdsCurrentColor)
                .toList();
        if (playable.isEmpty()) {
            return engine.phase() == GamePhase.TURN ? engine.drawCard() : engine.passAfterDrawing();
        }
        BotDecision decision = strategy.chooseMove(player, playable, random);
        return engine.playCard(decision.card(), decision.selectedColor(), decision.declaresUno());
    }

    private BotStrategy strategyFor(BotProfile profile) {
        return switch (profile) {
            case RANDOM -> RANDOM_STRATEGY;
            case GREEDY -> GREEDY_STRATEGY;
            case CONSERVATIVE -> CONSERVATIVE_STRATEGY;
            case AGGRESSIVE -> AGGRESSIVE_STRATEGY;
        };
    }

    private int findUnoCaller(GameEngine engine) {
        int size = engine.players().size();
        for (int offset = 0; offset < size; offset++) {
            int index = Math.floorMod(engine.currentPlayerIndex() + offset * engine.direction().step(), size);
            if (engine.players().get(index).kind() == PlayerKind.BOT && engine.canCatchUno(index)) {
                return index;
            }
        }
        return -1;
    }
}
