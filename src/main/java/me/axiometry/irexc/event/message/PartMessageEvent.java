package me.axiometry.irexc.event.message;

import me.axiometry.irexc.*;

public class PartMessageEvent extends MessageEvent {
	private final User user;
	private final Channel channel;
	private final String reason;

	public PartMessageEvent(String raw, User user, Channel channel, String reason) {
		super(raw);

		this.user = user;
		this.channel = channel;
		this.reason = reason;
	}

	public User getUser() {
		return user;
	}

	public Channel getChannel() {
		return channel;
	}

	public String getReason() {
		return reason;
	}

	@Override
	public String toString() {
		if(reason == null)
			return getName() + ": " + user.getName() + " left " + channel.getName();
		return getName() + ": " + user.getName() + " left " + channel.getName() + " (" + reason + ")";
	}
}
