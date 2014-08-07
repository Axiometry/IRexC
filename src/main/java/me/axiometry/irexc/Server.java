package me.axiometry.irexc;

import me.axiometry.irexc.parsing.MessageSource;

public class Server implements MessageSource {
	private final String name;

	public Server(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		return name.toLowerCase().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Server && name.equalsIgnoreCase(((Server) obj).name);
	}

	@Override
	public String toString() {
		return name;
	}
}
