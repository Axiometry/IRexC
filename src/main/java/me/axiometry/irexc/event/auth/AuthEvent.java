package me.axiometry.irexc.event.auth;

import me.axiometry.irexc.auth.Authenticator;
import me.axiometry.irexc.event.AbstractEvent;

public abstract class AuthEvent extends AbstractEvent {
	private final Authenticator authenticator;

	public AuthEvent(Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	public Authenticator getAuthenticator() {
		return authenticator;
	}
}
