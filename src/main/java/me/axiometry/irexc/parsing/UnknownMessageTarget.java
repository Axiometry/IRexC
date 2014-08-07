package me.axiometry.irexc.parsing;

public class UnknownMessageTarget implements MessageTarget {
	private final String name;

	public UnknownMessageTarget(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}
