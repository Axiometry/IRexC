package me.axiometry.irexc.parsing;

public class UnknownMessageSource implements MessageSource {
	private final String name;

	public UnknownMessageSource(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}
