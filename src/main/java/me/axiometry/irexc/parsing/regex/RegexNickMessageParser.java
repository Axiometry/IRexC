package me.axiometry.irexc.parsing.regex;

import me.axiometry.irexc.*;
import me.axiometry.irexc.event.message.NickMessageEvent;
import me.axiometry.irexc.parsing.ParseException;

public class RegexNickMessageParser extends RegexMessageParser<NickMessageEvent> {
	@Override
	public NickMessageEvent parse(Bot bot, RegexData data) throws ParseException {
		if(data.value(COMMAND_ID).equalsIgnoreCase("NICK")) {
			String[] params = params(data);
			if(params.length != 1)
				throw new ParseException("Wrong number of params on NICK: expected 1, found " + params.length);

			User user = bot.getUser(data.value(SENDER_NICKNAME_ID));
			user.setNickname(data.value(SENDER_NICKNAME_ID));
			if(data.value(SENDER_USERNAME_ID) != null)
				user.setUsername(data.value(SENDER_USERNAME_ID));
			if(data.value(SENDER_HOSTNAME_ID) != null)
				user.setHostname(data.value(SENDER_HOSTNAME_ID));
			String oldNickname = user.getNickname();
			String newNickname = params[0];
			return new NickMessageEvent(data.message(), user, oldNickname, newNickname);
		}
		return null;
	}
}
