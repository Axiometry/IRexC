package me.axiometry.irexc.event.irc;

import me.axiometry.irexc.*;

public class UserQuitLeaveEvent extends UserLeaveEvent {
	public UserQuitLeaveEvent(Channel channel, User user) {
		super(channel, user);
	}

	public UserQuitLeaveEvent(Channel channel, User user, String message) {
		super(channel, user, message);
	}
}
