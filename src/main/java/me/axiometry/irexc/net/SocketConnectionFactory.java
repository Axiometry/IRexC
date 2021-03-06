package me.axiometry.irexc.net;

import java.io.*;
import java.net.Socket;

public class SocketConnectionFactory implements ConnectionFactory {
	@Override
	public Connection createConnection(String host, int port) throws IOException {
		return new SocketConnection(host, port);
	}

	private final class SocketConnection implements Connection {
		private final Socket socket;

		public SocketConnection(String host, int port) throws IOException {
			socket = new Socket(host, port);
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return socket.getInputStream();
		}
		@Override
		public OutputStream getOutputStream() throws IOException {
			return socket.getOutputStream();
		}

		@Override
		public void close() {
			try {
				socket.close();
			} catch(IOException exception) {}
		}

		@Override
		public boolean isClosed() {
			return socket.isClosed();
		}
	}
}
