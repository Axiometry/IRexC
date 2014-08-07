package me.axiometry.irexc.parsing.regex;

import java.util.Arrays;

import me.axiometry.irexc.*;
import me.axiometry.irexc.event.message.InviteMessageEvent;
import me.axiometry.irexc.parsing.ParseException;

public class RegexInviteMessageParser extends RegexMessageParser<InviteMessageEvent> {
	@Override
	public InviteMessageEvent parse(Bot bot, RegexData data) throws ParseException {
		if(data.value(COMMAND_ID).equalsIgnoreCase("INVITE")) {
			String[] params = params(data);
			if(params.length != 2 || !NICKNAME_REGEX.matcher(params[0]).matches() || !CHANNEL_REGEX.matcher(params[1]).matches())
				throw new ParseException("Wrong params for INVITE command: expected 1 nickname and 1 channel, found " + Arrays.toString(params));

			User invitee = bot.getUser(data.value(SENDER_NICKNAME_ID));
			invitee.setNickname(data.value(SENDER_NICKNAME_ID));
			if(data.value(SENDER_USERNAME_ID) != null)
				invitee.setUsername(data.value(SENDER_USERNAME_ID));
			if(data.value(SENDER_HOSTNAME_ID) != null)
				invitee.setHostname(data.value(SENDER_HOSTNAME_ID));
			User invited = bot.getUser(params[0]);
			Channel channel = bot.getChannel(params[1]);
			return new InviteMessageEvent(data.message(), invitee, invited, channel);
		}
		return null;
	}
}
