package me.axiometry.irexc.event.irc;

import me.axiometry.irexc.User;
import me.axiometry.irexc.event.AbstractEvent;

public abstract class AbstractUserEvent extends AbstractEvent implements UserEvent {
	private final User user;

	public AbstractUserEvent(User user) {
		this.user = user;
	}

	@Override
	public User getUser() {
		return user;
	}
}
