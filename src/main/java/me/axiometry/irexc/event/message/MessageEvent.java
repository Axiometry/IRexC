package me.axiometry.irexc.event.message;

import me.axiometry.irexc.event.AbstractEvent;

public abstract class MessageEvent extends AbstractEvent {
	private final String raw;

	public MessageEvent(String raw) {
		if(raw == null)
			throw new NullPointerException("raw is null");
		this.raw = raw;
	}

	public String getRaw() {
		return raw;
	}

	@Override
	public String toString() {
		return getName() + ": " + raw;
	}
}
