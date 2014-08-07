package me.axiometry.irexc.event.auth;

import me.axiometry.irexc.auth.Authenticator;

public class AuthSuccessEvent extends AuthEvent {
	public AuthSuccessEvent(Authenticator authenticator) {
		super(authenticator);
	}
}
