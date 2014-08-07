package me.axiometry.irexc.auth;

import me.axiometry.irexc.Bot;

public interface Authenticator {
	public String getPassword();
	public boolean hasIdentified();

	public void register(Bot bot);
	public void unregister(Bot bot);
	public boolean isRegistered();
	public Bot getBot();
}
