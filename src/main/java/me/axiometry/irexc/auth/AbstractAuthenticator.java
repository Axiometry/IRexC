package me.axiometry.irexc.auth;

import me.axiometry.irexc.Bot;
import me.axiometry.irexc.event.EventListener;
import me.axiometry.irexc.event.auth.AuthSuccessEvent;

public abstract class AbstractAuthenticator implements Authenticator, EventListener {
	private Bot bot;
	private boolean identified;

	@Override
	public synchronized void register(Bot bot) {
		if(bot == null)
			throw new NullPointerException("Bot was null");
		if(this.bot != null)
			throw new IllegalStateException("Already registered");

		this.bot = bot;
		bot.getEventBus().register(this);
	}

	@Override
	public synchronized void unregister(Bot bot) {
		if(bot == null)
			throw new NullPointerException("Bot was null");
		if(this.bot == null)
			throw new IllegalStateException("Already unregistered");
		if(this.bot != bot)
			throw new IllegalStateException("Wrong bot");

		bot.getEventBus().unregister(this);
		this.bot = null;
		identified = false;
	}

	protected void onAuthenticationSuccess() {
		identified = true;

		bot.getEventBus().fire(new AuthSuccessEvent(this));
	}

	@Override
	public boolean hasIdentified() {
		return identified;
	}

	@Override
	public synchronized boolean isRegistered() {
		return bot != null;
	}

	@Override
	public Bot getBot() {
		return bot;
	}
}
