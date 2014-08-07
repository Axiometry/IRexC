package me.axiometry.irexc.net;

import java.io.IOException;

public interface ConnectionFactory {
	public Connection createConnection(String host, int port) throws IOException;
}
