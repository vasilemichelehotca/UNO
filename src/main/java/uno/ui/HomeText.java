package uno.ui;

/**
 * Editable text for the home and credits screens. Change these English strings,
 * then rebuild and restart the application. Keep %d in player-label templates;
 * it is replaced with the seat number. CREATOR_NAME is a placeholder for the
 * author of this Java adaptation, not the creator of the original UNO game.
 */
final class HomeText {
    static final String APPLICATION_TITLE = "UNO";

    // Home header and welcome card.
    static final String LOCAL_PLAY_HINT = "2-6 players";
    static final String WELCOME_TITLE_FIRST_LINE = "Start a";
    static final String WELCOME_TITLE_SECOND_LINE = "new game.";
    static final String WELCOME_DESCRIPTION = "Play with friends or bots.";

    // Match settings and help.
    static final String SETUP_TITLE = "Set up your table";
    static final String MATCH_SETTINGS_TITLE = "Match settings";
    static final String MODE_LABEL = "MODE";
    static final String PLAYER_COUNT_LABEL = "PLAYERS";
    static final String POINTS_TARGET_LABEL = "POINTS TARGET";
    static final String GAME_MODE_ACCESSIBLE_NAME = "Game mode";
    static final String PLAYER_COUNT_ACCESSIBLE_NAME = "Player count";
    static final String VICTORY_TARGET_ACCESSIBLE_NAME = "Victory target";
    static final String VICTORY_TARGET_TOOLTIP = "Points needed to win a points match";
    static final String POINTS_MODE_HELP = "Win rounds to collect points. First to the target wins the match.";
    static final String SINGLE_ROUND_HELP = "One round. The first player to empty their hand wins.";
    static final String PLAYERS_TITLE = "Who's playing?";
    static final String START_BUTTON = "Start game";
    static final String CREDITS_BUTTON = "Credits";
    static final String FOOTER_NOTE = "A classic 108-card deck. Draw penalties do not stack.";
    static final String INVALID_NUMBER_MESSAGE = "Enter a whole number within the allowed range.";

    // Player names, accessible control names and tooltips.
    static final String DEFAULT_PLAYER_NAME = "Player %d";
    static final String PLAYER_NAME_ACCESSIBLE_NAME = "Player %d name";
    static final String PLAYER_CONTROLLER_ACCESSIBLE_NAME = "Player %d controller";
    static final String PLAYER_PROFILE_ACCESSIBLE_NAME = "Player %d bot strategy";
    static final String PLAYER_NAME_TOOLTIP = "Player name";
    static final String PLAYER_CONTROLLER_TOOLTIP = "Choose a human or computer player";
    static final String BOT_PROFILE_TOOLTIP = "Random chooses freely; Greedy favours strong cards; "
            + "Conservative saves useful cards; Aggressive puts pressure on opponents";

    // Credits draft. Replace Your name with your own name before sharing.
    static final String CREDITS_HEADER = "ABOUT THIS PROJECT";
    static final String CREDITS_TITLE = "Developed for a unversity exam";
    static final String CREDITS_SUBTITLE = "A local Java adaptation of the famous UNO card game.";
    static final String CREATOR_NAME = "Vasile Michele Hotca";
    static final String CREATOR_ROLE = "Developer";
    static final String PROJECT_NOTE = "An individual programming project, with a Swing interface, "
            + "a separate game engine and a choice of opponents.";
    static final String TECHNOLOGY_NOTE = "Java  ·  Swing  ·  JUnit  ·  Eclipse";
    static final String ORIGINAL_GAME_LABEL = "THE ORIGINAL GAME";
    static final String ORIGINAL_GAME_NOTE = "UNO is the original card game. The creator credit above "
            + "refers to this Java adaptation.";
    static final String BACK_HOME_BUTTON = "Back to home";

    private HomeText() {
    }
}
