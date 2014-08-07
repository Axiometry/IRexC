package me.axiometry.irexc.event;

public interface EventBus {
	public void fire(Event event);
	public void fireWithError(Event event) throws EventFireException, UnsupportedOperationException;
	public void fireAsync(Event event) throws UnsupportedOperationException;

	public void register(EventListener listener);
	public void unregister(EventListener listener);
	public void clearListeners();

	public EventListener[] getListeners(Class<?> eventClass);
}
