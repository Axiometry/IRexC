package me.axiometry.irexc;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import me.axiometry.irexc.auth.Authenticator;
import me.axiometry.irexc.event.*;
import me.axiometry.irexc.event.bot.*;
import me.axiometry.irexc.event.irc.*;
import me.axiometry.irexc.event.message.*;
import me.axiometry.irexc.net.*;
import me.axiometry.irexc.parsing.*;
import me.axiometry.irexc.parsing.regex.*;

public class Bot extends User {
	private static final int DEFAULT_PORT = 6667;
	private static final ConnectionFactory DEFAULT_CONNECTION_FACTORY = new SocketConnectionFactory();

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	private static final String DEFAULT_LINE_ENDING = "\r\n";

	private final String host;
	private final int port;
	private final String realname;

	private final AtomicBoolean connected = new AtomicBoolean();
	private final ConnectionFactory connectionFactory;
	private Connection connection;

	private final Authenticator authenticator;
	private final Charset charset;
	private final String lineEnding;

	private final ExecutorService service;
	private ProcessThread processThread;
	private ReaderThread readerThread;
	private Future<?> processFuture, readerFuture;

	private final EventBus eventBus;
	private final ParserManager parser;

	protected Bot(Configuration config) {
		super(config.nickname);
		super.setUsername(config.username);
		realname = config.realname;

		host = config.host;
		port = config.port;

		connectionFactory = config.connectionFactory;
		service = Executors.newFixedThreadPool(2);

		authenticator = config.authenticator;
		charset = config.charset;
		lineEnding = config.lineEnding;

		eventBus = config.eventBus;
		parser = config.parser;

		if(authenticator != null)
			authenticator.register(this);
	}

	public synchronized final void connect() throws IOException {
		disconnect();

		connection = connectionFactory.createConnection(host, port);
		processFuture = service.submit(processThread = new ProcessThread());
		readerFuture = service.submit(readerThread = new ReaderThread(connection));
		connected.set(true);

		try {
			eventBus.fireWithError(new ConnectEvent(this));
		} catch(Exception exception) {
			handleListenerError(exception);
		}

		handleConnectIdentification();
	}

	protected void handleConnectIdentification() {
		sendRaw("NICK " + nickname);
		sendRaw("USER " + (username == null ? nickname : username) + " " + (hostname == null ? nickname : hostname) + " " + host + " "
				+ (realname == null ? nickname : realname));
	}

	public synchronized final void disconnect() {
		if(!connected.compareAndSet(true, false))
			return;

		try {
			eventBus.fireWithError(new DisconnectEvent(this));
		} catch(Exception exception) {
			handleListenerError(exception);
		} finally {
			if(connection != null) {
				connection.close();
				connection = null;
			}
			if(readerFuture != null) {
				readerThread = null;
				readerFuture.cancel(true);
				readerFuture = null;
			}
			if(processThread != null) {
				processThread.stop();
				processThread = null;
				processFuture.cancel(true);
				processFuture = null;
			}
		}
	}

	public final boolean sendMessage(MessageTarget target, String message) {
		return sendMessage(target, message, false);
	}

	public final boolean sendMessage(MessageTarget target, String message, boolean ctcp) {
		return sendRaw("PRIVMSG " + target.getName() + " :" + (ctcp ? "\u0001" : "") + message + (ctcp ? "\u0001" : ""));
	}

	public final boolean sendNotice(MessageTarget target, String message) {
		return sendNotice(target, message, false);
	}

	public final boolean sendNotice(MessageTarget target, String message, boolean ctcp) {
		return sendRaw("NOTICE " + target.getName() + " :" + (ctcp ? "\u0001" : "") + message + (ctcp ? "\u0001" : ""));
	}

	public synchronized final boolean sendRaw(String raw) {
		if(!connected.get())
			throw new IllegalStateException("Not connected");

		MessageSentEvent event = new MessageSentEvent(this, raw);
		try {
			eventBus.fireWithError(event);
		} catch(Exception exception) {
			handleListenerError(exception);
		}
		if(event.isCancelled())
			return false;

		try {
			OutputStream out = connection.getOutputStream();
			out.write(raw.concat(lineEnding).getBytes(charset));
			out.flush();
			return true;
		} catch(IOException exception) {
			handleIOError(exception);
			return false;
		}
	}

	private void parseLine(String line) {
		try {
			if(line.startsWith(":")) {
				String part = line.substring(line.indexOf(' ') + 1);
				part = part.substring(0, part.indexOf(' '));
				if(Integer.parseInt(part) == 353)
					return;
			}
		} catch(Exception e) {}

		MessageEvent message = null;
		try {
			message = parser.parse(this, line);
		} catch(ParseException exception) {
			handleParseError(exception);
		}
		if(message == null)
			message = new UnknownMessageEvent(line);

		try {
			handleMessage(message);
		} catch(Exception exception) {
			handleListenerError(exception);
		}

		try {
			eventBus.fireWithError(message);
		} catch(Exception exception) {
			handleListenerError(exception);
		}
	}

	protected void handleMessage(MessageEvent message) {
		Event event = null;
		if(message instanceof JoinMessageEvent) {
			JoinMessageEvent joinMessage = (JoinMessageEvent) message;
			Channel channel = joinMessage.getChannel();
			User user = joinMessage.getUser();
			channel.addUser(user);

			event = new UserJoinEvent(channel, user);
		} else if(message instanceof PartMessageEvent) {
			PartMessageEvent partMessage = (PartMessageEvent) message;
			Channel channel = partMessage.getChannel();
			User user = partMessage.getUser();
			channel.removeUser(user);

			String reason = partMessage.getReason();
			if(reason != null && !reason.isEmpty())
				event = new UserLeaveEvent(channel, user, reason);
			else
				event = new UserLeaveEvent(channel, user);
		} else if(message instanceof KickMessageEvent) {
			KickMessageEvent kickMessage = (KickMessageEvent) message;
			Channel channel = kickMessage.getChannel();
			User user = kickMessage.getUser();
			channel.removeUser(user);

			String reason = kickMessage.getReason();
			if(reason != null && !reason.isEmpty())
				event = new UserKickLeaveEvent(channel, user, reason);
			else
				event = new UserKickLeaveEvent(channel, user);
		} else if(message instanceof QuitMessageEvent) {
			QuitMessageEvent quitMessage = (QuitMessageEvent) message;
			String reason = quitMessage.getReason();
			User user = quitMessage.getUser();

			for(Channel channel : user.getChannels()) {
				channel.removeUser(user);

				UserQuitLeaveEvent quitEvent;
				if(reason != null && !reason.isEmpty())
					quitEvent = new UserQuitLeaveEvent(channel, user, reason);
				else
					quitEvent = new UserQuitLeaveEvent(channel, user);

				try {
					eventBus.fireWithError(quitEvent);
				} catch(Exception exception) {
					handleListenerError(exception);
				}
			}

			if(reason != null && !reason.isEmpty())
				event = new UserQuitEvent(user, reason);
			else
				event = new UserQuitEvent(user);
		} else if(message instanceof NickMessageEvent) {
			NickMessageEvent nickMessage = (NickMessageEvent) message;
			User user = nickMessage.getUser();
			user.setNickname(nickMessage.getNewNickname());

			event = new UserNickChangeEvent(user, nickMessage.getOldNickname(), nickMessage.getNewNickname());
		} else if(message instanceof PingMessageEvent) {
			PingMessageEvent pingMessage = (PingMessageEvent) message;
			if(pingMessage.getType() == PingMessageEvent.Type.PING)
				if(pingMessage.getMessage() != null)
					sendRaw("PONG :" + pingMessage.getMessage());
				else
					sendRaw("PONG");
		} else if(message instanceof ServerMessageEvent) {
			ServerMessageEvent serverMessage = (ServerMessageEvent) message;
			if(serverMessage.getCode() == ServerMessageEvent.RPL_NAMREPLY) {
				String[] options = serverMessage.getOptions();
				if(options.length < 1 || options.length > 2)
					return;
				Channel channel = getChannel(options[options.length - 1]);
				String[] users = serverMessage.getMessage().split(" ");
				for(String user : users)
					channel.addUser(getUser(user.matches("[\\@\\+].*") ? user.substring(1) : user));
			}
		}

		if(event != null) {
			try {
				eventBus.fireWithError(event);
			} catch(Exception exception) {
				handleListenerError(exception);
			}
		}
	}

	protected void handleListenerError(Exception exception) {
		exception.printStackTrace();
		if(exception instanceof EventFireException)
			for(EventException ex : ((EventFireException) exception).getExceptions())
				ex.printStackTrace();
	}

	protected void handleParseError(ParseException exception) {
	}

	protected void handleIOError(Exception exception) {
		exception.printStackTrace();
		disconnect();
	}

	public User getUser(String nickname) {
		if(nickname.equalsIgnoreCase(this.nickname))
			return this;
		for(Channel channel : getChannels()) {
			User user = channel.getUser(nickname);
			if(user != null)
				return user;
		}
		return new User(nickname);
	}

	@Override
	public Channel getChannel(String name) {
		Channel channel = super.getChannel(name);
		if(channel == null)
			return new Channel(name);
		return channel;
	}

	public Channel join(String channelName) {
		if(!connected.get())
			throw new IllegalStateException("Not connected");
		Channel channel = getChannel(channelName);
		return join(channel) ? channel : null;
	}

	public boolean join(Channel channel) {
		if(!connected.get())
			throw new IllegalStateException("Not connected");
		return sendRaw("JOIN " + channel.getName());
	}

	public Channel leave(String channelName) {
		if(!connected.get())
			throw new IllegalStateException("Not connected");
		Channel channel = getChannel(channelName);
		return leave(channel) ? channel : null;
	}

	public boolean leave(Channel channel) {
		if(!connected.get())
			throw new IllegalStateException("Not connected");
		return sendRaw("PART " + channel.getName());
	}

	@Override
	public void setNickname(String nickname) {
		if(getUser(nickname) != null && getUser(nickname) != this)
			throw new IllegalArgumentException("User already exists!");
		if(nickname.equalsIgnoreCase(this.nickname) || sendRaw("NICK " + nickname))
			super.setNickname(nickname);
	}

	protected void setNicknameNoMessage(String nickname) {
		super.setNickname(nickname);
	}

	public void quit() {
		sendRaw("QUIT");
	}

	public void quit(String message) {
		sendRaw("QUIT :" + message);
	}

	public final boolean isConnected() {
		return connected.get();
	}

	public final EventBus getEventBus() {
		return eventBus;
	}

	@Override
	public int hashCode() {
		return super.hashCode() + 1;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Bot && super.equals(obj);
	}

	public static Configuration configure() {
		return new Configuration();
	}

	public static final class Configuration {
		private String nickname, username, realname;

		private String host;
		private int port = DEFAULT_PORT;
		private ConnectionFactory connectionFactory = DEFAULT_CONNECTION_FACTORY;

		private Authenticator authenticator;
		private Charset charset = DEFAULT_CHARSET;
		private String lineEnding = DEFAULT_LINE_ENDING;

		private EventBus eventBus = new ConcurrentEventBus();
		private ParserManager parser;

		private Configuration() {
			parser = new ConcurrentParserManager(new RegexParserManager());
			parser.bind(UserMessageEvent.class, new RegexUserMessageParser());
			parser.bind(JoinMessageEvent.class, new RegexJoinMessageParser());
			parser.bind(PartMessageEvent.class, new RegexPartMessageParser());
			parser.bind(KickMessageEvent.class, new RegexKickMessageParser());
			parser.bind(QuitMessageEvent.class, new RegexQuitMessageParser());
			parser.bind(NickMessageEvent.class, new RegexNickMessageParser());
			parser.bind(PingMessageEvent.class, new RegexPingMessageParser());
			parser.bind(ModeMessageEvent.class, new RegexModeMessageParser());
			parser.bind(InviteMessageEvent.class, new RegexInviteMessageParser());
			parser.bind(ServerMessageEvent.class, new RegexServerMessageParser());
		}

		public Configuration nickname(String nickname) {
			if(nickname == null)
				throw new NullPointerException("nickname is null");
			this.nickname = nickname;
			return this;
		}

		public Configuration username(String username) {
			if(username != null && username.isEmpty())
				username = null;
			this.username = username;
			return this;
		}

		public Configuration realname(String realname) {
			if(realname != null && realname.isEmpty())
				realname = null;
			this.realname = realname;
			return this;
		}

		public Configuration host(String host) {
			if(host == null)
				throw new NullPointerException("host is null");
			this.host = host;
			return this;
		}

		public Configuration port(int port) {
			if(port < 0 || port > 65535)
				throw new IllegalArgumentException("port out of range");
			this.port = port;
			return this;
		}

		public Configuration connectionFactory(ConnectionFactory connectionFactory) {
			if(connectionFactory == null)
				throw new NullPointerException("connectionFactory is null");
			this.connectionFactory = connectionFactory;
			return this;
		}

		public Configuration authenticator(Authenticator authenticator) {
			this.authenticator = authenticator;
			return this;
		}

		public Configuration charset(Charset charset) {
			if(charset == null)
				throw new NullPointerException("charset is null");
			this.charset = charset;
			return this;
		}

		public Configuration lineEnding(String lineEnding) {
			if(lineEnding == null)
				throw new NullPointerException("lineEnding is null");
			if(lineEnding.isEmpty())
				throw new IllegalArgumentException("lineEnding is empty");
			this.lineEnding = lineEnding;
			return this;
		}

		public Configuration eventBus(EventBus eventBus) {
			if(eventBus == null)
				throw new NullPointerException("eventBus is null");
			this.eventBus = eventBus;
			return this;
		}

		public Configuration parser(ParserManager parser) {
			if(parser == null)
				throw new NullPointerException("parser is null");
			this.parser = parser;
			return this;
		}

		public String getNickname() {
			return nickname;
		}

		public String getUsername() {
			return username;
		}

		public String getRealname() {
			return realname;
		}

		public String getHost() {
			return host;
		}

		public int getPort() {
			return port;
		}

		public ConnectionFactory getConnectionFactory() {
			return connectionFactory;
		}

		public Authenticator getAuthenticator() {
			return authenticator;
		}

		public Charset getCharset() {
			return charset;
		}

		public String getLineEnding() {
			return lineEnding;
		}

		public EventBus getEventBus() {
			return eventBus;
		}

		public ParserManager getParser() {
			return parser;
		}

		public Bot create() {
			if(nickname == null || nickname.isEmpty())
				throw new IllegalStateException("missing nickname");
			if(host == null || host.isEmpty())
				throw new IllegalStateException("missing host");
			return new Bot(this);
		}
	}

	private final class ProcessThread implements Runnable {
		private final Queue<String> queue = new ArrayDeque<>();
		private final AtomicBoolean shutdown = new AtomicBoolean(false);

		@Override
		public void run() {
			while(!shutdown.get() && this == processThread) {
				String line = null;
				synchronized(queue) {
					while(!shutdown.get() && queue.isEmpty()) {
						try {
							queue.wait(500);
						} catch(InterruptedException exception) {}
					}
					if(!shutdown.get() && !queue.isEmpty())
						line = queue.poll();
				}
				if(line == null)
					continue;
				try {
					parseLine(line);
				} catch(Exception exception) {
					if(this == processThread)
						handleIOError(exception);
				}
			}
		}

		public void process(String line) {
			if(shutdown.get() || this != processThread)
				return;
			synchronized(queue) {
				queue.offer(line);
				queue.notifyAll();
			}
		}

		public void clear() {
			synchronized(queue) {
				queue.clear();
				queue.notifyAll();
			}
		}

		public void stop() {
			shutdown.set(true);
			clear();
		}
	}

	private final class ReaderThread implements Runnable {
		private final Connection connection;

		public ReaderThread(Connection connection) {
			this.connection = connection;
		}

		@Override
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset));
				while(!connection.isClosed()) {
					String line = reader.readLine();
					if(line == null)
						throw new IOException("End of stream");
					if(this == readerThread)
						processThread.process(line);
				}
			} catch(Exception exception) {
				if(this == readerThread)
					handleIOError(exception);
			}
		}
	}
}
