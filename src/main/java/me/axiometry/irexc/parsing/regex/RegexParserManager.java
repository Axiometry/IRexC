package me.axiometry.irexc.parsing.regex;

import me.axiometry.irexc.Bot;
import me.axiometry.irexc.event.message.MessageEvent;
import me.axiometry.irexc.parsing.*;

public final class RegexParserManager extends SimpleParserManager {
	private static final class RegexMessageParserTransformer implements MessageParserTransformer {
		private final RegexMessageParser.RegexData data;
		private final Bot bot;
		private final String line;

		private RegexMessageParserTransformer(RegexMessageParser.RegexData data, Bot bot, String line) {
			this.data = data;
			this.bot = bot;
			this.line = line;
		}

		public static RegexMessageParserTransformer wrap(Bot bot, String line) throws ParseException {
			RegexMessageParser.RegexData data = RegexMessageParser.parse(line);

			return new RegexMessageParserTransformer(data, bot, line);
		}

		@Override
		public <T extends MessageEvent> T parse(MessageParser<T> parser, Bot bot, String line) throws ParseException {
			if(!bot.equals(this.bot) || !line.equals(this.line))
				throw new ParseException("RegexMessageParserTransformer applied to wrong data");

			if(parser instanceof RegexMessageParser)
				return ((RegexMessageParser<T>) parser).parse(bot, data);
			return parser.parse(bot, line);
		}
	}

	@Override
	public MessageEvent parse(final Bot bot, final String line) throws ParseException {
		return super.parse(bot, line, RegexMessageParserTransformer.wrap(bot, line));
	}
}
