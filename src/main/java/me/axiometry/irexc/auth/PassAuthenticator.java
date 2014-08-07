package me.axiometry.irexc.auth;

import me.axiometry.irexc.event.EventHandler;
import me.axiometry.irexc.event.bot.ConnectEvent;

public class PassAuthenticator extends AbstractAuthenticator {
	private final String password;

	public PassAuthenticator(String password) {
		this.password = password;
	}

	@EventHandler
	public void onConnect(ConnectEvent event) {
		if(isRegistered() && getBot() == event.getBot()) {
			getBot().sendRaw("PASS " + password);

			onAuthenticationSuccess();
		}
	}

	@Override
	public String getPassword() {
		return password;
	}
}
