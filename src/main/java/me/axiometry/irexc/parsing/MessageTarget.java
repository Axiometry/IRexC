package me.axiometry.irexc.parsing;

public interface MessageTarget extends MessageParticipant {
	public boolean sendMessage(String message);
	public boolean sendNotice(String message);
}
