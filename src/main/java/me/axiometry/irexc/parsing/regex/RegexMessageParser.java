package me.axiometry.irexc.parsing.regex;

import java.util.regex.*;

import me.axiometry.irexc.Bot;
import me.axiometry.irexc.event.message.MessageEvent;
import me.axiometry.irexc.parsing.*;

public abstract class RegexMessageParser<T extends MessageEvent> implements MessageParser<T> {
	// @formatter:off
	private static final String PATTERN_ADDR_PART = "[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]";
	private static final String PATTERN_HOST_PART = "[a-zA-Z0-9_]|[a-zA-Z0-9_][a-zA-Z0-9_\\-\\/]*[a-zA-Z0-9_]";
	public static final Pattern ADDRESS_REGEX = Pattern.compile("(((" + PATTERN_ADDR_PART + ")\\.){3}(" + PATTERN_ADDR_PART + "))");
	public static final Pattern HOSTNAME_REGEX = Pattern.compile("(((" + PATTERN_HOST_PART + ")\\.)*(" + PATTERN_HOST_PART + "))");
	public static final Pattern ADDRESS_HOSTNAME_REGEX = Pattern.compile("(" + ADDRESS_REGEX + "|" + HOSTNAME_REGEX + ")");

	public static final Pattern NICKNAME_REGEX = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_\\-\\[\\]\\\\\\`\\^\\{\\}]*)");
	public static final Pattern USERNAME_REGEX = Pattern.compile("([^ \0\r\n\\@]+)");

	public static final Pattern CHANNEL_REGEX = Pattern.compile("((\\#|\\&)[^ \0\7\r\n\\,]+)");
	public static final Pattern MASK_REGEX = Pattern.compile("((\\#|\\$)[^ \0\7\r\n\\,]+)");

	public static final Pattern TARGET_REGEX = Pattern.compile("(" + CHANNEL_REGEX + "|" + MASK_REGEX + "|" + NICKNAME_REGEX + "|" + USERNAME_REGEX + "\\@" + ADDRESS_HOSTNAME_REGEX + ")");
	public static final Pattern FULL_USER_REGEX = Pattern.compile(NICKNAME_REGEX + "(\\!" + USERNAME_REGEX + ")?(\\@" + ADDRESS_HOSTNAME_REGEX + ")?");
	public static final Pattern SENDER_REGEX = Pattern.compile("(" + ADDRESS_HOSTNAME_REGEX + "|" + FULL_USER_REGEX + ")");
	public static final Pattern MESSAGE_REGEX = Pattern.compile("(\\:" + SENDER_REGEX  + "[ ]+)?([a-zA-Z]+|[0-9]{3})([ ]+(([^ \\:\0\r\n][^ \0\r\n]*)([ ]+([^ \\:\0\r\n][^ \0\r\n]*))*))?([ ]+(\\:([^\0\r\n]*)))?");

	public static final int SENDER_ID                  = 2;
	public static final int SENDER_ADDRESS_HOSTNAME_ID = 3;
	public static final int SENDER_NICKNAME_ID         = 12;
	public static final int SENDER_USERNAME_ID         = 14;
	public static final int SENDER_HOSTNAME_ID         = 16;
	public static final int COMMAND_ID                 = 25;
	public static final int MIDDLE_INFO_ID             = 27;
	public static final int TRAILING_INFO_ID           = 33;
	// @formatter:on

	public static RegexData parse(String line) throws ParseException {
		final Matcher matcher;
		try {
			matcher = MESSAGE_REGEX.matcher(line);
		} catch(Exception exception) {
			throw new ParseException("Exception attempting to match line", exception);
		}
		if(!matcher.matches())
			throw new ParseException("Line does not match");

		return new RegexData() {
			@Override
			public String message() {
				return matcher.group();
			}

			@Override
			public String value(int id) {
				if(id < 1 || id > matcher.groupCount())
					return null;
				return matcher.group(id);
			}
		};
	}

	@Override
	public final T parse(Bot bot, String line) throws ParseException {
		RegexData data = parse(line);

		return parse(bot, data);
	}

	public abstract T parse(Bot bot, RegexData data) throws ParseException;

	protected final String[] params(RegexData data) {
		String middle = data.value(MIDDLE_INFO_ID);
		String trailing = data.value(TRAILING_INFO_ID);
		if(middle != null) {
			String[] middleParts = middle.split(" ");
			String[] params = new String[middleParts.length + (trailing != null ? 1 : 0)];
			for(int i = 0; i < middleParts.length; i++)
				params[i] = middleParts[i];
			if(trailing != null)
				params[params.length - 1] = trailing;
			return params;
		} else if(trailing != null)
			return new String[] { trailing };
		return new String[0];
	}

	public interface RegexData {
		public String message();
		public String value(int id);
	}
}