package me.axiometry.irexc.event.message;

import me.axiometry.irexc.User;

public class QuitMessageEvent extends MessageEvent {
	private final User user;
	private final String reason;

	public QuitMessageEvent(String raw, User user, String reason) {
		super(raw);

		this.user = user;
		this.reason = reason;
	}

	public User getUser() {
		return user;
	}

	public String getReason() {
		return reason;
	}

	@Override
	public String toString() {
		if(reason == null)
			return getName() + ": " + user.getName() + " quit";
		return getName() + ": " + user.getName() + " quit (" + reason + ")";
	}
}
