package me.axiometry.irexc.parsing;

import me.axiometry.irexc.Bot;

public interface MessageTarget extends MessageParticipant {
	public boolean sendMessage(Bot bot, String message);
	public boolean sendNotice(Bot bot, String message);
}
