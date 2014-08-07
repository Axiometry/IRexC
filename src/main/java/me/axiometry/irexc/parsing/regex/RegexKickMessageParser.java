package me.axiometry.irexc.parsing.regex;

import me.axiometry.irexc.*;
import me.axiometry.irexc.event.message.KickMessageEvent;
import me.axiometry.irexc.parsing.ParseException;

public class RegexKickMessageParser extends RegexMessageParser<KickMessageEvent> {
	@Override
	public KickMessageEvent parse(Bot bot, RegexData data) throws ParseException {
		if(data.value(COMMAND_ID).equalsIgnoreCase("KICK")) {
			String[] params = params(data);
			if(params.length != 3)
				throw new ParseException("Wrong number of params on KICK: expected 3, found " + params.length);

			User source = bot.getUser(data.value(SENDER_NICKNAME_ID));
			source.setNickname(data.value(SENDER_NICKNAME_ID));
			if(data.value(SENDER_USERNAME_ID) != null)
				source.setUsername(data.value(SENDER_USERNAME_ID));
			if(data.value(SENDER_HOSTNAME_ID) != null)
				source.setHostname(data.value(SENDER_HOSTNAME_ID));
			Channel channel = bot.getChannel(params[0].split("[\\,]")[0]);
			User user = bot.getUser(params[1].split("[\\,]")[1]);
			String reason = params[2];
			return new KickMessageEvent(data.message(), source, channel, user, reason);
		}
		return null;
	}
}
