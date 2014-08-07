package me.axiometry.irexc;

import java.util.*;

import me.axiometry.irexc.parsing.*;

public class User implements MessageSource, MessageTarget {
	protected String nickname, username, hostname;
	private final Set<Channel> channels;
	private final List<UserListener> listeners;

	User(String nickname) {
		this.nickname = nickname;

		channels = Collections.synchronizedSet(new HashSet<Channel>());
		listeners = Collections.synchronizedList(new LinkedList<UserListener>());
	}

	@Override
	public String getName() {
		return nickname;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	void joinChannel(Channel channel) {
		channels.add(channel);

		synchronized(listeners) {
			for(UserListener listener : listeners) {
				try {
					listener.onChannelJoin(this, channel);
				} catch(Exception exception) {}
			}
		}
	}

	void leaveChannel(Channel channel) {
		channels.remove(channel);

		synchronized(listeners) {
			for(UserListener listener : listeners) {
				try {
					listener.onChannelLeave(this, channel);
				} catch(Exception exception) {}
			}
		}
	}

	public boolean isInChannel(Channel channel) {
		return channels.contains(channel);
	}

	public Channel getChannel(String name) {
		if(!name.startsWith("#") && !name.startsWith("&"))
			throw new IllegalArgumentException("Invalid channel name prefix");
		synchronized(channels) {
			for(Channel channel : channels)
				if(name.equalsIgnoreCase(channel.getName()))
					return channel;
			return null;
		}
	}
	public Channel[] getChannels() {
		return channels.toArray(new Channel[0]);
	}

	public void registerListener(UserListener listener) {
		listeners.add(listener);
	}

	public void unregisterListener(UserListener listener) {
		listeners.remove(listener);
	}

	@Override
	public int hashCode() {
		return nickname.toLowerCase().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof User && nickname.equalsIgnoreCase(((User) obj).nickname);
	}

	@Override
	public String toString() {
		return getName();
	}
}
