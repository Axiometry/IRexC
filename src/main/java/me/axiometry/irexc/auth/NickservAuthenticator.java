package me.axiometry.irexc.auth;

import me.axiometry.irexc.event.EventHandler;
import me.axiometry.irexc.event.message.ServerMessageEvent;

public class NickservAuthenticator extends AbstractAuthenticator {
	private final String nickservName;
	private final String password;

	public NickservAuthenticator(String nickservName, String password) {
		this.nickservName = nickservName;
		this.password = password;
	}

	@EventHandler
	public synchronized void onServerMessage(ServerMessageEvent event) {
		if(isRegistered() && !hasIdentified() && event.getCode() == ServerMessageEvent.RPL_ENDOFMOTD) {
			getBot().sendMessage(getBot().getUser("NickServ"), "identify " + password);

			onAuthenticationSuccess();
		}
	}

	public String getNickservName() {
		return nickservName;
	}

	@Override
	public String getPassword() {
		return password;
	}
}
