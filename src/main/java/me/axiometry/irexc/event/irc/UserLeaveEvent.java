package me.axiometry.irexc.event.irc;

import me.axiometry.irexc.*;

public class UserLeaveEvent extends AbstractChannelUserEvent {
	private final String message;

	public UserLeaveEvent(Channel channel, User user) {
		this(channel, user, null);
	}

	public UserLeaveEvent(Channel channel, User user, String message) {
		super(channel, user);

		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
