package me.axiometry.irexc.event.irc;

import me.axiometry.irexc.User;

public class UserQuitEvent extends AbstractUserEvent {
	private final String message;

	public UserQuitEvent(User user) {
		this(user, null);
	}

	public UserQuitEvent(User user, String message) {
		super(user);

		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
