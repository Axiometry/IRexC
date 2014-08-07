package me.axiometry.irexc.parsing.regex;

import me.axiometry.irexc.Bot;
import me.axiometry.irexc.event.message.PingMessageEvent;
import me.axiometry.irexc.parsing.ParseException;

public class RegexPingMessageParser extends RegexMessageParser<PingMessageEvent> {
	@Override
	public PingMessageEvent parse(Bot bot, RegexData data) throws ParseException {
		boolean ping = data.value(COMMAND_ID).equalsIgnoreCase("PING");
		if(ping || data.value(COMMAND_ID).equalsIgnoreCase("PONG")) {
			String[] params = params(data);
			if(params.length != 1)
				throw new ParseException("Wrong number of params on PING/PONG: expected 1, found " + params.length);

			return new PingMessageEvent(data.message(), ping ? PingMessageEvent.Type.PING : PingMessageEvent.Type.PONG, params[0]);
		}
		return null;
	}
}
