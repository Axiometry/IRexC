package me.axiometry.irexc.net;

import java.net.InetSocketAddress;

public class SingleSocksProxiedConnectionFactory extends SocksProxiedConnectionFactory {
	private InetSocketAddress address;

	public SingleSocksProxiedConnectionFactory(ConnectionFactory targetFactory) {
		this(targetFactory, null);
	}

	public SingleSocksProxiedConnectionFactory(ConnectionFactory targetFactory, String host, int port) {
		this(targetFactory, InetSocketAddress.createUnresolved(host, port));
	}

	public SingleSocksProxiedConnectionFactory(ConnectionFactory targetFactory, InetSocketAddress address) {
		super(targetFactory);
		this.address = address;
	}

	@Override
	protected InetSocketAddress getProxyAddress() {
		return address;
	}

	public void setProxyAddress(String host, int port) {
		setProxyAddress(InetSocketAddress.createUnresolved(host, port));
	}

	public void setProxyAddress(InetSocketAddress address) {
		this.address = address;
	}
}
