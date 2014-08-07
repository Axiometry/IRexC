package me.axiometry.irexc.parsing;

import java.util.concurrent.locks.*;

import me.axiometry.irexc.Bot;
import me.axiometry.irexc.event.message.MessageEvent;

public final class ConcurrentParserManager implements ParserManager {
	private final ParserManager delegate;
	protected final Lock readLock, writeLock;

	public ConcurrentParserManager(ParserManager delegate) {
		this.delegate = delegate;

		ReadWriteLock lock = new ReentrantReadWriteLock(true);
		readLock = lock.readLock();
		writeLock = lock.writeLock();
	}

	@Override
	public <T extends MessageEvent> void bind(Class<T> messageType, MessageParser<T> parser) {
		writeLock.lock();
		try {
			delegate.bind(messageType, parser);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public <T extends MessageEvent> void unbind(Class<T> messageType) {
		writeLock.lock();
		try {
			delegate.unbind(messageType);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public <T extends MessageEvent> void unbind(MessageParser<T> parser) {
		writeLock.lock();
		try {
			delegate.unbind(parser);
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public <T extends MessageEvent> MessageParser<T> getParser(Class<T> messageType) {
		readLock.lock();
		try {
			return delegate.getParser(messageType);
		} finally {
			readLock.unlock();
		}
	}

	@Override
	public MessageEvent parse(Bot bot, String line) throws ParseException {
		readLock.lock();
		try {
			return delegate.parse(bot, line);
		} finally {
			readLock.unlock();
		}
	}
}
