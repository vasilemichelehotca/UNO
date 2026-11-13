package uno.bot;

import uno.core.Card;
import uno.core.CardColor;

/** Complete choice made by a bot for one card play. */
public record BotDecision(Card card, CardColor selectedColor, boolean declaresUno) {
}
