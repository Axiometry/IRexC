package me.axiometry.irexc.event.irc;

import me.axiometry.irexc.*;

public class UserJoinEvent extends AbstractChannelUserEvent {
	public UserJoinEvent(Channel channel, User user) {
		super(channel, user);
	}
}
