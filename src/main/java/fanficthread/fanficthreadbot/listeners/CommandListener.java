package fanficthread.fanficthreadbot.listeners;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fanficthread.fanficthreadbot.FanficThreadBot;
import fanficthread.fanficthreadbot.Util;
import fanficthread.fanficthreadbot.command.CommandSource;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandListener extends AbstractListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandListener.class);

    public CommandListener(FanficThreadBot bot)
    {
        super(bot);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event)
    {
        final long guildId = event.getGuild().getIdLong();
        final long channelId = event.getChannel().getIdLong();
        final long messageId = event.getMessageIdLong();
        final TextChannel commandChannel = event.getChannel();

        if (!bot.isLockedGuild(guildId))
        {
            LOGGER.debug("GuildMessageReceivedEvent({}/{}) from unknown guild ({}), skipped", channelId, messageId, guildId);
            return;
        }
        final User user = event.getAuthor();
        if (user == null)
        {
            LOGGER.debug("GuildMessageReceivedEvent({}/{}) from non-user (webhook?), skipped", channelId, messageId);
            return;
        }
        final long userId = user.getIdLong();
        final Member member = event.getMember();
        if (member == null || user.isBot())
        {
            LOGGER.debug("GuildMessageReceivedEvent({}/{}) from non-member or bot (user:{}), skipped", channelId, messageId, user.getIdLong());
            return;
        }

        final Message message = event.getMessage();
        String content = message.getContentRaw();
        String[] lines = content.split("\n");
        final String commandPrefix = Util.getUserMention(bot.getBotUserId()) + " ";

        for (String line : lines)
        {
            executeCommand(line, commandPrefix, channelId, messageId, commandChannel, member, userId);
        }
    }

    private void executeCommand(String content, String commandPrefix, long channelId, long messageId, TextChannel commandChannel, Member member, long userId)
    {
        if (!content.startsWith(commandPrefix))
        {
            LOGGER.debug("Message({}/{}) does not start with command prefix, skipped", channelId, messageId);
            return;
        }

        content = content.substring(commandPrefix.length());
        if (content.isEmpty())
        {
            LOGGER.debug("Message({}/{}) consists only of command prefix, skipped", channelId, messageId);
            return;
        }

        CommandSource commandSource = new CommandSource(bot, commandChannel, member);
        CommandDispatcher<CommandSource> commandDispatcher = bot.getCommandDispatcher();
        ParseResults<CommandSource> parseResults = commandDispatcher.parse(content, commandSource);
        try
        {
            commandDispatcher.execute(parseResults);
            LOGGER.debug("Command(member:{}, message:{}/{}, command:'{}') successfully executed", userId, channelId, messageId, content);
        } catch (CommandSyntaxException e)
        {
            if (e.getCursor() == 0)
            {
                commandChannel.sendMessage("Неизвестная команда. **help** для просмотра списка команд.").queue();
                LOGGER.debug("Command(member:{}, message:{}/{}, command:'{}') does not exists", userId, channelId, messageId, content);
            } else
            {
                LOGGER.error("Command(member:{}, message:{}/{}, command:'{}') failed to parse", userId, channelId, messageId, content, e);
                commandChannel.sendMessage("Ошибка синтаксиса команды: " + e.getMessage()).queue();
            }
        } catch (RuntimeException e)
        {
            LOGGER.error("Command(member:{}, message:{}/{}, command:'{}') failed to execute", userId, channelId, messageId, content, e);
            commandChannel.sendMessage("Непредвиденная ошибка про выполнении команды. Попробуйте ещё раз.").queue();
        }
    }
}
