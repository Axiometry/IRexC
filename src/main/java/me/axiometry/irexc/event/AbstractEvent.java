package me.axiometry.irexc.event;

public abstract class AbstractEvent implements Event {
	@Override
	public String getName() {
		return getClass().getSimpleName();
	}
}
