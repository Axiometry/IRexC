package me.axiometry.irexc.command;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import me.axiometry.irexc.Bot;
import me.axiometry.irexc.event.*;
import me.axiometry.irexc.event.message.UserMessageEvent;
import me.axiometry.irexc.parsing.*;

public class CommandBot extends Bot {
	public enum CollisionHandling {
		ERROR, SKIP, OVERWRITE
	}

	private abstract class CommandContainer {
		private final String name, description;

		public CommandContainer(String name, String description) {
			this.name = name;
			this.description = description;
		}

		public abstract void execute(CommandContext context) throws CommandException;
	}

	private final class DirectCommandContainer extends CommandContainer {
		private final Command command;

		public DirectCommandContainer(String name, String description, Command command) {
			super(name, description);

			this.command = command;
		}

		@Override
		public void execute(CommandContext context) throws CommandException {
			try {
				command.execute(context);
			} catch(Throwable throwable) {
				throw new CommandException(throwable);
			}
		}
	}

	private final class ReflectedCommandContainer extends CommandContainer {
		private final CommandHandler handler;
		private final Method method;

		public ReflectedCommandContainer(String name, String description, CommandHandler handler, Method method) {
			super(name, description);

			this.handler = handler;
			this.method = method;
		}

		@Override
		public void execute(CommandContext context) throws CommandException {
			try {
				method.invoke(handler, context);
			} catch(InvocationTargetException exception) {
				throw new CommandException(exception.getCause());
			} catch(Throwable throwable) {
				throw new CommandException(throwable);
			}
		}
	}

	private final ConcurrentHashMap<String, CommandContainer> commands = new ConcurrentHashMap<>();

	private final String commandPrefix;

	protected <T extends GenericConfiguration<T, S>, S extends CommandBot> CommandBot(GenericConfiguration<T, S> config) {
		super(config);

		commandPrefix = config.commandPrefix;

		getEventBus().register(new EventListener() {
			@EventHandler
			public final void onUserMessage(UserMessageEvent event) {
				String message = event.getMessage();
				if(!message.startsWith(commandPrefix))
					return;

				String[] parts = message.substring(commandPrefix.length()).split(" ");
				String command = parts[0].toLowerCase();
				String[] arguments = Arrays.copyOfRange(parts, 1, parts.length);
				CommandContext context = new CommandContext(command, event.getSource(), event.getTarget(), arguments);
				try {
					if(!execute(context))
						determineReturnTarget(context).sendMessage("Unknown command.");
				} catch(CommandException exception) {
					handleException(context, exception);
				}
			}
		});
	}

	protected void handleException(CommandContext context, CommandException exception) {
		String message = "Error occurred executing '" + context.command() + "': " + exception;
		
		Throwable cause = exception.getCause();
		if(cause != null && cause instanceof ArgumentException) {
			message = "Wrong usage: " + ((ArgumentException) cause).getMessage();
			if(cause.getCause() != null)
				message += " / Error: " + cause.getCause();
		}
		
		determineReturnTarget(context).sendMessage(message);
	}
	
	public MessageTarget determineReturnTarget(CommandContext context) {
		return super.determineReturnTarget(context.source(), context.target());
	}

	public boolean execute(String command, String... arguments) throws CommandException {
		return execute(new CommandContext(command, arguments));
	}

	public boolean execute(CommandContext context) throws CommandException {
		CommandContainer container = commands.get(context.command().toLowerCase());
		if(container != null)
			container.execute(context);
		return container != null;
	}

	public void registerCommand(String name, String description, Command command) throws CommandRegistrationException {
		registerCommand(name, description, command, CollisionHandling.ERROR);
	}
	public void registerCommand(String name, String description, Command command, CollisionHandling handling) throws CommandRegistrationException {
		registerCommand(new DirectCommandContainer(name, description, command), handling);
	}

	public void registerCommands(CommandHandler handler) throws CommandRegistrationException {
		registerCommands(handler, CollisionHandling.ERROR);
	}
	
	public void registerCommands(CommandHandler handler, CollisionHandling handling) throws CommandRegistrationException {
		Class<?> c = handler.getClass();

		for(Method method : c.getMethods()) {
			CommandHandler.Command command = method.getAnnotation(CommandHandler.Command.class);
			if(command == null)
				continue;

			Class<?>[] parameterTypes = method.getParameterTypes();
			if(parameterTypes.length != 1 || !parameterTypes[0].equals(CommandContext.class))
				throw new CommandRegistrationException("Incorrect argument types for method '" + method.toString() + "', expected (CommandContext)");
			if(!method.getReturnType().equals(Void.TYPE))
				throw new CommandRegistrationException("Incorrect return type for method '" + method.getReturnType() + "', expected <void>");

			registerCommand(new ReflectedCommandContainer(command.name(), command.description(), handler, method), handling);
		}
	}

	private void registerCommand(CommandContainer container, CollisionHandling handling) throws CommandRegistrationException {
		if(handling == CollisionHandling.OVERWRITE)
			commands.put(container.name.toLowerCase(), container);
		else if(commands.putIfAbsent(container.name.toLowerCase(), container) != null)
			if(handling == CollisionHandling.ERROR)
				throw new CommandRegistrationException("Command name collision for '" + container.name + "'!");
	}

	public void unregisterCommands(CommandHandler handler) {
		Class<?> c = handler.getClass();

		for(Method method : c.getMethods()) {
			CommandHandler.Command command = method.getAnnotation(CommandHandler.Command.class);
			if(command == null)
				continue;

			unregisterCommand(command.name());
		}
	}

	public void unregisterCommand(String name) {
		commands.remove(name.toLowerCase());
	}

	public String[] getCommands() {
		return commands.keySet().toArray(new String[0]);
	}

	public String getCommandDescription(String command) {
		CommandContainer container = commands.get(command.toLowerCase());
		return container != null ? container.description : null;
	}

	public static final class Configuration extends GenericConfiguration<Configuration, CommandBot> {
		public Configuration() {
			super(Configuration.class);
		}

		@Override
		public CommandBot create() {
			return new CommandBot(this);
		}
	}

	protected static abstract class GenericConfiguration<T extends GenericConfiguration<T, S>, S extends CommandBot> extends Bot.GenericConfiguration<T, S> {
		private String commandPrefix = "!";

		protected GenericConfiguration(Class<T> type) {
			super(type);
		}

		public T commandPrefix(String commandPrefix) {
			if(commandPrefix == null)
				throw new NullPointerException();
			this.commandPrefix = commandPrefix;

			return type.cast(this);
		}

		public String getCommandPrefix() {
			return commandPrefix;
		}
	}
}
