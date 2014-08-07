package me.axiometry.irexc.net;

import java.io.*;

public interface Connection {
	public InputStream getInputStream() throws IOException;
	public OutputStream getOutputStream() throws IOException;

	public boolean isClosed();
	public void close();
}
