package me.axiometry.irexc.event.irc;

import me.axiometry.irexc.Channel;
import me.axiometry.irexc.event.AbstractEvent;

public abstract class AbstractChannelEvent extends AbstractEvent implements ChannelEvent {
	private final Channel channel;

	public AbstractChannelEvent(Channel channel) {
		this.channel = channel;
	}

	@Override
	public Channel getChannel() {
		return channel;
	}
}
