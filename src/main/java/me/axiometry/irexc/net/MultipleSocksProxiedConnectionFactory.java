package me.axiometry.irexc.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class MultipleSocksProxiedConnectionFactory extends SocksProxiedConnectionFactory implements SwitchableProxiedConnectionFactory {
	public enum SwitchCase {
		ON_ERROR, ON_CONNECT
	}

	public enum SelectionMode {
		RANDOM, SEQUENTIAL
	}

	private final List<InetSocketAddress> addresses;
	private final AtomicReference<InetSocketAddress> address;
	private int addressCounter;

	private SwitchCase switchCase = SwitchCase.ON_CONNECT;
	private SelectionMode selectionMode = SelectionMode.RANDOM;

	public MultipleSocksProxiedConnectionFactory(ConnectionFactory targetFactory) {
		super(targetFactory);

		addresses = new ArrayList<InetSocketAddress>();
		address = new AtomicReference<>();
	}

	@Override
	public Connection createConnection(String hostname, int port) throws IOException {
		try {
			return super.createConnection(hostname, port);
		} catch(RuntimeException | IOException exception) {
			if(switchCase == SwitchCase.ON_ERROR)
				switchProxy();
			throw exception;
		} finally {
			if(switchCase == SwitchCase.ON_CONNECT)
				switchProxy();
		}
	}

	@Override
	public void switchProxy() {
		switch(selectionMode) {
		case SEQUENTIAL:
			if(addressCounter >= addresses.size())
				addressCounter = 0;
			int index = addressCounter++;

			address.set(addresses.get(index));
			break;
		case RANDOM:
		default:
			address.set(addresses.get((int) (Math.random() * addresses.size())));
		}
	}

	public void addProxy(InetSocketAddress address) {
		if(address == null)
			throw new NullPointerException();

		synchronized(addresses) {
			addresses.add(address);
			this.address.compareAndSet(null, address);
		}
	}

	public void removeProxy(InetSocketAddress address) {
		if(address == null)
			throw new NullPointerException();

		synchronized(addresses) {
			addresses.remove(address);
			this.address.compareAndSet(address, addresses.size() > 0 ? addresses.get(0) : null);
		}
	}

	public SwitchCase getSwitchCase() {
		return switchCase;
	}

	public SelectionMode getSelectionMode() {
		return selectionMode;
	}

	public void setSwitchCase(SwitchCase switchCase) {
		this.switchCase = switchCase;
	}

	public void setSelectionMode(SelectionMode selectionMode) {
		this.selectionMode = selectionMode;
	}

	@Override
	protected InetSocketAddress getProxyAddress() {
		synchronized(addresses) {
			return address.get();
		}
	}
}