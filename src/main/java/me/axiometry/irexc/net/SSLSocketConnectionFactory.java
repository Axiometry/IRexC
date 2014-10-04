package me.axiometry.irexc.net;

import java.io.*;
import java.net.Socket;

import javax.net.ssl.*;

public class SSLSocketConnectionFactory implements ConnectionFactory {
	@Override
	public Connection createConnection(String host, int port) throws IOException {
		return new SSLSocketConnection(host, port);
	}

	private final class SSLSocketConnection implements Connection {
		private final Socket socket;

		public SSLSocketConnection(String host, int port) throws IOException {
            socket = SSLSocketFactory.getDefault().createSocket(host, port);
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
