package me.axiometry.irexc.event;


public final class EventFireException extends Exception {
	private static final long serialVersionUID = -1392819062389929428L;

	private final EventException[] exceptions;

	public EventFireException(EventException... exceptions) {
		this.exceptions = exceptions.clone();
	}

	public EventFireException(String message, EventException... exceptions) {
		super(message);

		this.exceptions = exceptions.clone();
	}

	public EventException[] getExceptions() {
		return exceptions.clone();
	}
}
