package me.axiometry.irexc.parsing;

import me.axiometry.irexc.Bot;

public class UnknownMessageTarget implements MessageTarget {
	private final String name;
	
	public UnknownMessageTarget() {
		this(null);
	}

	public UnknownMessageTarget(String name) {
		this.name = name;
	}
	
	@Override
	public boolean sendMessage(Bot bot, String message) {
		return false;
	}
	
	@Override
	public boolean sendNotice(Bot bot, String message) {
		return false;
	}

	@Override
	public String getName() {
		return name;
	}
}
