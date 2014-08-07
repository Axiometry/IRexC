package me.axiometry.irexc.event.auth;

import me.axiometry.irexc.auth.Authenticator;

public class AuthFailEvent extends AuthEvent {
	public AuthFailEvent(Authenticator authenticator) {
		super(authenticator);
	}
}
