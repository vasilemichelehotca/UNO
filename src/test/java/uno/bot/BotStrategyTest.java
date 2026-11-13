package uno.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import uno.core.BotPlayers;
import uno.core.BotProfile;
import uno.core.Card;
import uno.core.CardColor;
import uno.core.CardKind;
import uno.core.Player;

class BotStrategyTest {
    private static final List<BotStrategy> STRATEGIES = List.of(new RandomBotStrategy(), new GreedyBotStrategy(),
            new ConservativeBotStrategy(), new AggressiveBotStrategy());
    private static final List<BotStrategy> FIXED_STRATEGIES = List.of(new GreedyBotStrategy(),
            new ConservativeBotStrategy(), new AggressiveBotStrategy());

    @Test
    void greedyPrefersADrawTwoToANumber() {
        Card drawTwo = Card.action(CardColor.BLUE, CardKind.DRAW_TWO);
        List<Card> cards = List.of(Card.number(CardColor.RED, 9), drawTwo);
        BotDecision decision = choose(new GreedyBotStrategy(), cards, cards);

        assertEquals(drawTwo, decision.card());
    }

    @Test
    void greedyKeepsOrdinaryWildEvenWhenTheAlternativeIsZero() {
        Card zero = Card.number(CardColor.RED, 0);
        List<Card> cards = List.of(Card.wild(CardKind.WILD), zero);
        BotDecision decision = choose(new GreedyBotStrategy(), cards, cards);

        assertEquals(zero, decision.card());
    }

    @Test
    void randomChoicesCanBeReproducedWithASeed() {
        List<Card> cards = List.of(Card.number(CardColor.RED, 9), Card.wild(CardKind.WILD));
        Player player = BotPlayers.withHand(BotProfile.RANDOM, cards);
        BotStrategy strategy = new RandomBotStrategy();
        Random first = new Random(25);
        Random second = new Random(25);

        for (int choice = 0; choice < 20; choice++) {
            assertEquals(strategy.chooseMove(player, cards, first), strategy.chooseMove(player, cards, second));
        }
    }

    @Test
    void conservativeStartsWithTheHighestNumberAndSavesSpecials() {
        Card nine = Card.number(CardColor.BLUE, 9);
        List<Card> hand = List.of(Card.action(CardColor.BLUE, CardKind.DRAW_TWO),
                Card.wild(CardKind.WILD), Card.number(CardColor.BLUE, 2), nine);

        assertEquals(nine, choose(new ConservativeBotStrategy(), hand, hand).card());
    }

    @Test
    void conservativeUsesAColouredActionBeforeSpendingAWild() {
        Card skip = Card.action(CardColor.BLUE, CardKind.SKIP);
        Card wild = Card.wild(CardKind.WILD);
        List<Card> hand = List.of(wild, skip, Card.number(CardColor.GREEN, 3),
                Card.number(CardColor.YELLOW, 4));

        assertEquals(skip, choose(new ConservativeBotStrategy(), hand, List.of(wild, skip)).card());
    }

    @Test
    void conservativeKeepsDrawFourWhenAnOrdinaryWildIsTheOnlyAlternative() {
        Card wild = Card.wild(CardKind.WILD);
        Card drawFour = Card.wild(CardKind.WILD_DRAW_FOUR);
        List<Card> hand = List.of(drawFour, wild, Card.number(CardColor.GREEN, 3),
                Card.number(CardColor.BLUE, 4));

        assertEquals(wild, choose(new ConservativeBotStrategy(), hand, List.of(drawFour, wild)).card());
    }

    @Test
    void conservativeChangesItsPolicyAtExactlyThreeCards() {
        Card wild = Card.wild(CardKind.WILD);
        Card nine = Card.number(CardColor.BLUE, 9);
        Card drawTwo = Card.action(CardColor.BLUE, CardKind.DRAW_TWO);
        List<Card> playable = List.of(wild, nine, drawTwo);
        List<Card> earlyHand = List.of(wild, nine, drawTwo, Card.number(CardColor.GREEN, 2));

        assertEquals(nine, choose(new ConservativeBotStrategy(), earlyHand, playable).card());
        assertEquals(wild, choose(new ConservativeBotStrategy(), playable, playable).card());
    }

    @Test
    void conservativeLateGameShedsHighPointCardsAndBreaksActionTiesWithPenalties() {
        List<Card> ranked = List.of(Card.wild(CardKind.WILD_DRAW_FOUR), Card.wild(CardKind.WILD),
                Card.action(CardColor.BLUE, CardKind.DRAW_TWO), Card.action(CardColor.BLUE, CardKind.SKIP),
                Card.action(CardColor.BLUE, CardKind.REVERSE), Card.number(CardColor.BLUE, 9),
                Card.number(CardColor.BLUE, 0));
        for (int index = 0; index < ranked.size() - 1; index++) {
            Card preferred = ranked.get(index);
            List<Card> pair = List.of(ranked.get(index + 1), preferred);
            assertEquals(preferred, choose(new ConservativeBotStrategy(), pair, pair).card());
        }
    }

    @Test
    void aggressivePrioritisesAttacksThenNumbersBeforeReverse() {
        List<Card> ranked = List.of(Card.wild(CardKind.WILD_DRAW_FOUR),
                Card.action(CardColor.BLUE, CardKind.DRAW_TWO), Card.action(CardColor.BLUE, CardKind.SKIP),
                Card.number(CardColor.BLUE, 9), Card.number(CardColor.BLUE, 0),
                Card.action(CardColor.BLUE, CardKind.REVERSE), Card.wild(CardKind.WILD));
        for (int index = 0; index < ranked.size() - 1; index++) {
            Card preferred = ranked.get(index);
            List<Card> pair = List.of(ranked.get(index + 1), preferred);
            assertEquals(preferred, choose(new AggressiveBotStrategy(), pair, pair).card());
        }
    }

    @Test
    void aggressiveHasADifferentNonAttackFallbackFromGreedy() {
        Card reverse = Card.action(CardColor.BLUE, CardKind.REVERSE);
        Card number = Card.number(CardColor.BLUE, 9);
        List<Card> hand = List.of(reverse, number);

        assertEquals(reverse, choose(new GreedyBotStrategy(), hand, hand).card());
        assertEquals(number, choose(new AggressiveBotStrategy(), hand, hand).card());
    }

    @Test
    void fixedStrategiesKeepTheFirstCandidateWhenPrioritiesTie() {
        Card blueSkip = Card.action(CardColor.BLUE, CardKind.SKIP);
        Card redSkip = Card.action(CardColor.RED, CardKind.SKIP);
        for (BotStrategy strategy : FIXED_STRATEGIES) {
            for (List<Card> pair : List.of(List.of(blueSkip, redSkip), List.of(redSkip, blueSkip))) {
                assertEquals(pair.get(0), choose(strategy, pair, pair).card());
            }
        }
        List<Card> earlyHand = List.of(blueSkip, redSkip, Card.number(CardColor.GREEN, 3),
                Card.number(CardColor.YELLOW, 4));
        assertEquals(blueSkip, choose(new ConservativeBotStrategy(), earlyHand,
                List.of(blueSkip, redSkip)).card());
        Card reverse = Card.action(CardColor.BLUE, CardKind.REVERSE);
        Card drawTwo = Card.action(CardColor.BLUE, CardKind.DRAW_TWO);
        List<Card> actionHand = List.of(reverse, drawTwo, Card.number(CardColor.GREEN, 3),
                Card.number(CardColor.YELLOW, 4));
        for (List<Card> order : List.of(List.of(reverse, drawTwo), List.of(drawTwo, reverse))) {
            assertEquals(order.get(0), choose(new ConservativeBotStrategy(), actionHand, order).card());
        }
    }

    @Test
    void fixedStrategiesSelectTheMostCommonColourInTheirOwnHand() {
        for (CardKind kind : List.of(CardKind.WILD, CardKind.WILD_DRAW_FOUR)) {
            Card wild = Card.wild(kind);
            List<Card> hand = List.of(wild, Card.number(CardColor.RED, 9), Card.number(CardColor.BLUE, 1),
                    Card.number(CardColor.BLUE, 2), Card.number(CardColor.GREEN, 3));
            for (BotStrategy strategy : FIXED_STRATEGIES) {
                assertEquals(CardColor.BLUE, choose(strategy, hand, List.of(wild)).selectedColor());
            }
        }
    }

    @Test
    void fixedStrategiesResolveColourTiesInTheDocumentedOrder() {
        Card wild = Card.wild(CardKind.WILD);
        List<CardColor> colours = CardColor.playableColors();
        for (BotStrategy strategy : FIXED_STRATEGIES) {
            for (int first = 0; first < colours.size(); first++) {
                for (int second = first + 1; second < colours.size(); second++) {
                    List<Card> hand = List.of(wild, Card.number(colours.get(second), 1),
                            Card.number(colours.get(first), 2));
                    assertEquals(colours.get(first), choose(strategy, hand, List.of(wild)).selectedColor());
                }
            }
            assertEquals(CardColor.RED, choose(strategy, List.of(wild), List.of(wild)).selectedColor());
        }
    }

    @Test
    void everyStrategyDeclaresUnoOnlyWhenAPlayLeavesOneCard() {
        Card played = Card.number(CardColor.BLUE, 5);
        Card other = Card.number(CardColor.GREEN, 2);
        for (BotStrategy strategy : STRATEGIES) {
            assertTrue(choose(strategy, List.of(played, other), List.of(played)).declaresUno());
            assertFalse(choose(strategy, List.of(played), List.of(played)).declaresUno());
            assertFalse(choose(strategy, List.of(played, other, Card.wild(CardKind.WILD)),
                    List.of(played)).declaresUno());
        }
    }

    @Test
    void everyStrategyKeepsItsDocumentedChallengeProbability() {
        List<Double> probabilities = List.of(0.15, 0.35, 0.10, 0.50);
        for (int index = 0; index < STRATEGIES.size(); index++) {
            BotStrategy strategy = STRATEGIES.get(index);
            double threshold = probabilities.get(index);
            assertTrue(strategy.shouldChallenge(fixedChance(threshold - 0.0001)));
            assertFalse(strategy.shouldChallenge(fixedChance(threshold)));
        }
    }

    private static BotDecision choose(BotStrategy strategy, List<Card> hand, List<Card> playable) {
        Player player = BotPlayers.withHand(BotProfile.GREEDY, hand);
        return strategy.chooseMove(player, playable, new Random(1));
    }

    private static Random fixedChance(double value) {
        return new Random(0) {
            private static final long serialVersionUID = 1L;

            @Override
            public double nextDouble() {
                return value;
            }
        };
    }
}
