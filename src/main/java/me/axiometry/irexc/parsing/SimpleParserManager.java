package me.axiometry.irexc.parsing;

import java.util.*;

import me.axiometry.irexc.Bot;
import me.axiometry.irexc.event.message.MessageEvent;

public class SimpleParserManager implements ParserManager {
	public interface MessageParserTransformer {
		public <T extends MessageEvent> T parse(MessageParser<T> parser, Bot bot, String line) throws ParseException;
	}

	private final Map<Class<MessageEvent>, MessageParser<MessageEvent>> parsers;

	public SimpleParserManager() {
		parsers = new HashMap<Class<MessageEvent>, MessageParser<MessageEvent>>();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends MessageEvent> void bind(Class<T> messageType, MessageParser<T> parser) {
		parsers.put((Class<MessageEvent>) messageType, (MessageParser<MessageEvent>) parser);
	}

	@Override
	public <T extends MessageEvent> void unbind(Class<T> messageType) {
		parsers.remove(messageType);
	}

	@Override
	public <T extends MessageEvent> void unbind(MessageParser<T> parser) {
		Iterator<MessageParser<MessageEvent>> iterator = parsers.values().iterator();
		while(iterator.hasNext()) {
			MessageParser<MessageEvent> value = iterator.next();

			if(parser.equals(value))
				iterator.remove();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends MessageEvent> MessageParser<T> getParser(Class<T> messageType) {
		return (MessageParser<T>) parsers.get(messageType);
	}

	@Override
	public MessageEvent parse(Bot bot, String line) throws ParseException {
		return parse(bot, line, null);
	}

	protected final MessageEvent parse(Bot bot, String line, MessageParserTransformer transformer) throws ParseException {
		ParseException compositeException = new ParseException("No parser found");
		MessageEvent message = null;
		for(MessageParser<?> parser : parsers.values()) {
			try {
				if(transformer != null)
					message = transformer.parse(parser, bot, line);
				else
					message = parser.parse(bot, line);

				if(message != null)
					break;
			} catch(ParseException exception) {
				compositeException.addSuppressed(exception);
			}
		}
		if(message == null)
			throw compositeException;
		return message;
	}
}
