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

	protected <T extends GenericConfiguration<T, S>, S extends Bot> Bot(GenericConfiguration<T, S> config) {
		super(null, config.nickname);
		super.setUsername(config.username);

		if(config.nickname == null || config.nickname.isEmpty())
			throw new IllegalStateException("missing nickname");
		if(config.host == null || config.host.isEmpty())
			throw new IllegalStateException("missing host");
		
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
		message = (ctcp ? "\u0001" : "") + message + (ctcp ? "\u0001" : "");
		return target.sendMessage(message);
	}

	public final boolean sendNotice(MessageTarget target, String message) {
		return sendNotice(target, message, false);
	}

	public final boolean sendNotice(MessageTarget target, String message, boolean ctcp) {
		message = (ctcp ? "\u0001" : "") + message + (ctcp ? "\u0001" : "");
		return target.sendNotice(message);
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
	
	public MessageTarget determineReturnTarget(MessageSource from, MessageTarget to) {
		if(to instanceof Channel)
			return to;
		else if(from instanceof MessageTarget)
			return (MessageTarget) from;
		else if(to.equals(this))
			return new UnknownMessageTarget();
		return to;
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
		return new User(this, nickname);
	}

	@Override
	public Channel getChannel(String name) {
		Channel channel = super.getChannel(name);
		if(channel == null)
			return new Channel(this, name);
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
	public Bot getBot() {
		return this;
	}

	@Override
	public int hashCode() {
		return super.hashCode() + 1;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Bot && super.equals(obj);
	}
	
	public static final class Configuration extends GenericConfiguration<Configuration, Bot> {
		public Configuration() {
			super(Configuration.class);
		}
		
		@Override
		public Bot create() {
			return new Bot(this);
		}
	}

	protected static abstract class GenericConfiguration<T extends GenericConfiguration<T, S>, S extends Bot> {
		protected final Class<T> type;
		
		private String nickname, username, realname;

		private String host;
		private int port = DEFAULT_PORT;
		private ConnectionFactory connectionFactory = DEFAULT_CONNECTION_FACTORY;

		private Authenticator authenticator;
		private Charset charset = DEFAULT_CHARSET;
		private String lineEnding = DEFAULT_LINE_ENDING;

		private EventBus eventBus = new ConcurrentEventBus();
		private ParserManager parser;

		protected GenericConfiguration(Class<T> type) {
			if(!type.equals(getClass()))
				throw new IllegalArgumentException();
			this.type = type;
			
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

		public T nickname(String nickname) {
			if(nickname == null)
				throw new NullPointerException("nickname is null");
			this.nickname = nickname;
			return type.cast(this);
		}

		public T username(String username) {
			if(username != null && username.isEmpty())
				username = null;
			this.username = username;
			return type.cast(this);
		}

		public T realname(String realname) {
			if(realname != null && realname.isEmpty())
				realname = null;
			this.realname = realname;
			return type.cast(this);
		}

		public T host(String host) {
			if(host == null)
				throw new NullPointerException("host is null");
			this.host = host;
			return type.cast(this);
		}

		public T port(int port) {
			if(port < 0 || port > 65535)
				throw new IllegalArgumentException("port out of range");
			this.port = port;
			return type.cast(this);
		}

		public T connectionFactory(ConnectionFactory connectionFactory) {
			if(connectionFactory == null)
				throw new NullPointerException("connectionFactory is null");
			this.connectionFactory = connectionFactory;
			return type.cast(this);
		}

		public T authenticator(Authenticator authenticator) {
			this.authenticator = authenticator;
			return type.cast(this);
		}

		public T charset(Charset charset) {
			if(charset == null)
				throw new NullPointerException("charset is null");
			this.charset = charset;
			return type.cast(this);
		}

		public T lineEnding(String lineEnding) {
			if(lineEnding == null)
				throw new NullPointerException("lineEnding is null");
			if(lineEnding.isEmpty())
				throw new IllegalArgumentException("lineEnding is empty");
			this.lineEnding = lineEnding;
			return type.cast(this);
		}

		public T eventBus(EventBus eventBus) {
			if(eventBus == null)
				throw new NullPointerException("eventBus is null");
			this.eventBus = eventBus;
			return type.cast(this);
		}

		public T parser(ParserManager parser) {
			if(parser == null)
				throw new NullPointerException("parser is null");
			this.parser = parser;
			return type.cast(this);
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

		public abstract S create();
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
