package team.leomcm.yucca.dump;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Collection;
import java.util.List;

public class ModelLayerArgumentType implements ArgumentType<String> {

	private static final List<String> EXAMPLES = List.of("all", "minecraft:warden#main");

	public static ModelLayerArgumentType modelLayer() {
		return new ModelLayerArgumentType();
	}

	public static String getModelLayer(CommandContext<?> ctx, String name) {
		return ctx.getArgument(name, String.class);
	}

	@Override
	public String parse(StringReader reader) throws CommandSyntaxException {
		if (reader.canRead() && (reader.peek() == '"' || reader.peek() == '\'')) {
			return reader.readQuotedString();
		}

		int start = reader.getCursor();
		while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
			reader.skip();
		}
		if (reader.getCursor() == start) {
			throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
		}
		return reader.getString().substring(start, reader.getCursor());
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
