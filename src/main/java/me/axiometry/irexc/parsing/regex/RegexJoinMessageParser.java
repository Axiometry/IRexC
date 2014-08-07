package me.axiometry.irexc.parsing.regex;

import me.axiometry.irexc.*;
import me.axiometry.irexc.event.message.JoinMessageEvent;
import me.axiometry.irexc.parsing.ParseException;

public class RegexJoinMessageParser extends RegexMessageParser<JoinMessageEvent> {
	@Override
	public JoinMessageEvent parse(Bot bot, RegexData data) throws ParseException {
		if(data.value(COMMAND_ID).equalsIgnoreCase("JOIN")) {
			String[] params = params(data);
			if(params.length != 1)
				throw new ParseException("Wrong number of params on JOIN: expected 1, found " + params.length);

			User user = bot.getUser(data.value(SENDER_NICKNAME_ID));
			user.setNickname(data.value(SENDER_NICKNAME_ID));
			if(data.value(SENDER_USERNAME_ID) != null)
				user.setUsername(data.value(SENDER_USERNAME_ID));
			if(data.value(SENDER_HOSTNAME_ID) != null)
				user.setHostname(data.value(SENDER_HOSTNAME_ID));
			Channel channel = bot.getChannel(params[0].split("[\\,]")[0]);
			return new JoinMessageEvent(data.message(), user, channel);
		}
		return null;
	}
}
