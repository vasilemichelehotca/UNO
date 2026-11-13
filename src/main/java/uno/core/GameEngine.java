package uno.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Authoritative state machine for a game of classic UNO. The class has no Swing
 * dependency: commands mutate the game only after checking their preconditions,
 * while the GUI and bots are separate clients of this API.
 */
public final class GameEngine {
    private final GameConfig config;
    private final Random random;
    private final List<Player> players;
    private final List<String> events = new ArrayList<>();
    private final List<String> eventView = Collections.unmodifiableList(events);

    private Deck deck;
    private Card topCard;
    private CardColor currentColor;
    private Direction direction = Direction.CLOCKWISE;
    private int currentPlayerIndex;
    private GamePhase phase = GamePhase.TURN;
    private Card drawnCard;
    private PendingWildDrawFour pendingWildDrawFour;
    private int unoTargetIndex = -1;
    private int roundNumber;
    private RoundResult lastRoundResult;
    private Player gameWinner;

    /** Creates and immediately deals the first round of a new game. */
    public GameEngine(GameConfig config) {
        this(config, new Random());
    }

    /** Creates a game using the supplied random source for shuffles. */
    public GameEngine(GameConfig config, Random random) {
        this.config = Objects.requireNonNull(config, "config");
        this.random = Objects.requireNonNull(random, "random");
        players = config.players().stream().map(Player::new).toList();
        startRound();
    }

    /** Returns the immutable configuration selected before the game started. */
    public GameConfig config() {
        return config;
    }

    /** Returns all players in their stable table order. */
    public List<Player> players() {
        return players;
    }

    /** Returns the player whose action or response is currently required. */
    public Player currentPlayer() {
        return players.get(currentPlayerIndex);
    }

    /** Returns the index of the current player. */
    public int currentPlayerIndex() {
        return currentPlayerIndex;
    }

    /** Returns the visible card on the discard pile. */
    public Card topCard() {
        return topCard;
    }

    /** Returns the colour currently used to match cards. */
    public CardColor currentColor() {
        return currentColor;
    }

    /** Returns the current rotation direction. */
    public Direction direction() {
        return direction;
    }

    /** Returns the state of the turn state machine. */
    public GamePhase phase() {
        return phase;
    }

    /** Returns the current round number, beginning at one. */
    public int roundNumber() {
        return roundNumber;
    }

    /** Returns how many cards remain in the draw pile before any future recycle. */
    public int drawPileSize() {
        return deck.drawPileSize();
    }

    /** Returns the discard pile size, including the visible card. */
    public int discardPileSize() {
        return deck.discardPileSize();
    }

    /** Returns the last completed round, or {@code null} during the first round. */
    public RoundResult lastRoundResult() {
        return lastRoundResult;
    }

    /** Returns the winner once the whole game has ended, otherwise {@code null}. */
    public Player gameWinner() {
        return gameWinner;
    }

    /** Returns a read-only view of the recent game event log. */
    public List<String> events() {
        return eventView;
    }

    /**
     * Returns every card the current player may attempt in the current phase.
     * A Wild Draw Four appears here even when it is illegal, because the physical
     * game resolves an illegal play by a later challenge rather than by exposing
     * the player's entire hand before the play.
     */
    public List<Card> playableCards() {
        if (phase != GamePhase.TURN && phase != GamePhase.DRAWN_CARD_AVAILABLE) {
            return List.of();
        }
        List<Card> candidates = phase == GamePhase.DRAWN_CARD_AVAILABLE
                ? List.of(drawnCard)
                : currentPlayer().hand();
        return candidates.stream().filter(this::isPlayable).toList();
    }

    /** Returns whether a card matches the currently visible state. */
    public boolean isPlayable(Card card) {
        Objects.requireNonNull(card, "card");
        if (card.kind().isWild()) {
            return true;
        }
        if (card.color() == currentColor) {
            return true;
        }
        return card.kind() == CardKind.NUMBER
                ? topCard.kind() == CardKind.NUMBER && card.number().equals(topCard.number())
                : card.kind() == topCard.kind();
    }

    /** Returns whether the current player may call out an unprotected UNO. */
    public boolean canCurrentPlayerCatchUno() {
        return canCatchUno(currentPlayerIndex);
    }

    /** Returns whether a player may catch a missed UNO before the next action. */
    public boolean canCatchUno(int playerIndex) {
        return unoTargetIndex >= 0
                && playerIndex >= 0 && playerIndex < players.size()
                && unoTargetIndex != playerIndex
                && phase != GamePhase.ROUND_OVER
                && phase != GamePhase.GAME_OVER;
    }

    /** Returns the player who may be penalised for not declaring UNO, if any. */
    public Player unoTarget() {
        return unoTargetIndex < 0 ? null : players.get(unoTargetIndex);
    }

    /**
     * Plays a card from the current player's hand. A wild card needs a selected
     * colour. The declaration flag is meaningful only when the move leaves one
     * card in the player's hand.
     */
    public ActionResult playCard(Card card, CardColor selectedColor, boolean declaredUno) {
        if (phase != GamePhase.TURN && phase != GamePhase.DRAWN_CARD_AVAILABLE) {
            return ActionResult.rejected("A card cannot be played at this moment.");
        }
        Player actor = currentPlayer();
        if (!actor.hand().contains(card)) {
            return ActionResult.rejected("That card is not in the current hand.");
        }
        if (phase == GamePhase.DRAWN_CARD_AVAILABLE && !card.equals(drawnCard)) {
            return ActionResult.rejected("Only the card drawn this turn may now be played.");
        }
        if (!isPlayable(card)) {
            return ActionResult.rejected("That card does not match the current colour or symbol.");
        }
        if (card.kind().isWild() && (selectedColor == null || selectedColor == CardColor.NONE)) {
            return ActionResult.rejected("Choose a new colour for a Wild card.");
        }

        expireUnoOpportunity();
        boolean wildDrawFourWasLegal = card.kind() != CardKind.WILD_DRAW_FOUR || !actor.hasColor(currentColor);
        actor.removeCard(card);
        deck.discard(card);
        topCard = card;
        drawnCard = null;
        currentColor = card.kind().isWild() ? selectedColor : card.color();
        addEvent(actor.name() + " played " + card.label()
                + (card.kind().isWild() ? " and selected " + currentColor.displayName() + "." : "."));

        handleUnoAfterPlay(actor, declaredUno);
        switch (card.kind()) {
            case NUMBER -> advance(1);
            case SKIP -> {
                addEvent(players.get(nextIndex(currentPlayerIndex)).name() + " is skipped.");
                advance(2);
            }
            case REVERSE -> {
                direction = direction.reversed();
                if (players.size() == 2) {
                    addEvent("With two players, Reverse acts as Skip.");
                    advance(2);
                } else {
                    addEvent("Direction changed to " + direction.displayName() + ".");
                    advance(1);
                }
            }
            case DRAW_TWO -> {
                int target = nextIndex(currentPlayerIndex);
                int drawn = drawCards(target, 2);
                addEvent(players.get(target).name() + " drew " + drawn + " card(s) and is skipped.");
                advance(2);
            }
            case WILD -> advance(1);
            case WILD_DRAW_FOUR -> {
                int target = nextIndex(currentPlayerIndex);
                pendingWildDrawFour = new PendingWildDrawFour(currentPlayerIndex, target, wildDrawFourWasLegal);
                currentPlayerIndex = target;
                phase = GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE;
                addEvent(players.get(target).name() + " must accept or challenge the Wild Draw Four.");
            }
        }
        // Draw penalties belong to the round being scored. A +4 must also leave
        // the next player time to respond before an empty hand wins the round.
        if (actor.handSize() == 0 && pendingWildDrawFour == null) {
            completeRound(actor);
            return ActionResult.accepted("The round is over.");
        }
        return ActionResult.accepted("Card played.");
    }

    /** Draws one card for the current player. A playable drawn card may be used immediately. */
    public ActionResult drawCard() {
        if (phase != GamePhase.TURN) {
            return ActionResult.rejected("Drawing is not available now.");
        }
        expireUnoOpportunity();
        Card card = deck.draw();
        if (card == null) {
            addEvent(currentPlayer().name() + " could not draw: no cards remain in the piles. Turn passed.");
            advance(1);
            return ActionResult.accepted("No card was available; the turn passed.");
        }
        Player actor = currentPlayer();
        actor.addCard(card);
        addEvent(actor.name() + " drew a card.");
        if (isPlayable(card)) {
            drawnCard = card;
            phase = GamePhase.DRAWN_CARD_AVAILABLE;
            return ActionResult.accepted("The drawn card is playable; play it or pass.");
        }
        addEvent("The drawn card could not be played.");
        advance(1);
        return ActionResult.accepted("The drawn card was not playable.");
    }

    /** Keeps a playable card drawn this turn and ends the turn. */
    public ActionResult passAfterDrawing() {
        if (phase != GamePhase.DRAWN_CARD_AVAILABLE) {
            return ActionResult.rejected("There is no drawn playable card to pass on.");
        }
        addEvent(currentPlayer().name() + " kept the drawn card and passed.");
        advance(1);
        return ActionResult.accepted("Turn passed.");
    }

    /** Applies the two-card penalty for a valid immediate UNO call-out. */
    public ActionResult catchUno() {
        return catchUno(currentPlayerIndex);
    }

    /**
     * Calls out a missed UNO on behalf of an opponent. This also lets a skipped
     * player catch UNO when a two-player action gives the offender another turn.
     */
    public ActionResult catchUno(int callerIndex) {
        if (!canCatchUno(callerIndex)) {
            return ActionResult.rejected("There is no valid UNO declaration to challenge.");
        }
        Player target = players.get(unoTargetIndex);
        int drawn = drawCards(unoTargetIndex, 2);
        unoTargetIndex = -1;
        addEvent(players.get(callerIndex).name() + " caught " + target.name() + " without UNO. "
                + target.name() + " drew " + drawn + " card(s).");
        return ActionResult.accepted("UNO penalty applied.");
    }

    /** Accepts the pending Wild Draw Four and skips the target after four cards. */
    public ActionResult acceptWildDrawFour() {
        if (phase != GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE) {
            return ActionResult.rejected("There is no Wild Draw Four to accept.");
        }
        expireUnoOpportunity();
        Player source = players.get(pendingWildDrawFour.sourcePlayerIndex());
        Player target = currentPlayer();
        int drawn = drawCards(currentPlayerIndex, 4);
        pendingWildDrawFour = null;
        addEvent(target.name() + " accepted the Wild Draw Four, drew " + drawn + " card(s), and is skipped.");
        if (source.handSize() == 0) {
            completeRound(source);
        } else {
            advance(1);
        }
        return ActionResult.accepted("Wild Draw Four accepted.");
    }

    /** Resolves the pending Wild Draw Four challenge according to the captured legality. */
    public ActionResult challengeWildDrawFour() {
        if (phase != GamePhase.AWAITING_WILD_DRAW_FOUR_RESPONSE || pendingWildDrawFour == null) {
            return ActionResult.rejected("There is no Wild Draw Four to challenge.");
        }
        PendingWildDrawFour pending = pendingWildDrawFour;
        expireUnoOpportunity();
        Player source = players.get(pending.sourcePlayerIndex());
        Player challenger = currentPlayer();
        pendingWildDrawFour = null;
        if (!pending.legal()) {
            int drawn = drawCards(pending.sourcePlayerIndex(), 4);
            addEvent(challenger.name() + " won the challenge. " + source.name() + " drew " + drawn
                    + " card(s); " + challenger.name() + " keeps the turn.");
            phase = GamePhase.TURN;
            drawnCard = null;
            return ActionResult.accepted("Challenge succeeded.");
        }
        int drawn = drawCards(currentPlayerIndex, 6);
        addEvent(challenger.name() + " lost the challenge, drew " + drawn + " card(s), and is skipped.");
        if (source.handSize() == 0) {
            completeRound(source);
        } else {
            advance(1);
        }
        return ActionResult.accepted("Challenge failed.");
    }

    /** Starts another round of an unfinished points match. */
    public ActionResult startNextRound() {
        if (phase != GamePhase.ROUND_OVER) {
            return ActionResult.rejected("A new round is available only after an unfinished round.");
        }
        startRound();
        return ActionResult.accepted("Next round started.");
    }

    private void startRound() {
        roundNumber++;
        deck = Deck.standard(random);
        for (Player player : players) {
            player.clearHand();
        }
        for (int card = 0; card < 7; card++) {
            for (Player player : players) {
                player.addCard(requireDraw());
            }
        }
        topCard = deck.revealStartingNumberCard();
        currentColor = topCard.color();
        direction = Direction.CLOCKWISE;
        currentPlayerIndex = 0;
        phase = GamePhase.TURN;
        drawnCard = null;
        pendingWildDrawFour = null;
        unoTargetIndex = -1;
        lastRoundResult = null;
        addEvent("Round " + roundNumber + " started. " + players.get(0).name() + " plays first.");
    }

    private Card requireDraw() {
        Card card = deck.draw();
        if (card == null) {
            throw new IllegalStateException("The deck ran out during initial dealing.");
        }
        return card;
    }

    private void handleUnoAfterPlay(Player actor, boolean declaredUno) {
        if (actor.handSize() != 1) {
            return;
        }
        if (declaredUno) {
            addEvent(actor.name() + " declared UNO.");
        } else {
            unoTargetIndex = currentPlayerIndex;
            addEvent(actor.name() + " has one card but did not declare UNO.");
        }
    }

    private void expireUnoOpportunity() {
        if (unoTargetIndex >= 0) {
            addEvent("The opportunity to catch " + players.get(unoTargetIndex).name() + " expired.");
            unoTargetIndex = -1;
        }
    }

    private int drawCards(int playerIndex, int amount) {
        int drawn = 0;
        for (int index = 0; index < amount; index++) {
            Card card = deck.draw();
            if (card == null) {
                break;
            }
            players.get(playerIndex).addCard(card);
            drawn++;
        }
        if (unoTargetIndex == playerIndex && players.get(playerIndex).handSize() != 1) {
            unoTargetIndex = -1;
        }
        return drawn;
    }

    private void advance(int places) {
        currentPlayerIndex = wrap(currentPlayerIndex + direction.step() * places);
        phase = GamePhase.TURN;
        drawnCard = null;
    }

    private int nextIndex(int from) {
        return wrap(from + direction.step());
    }

    private int wrap(int index) {
        return Math.floorMod(index, players.size());
    }

    private void completeRound(Player winner) {
        int points = players.stream()
                .filter(player -> player != winner)
                .flatMap(player -> player.hand().stream())
                .mapToInt(Card::pointValue)
                .sum();
        winner.addScore(points);
        boolean finished = config.mode() == GameMode.SINGLE_ROUND || winner.score() >= config.victoryTarget();
        if (finished) {
            gameWinner = config.mode() == GameMode.SINGLE_ROUND ? winner : highestScoringPlayer();
            phase = GamePhase.GAME_OVER;
            addEvent(winner.name() + " ended the round and earned " + points + " point(s). "
                    + gameWinner.name() + " won the game.");
        } else {
            phase = GamePhase.ROUND_OVER;
            addEvent(winner.name() + " ended the round and earned " + points + " point(s). Choose Next round.");
        }
        lastRoundResult = new RoundResult(roundNumber, winner.name(), points, winner.score(), finished);
        pendingWildDrawFour = null;
        unoTargetIndex = -1;
    }

    private Player highestScoringPlayer() {
        Player leader = players.get(0);
        for (int index = 1; index < players.size(); index++) {
            Player player = players.get(index);
            if (player.score() > leader.score()) {
                leader = player;
            }
        }
        // Equal totals keep the first seat, as specified for points matches.
        return leader;
    }

    private void addEvent(String event) {
        events.add(event);
        if (events.size() > 80) {
            events.remove(0);
        }
    }

    /**
     * Builds a deterministic, package-visible test scenario. Production code
     * never uses this helper; it keeps rule tests small and independent of a
     * shuffled deal.
     */
    static GameEngine scenario(GameConfig config, List<List<Card>> hands, Deck deck, Card topCard,
                               CardColor activeColor, int activePlayer) {
        GameEngine engine = new GameEngine(config, new Random(7));
        if (hands.size() != engine.players.size()) {
            throw new IllegalArgumentException("A hand is required for every player.");
        }
        if (!CardColor.playableColors().contains(activeColor)) {
            throw new IllegalArgumentException("The active colour must be a playable colour.");
        }
        Objects.checkIndex(activePlayer, engine.players.size());
        for (int index = 0; index < hands.size(); index++) {
            engine.players.get(index).clearHand();
            hands.get(index).forEach(engine.players.get(index)::addCard);
        }
        engine.topCard = Objects.requireNonNull(topCard, "topCard");
        engine.deck = Objects.requireNonNull(deck, "deck");
        engine.currentColor = Objects.requireNonNull(activeColor, "activeColor");
        engine.currentPlayerIndex = activePlayer;
        engine.direction = Direction.CLOCKWISE;
        engine.phase = GamePhase.TURN;
        engine.drawnCard = null;
        engine.pendingWildDrawFour = null;
        engine.unoTargetIndex = -1;
        engine.events.clear();
        return engine;
    }
}
