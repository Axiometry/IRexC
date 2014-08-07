package me.axiometry.irexc.event.irc;

import me.axiometry.irexc.Channel;
import me.axiometry.irexc.event.Event;

public interface ChannelEvent extends Event {
	public Channel getChannel();
}
