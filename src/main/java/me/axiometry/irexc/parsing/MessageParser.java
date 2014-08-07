package me.axiometry.irexc.parsing;

import me.axiometry.irexc.Bot;
import me.axiometry.irexc.event.message.MessageEvent;

public interface MessageParser<T extends MessageEvent> {
	public T parse(Bot bot, String line) throws ParseException;
}
