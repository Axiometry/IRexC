package me.axiometry.irexc.parsing.regex;

import me.axiometry.irexc.*;
import me.axiometry.irexc.event.message.UserMessageEvent;
import me.axiometry.irexc.parsing.*;

public class RegexUserMessageParser extends RegexMessageParser<UserMessageEvent> {
	@Override
	public UserMessageEvent parse(Bot bot, RegexData data) throws ParseException {
		if(data.value(COMMAND_ID).equalsIgnoreCase("PRIVMSG") || data.value(COMMAND_ID).equalsIgnoreCase("NOTICE")) {
			String[] params = params(data);
			if(params.length != 2)
				throw new ParseException("Wrong number of params on PRIVMSG/NOTICE: expected 2, found " + params.length);

			MessageSource source;
			if(data.value(SENDER_NICKNAME_ID) != null) {
				User user = bot.getUser(data.value(SENDER_NICKNAME_ID));
				user.setNickname(data.value(SENDER_NICKNAME_ID));
				if(data.value(SENDER_USERNAME_ID) != null)
					user.setUsername(data.value(SENDER_USERNAME_ID));
				if(data.value(SENDER_HOSTNAME_ID) != null)
					user.setHostname(data.value(SENDER_HOSTNAME_ID));
				source = user;
			} else if(data.value(SENDER_ADDRESS_HOSTNAME_ID) != null)
				source = new Server(data.value(SENDER_ADDRESS_HOSTNAME_ID));
			else
				source = new UnknownMessageSource(data.value(SENDER_ID));

			String targetName = params[0].split("[\\,]")[0];
			MessageTarget target;
			if(NICKNAME_REGEX.matcher(targetName).matches())
				target = bot.getUser(targetName);
			else if(CHANNEL_REGEX.matcher(targetName).matches())
				target = bot.getChannel(targetName);
			else
				target = new UnknownMessageTarget(targetName);

			String message = params[1];
			UserMessageEvent.Type type = data.value(COMMAND_ID).equalsIgnoreCase("PRIVMSG") ? UserMessageEvent.Type.MESSAGE : UserMessageEvent.Type.NOTICE;
			boolean ctcp = message.startsWith("\u0001") && message.endsWith("\u0001");
			if(ctcp)
				message = message.substring(1, message.length() - 1);

			return new UserMessageEvent(data.message(), source, target, type, message, ctcp);
		}
		return null;
	}
}
