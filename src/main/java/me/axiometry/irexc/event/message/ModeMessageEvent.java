package me.axiometry.irexc.event.message;

import me.axiometry.irexc.parsing.*;

public class ModeMessageEvent extends MessageEvent {
	private final MessageSource source;
	private final MessageTarget target;
	private final char[] modes;
	private final boolean addingModes;
	private final String[] arguments;

	public ModeMessageEvent(String raw, MessageSource source, MessageTarget target, char[] modes, boolean addingModes) {
		this(raw, source, target, modes, addingModes, new String[0]);
	}

	public ModeMessageEvent(String raw, MessageSource source, MessageTarget target, char[] modes, boolean addingModes, String[] arguments) {
		super(raw);

		this.source = source;
		this.target = target;
		this.modes = modes.clone();
		this.addingModes = addingModes;
		this.arguments = arguments.clone();
	}

	public MessageSource getSource() {
		return source;
	}

	public MessageTarget getTarget() {
		return target;
	}

	public char[] getModes() {
		return modes.clone();
	}

	public boolean isAddingModes() {
		return addingModes;
	}

	public String[] getArguments() {
		return arguments.clone();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getName()).append(": ");
		if(source != null)
			builder.append(source.getName()).append(" sets mode ");
		else
			builder.append("Mode set ");
		builder.append(addingModes ? '+' : '-');
		for(char mode : modes)
			builder.append(mode);
		builder.append(" on ").append(target.getName());
		for(int i = 0; i < arguments.length; i++) {
			if(i == 0)
				builder.append(" (");
			builder.append(arguments[i]).append(i == arguments.length - 1 ? ")" : ", ");
		}
		return builder.toString();
	}
}
