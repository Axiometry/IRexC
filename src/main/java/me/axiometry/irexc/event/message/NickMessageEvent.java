package me.axiometry.irexc.event.message;

import me.axiometry.irexc.User;

public class NickMessageEvent extends MessageEvent {
	private final User user;
	private final String oldNickname, newNickname;

	public NickMessageEvent(String raw, User user, String oldNickname, String newNickname) {
		super(raw);
		this.user = user;
		this.oldNickname = oldNickname;
		this.newNickname = newNickname;
	}

	public User getUser() {
		return user;
	}

	public String getOldNickname() {
		return oldNickname;
	}

	public String getNewNickname() {
		return newNickname;
	}

	@Override
	public String toString() {
		return getName() + ": " + oldNickname + " is now known as " + newNickname + " (" + user.getName() + ")";
	}
}
