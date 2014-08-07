package me.axiometry.irexc.event;

public interface CancellableEvent extends Event {
	public boolean isCancelled();
	public void setCancelled(boolean cancelled);
}
