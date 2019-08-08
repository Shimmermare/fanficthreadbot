package fanficthread.fanficthreadbot.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

public class UserArgumentType implements ArgumentType<Long>
{
    public static final SimpleCommandExceptionType USER_FORMAT = new SimpleCommandExceptionType(() -> "User argument does not match format '<@snowflake>'");

    protected UserArgumentType()
    {
    }

    public static UserArgumentType user()
    {
        return new UserArgumentType();
    }

    @Override
    public Long parse(StringReader reader) throws CommandSyntaxException
    {
        final int start = reader.getCursor();
        if (reader.read() != '<' || reader.read() != '@')
        {
            reader.setCursor(start);
            throw USER_FORMAT.createWithContext(reader);
        }
        if (reader.peek() == '!') reader.read();

        long l = reader.readLong();
        if (reader.read() != '>')
        {
            reader.setCursor(start);
            throw USER_FORMAT.createWithContext(reader);
        }
        return l;
    }
}
