package fanficthread.fanficthreadbot.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fanficthread.fanficthreadbot.FanficThreadBot;
import fanficthread.fanficthreadbot.command.CommandSource;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fanficthread.fanficthreadbot.command.Commands.literal;

public final class BotCommand
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BotCommand.class);

    private BotCommand()
    {
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(literal("bot")
                .requires(s -> s.getMember().hasPermission(Permission.ADMINISTRATOR))
                .then(literal("shutdown")
                        .executes(BotCommand::executeShutdown)
                )
                .then(literal("save")
                        .executes(BotCommand::executeSave)
                )
        );
    }

    private static int executeSave(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final TextChannel commandChannel = source.getChannel();

        commandChannel.sendMessage("Состояние и настройки бота сохранены.").queue();
        LOGGER.info("Bot manual save issued");
        bot.forceSave();

        return 45777241;
    }

    private static int executeShutdown(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final TextChannel commandChannel = source.getChannel();

        commandChannel.sendMessage("Бот будет выключен через несколько секунд.").queue();
        LOGGER.info("Bot manual shutdown issued");
        bot.shutdown(1);

        return 4414222;
    }
}
