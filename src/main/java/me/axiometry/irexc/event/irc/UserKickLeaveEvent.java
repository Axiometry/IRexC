package me.axiometry.irexc.event.irc;

import me.axiometry.irexc.*;

public class UserKickLeaveEvent extends UserLeaveEvent {
	public UserKickLeaveEvent(Channel channel, User user) {
		super(channel, user);
	}

	public UserKickLeaveEvent(Channel channel, User user, String message) {
		super(channel, user, message);
	}
}
