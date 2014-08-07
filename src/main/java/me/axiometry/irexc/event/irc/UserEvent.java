package me.axiometry.irexc.event.irc;

import me.axiometry.irexc.User;
import me.axiometry.irexc.event.Event;

public interface UserEvent extends Event {
	public User getUser();
}
