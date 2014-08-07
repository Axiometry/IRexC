package me.axiometry.irexc.event.message;

import me.axiometry.irexc.parsing.*;

public class UserMessageEvent extends MessageEvent {
	public enum Type {
		MESSAGE, NOTICE
	}

	private final MessageSource source;
	private final MessageTarget target;
	private final Type type;
	private final String message;
	private final boolean ctcp;

	public UserMessageEvent(String raw, MessageSource source, MessageTarget target, Type type, String message, boolean ctcp) {
		super(raw);

		this.source = source;
		this.target = target;
		this.type = type;
		this.message = message;
		this.ctcp = ctcp;

	}

	public MessageSource getSource() {
		return source;
	}

	public MessageTarget getTarget() {
		return target;
	}

	public Type getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public boolean isCtcp() {
		return ctcp;
	}

	@Override
	public String toString() {
		return getName() + ": " + (type == Type.MESSAGE ? "Message" : "Notice") + (ctcp ? " (CTCP)" : "") + " from " + source.getName() + " to "
				+ target.getName() + ": " + message;
	}
}
