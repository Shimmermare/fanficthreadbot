package fanficthread.fanficthreadbot.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

public class ChannelArgumentType implements ArgumentType<Long>
{
    public static final SimpleCommandExceptionType CHANNEL_FORMAT = new SimpleCommandExceptionType(() -> "Channel argument does not match format '<#snowflake>'");

    protected ChannelArgumentType()
    {
    }

    public static ChannelArgumentType channel()
    {
        return new ChannelArgumentType();
    }

    @Override
    public Long parse(StringReader reader) throws CommandSyntaxException
    {
        final int start = reader.getCursor();
        if (reader.read() != '<' || reader.read() != '#')
        {
            reader.setCursor(start);
            throw CHANNEL_FORMAT.createWithContext(reader);
        }
        long l = reader.readLong();
        if (reader.read() != '>')
        {
            reader.setCursor(start);
            throw CHANNEL_FORMAT.createWithContext(reader);
        }
        return l;
    }
}
