package me.axiometry.irexc.event.message;

import me.axiometry.irexc.*;

public class KickMessageEvent extends MessageEvent {
	private final User source;
	private final Channel channel;
	private final User user;
	private final String reason;

	public KickMessageEvent(String raw, User source, Channel channel, User user, String reason) {
		super(raw);

		this.source = source;
		this.channel = channel;
		this.user = user;
		this.reason = reason;
	}

	public User getSource() {
		return source;
	}

	public Channel getChannel() {
		return channel;
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
			return getName() + ": " + user.getName() + " was kicked from " + channel.getName() + " by " + source.getName();
		return getName() + ": " + user.getName() + " was kicked from " + channel.getName() + " by " + source.getName() + " (" + reason + ")";
	}
}
