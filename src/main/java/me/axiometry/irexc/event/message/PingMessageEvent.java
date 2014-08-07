package me.axiometry.irexc.event.message;


public class PingMessageEvent extends MessageEvent {
	public enum Type {
		PING, PONG
	}

	private final Type type;
	private final String message;

	public PingMessageEvent(String raw, Type type, String message) {
		super(raw);

		this.type = type;
		this.message = message;
	}

	public Type getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}
}
