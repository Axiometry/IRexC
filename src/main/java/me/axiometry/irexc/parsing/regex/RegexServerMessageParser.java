package me.axiometry.irexc.parsing.regex;

import java.util.Arrays;

import me.axiometry.irexc.*;
import me.axiometry.irexc.event.message.ServerMessageEvent;
import me.axiometry.irexc.parsing.ParseException;

public class RegexServerMessageParser extends RegexMessageParser<ServerMessageEvent> {
	@Override
	public ServerMessageEvent parse(Bot bot, RegexData data) throws ParseException {
		if(data.value(COMMAND_ID).matches("[0-9]{3}")) {
			String[] params = params(data);
			if(params.length < 2)
				throw new ParseException("Wrong number of params on server response: expected >=2, found " + params.length);

			if(data.value(SENDER_ADDRESS_HOSTNAME_ID) == null)
				throw new ParseException("Wrong sender type in server message: " + data.value(SENDER_ID));
			Server server = new Server(data.value(SENDER_ID));
			int responseCode = Integer.parseInt(data.value(COMMAND_ID));
			String target = params[0];
			String[] options = Arrays.copyOfRange(params, 1, params.length - 1);
			String message = params[params.length - 1];

			return new ServerMessageEvent(data.message(), server, responseCode, target, options, message);
		}
		return null;
	}
}
