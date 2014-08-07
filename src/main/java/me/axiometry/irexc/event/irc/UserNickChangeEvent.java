package me.axiometry.irexc.event.irc;

import me.axiometry.irexc.User;

public class UserNickChangeEvent extends AbstractUserEvent {
	private final String oldNickname, newNickname;

	public UserNickChangeEvent(User user, String oldNickname, String newNickname) {
		super(user);

		this.oldNickname = oldNickname;
		this.newNickname = newNickname;
	}

	public String getOldNickname() {
		return oldNickname;
	}

	public String getNewNickname() {
		return newNickname;
	}
}
