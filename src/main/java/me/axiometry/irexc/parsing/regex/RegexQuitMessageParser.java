package me.axiometry.irexc.parsing.regex;

import me.axiometry.irexc.*;
import me.axiometry.irexc.event.message.QuitMessageEvent;
import me.axiometry.irexc.parsing.ParseException;

public class RegexQuitMessageParser extends RegexMessageParser<QuitMessageEvent> {
	@Override
	public QuitMessageEvent parse(Bot bot, RegexData data) throws ParseException {
		if(data.value(COMMAND_ID).equalsIgnoreCase("QUIT")) {
			String[] params = params(data);
			if(params.length != 1)
				throw new ParseException("Wrong number of params on QUIT: expected 1, found " + params.length);

			User user = bot.getUser(data.value(SENDER_NICKNAME_ID));
			user.setNickname(data.value(SENDER_NICKNAME_ID));
			if(data.value(SENDER_USERNAME_ID) != null)
				user.setUsername(data.value(SENDER_USERNAME_ID));
			if(data.value(SENDER_HOSTNAME_ID) != null)
				user.setHostname(data.value(SENDER_HOSTNAME_ID));
			String reason = params[0];
			return new QuitMessageEvent(data.message(), user, reason);
		}
		return null;
	}
}
