package me.axiometry.irexc.event.message;

import me.axiometry.irexc.*;

public class InviteMessageEvent extends MessageEvent {
	private final User invitee;
	private final User invited;
	private final Channel channel;

	public InviteMessageEvent(String raw, User invitee, User invited, Channel channel) {
		super(raw);

		this.invitee = invitee;
		this.invited = invited;
		this.channel = channel;
	}

	public User getInvitee() {
		return invitee;
	}

	public User getInvited() {
		return invited;
	}

	public Channel getChannel() {
		return channel;
	}

	@Override
	public String toString() {
		return getName() + ": " + invitee.getName() + " invited " + invited.getName() + " to " + channel.getName();
	}
}
