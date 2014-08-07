package me.axiometry.irexc.event.irc;

import me.axiometry.irexc.*;
import me.axiometry.irexc.event.AbstractEvent;

public abstract class AbstractChannelUserEvent extends AbstractEvent implements ChannelEvent, UserEvent {
	private final Channel channel;
	private final User user;

	public AbstractChannelUserEvent(Channel channel, User user) {
		this.channel = channel;
		this.user = user;
	}

	@Override
	public Channel getChannel() {
		return channel;
	}

	@Override
	public User getUser() {
		return user;
	}
}
