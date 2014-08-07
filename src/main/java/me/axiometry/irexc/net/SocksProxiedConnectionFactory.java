package me.axiometry.irexc.net;

import java.io.*;
import java.net.*;

@SuppressWarnings("unused")
public abstract class SocksProxiedConnectionFactory implements ProxiedConnectionFactory {
	// SOCKS constants (from java.net.SocksConstants)
	// @formatter:off
	private static final int PROTO_VERS4			= 4;
	private static final int PROTO_VERS				= 5;
	private static final int DEFAULT_PORT			= 1080;

	private static final int NO_AUTH				= 0;
	private static final int GSSAPI					= 1;
	private static final int USER_PASSW				= 2;
	private static final int NO_METHODS				= -1;

	private static final int CONNECT				= 1;
	private static final int BIND					= 2;
	private static final int UDP_ASSOC				= 3;

	private static final int IPV4					= 1;
	private static final int DOMAIN_NAME			= 3;
	private static final int IPV6					= 4;

	private static final int REQUEST_OK				= 0;
	private static final int GENERAL_FAILURE		= 1;
	private static final int NOT_ALLOWED			= 2;
	private static final int NET_UNREACHABLE		= 3;
	private static final int HOST_UNREACHABLE		= 4;
	private static final int CONN_REFUSED			= 5;
	private static final int TTL_EXPIRED			= 6;
	private static final int CMD_NOT_SUPPORTED		= 7;
	private static final int ADDR_TYPE_NOT_SUP		= 8;
	// @formatter:on

	private final ConnectionFactory targetFactory;

	public SocksProxiedConnectionFactory(ConnectionFactory targetFactory) {
		this.targetFactory = targetFactory;
	}

	protected abstract InetSocketAddress getProxyAddress();

	@Override
	public Connection createConnection(String hostname, int port) throws IOException {
		InetSocketAddress address = getProxyAddress();
		if(address == null)
			return targetFactory.createConnection(hostname, port);
		Connection proxyConnection = targetFactory.createConnection(address.getHostString(), address.getPort());
		InetSocketAddress epoint = InetSocketAddress.createUnresolved(hostname, port);// new InetSocketAddress(hostname, port);

		// cmdIn & cmdOut were intialized during the privilegedConnect() call
		BufferedOutputStream out = new BufferedOutputStream(proxyConnection.getOutputStream(), 512);
		InputStream in = proxyConnection.getInputStream();

		/*if(epoint.isUnresolved())
			throw new UnknownHostException(epoint.toString());
		connectV4(in, out, epoint);
		if("".equals(""))
			return proxyConnection;*/

		// This is SOCKS V5
		out.write(PROTO_VERS);
		out.write(2);
		out.write(NO_AUTH);
		out.write(USER_PASSW);
		out.flush();
		byte[] data = new byte[2];
		int i = readSocksReply(in, data);
		if(i != 2 || (data[0]) != PROTO_VERS) {
			// Maybe it's not a V5 sever after all
			// Let's try V4 before we give up
			// SOCKS Protocol version 4 doesn't know how to deal with
			// DOMAIN type of addresses (unresolved addresses here)
			epoint = new InetSocketAddress(hostname, port);
			if(epoint.isUnresolved())
				throw new UnknownHostException(epoint.toString());
			connectV4(in, out, epoint);
		} else {
			if((data[1]) == NO_METHODS)
				throw new SocketException("SOCKS : No acceptable methods");
			connectV5(in, out, epoint);
		}
		return proxyConnection;
	}

	private void connectV5(InputStream in, OutputStream out, InetSocketAddress epoint) throws IOException {
		/*if(!authenticate(data[1], in, out)) {
			throw new SocketException("SOCKS : authentication failed");
		}*/
		out.write(PROTO_VERS);
		out.write(CONNECT);
		out.write(0);
		/* Test for IPV4/IPV6/Unresolved */
		if(epoint.isUnresolved()) {
			out.write(DOMAIN_NAME);
			out.write(epoint.getHostName().length());
			try {
				out.write(epoint.getHostName().getBytes("ISO-8859-1"));
			} catch(java.io.UnsupportedEncodingException uee) {
				assert false;
			}
			out.write((epoint.getPort() >> 8) & 0xff);
			out.write((epoint.getPort() >> 0) & 0xff);
		} else if(epoint.getAddress() instanceof Inet6Address) {
			out.write(IPV6);
			out.write(epoint.getAddress().getAddress());
			out.write((epoint.getPort() >> 8) & 0xff);
			out.write((epoint.getPort() >> 0) & 0xff);
		} else {
			out.write(IPV4);
			out.write(epoint.getAddress().getAddress());
			out.write((epoint.getPort() >> 8) & 0xff);
			out.write((epoint.getPort() >> 0) & 0xff);
		}
		out.flush();
		byte[] data = new byte[4];
		int i = readSocksReply(in, data);
		if(i != 4)
			throw new SocketException("Reply from SOCKS server has bad length");
		SocketException ex = null;
		int len;
		byte[] addr;
		switch(data[1]) {
		case REQUEST_OK:
			// success!
			switch(data[3]) {
			case IPV4:
				addr = new byte[4];
				i = readSocksReply(in, addr);
				if(i != 4)
					throw new SocketException("Reply from SOCKS server badly formatted");
				data = new byte[2];
				i = readSocksReply(in, data);
				if(i != 2)
					throw new SocketException("Reply from SOCKS server badly formatted");
				break;
			case DOMAIN_NAME:
				len = data[1];
				byte[] host = new byte[len];
				i = readSocksReply(in, host);
				if(i != len)
					throw new SocketException("Reply from SOCKS server badly formatted");
				data = new byte[2];
				i = readSocksReply(in, data);
				if(i != 2)
					throw new SocketException("Reply from SOCKS server badly formatted");
				break;
			case IPV6:
				len = data[1];
				addr = new byte[len];
				i = readSocksReply(in, addr);
				if(i != len)
					throw new SocketException("Reply from SOCKS server badly formatted");
				data = new byte[2];
				i = readSocksReply(in, data);
				if(i != 2)
					throw new SocketException("Reply from SOCKS server badly formatted");
				break;
			default:
				ex = new SocketException("Reply from SOCKS server contains wrong code");
				break;
			}
			break;
		case GENERAL_FAILURE:
			ex = new SocketException("SOCKS server general failure");
			break;
		case NOT_ALLOWED:
			ex = new SocketException("SOCKS: Connection not allowed by ruleset");
			break;
		case NET_UNREACHABLE:
			ex = new SocketException("SOCKS: Network unreachable");
			break;
		case HOST_UNREACHABLE:
			ex = new SocketException("SOCKS: Host unreachable");
			break;
		case CONN_REFUSED:
			ex = new SocketException("SOCKS: Connection refused");
			break;
		case TTL_EXPIRED:
			ex = new SocketException("SOCKS: TTL expired");
			break;
		case CMD_NOT_SUPPORTED:
			ex = new SocketException("SOCKS: Command not supported");
			break;
		case ADDR_TYPE_NOT_SUP:
			ex = new SocketException("SOCKS: address type not supported");
			break;
		}
		if(ex != null) {
			in.close();
			out.close();
			throw ex;
		}
	}

	private void connectV4(InputStream in, OutputStream out, InetSocketAddress endpoint) throws IOException {
		if(!(endpoint.getAddress() instanceof Inet4Address)) {
			throw new SocketException("SOCKS V4 requires IPv4 only addresses");
		}
		out.write(PROTO_VERS4);
		out.write(CONNECT);
		out.write((endpoint.getPort() >> 8) & 0xff);
		out.write((endpoint.getPort() >> 0) & 0xff);
		out.write(endpoint.getAddress().getAddress());
		String userName = "not-a-username";
		try {
			out.write(userName.getBytes("ISO-8859-1"));
		} catch(java.io.UnsupportedEncodingException uee) {
			assert false;
		}
		out.write(0);
		out.flush();
		byte[] data = new byte[8];
		int n = readSocksReply(in, data);
		if(n != 8)
			throw new SocketException("Reply from SOCKS server has bad length: " + n);
		if(data[0] != 0 && data[0] != 4)
			throw new SocketException("Reply from SOCKS server has bad version");
		SocketException ex = null;
		switch(data[1]) {
		case 90:
			// Success!
			break;
		case 91:
			ex = new SocketException("SOCKS request rejected");
			break;
		case 92:
			ex = new SocketException("SOCKS server couldn't reach destination");
			break;
		case 93:
			ex = new SocketException("SOCKS authentication failed");
			break;
		default:
			ex = new SocketException("Reply from SOCKS server contains bad status");
			break;
		}
		if(ex != null) {
			in.close();
			out.close();
			throw ex;
		}
	}

	private int readSocksReply(InputStream in, byte[] data) throws IOException {
		int len = data.length;
		int received = 0;
		for(int attempts = 0; received < len && attempts < 3; attempts++) {
			int count = in.read(data, received, len - received);
			if(count < 0)
				throw new SocketException("End of stream during reply from SOCKS server");
			received += count;
		}
		return received;
	}

	@Override
	public ConnectionFactory getUnderlyingFactory() {
		return targetFactory;
	}
}
