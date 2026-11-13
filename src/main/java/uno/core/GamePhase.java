package uno.core;

/** Internal state of the active turn. */
public enum GamePhase {
    TURN,
    DRAWN_CARD_AVAILABLE,
    AWAITING_WILD_DRAW_FOUR_RESPONSE,
    ROUND_OVER,
    GAME_OVER
}
