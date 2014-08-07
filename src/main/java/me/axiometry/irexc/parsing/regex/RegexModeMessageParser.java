package me.axiometry.irexc.parsing.regex;

import me.axiometry.irexc.*;
import me.axiometry.irexc.event.message.ModeMessageEvent;
import me.axiometry.irexc.parsing.*;

public class RegexModeMessageParser extends RegexMessageParser<ModeMessageEvent> {
	@Override
	public ModeMessageEvent parse(Bot bot, RegexData data) throws ParseException {
		if(data.value(COMMAND_ID).equalsIgnoreCase("MODE")) {
			String[] params = params(data);
			if(params.length < 2)
				throw new ParseException("Wrong number of params on MODE: expected >=2, found " + params.length);

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

			MessageTarget target;
			if(NICKNAME_REGEX.matcher(params[0]).matches())
				target = bot.getUser(params[0]);
			else if(CHANNEL_REGEX.matcher(params[0]).matches())
				target = bot.getChannel(params[0]);
			else
				target = new UnknownMessageTarget(params[0]);

			if(!params[1].matches("[\\+\\-][a-zA-Z]+"))
				throw new ParseException("Invalid mode on MODE: " + params[1]);
			boolean addingModes = params[1].charAt(0) == '+';
			char[] modes = new char[params[1].length() - 1];
			for(int i = 0; i < modes.length; i++)
				modes[i] = params[1].charAt(i + 1);
			String[] arguments = new String[params.length - 2];
			for(int i = 0; i < arguments.length; i++)
				arguments[i] = params[i + 2];

			return new ModeMessageEvent(data.message(), source, target, modes, addingModes, arguments);
		}
		return null;
	}
}
