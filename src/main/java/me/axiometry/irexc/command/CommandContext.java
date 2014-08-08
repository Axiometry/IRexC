package me.axiometry.irexc.command;

import me.axiometry.irexc.parsing.*;

public final class CommandContext {
	private final String command;
	private final MessageSource source;
	private final MessageTarget target;
	private final String[] args;

	public CommandContext(String command, String[] args) {
		this(command, new UnknownMessageSource(), args);
	}

	public CommandContext(String command, MessageSource source, String[] args) {
		this(command, source, new UnknownMessageTarget(), args);
	}

	public CommandContext(String command, MessageSource source, MessageTarget target, String[] args) {
		this.command = command;
		this.source = source;
		this.target = target;
		this.args = args;
	}

	public String command() {
		return command;
	}

	public MessageSource source() {
		return source;
	}

	public MessageTarget target() {
		return target;
	}

	public String[] args() {
		return args.clone();
	}

	public String arg(int i) {
		if(i >= args.length)
			throw new MissingArgumentException("Expected argument " + (i + 1) + " but found " + args.length + " argument(s)");
		return args[i];
	}
	
	public boolean hasArg(int i) {
		return i < args.length;
	}

	public String argsBetween(int start, int end) {
		StringBuilder builder = new StringBuilder();
		for(int i = start; i <= end; i++) {
			if(i != start)
				builder.append(' ');
			builder.append(arg(i));
		}
		return builder.toString();
	}
	
	public String argsBefore(int end) {
		return argsBetween(0, end);
	}

	public String argsAfter(int start) {
		return argsBetween(start, args.length - 1);
	}
	
	public boolean hasArgs(int start, int end) {
		return start >= 0 && end < args.length;
	}

	public int argInt(int i) {
		try {
			return Integer.parseInt(arg(i));
		} catch(NumberFormatException exception) {
			throw new InvalidArgumentException("Expected integer argument " + (i + 1) + ", found '" + arg(i) + "'");
		}
	}
	
	public boolean isArgInt(int i) {
		if(hasArg(i))
			try {
				Integer.parseInt(arg(i));
				return true;
			} catch(NumberFormatException exception) {}
		return false;
	}

	public long argLong(int i) throws CommandException {
		try {
			return Long.parseLong(arg(i));
		} catch(NumberFormatException exception) {
			throw new InvalidArgumentException("Expected integer argument " + (i + 1) + ", found '" + arg(i) + "'");
		}
	}
	
	public boolean isArgLong(int i) {
		if(hasArg(i))
			try {
				Long.parseLong(arg(i));
				return true;
			} catch(NumberFormatException exception) {}
		return false;
	}

	public double argDouble(int i) {
		try {
			return Double.parseDouble(arg(i));
		} catch(NumberFormatException exception) {
			throw new InvalidArgumentException("Expected decimal argument " + (i + 1) + ", found '" + arg(i) + "'");
		}
	}
	
	public boolean isArgDouble(int i) {
		if(hasArg(i))
			try {
				Double.parseDouble(arg(i));
				return true;
			} catch(NumberFormatException exception) {}
		return false;
	}

	public boolean argBoolean(int i) throws CommandException {
		String arg = arg(i).toLowerCase();
		if(arg.equals("yes") || arg.equals("true") || arg.equals("on"))
			return true;
		else if(arg.equals("no") || arg.equals("false") || arg.equals("off"))
			return false;
		throw new InvalidArgumentException("Expected yes/no argument " + (i + 1) + ", found '" + arg + "'");
	}
	
	public boolean isArgBoolean(int i) {
		if(hasArg(i)) {
			String arg = arg(i).toLowerCase();
			if(arg.equals("yes") || arg.equals("true") || arg.equals("on"))
				return true;
			else if(arg.equals("no") || arg.equals("false") || arg.equals("off"))
				return true;
		}
		return false;
	}
}