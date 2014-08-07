package me.axiometry.irexc.net;


public interface ProxiedConnectionFactory extends ConnectionFactory {
	public ConnectionFactory getUnderlyingFactory();
}
