package me.axiometry.irexc.command;

import java.lang.annotation.*;

public interface CommandHandler {
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Command {
		public String name();
		public String description();
	}
}
