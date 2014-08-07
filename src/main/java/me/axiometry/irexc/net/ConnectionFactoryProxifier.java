package me.axiometry.irexc.net;

public interface ConnectionFactoryProxifier {
	public ProxiedConnectionFactory proxify(ConnectionFactory factory);
}
