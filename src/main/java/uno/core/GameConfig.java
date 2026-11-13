package uno.core;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Immutable, validated configuration selected before a game starts. */
public record GameConfig(GameMode mode, List<PlayerConfig> players, int victoryTarget) {
    /** The default score needed to win a points match. */
    public static final int DEFAULT_VICTORY_TARGET = 500;

    /** Validates a complete game configuration. */
    public GameConfig {
        Objects.requireNonNull(mode, "mode");
        players = List.copyOf(players);
        if (players.size() < 2 || players.size() > 6) {
            throw new IllegalArgumentException("UNO needs from 2 to 6 players.");
        }
        long uniqueNames = players.stream().map(PlayerConfig::name)
                .map(name -> name.trim().toLowerCase(Locale.ROOT)).distinct().count();
        if (uniqueNames != players.size()) {
            throw new IllegalArgumentException("Player names must be unique.");
        }
        if (victoryTarget < 1) {
            throw new IllegalArgumentException("The victory target must be positive.");
        }
    }

    /** Configuration for one seat at the table. */
    public record PlayerConfig(String name, PlayerKind kind, BotProfile botProfile) {
        /** Validates a player name, controller kind and optional bot profile. */
        public PlayerConfig {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Every player needs a name.");
            }
            Objects.requireNonNull(kind, "kind");
            if (kind == PlayerKind.BOT && botProfile == null) {
                throw new IllegalArgumentException("A computer player needs a strategy.");
            }
            if (kind == PlayerKind.HUMAN) {
                botProfile = null;
            }
        }
    }
}
