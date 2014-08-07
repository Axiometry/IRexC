package me.axiometry.irexc.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.locks.*;

public class ConcurrentEventBus implements EventBus {
	private final Map<Class<? extends Event>, ConcurrentEventDelegate<?>> delegates;
	private final Lock readLock, writeLock;

	public ConcurrentEventBus() {
		delegates = new HashMap<>();

		// Fairness to decrease starvation (registry occurs near initialization, so initial throughput decrease is acceptable)
		ReadWriteLock lock = new ReentrantReadWriteLock(true);
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}

	@Override
	public void fire(Event event) {
		List<ConcurrentEventDelegate<?>> targets = findTargetDelegates(event.getClass());

		for(ConcurrentEventDelegate<?> target : targets) {
			try {
				fireDelegated(target, event);
			} catch(EventFireException exception) {}
		}
	}

	@Override
	public void fireWithError(Event event) throws EventFireException {
		List<ConcurrentEventDelegate<?>> targets = findTargetDelegates(event.getClass());

		List<EventException> exceptions = new ArrayList<>();
		for(ConcurrentEventDelegate<?> target : targets) {
			try {
				fireDelegated(target, event);
			} catch(EventFireException exception) {
				for(EventException cause : exception.getExceptions())
					exceptions.add(cause);
			}
		}

		if(!exceptions.isEmpty())
			throw new EventFireException("Exception occurred firing " + event.getName(), exceptions.toArray(new EventException[exceptions.size()]));
	}

	@Override
	public void fireAsync(final Event event) {
		new Thread() {
			@Override
			public void run() {
				fire(event);
			}
		}.start();
	}

	private List<ConcurrentEventDelegate<?>> findTargetDelegates(Class<?> eventClass) {
		List<ConcurrentEventDelegate<?>> targets = new ArrayList<>();

		readLock.lock();
		try {
			Class<?> delegateEventClass = eventClass;
			while(Event.class.isAssignableFrom(delegateEventClass)) {
				ConcurrentEventDelegate<?> target = delegates.get(delegateEventClass);
				if(target != null)
					targets.add(target);
				delegateEventClass = delegateEventClass.getSuperclass();
			}
		} finally {
			readLock.unlock();
		}

		return targets;
	}

	private <T extends Event> void fireDelegated(ConcurrentEventDelegate<T> delegate, Event event) throws EventFireException {
		delegate.handleEvent(delegate.getEventClass().cast(event));
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void register(EventListener listener) {
		List<Method> eventHandlers = new ArrayList<>();
		Class<?> listenerClass = listener.getClass();
		while(listenerClass != null && EventListener.class.isAssignableFrom(listenerClass)) {
			for(Method method : listenerClass.getDeclaredMethods()) {
				if(method.getAnnotation(EventHandler.class) == null || isMethodOverriden(method, listener.getClass()))
					continue;
				if(!method.isAccessible())
					method.setAccessible(true);
				if(method.getParameterTypes().length != 1)
					throw new IllegalArgumentException(String.format(	"Method %s in class %s has incorrect amount of parameters",
																		method,
																		listenerClass.getName()));
				eventHandlers.add(method);
			}
			listenerClass = listenerClass.getSuperclass();
		}

		writeLock.lock();
		try {
			for(Method method : eventHandlers) {
				Class<? extends Event> eventClass = method.getParameterTypes()[0].asSubclass(Event.class);
				ConcurrentEventDelegate<?> delegate = delegates.get(eventClass);
				if(delegate == null) {
					delegate = new ConcurrentEventDelegate(eventClass);
					delegates.put(eventClass, delegate);
				}
				delegate.registerHandler(listener, method);
			}
		} finally {
			writeLock.unlock();
		}
	}
	private boolean isMethodOverriden(Method method, Class<?> topLevelClass) {
		Class<?> declaringClass = method.getDeclaringClass();
		while(!declaringClass.equals(topLevelClass) && !Object.class.equals(topLevelClass)) {
			try {
				topLevelClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
				return true;
			} catch(NoSuchMethodException exception) {}
			topLevelClass = topLevelClass.getSuperclass();
		}
		return false;
	}

	@Override
	public void unregister(EventListener listener) {
		List<Method> eventHandlers = new ArrayList<>();
		Class<?> listenerClass = listener.getClass();
		while(listenerClass != null && EventListener.class.isAssignableFrom(listenerClass)) {
			for(Method method : listenerClass.getDeclaredMethods()) {
				if(method.getAnnotation(EventHandler.class) == null || isMethodOverriden(method, listener.getClass()))
					continue;
				if(!method.isAccessible())
					method.setAccessible(true);
				if(method.getParameterTypes().length != 1)
					throw new IllegalArgumentException(String.format(	"Method %s in class %s has incorrect amount of parameters",
																		method,
																		listenerClass.getName()));
				eventHandlers.add(method);
			}
			listenerClass = listenerClass.getSuperclass();
		}

		writeLock.lock();
		try {
			for(Method method : eventHandlers) {
				Class<? extends Event> eventClass = method.getParameterTypes()[0].asSubclass(Event.class);
				delegates.remove(eventClass);
			}
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void clearListeners() {
		writeLock.lock();
		try {
			for(ConcurrentEventDelegate<?> delegate : delegates.values())
				delegate.clearHandlers();
			delegates.clear();
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public EventListener[] getListeners(Class<?> eventClass) {
		readLock.lock();
		try {
			Set<EventListener> listeners = new HashSet<>();
			for(ConcurrentEventDelegate<?> delegate : delegates.values()) {
				if(eventClass.isAssignableFrom(delegate.getEventClass()))
					listeners.addAll(delegate.getHandlers().keySet());
			}
			return listeners.toArray(new EventListener[listeners.size()]);
		} finally {
			readLock.unlock();
		}
	}
}
