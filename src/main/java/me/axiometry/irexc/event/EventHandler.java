package me.axiometry.irexc.event;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventHandler {
	public boolean ignoreCancelled() default true;
	public double priority() default 1;
}