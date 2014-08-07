package me.axiometry.irexc.event.message;

import me.axiometry.irexc.*;

public class JoinMessageEvent extends MessageEvent {
	private final User user;
	private final Channel channel;

	public JoinMessageEvent(String raw, User user, Channel channel) {
		super(raw);

		this.user = user;
		this.channel = channel;
	}

	public User getUser() {
		return user;
	}

	public Channel getChannel() {
		return channel;
	}

	@Override
	public String toString() {
		return getName() + ": " + user.getName() + " joined " + channel.getName();
	}
}
