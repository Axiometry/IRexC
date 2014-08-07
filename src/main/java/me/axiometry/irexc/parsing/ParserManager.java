package me.axiometry.irexc.parsing;

import me.axiometry.irexc.Bot;
import me.axiometry.irexc.event.message.MessageEvent;

public interface ParserManager {
	public <T extends MessageEvent> void bind(Class<T> messageType, MessageParser<T> parser);
	public <T extends MessageEvent> void unbind(Class<T> messageType);
	public <T extends MessageEvent> void unbind(MessageParser<T> parser);
	public <T extends MessageEvent> MessageParser<T> getParser(Class<T> messageType);

	public MessageEvent parse(Bot bot, String line) throws ParseException;
}
