package me.axiometry.irexc.net;

import java.io.*;
import java.lang.reflect.Field;
import java.net.*;

public final class ConnectionSocket extends Socket {
	private final Connection connection;
	private final InetAddress address;

	public ConnectionSocket(Connection connection, String targetHost, int targetPort) throws IOException {
		super((java.net.SocketImpl) null);

		this.connection = connection;
		InetSocketAddress address = InetSocketAddress.createUnresolved(targetHost, targetPort);
		this.address = address.getAddress();

		try {
			Field field = Socket.class.getDeclaredField("connected");
			field.setAccessible(true);
			field.setBoolean(this, true);
		} catch(Exception exception) {
			throw new IOException("Could not set connected", exception);
		}
	}

	public static ConnectionSocket create(ConnectionFactory factory, InetSocketAddress target) throws IOException {
		String targetHost = target.getHostString();
		int targetPort = target.getPort();

		Connection connection = factory.createConnection(targetHost, targetPort);
		return new ConnectionSocket(connection, targetHost, targetPort);
	}

	@Override
	public InetAddress getInetAddress() {
		return address;
	}

	@Override
	public InetAddress getLocalAddress() {
		return address;
	}

	@Override
	public int getPort() {
		return 80;
	}

	@Override
	public int getLocalPort() {
		return 80;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return connection.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return connection.getOutputStream();
	}

	@Override
	public void setTcpNoDelay(boolean on) throws SocketException {
	}

	@Override
	public boolean getTcpNoDelay() throws SocketException {
		return false;
	}

	@Override
	public void setSoLinger(boolean on, int val) throws SocketException {
	}

	@Override
	public int getSoLinger() throws SocketException {
		return -1;
	}

	@Override
	public synchronized void setSoTimeout(int timeout) throws SocketException {
	}

	@Override
	public synchronized int getSoTimeout() throws SocketException {
		return 0;
	}

	@Override
	public synchronized void close() throws IOException {
		connection.close();
	}

	@Override
	public String toString() {
		return "Connection" + connection.toString();
	}
}