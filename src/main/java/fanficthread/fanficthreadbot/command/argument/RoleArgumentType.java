package fanficthread.fanficthreadbot.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

public class RoleArgumentType implements ArgumentType<Long>
{
    public static final SimpleCommandExceptionType ROLE_FORMAT = new SimpleCommandExceptionType(() -> "Role argument does not match format '<@&snowflake>'");

    protected RoleArgumentType()
    {
    }

    public static RoleArgumentType role()
    {
        return new RoleArgumentType();
    }

    @Override
    public Long parse(StringReader reader) throws CommandSyntaxException
    {
        final int start = reader.getCursor();
        if (reader.read() != '<' || reader.read() != '@' || reader.read() != '&')
        {
            reader.setCursor(start);
            throw ROLE_FORMAT.createWithContext(reader);
        }
        long l = reader.readLong();
        if (reader.read() != '>')
        {
            reader.setCursor(start);
            throw ROLE_FORMAT.createWithContext(reader);
        }
        return l;
    }
}