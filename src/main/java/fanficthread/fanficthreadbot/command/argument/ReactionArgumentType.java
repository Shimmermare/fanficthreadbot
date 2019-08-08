package fanficthread.fanficthreadbot.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

/**
 * Reaction != emoji! Reactions are :name: format only.
 */
public class ReactionArgumentType implements ArgumentType<Long>
{
    public static final SimpleCommandExceptionType REACTION_FORMAT = new SimpleCommandExceptionType(() -> "Reaction argument does not match format ':name:'");

    protected ReactionArgumentType()
    {
    }

    public static ReactionArgumentType reaction()
    {
        return new ReactionArgumentType();
    }

    @Override
    public Long parse(StringReader reader) throws CommandSyntaxException
    {
        final int start = reader.getCursor();

        if (reader.read() != '<' || reader.read() != ':')
        {
            reader.setCursor(start);
            throw REACTION_FORMAT.createWithContext(reader);
        }
        reader.readStringUntil(':');
        long l = reader.readLong();
        if (reader.read() != '>')
        {
            reader.setCursor(start);
            throw REACTION_FORMAT.createWithContext(reader);
        }
        return l;
    }
}