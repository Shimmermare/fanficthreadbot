package fanficthread.fanficthreadbot.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fanficthread.fanficthreadbot.BotSettings;
import fanficthread.fanficthreadbot.FanficThreadBot;
import fanficthread.fanficthreadbot.Util;
import fanficthread.fanficthreadbot.command.CommandSource;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;

import static fanficthread.fanficthreadbot.command.Commands.argument;
import static fanficthread.fanficthreadbot.command.Commands.literal;
import static fanficthread.fanficthreadbot.command.argument.ChannelArgumentType.channel;

public final class SpoilerCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(literal("spoiler")
                .requires(cs -> cs.getMember().hasPermission(Permission.ADMINISTRATOR))
                .then(literal("enable")
                        .executes(SpoilerCommand::executeEnable)
                )
                .then(literal("disable")
                        .executes(SpoilerCommand::executeDisable)
                )
                .then(literal("whitelist")
                        .then(argument("channel-id", channel())
                                .then(literal("add")
                                        .executes(SpoilerCommand::executeWhitelistAdd)
                                )
                                .then(literal("remove")
                                        .executes(SpoilerCommand::executeWhitelistRemove)
                                )
                                .executes(SpoilerCommand::executeWhitelistInfo)
                        )
                )
                .executes(SpoilerCommand::executeInfo)
        );
    }

    private static int executeEnable(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();

        settings.setSpoilerEnabled(true);
        commandChannel.sendMessage("Авто-спойлер включен.").queue();

        return 4278877;
    }


    private static int executeDisable(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();

        settings.setSpoilerEnabled(false);
        commandChannel.sendMessage("Авто-спойлер выключен.").queue();

        return 54877114;
    }

    private static int executeWhitelistAdd(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long channelId = context.getArgument("channel-id", Long.class);

        settings.addToSpoilerWhitelist(channelId);
        commandChannel.sendMessage("Канал " + Util.getChannelMention(channelId) + " добавлен в вайтлист.").queue();

        return 878782721;
    }

    private static int executeWhitelistRemove(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long channelId = context.getArgument("channel-id", Long.class);

        settings.removeFromSpoilerWhitelist(channelId);
        commandChannel.sendMessage("Канал " + Util.getChannelMention(channelId) + " удалён из вайтлиста.").queue();

        return 945175171;
    }

    private static int executeWhitelistInfo(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long channelId = context.getArgument("channel-id", Long.class);

        commandChannel.sendMessage("Канал " + Util.getChannelMention(channelId) + (settings.isInSpoilerWhitelist(channelId) ? "" : " не") + " в вайтлисте.").queue();

        return 55627171;
    }

    private static int executeInfo(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();

        StringBuilder sb = new StringBuilder();
        sb.append("**Авто-спойлер**");
        sb.append("\nВключено: ").append(settings.isSpoilerEnabled() ? "да" : "нет").append(".");
        sb.append("\nИсключения: ");
        for (long channelId : settings.getSpolierWhitelist())
        {
            sb.append(Util.getChannelMention(channelId)).append(" ");
        }
        commandChannel.sendMessage(sb.toString()).queue();

        return 278676711;
    }

    private SpoilerCommand()
    {

    }
}
