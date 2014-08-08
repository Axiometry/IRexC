package me.axiometry.irexc;

import java.util.*;

import me.axiometry.irexc.parsing.MessageTarget;

public final class Channel implements MessageTarget {
	private final Bot bot;
	
	private final String name;
	private final Set<User> users;

	Channel(Bot bot, String name) {
		this.bot = bot;
		this.name = name;

		users = Collections.synchronizedSet(new HashSet<User>());
	}

	@Override
	public String getName() {
		return name;
	}

	void addUser(User user) {
		users.add(user);
		user.joinChannel(this);
	}

	void removeUser(User user) {
		users.remove(user);
		user.leaveChannel(this);
	}

	public boolean containsUser(User user) {
		return users.contains(user);
	}

	public User getUser(String nickname) {
		synchronized(users) {
			for(User user : users)
				if(nickname.equalsIgnoreCase(user.getNickname()))
					return user;
			return null;
		}
	}

	public User[] getUsers() {
		return users.toArray(new User[0]);
	}

	@Override
	public boolean sendMessage(String message) {
		return bot.sendRaw("PRIVMSG " + name + " :" + message);
	}

	@Override
	public boolean sendNotice(String message) {
		return bot.sendRaw("NOTICE " + name + " :" + message);
	}
	
	public Bot getBot() {
		return bot;
	}

	@Override
	public int hashCode() {
		return name.toLowerCase().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Channel && name.equalsIgnoreCase(((Channel) obj).name);
	}

	@Override
	public String toString() {
		return getName();
	}
}
