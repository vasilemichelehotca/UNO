package uno.core;

/** Summary created after the winning play and any draw penalty have been resolved. */
public record RoundResult(int roundNumber, String winnerName, int awardedPoints,
                          int winnerTotalScore, boolean gameFinished) {
}
