package me.axiometry.irexc.event.bot;

import me.axiometry.irexc.Bot;
import me.axiometry.irexc.event.AbstractEvent;

public abstract class BotEvent extends AbstractEvent {
	private final Bot bot;

	public BotEvent(Bot bot) {
		this.bot = bot;
	}

	public Bot getBot() {
		return bot;
	}
}
