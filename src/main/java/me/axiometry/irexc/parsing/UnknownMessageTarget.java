package me.axiometry.irexc.parsing;

public class UnknownMessageTarget implements MessageTarget {
	private final String name;
	
	public UnknownMessageTarget() {
		this(null);
	}

	public UnknownMessageTarget(String name) {
		this.name = name;
	}
	
	@Override
	public boolean sendMessage(String message) {
		return false;
	}
	
	@Override
	public boolean sendNotice(String message) {
		return false;
	}

	@Override
	public String getName() {
		return name;
	}
}
