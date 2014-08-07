package me.axiometry.irexc.parsing.regex;

import me.axiometry.irexc.*;
import me.axiometry.irexc.event.message.PartMessageEvent;
import me.axiometry.irexc.parsing.ParseException;

public class RegexPartMessageParser extends RegexMessageParser<PartMessageEvent> {
	@Override
	public PartMessageEvent parse(Bot bot, RegexData data) throws ParseException {
		if(data.value(COMMAND_ID).equalsIgnoreCase("PART")) {
			String[] params = params(data);
			if(params.length != 2)
				throw new ParseException("Wrong number of params on PART: expected 2, found " + params.length);

			User user = bot.getUser(data.value(SENDER_NICKNAME_ID));
			user.setNickname(data.value(SENDER_NICKNAME_ID));
			if(data.value(SENDER_USERNAME_ID) != null)
				user.setUsername(data.value(SENDER_USERNAME_ID));
			if(data.value(SENDER_HOSTNAME_ID) != null)
				user.setHostname(data.value(SENDER_HOSTNAME_ID));
			Channel channel = bot.getChannel(params[0].split("[\\,]")[0]);
			String reason = params[1];
			return new PartMessageEvent(data.message(), user, channel, reason);
		}
		return null;
	}
}
