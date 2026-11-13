package uno.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import uno.core.BotPlayers;
import uno.core.BotProfile;
import uno.core.Card;
import uno.core.CardColor;
import uno.core.CardKind;
import uno.core.GameConfig;
import uno.core.GameEngine;
import uno.core.GameMode;
import uno.core.GamePhase;
import uno.core.Player;
import uno.core.PlayerKind;

class BotControllerTest {
    @Test
    void controllerRejectsHumanTurns() {
        GameConfig config = new GameConfig(GameMode.SINGLE_ROUND, List.of(
                new GameConfig.PlayerConfig("Human", PlayerKind.HUMAN, null),
                new GameConfig.PlayerConfig("Bot", PlayerKind.BOT, BotProfile.GREEDY)), 500);

        assertFalse(new BotController().takeTurn(new GameEngine(config, new Random(1))).accepted());
    }

    @Test
    void controllerUsesTheConservativePolicyInsteadOfGreedy() {
        Card number = Card.number(CardColor.RED, 9);
        GameEngine game = BotPlayers.game(BotProfile.CONSERVATIVE,
                List.of(Card.action(CardColor.RED, CardKind.DRAW_TWO), number,
                        Card.wild(CardKind.WILD), Card.number(CardColor.BLUE, 3)),
                List.of(Card.number(CardColor.GREEN, 2)), Card.number(CardColor.RED, 0));

        assertTrue(new BotController(new Random(1)).takeTurn(game).accepted());

        assertEquals(number, game.topCard());
        assertEquals(3, game.players().get(0).handSize());
        assertConserved(game);
    }

    @Test
    void controllerUsesTheAggressiveNonAttackFallbackInsteadOfGreedy() {
        Card zero = Card.number(CardColor.RED, 0);
        GameEngine game = BotPlayers.game(BotProfile.AGGRESSIVE,
                List.of(Card.action(CardColor.RED, CardKind.REVERSE), zero),
                List.of(Card.number(CardColor.GREEN, 2)), Card.number(CardColor.RED, 3));

        assertTrue(new BotController(new Random(1)).takeTurn(game).accepted());

        assertEquals(zero, game.topCard());
        assertEquals(1, game.currentPlayerIndex());
        assertNull(game.unoTarget());
        assertConserved(game);
    }

    @Test
    void aggressiveUsesALegalDrawFourBeforeAPlayableDrawTwo() {
        Card drawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        GameEngine game = BotPlayers.game(BotProfile.AGGRESSIVE,
                List.of(Card.action(CardColor.BLUE, CardKind.DRAW_TWO), drawFour,
                        Card.number(CardColor.BLUE, 9)),
                List.of(Card.number(CardColor.GREEN, 2)), Card.action(CardColor.RED, CardKind.DRAW_TWO));

        assertTrue(new BotController(new Random(1)).takeTurn(game).accepted());

        assertEquals(drawFour, game.topCard());
        assertEquals(CardColor.BLUE, game.currentColor());
        assertEquals(GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE, game.phase());
        assertConserved(game);
    }

    @Test
    void everyProfileRejectsAnIllegalDrawFourAndPlaysItsMatchingColour() {
        for (BotProfile profile : BotProfile.values()) {
            Card number = Card.number(CardColor.RED, 5);
            GameEngine game = BotPlayers.game(profile, List.of(Card.wild(CardKind.WILD_DRAW_FOUR), number),
                    List.of(Card.number(CardColor.GREEN, 2)), Card.number(CardColor.RED, 9));

            assertTrue(new BotController(new Random(1)).takeTurn(game).accepted());

            assertEquals(number, game.topCard(), profile.displayName());
            assertEquals(1, game.players().get(0).handSize());
            assertNull(game.unoTarget());
            assertConserved(game);
        }
    }

    @Test
    void everyProfileKeepsAnIllegalDrawFourDrawnDuringTheTurn() {
        Card drawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        Card top = Card.number(CardColor.RED, 9);
        for (BotProfile profile : BotProfile.values()) {
            GameEngine game = BotPlayers.game(profile,
                    List.of(Card.number(CardColor.RED, 5), Card.number(CardColor.BLUE, 1)),
                    List.of(Card.number(CardColor.GREEN, 2)), top, drawFour);
            assertTrue(game.drawCard().accepted());
            assertEquals(GamePhase.DRAWN_CARD_AVAILABLE, game.phase());

            assertTrue(new BotController(new Random(1)).takeTurn(game).accepted());

            assertEquals(top, game.topCard(), profile.displayName());
            assertEquals(3, game.players().get(0).handSize());
            assertEquals(1, game.currentPlayerIndex());
            assertEquals(GamePhase.TURN, game.phase());
            assertConserved(game);
        }
    }

    @Test
    void mixedProfileGamesFinishWithTwoToSixPlayers() {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            BotProfile[] profiles = BotProfile.values();
            for (int count = 2; count <= 6; count++) {
                List<BotProfile> seats = new ArrayList<>();
                for (int index = 0; index < count; index++) {
                    seats.add(profiles[(index + count) % profiles.length]);
                }
                playGame(seats, count);
                playGame(seats, count + 100);
            }
        });
    }

    @Test
    void eachProfileCanFinishHomogeneousGamesWithTwoToSixPlayers() {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            for (BotProfile profile : BotProfile.values()) {
                for (int count = 2; count <= 6; count++) {
                    List<BotProfile> seats = new ArrayList<>();
                    for (int index = 0; index < count; index++) {
                        seats.add(profile);
                    }
                    playGame(seats, count);
                    playGame(seats, count + 100);
                }
            }
        });
    }

    private static void playGame(List<BotProfile> profiles, int seed) {
        List<GameConfig.PlayerConfig> players = new ArrayList<>();
        for (int index = 0; index < profiles.size(); index++) {
            players.add(new GameConfig.PlayerConfig("Bot " + index, PlayerKind.BOT, profiles.get(index)));
        }
        String scenario = profiles + ", seed " + seed;
        GameEngine game = new GameEngine(new GameConfig(GameMode.SINGLE_ROUND, players, 500), new Random(seed));
        BotController controller = new BotController(new Random(seed + 42));
        for (int action = 0; action < 20_000 && game.phase() != GamePhase.GAME_OVER; action++) {
            GamePhase previousPhase = game.phase();
            int actor = game.currentPlayerIndex();
            int handSize = game.currentPlayer().handSize();
            boolean holdsCurrentColour = game.currentPlayer().hand().stream()
                    .anyMatch(card -> card.color() == game.currentColor());
            boolean canPlay = game.playableCards().stream()
                    .anyMatch(card -> card.kind() != CardKind.WILD_DRAW_FOUR || !holdsCurrentColour);

            assertTrue(controller.takeTurn(game).accepted(), "Invalid bot action: " + scenario);

            if (canPlay) {
                assertEquals(handSize - 1, game.players().get(actor).handSize(),
                        "The bot should play rather than draw: " + scenario);
            }
            if (previousPhase != GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE
                    && game.phase() == GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE) {
                assertFalse(holdsCurrentColour, "An illegal +4 was played: " + scenario);
            }
            assertNull(game.unoTarget(), "The bot failed to declare UNO: " + scenario);
            assertConserved(game);
        }
        assertEquals(GamePhase.GAME_OVER, game.phase(), "Game did not finish: " + scenario);
    }

    private static void assertConserved(GameEngine game) {
        int handCards = game.players().stream().mapToInt(Player::handSize).sum();
        assertEquals(108, handCards + game.drawPileSize() + game.discardPileSize());
    }
}
