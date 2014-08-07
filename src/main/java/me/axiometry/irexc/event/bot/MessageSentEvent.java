package me.axiometry.irexc.event.bot;

import me.axiometry.irexc.Bot;
import me.axiometry.irexc.event.CancellableEvent;

public class MessageSentEvent extends BotEvent implements CancellableEvent {
	private final String rawMessage;

	private boolean cancelled = false;

	public MessageSentEvent(Bot bot, String rawMessage) {
		super(bot);

		this.rawMessage = rawMessage;
	}

	public String getRawMessage() {
		return rawMessage;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
