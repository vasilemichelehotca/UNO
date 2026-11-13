package uno.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Mutable state belonging to one player during a game. */
public final class Player {
    private final String name;
    private final PlayerKind kind;
    private final BotProfile botProfile;
    private final List<Card> hand = new ArrayList<>();
    private final List<Card> handView = Collections.unmodifiableList(hand);
    private int score;

    /** Creates a player from a setup-screen configuration. */
    public Player(GameConfig.PlayerConfig config) {
        Objects.requireNonNull(config, "config");
        name = config.name().trim();
        kind = config.kind();
        botProfile = config.botProfile();
    }

    /** Returns the player name. */
    public String name() {
        return name;
    }

    /** Returns the controller type. */
    public PlayerKind kind() {
        return kind;
    }

    /** Returns the selected bot strategy, or {@code null} for humans. */
    public BotProfile botProfile() {
        return botProfile;
    }

    /** Returns an immutable view of the cards in this player's hand. */
    public List<Card> hand() {
        return handView;
    }

    /** Returns how many cards this player owns. */
    public int handSize() {
        return hand.size();
    }

    /** Returns the accumulated score in a points match. */
    public int score() {
        return score;
    }

    void addCard(Card card) {
        hand.add(Objects.requireNonNull(card, "card"));
    }

    boolean removeCard(Card card) {
        return hand.remove(card);
    }

    void clearHand() {
        hand.clear();
    }

    void addScore(int points) {
        score += points;
    }

    boolean hasColor(CardColor color) {
        return hand.stream().anyMatch(card -> card.color() == color);
    }
}
