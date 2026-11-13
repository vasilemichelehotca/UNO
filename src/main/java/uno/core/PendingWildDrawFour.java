package uno.core;

/**
 * Private evidence retained until the target accepts or challenges a Wild Draw
 * Four. The legality flag is deliberately captured before the card is removed.
 */
record PendingWildDrawFour(int sourcePlayerIndex, int targetPlayerIndex, boolean legal) {
}
