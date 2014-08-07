package me.axiometry.irexc;

public interface UserListener {
	public void onChannelJoin(User user, Channel channel);
	public void onChannelLeave(User user, Channel channel);
}
