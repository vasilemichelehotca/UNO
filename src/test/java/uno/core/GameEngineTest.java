package uno.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import uno.bot.BotController;

class GameEngineTest {
    @Test
    void initialDealUsesSevenCardsEachAndOneVisibleNumber() {
        for (int playerCount = 2; playerCount <= 6; playerCount++) {
            GameEngine game = new GameEngine(GameFixture.config(GameMode.SINGLE_ROUND, 500, playerCount),
                    new Random(42));

            assertTrue(game.players().stream().allMatch(player -> player.handSize() == 7));
            assertEquals(CardKind.NUMBER, game.topCard().kind());
            assertEquals(game.topCard().color(), game.currentColor());
            assertEquals(1, game.roundNumber());
            assertEquals(GamePhase.TURN, game.phase());
            assertConserved(game);
        }
    }

    @Test
    void drawTwoMakesNextPlayerDrawAndLoseTurn() {
        Card drawTwo = Card.action(CardColor.RED, CardKind.DRAW_TWO);
        GameEngine game = GameFixture.game(List.of(
                List.of(drawTwo, Card.number(CardColor.BLUE, 1)),
                List.of(Card.number(CardColor.BLUE, 5))), Card.number(CardColor.RED, 3));

        assertTrue(game.playCard(drawTwo, null, true).accepted());

        assertEquals(3, game.players().get(1).handSize());
        assertEquals(0, game.currentPlayerIndex());
        assertEquals(GamePhase.TURN, game.phase());
        assertConserved(game);
    }

    @Test
    void finalDrawTwoAddsThePenaltyCardsBeforeScoring() {
        Card drawTwo = Card.action(CardColor.RED, CardKind.DRAW_TWO);
        GameEngine game = GameFixture.game(List.of(
                List.of(drawTwo), List.of(Card.number(CardColor.GREEN, 7))),
                Card.number(CardColor.RED, 3), Card.number(CardColor.BLUE, 8),
                Card.action(CardColor.YELLOW, CardKind.SKIP));

        assertTrue(game.playCard(drawTwo, null, false).accepted());

        assertEquals(GamePhase.GAME_OVER, game.phase());
        assertEquals(3, game.players().get(1).handSize());
        assertEquals(35, game.lastRoundResult().awardedPoints());
        assertEquals("Alice", game.gameWinner().name());
        assertConserved(game);
    }

    @Test
    void finalWildDrawFourWaitsForAcceptanceAndScoresFourDrawnCards() {
        Card wildDrawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        GameEngine game = GameFixture.game(List.of(List.of(wildDrawFour),
                List.of(Card.number(CardColor.GREEN, 7))), Card.number(CardColor.RED, 3),
                Card.number(CardColor.BLUE, 1), Card.number(CardColor.BLUE, 2),
                Card.number(CardColor.BLUE, 3), Card.number(CardColor.BLUE, 4));

        assertTrue(game.playCard(wildDrawFour, CardColor.BLUE, false).accepted());

        assertEquals(GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE, game.phase());
        assertNull(game.lastRoundResult());
        assertNull(game.gameWinner());
        assertEquals(0, game.players().get(0).score());
        assertTrue(game.acceptWildDrawFour().accepted());
        assertEquals(GamePhase.GAME_OVER, game.phase());
        assertEquals(5, game.players().get(1).handSize());
        assertEquals(17, game.lastRoundResult().awardedPoints());
        assertConserved(game);
    }

    @Test
    void challengingAFinalWildDrawFourScoresAllSixPenaltyCards() {
        Card wildDrawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        GameEngine game = GameFixture.game(List.of(List.of(wildDrawFour),
                List.of(Card.number(CardColor.GREEN, 7))), Card.number(CardColor.RED, 3),
                Card.number(CardColor.BLUE, 1), Card.number(CardColor.BLUE, 2),
                Card.number(CardColor.BLUE, 3), Card.number(CardColor.BLUE, 4),
                Card.number(CardColor.BLUE, 5), Card.number(CardColor.BLUE, 6));

        game.playCard(wildDrawFour, CardColor.BLUE, false);
        assertTrue(game.challengeWildDrawFour().accepted());

        assertEquals(GamePhase.GAME_OVER, game.phase());
        assertEquals(7, game.players().get(1).handSize());
        assertEquals(28, game.lastRoundResult().awardedPoints());
        assertConserved(game);
    }

    @Test
    void successfulChallengeAddsFourCardsToTheOffenderAndKeepsChallengerTurn() {
        Card wildDrawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        GameEngine game = GameFixture.game(List.of(
                List.of(wildDrawFour, Card.number(CardColor.RED, 5)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3));

        game.playCard(wildDrawFour, CardColor.BLUE, false);
        assertTrue(game.challengeWildDrawFour().accepted());

        assertEquals(5, game.players().get(0).handSize());
        assertEquals(1, game.currentPlayerIndex());
        assertEquals(GamePhase.TURN, game.phase());
        assertEquals(CardColor.BLUE, game.currentColor());
        assertNull(game.lastRoundResult());
        assertNull(game.gameWinner());
        assertFalse(game.canCurrentPlayerCatchUno());
        assertConserved(game);
    }

    @Test
    void failedChallengeCostsSixAndSkipsChallenger() {
        Card wildDrawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        GameEngine game = GameFixture.game(List.of(
                List.of(wildDrawFour, Card.number(CardColor.BLUE, 5)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3));

        game.playCard(wildDrawFour, CardColor.BLUE, false);
        assertTrue(game.challengeWildDrawFour().accepted());

        assertEquals(7, game.players().get(1).handSize());
        assertEquals(0, game.currentPlayerIndex());
        assertEquals(GamePhase.TURN, game.phase());
        assertFalse(game.canCurrentPlayerCatchUno());
        assertNull(game.unoTarget());
        assertConserved(game);
    }

    @Test
    void aMatchingNumberDoesNotMakeAWildDrawFourIllegal() {
        Card wildDrawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        GameEngine game = GameFixture.game(List.of(
                List.of(wildDrawFour, Card.number(CardColor.BLUE, 3)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3));

        game.playCard(wildDrawFour, CardColor.GREEN, true);
        game.challengeWildDrawFour();

        assertEquals(1, game.players().get(0).handSize());
        assertEquals(7, game.players().get(1).handSize());
        assertConserved(game);
    }

    @Test
    void theNextPlayerCanCatchUnoBeforeRespondingToAWildDrawFour() {
        Card wildDrawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        GameEngine game = GameFixture.game(List.of(
                List.of(wildDrawFour, Card.number(CardColor.BLUE, 5)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3));

        game.playCard(wildDrawFour, CardColor.BLUE, false);
        assertTrue(game.canCurrentPlayerCatchUno());
        assertTrue(game.catchUno().accepted());
        assertEquals(3, game.players().get(0).handSize());
        assertEquals(GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE, game.phase());
        assertTrue(game.acceptWildDrawFour().accepted());
        assertConserved(game);
    }

    @Test
    void catchingUnoDoesNotChangeTheRecordedWildDrawFourLegality() {
        Card wildDrawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        GameEngine game = GameFixture.game(List.of(
                List.of(wildDrawFour, Card.number(CardColor.BLUE, 5)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3),
                Card.number(CardColor.RED, 7), Card.number(CardColor.RED, 8));

        game.playCard(wildDrawFour, CardColor.BLUE, false);
        game.catchUno();
        game.challengeWildDrawFour();

        assertEquals(3, game.players().get(0).handSize());
        assertEquals(7, game.players().get(1).handSize());
        assertConserved(game);
    }

    @Test
    void acceptingWildDrawFourClosesTheUnoWindowBeforeTheFollowingTurn() {
        Card wildDrawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        GameEngine game = GameFixture.game(List.of(
                List.of(wildDrawFour, Card.number(CardColor.BLUE, 5)),
                List.of(Card.number(CardColor.GREEN, 2)),
                List.of(Card.number(CardColor.YELLOW, 4))), Card.number(CardColor.RED, 3));

        game.playCard(wildDrawFour, CardColor.BLUE, false);
        assertTrue(game.canCurrentPlayerCatchUno());
        game.acceptWildDrawFour();

        assertEquals(2, game.currentPlayerIndex());
        assertFalse(game.canCurrentPlayerCatchUno());
        assertNull(game.unoTarget());
        assertFalse(game.catchUno().accepted());
        assertConserved(game);
    }

    @Test
    void skippedOpponentCanCatchUnoWhenAnActionGivesTheOffenderAnotherTurn() {
        for (CardKind kind : List.of(CardKind.SKIP, CardKind.REVERSE, CardKind.DRAW_TWO)) {
            Card action = Card.action(CardColor.RED, kind);
            GameEngine game = GameFixture.game(List.of(
                    List.of(action, Card.number(CardColor.BLUE, 1)),
                    List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3));

            game.playCard(action, null, false);

            assertEquals(0, game.currentPlayerIndex());
            assertFalse(game.canCurrentPlayerCatchUno());
            assertTrue(game.canCatchUno(1));
            assertTrue(game.catchUno(1).accepted());
            assertEquals(3, game.players().get(0).handSize());
            assertEquals(0, game.currentPlayerIndex());
            assertConserved(game);
        }
    }

    @Test
    void aSkippedBotCatchesUnoBeforeTheHumanTakesAnotherTurn() {
        GameConfig config = new GameConfig(GameMode.SINGLE_ROUND, List.of(
                new GameConfig.PlayerConfig("Alice", PlayerKind.HUMAN, null),
                new GameConfig.PlayerConfig("Bob", PlayerKind.BOT, BotProfile.RANDOM)), 500);
        Card skip = Card.action(CardColor.RED, CardKind.SKIP);
        GameEngine game = GameFixture.game(config, List.of(
                List.of(skip, Card.number(CardColor.BLUE, 5)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3),
                CardColor.RED, 0, List.of());
        BotController controller = new BotController(new Random(1));

        game.playCard(skip, null, false);
        assertEquals(PlayerKind.HUMAN, game.currentPlayer().kind());
        assertTrue(controller.hasPendingAction(game));
        assertTrue(controller.takeTurn(game).accepted());

        assertEquals(3, game.players().get(0).handSize());
        assertEquals(0, game.currentPlayerIndex());
        assertFalse(controller.hasPendingAction(game));
        assertConserved(game);
    }

    @Test
    void continuingASecondTurnClosesTheSkippedOpponentsUnoWindow() {
        Card skip = Card.action(CardColor.RED, CardKind.SKIP);
        GameEngine game = GameFixture.game(List.of(
                List.of(skip, Card.number(CardColor.BLUE, 5)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3),
                Card.number(CardColor.YELLOW, 8));

        game.playCard(skip, null, false);
        assertTrue(game.canCatchUno(1));
        game.drawCard();

        assertNull(game.unoTarget());
        assertFalse(game.catchUno(0).accepted());
        assertFalse(game.catchUno(1).accepted());
        assertFalse(game.catchUno(-1).accepted());
        assertFalse(game.catchUno(2).accepted());
        assertConserved(game);
    }

    @Test
    void reverseChangesTheTurnOrderForThreePlayers() {
        Card reverse = Card.action(CardColor.RED, CardKind.REVERSE);
        GameEngine game = GameFixture.game(List.of(
                List.of(reverse, Card.number(CardColor.BLUE, 1)),
                List.of(Card.number(CardColor.GREEN, 2)),
                List.of(Card.number(CardColor.YELLOW, 4))), Card.number(CardColor.RED, 3));

        game.playCard(reverse, null, true);

        assertEquals(Direction.COUNTERCLOCKWISE, game.direction());
        assertEquals(2, game.currentPlayerIndex());
        assertConserved(game);
    }

    @Test
    void immediateUnoCatchAddsTwoCardsAndCannotBeRepeated() {
        Card played = Card.number(CardColor.RED, 5);
        GameEngine game = GameFixture.game(List.of(
                List.of(played, Card.number(CardColor.BLUE, 1)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3));

        game.playCard(played, null, false);
        assertTrue(game.canCurrentPlayerCatchUno());
        assertTrue(game.catchUno().accepted());

        assertEquals(3, game.players().get(0).handSize());
        assertFalse(game.catchUno().accepted());
        assertConserved(game);
    }

    @Test
    void rejectedMovesDoNotCloseTheUnoWindow() {
        Card played = Card.number(CardColor.RED, 5);
        Card invalid = Card.number(CardColor.GREEN, 2);
        GameEngine game = GameFixture.game(List.of(
                List.of(played, Card.number(CardColor.BLUE, 1)), List.of(invalid)),
                Card.number(CardColor.RED, 3));

        game.playCard(played, null, false);
        assertFalse(game.playCard(invalid, null, false).accepted());

        assertTrue(game.canCurrentPlayerCatchUno());
        assertEquals(1, game.currentPlayerIndex());
        assertConserved(game);
    }

    @Test
    void drawingClosesThePreviousUnoWindow() {
        Card played = Card.number(CardColor.RED, 5);
        GameEngine game = GameFixture.game(List.of(
                List.of(played, Card.number(CardColor.BLUE, 1)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3),
                Card.number(CardColor.YELLOW, 8));

        game.playCard(played, null, false);
        assertTrue(game.drawCard().accepted());

        assertNull(game.unoTarget());
        assertFalse(game.catchUno().accepted());
        assertConserved(game);
    }

    @Test
    void onlyTheDrawnCardCanBePlayedAfterDrawing() {
        Card oldPlayableCard = Card.number(CardColor.RED, 5);
        Card drawnCard = Card.number(CardColor.RED, 8);
        GameEngine game = GameFixture.game(List.of(
                List.of(oldPlayableCard, Card.number(CardColor.BLUE, 1)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3), drawnCard);

        assertTrue(game.drawCard().accepted());

        assertEquals(GamePhase.DRAWN_CARD_AVAILABLE, game.phase());
        assertEquals(List.of(drawnCard), game.playableCards());
        assertFalse(game.drawCard().accepted());
        assertFalse(game.playCard(oldPlayableCard, null, false).accepted());
        assertTrue(game.playCard(drawnCard, null, false).accepted());
        assertEquals(1, game.currentPlayerIndex());
        assertConserved(game);
    }

    @Test
    void aPlayableDrawnCardCanBeKept() {
        GameEngine game = GameFixture.game(List.of(
                List.of(Card.number(CardColor.BLUE, 1)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3),
                Card.number(CardColor.RED, 8));

        game.drawCard();
        assertTrue(game.passAfterDrawing().accepted());

        assertEquals(2, game.players().get(0).handSize());
        assertEquals(GamePhase.TURN, game.phase());
        assertEquals(1, game.currentPlayerIndex());
        assertFalse(game.passAfterDrawing().accepted());
        assertConserved(game);
    }

    @Test
    void anUnplayableDrawnCardAutomaticallyEndsTheTurn() {
        GameEngine game = GameFixture.game(List.of(
                List.of(Card.number(CardColor.BLUE, 1)),
                List.of(Card.number(CardColor.GREEN, 2))), Card.number(CardColor.RED, 3),
                Card.number(CardColor.YELLOW, 8));

        assertTrue(game.drawCard().accepted());

        assertEquals(GamePhase.TURN, game.phase());
        assertEquals(1, game.currentPlayerIndex());
        assertConserved(game);
    }

    @Test
    void anEmptyDrawPilePassesTheTurnInsteadOfRejectingForever() {
        Card top = Card.number(CardColor.RED, 3);
        Card firstHand = Card.number(CardColor.BLUE, 1);
        List<Card> otherHand = GameFixture.standardCards();
        assertTrue(otherHand.remove(top));
        assertTrue(otherHand.remove(firstHand));
        GameEngine game = GameFixture.game(List.of(List.of(firstHand), otherHand), top);

        assertEquals(0, game.drawPileSize());
        assertTrue(game.drawCard().accepted());

        assertEquals(1, game.currentPlayerIndex());
        assertEquals(GamePhase.TURN, game.phase());
        assertEquals(1, game.players().get(0).handSize());
        assertConserved(game);
    }

    @Test
    void aPenaltyDrawsOnlyTheCardsThatCanActuallyBeRecycled() {
        Card top = Card.number(CardColor.RED, 3);
        Card drawTwo = Card.action(CardColor.RED, CardKind.DRAW_TWO);
        Card remainingCard = Card.number(CardColor.BLUE, 1);
        List<Card> otherHand = GameFixture.standardCards();
        otherHand.remove(top);
        otherHand.remove(drawTwo);
        otherHand.remove(remainingCard);
        int initialSize = otherHand.size();
        GameEngine game = GameFixture.game(List.of(List.of(drawTwo, remainingCard), otherHand), top);

        game.playCard(drawTwo, null, true);

        assertEquals(initialSize + 1, game.players().get(1).handSize());
        assertEquals(1, game.discardPileSize());
        assertEquals(drawTwo, game.topCard());
        assertConserved(game);
    }

    @Test
    void normalFinalCardAwardsTheOtherHandsAndBlocksLaterMoves() {
        Card finalCard = Card.number(CardColor.RED, 5);
        GameEngine game = GameFixture.game(List.of(List.of(finalCard),
                List.of(Card.number(CardColor.GREEN, 7), Card.action(CardColor.BLUE, CardKind.SKIP))),
                Card.number(CardColor.RED, 1));

        game.playCard(finalCard, null, false);

        assertEquals(GamePhase.GAME_OVER, game.phase());
        assertEquals(27, game.lastRoundResult().awardedPoints());
        assertSame(game.players().get(0), game.gameWinner());
        assertFalse(game.drawCard().accepted());
        assertFalse(game.acceptWildDrawFour().accepted());
        assertFalse(game.challengeWildDrawFour().accepted());
        assertFalse(game.startNextRound().accepted());
        assertConserved(game);
    }

    @Test
    void pointsMatchKeepsScoresAndResetsRoundState() {
        GameConfig config = GameFixture.config(GameMode.POINTS, 100, 2);
        Card finalCard = Card.number(CardColor.RED, 5);
        GameEngine game = GameFixture.game(config, List.of(List.of(finalCard),
                List.of(Card.number(CardColor.GREEN, 7))), Card.number(CardColor.RED, 1),
                CardColor.RED, 0, List.of());

        game.playCard(finalCard, null, false);

        assertEquals(GamePhase.ROUND_OVER, game.phase());
        assertEquals(7, game.players().get(0).score());
        assertNotNull(game.lastRoundResult());
        assertNull(game.gameWinner());
        assertTrue(game.startNextRound().accepted());
        assertEquals(2, game.roundNumber());
        assertEquals(GamePhase.TURN, game.phase());
        assertEquals(7, game.players().get(0).score());
        assertTrue(game.players().stream().allMatch(player -> player.handSize() == 7));
        assertNull(game.lastRoundResult());
        assertConserved(game);
    }

    @Test
    void reachingThePointsTargetEndsTheMatch() {
        GameConfig config = GameFixture.config(GameMode.POINTS, 5, 2);
        Card finalCard = Card.number(CardColor.RED, 5);
        GameEngine game = GameFixture.game(config, List.of(List.of(finalCard),
                List.of(Card.number(CardColor.GREEN, 7))), Card.number(CardColor.RED, 1),
                CardColor.RED, 0, List.of());

        game.playCard(finalCard, null, false);

        assertEquals(GamePhase.GAME_OVER, game.phase());
        assertSame(game.players().get(0), game.gameWinner());
        assertTrue(game.lastRoundResult().gameFinished());
    }

    @Test
    void invalidWildSelectionDoesNotChangeTheGame() {
        Card wild = Card.wild(CardKind.WILD);
        Card top = Card.number(CardColor.RED, 3);
        GameEngine game = GameFixture.game(List.of(List.of(wild),
                List.of(Card.number(CardColor.GREEN, 2))), top);

        assertFalse(game.playCard(wild, null, false).accepted());
        assertFalse(game.playCard(wild, CardColor.NONE, false).accepted());

        assertEquals(top, game.topCard());
        assertEquals(1, game.players().get(0).handSize());
        assertEquals(0, game.currentPlayerIndex());
        assertEquals(GamePhase.TURN, game.phase());
        assertConserved(game);
    }

    @Test
    void aWildDrawFourResponseMustBeResolvedBeforeAnotherTurnAction() {
        Card wildDrawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        Card otherCard = Card.number(CardColor.BLUE, 2);
        GameEngine game = GameFixture.game(List.of(
                List.of(wildDrawFour, Card.number(CardColor.GREEN, 5)), List.of(otherCard)),
                Card.number(CardColor.RED, 3));

        game.playCard(wildDrawFour, CardColor.BLUE, true);

        assertFalse(game.drawCard().accepted());
        assertFalse(game.playCard(otherCard, null, false).accepted());
        assertFalse(game.passAfterDrawing().accepted());
        assertFalse(game.startNextRound().accepted());
        assertEquals(GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE, game.phase());
        assertEquals(1, game.currentPlayerIndex());
        assertConserved(game);
    }

    @Test
    void matchingUsesTheSelectedWildColorAndRegularActionSymbols() {
        Card top = Card.wild(CardKind.WILD);
        GameEngine game = GameFixture.game(GameFixture.config(GameMode.SINGLE_ROUND, 500, 2),
                List.of(List.of(Card.number(CardColor.BLUE, 2)),
                        List.of(Card.number(CardColor.GREEN, 3))), top, CardColor.BLUE, 0, List.of());

        assertTrue(game.isPlayable(Card.number(CardColor.BLUE, 8)));
        assertFalse(game.isPlayable(Card.number(CardColor.RED, 8)));
        assertTrue(game.isPlayable(Card.wild(CardKind.WILD_DRAW_FOUR)));

        Card redSkip = Card.action(CardColor.RED, CardKind.SKIP);
        GameEngine actionGame = GameFixture.game(List.of(List.of(Card.number(CardColor.RED, 2)),
                List.of(Card.number(CardColor.GREEN, 3))), redSkip);
        assertTrue(actionGame.isPlayable(Card.action(CardColor.BLUE, CardKind.SKIP)));
        assertFalse(actionGame.isPlayable(Card.action(CardColor.BLUE, CardKind.REVERSE)));
        assertTrue(actionGame.isPlayable(Card.number(CardColor.RED, 8)));
    }

    @Test
    void exposedCollectionsCannotBeChangedByCallers() {
        GameEngine game = new GameEngine(GameFixture.config(GameMode.SINGLE_ROUND, 500, 2),
                new Random(42));
        List<String> events = game.events();
        List<Card> hand = game.currentPlayer().hand();

        assertThrows(UnsupportedOperationException.class, () -> game.players().clear());
        assertThrows(UnsupportedOperationException.class, events::clear);
        assertThrows(UnsupportedOperationException.class, hand::clear);
        assertThrows(UnsupportedOperationException.class, () -> game.playableCards().clear());
        assertSame(events, game.events());
        assertSame(hand, game.currentPlayer().hand());
    }

    @Test
    void fixtureRejectsMoreCardsThanTheDeckContains() {
        Card redZero = Card.number(CardColor.RED, 0);
        assertThrows(IllegalArgumentException.class, () -> GameFixture.game(List.of(
                List.of(redZero, redZero), List.of(Card.number(CardColor.BLUE, 1))),
                Card.number(CardColor.GREEN, 3)));
    }

    private static void assertConserved(GameEngine game) {
        int heldCards = game.players().stream().mapToInt(Player::handSize).sum();
        assertEquals(108, heldCards + game.drawPileSize() + game.discardPileSize());
    }
}
