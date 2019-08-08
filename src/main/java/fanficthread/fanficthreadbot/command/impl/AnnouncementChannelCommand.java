package fanficthread.fanficthreadbot.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fanficthread.fanficthreadbot.AnnouncementChannel;
import fanficthread.fanficthreadbot.BotSettings;
import fanficthread.fanficthreadbot.FanficThreadBot;
import fanficthread.fanficthreadbot.command.CommandSource;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static fanficthread.fanficthreadbot.Util.getChannelMention;
import static fanficthread.fanficthreadbot.command.Commands.argument;
import static fanficthread.fanficthreadbot.command.Commands.literal;
import static fanficthread.fanficthreadbot.command.argument.ChannelArgumentType.channel;

/**
 * Announcement channels management command.
 * <p>
 * achannel                                 -> show list of announcement channels
 * achannel #channel                        -> status info
 * achannel #channel enable                 -> make #channel announcement only
 * achannel #channel repost #repost-channel -> set repost channel for announcement channel
 * achannel #channel disable                -> make #channel a normal text channel
 */
public final class AnnouncementChannelCommand
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnouncementChannelCommand.class);

    private static final BiConsumer<TextChannel, Long> SEND_NOT_ANNOUNCEMENT_CHANNEL =
            (channel, id) -> channel.sendMessage("Канал " + getChannelMention(id) + " не является каналом для объявлений.").queue();
    private static final Consumer<Long> LOG_NOT_ANNOUNCEMENT_CHANNEL =
            (id) -> LOGGER.debug("Channel {} is not an announcement channel", id);

    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(literal("achannel")
                .then(argument("channel", channel())
                        .requires(s -> s.getMember().hasPermission(Permission.ADMINISTRATOR))
                        .then(literal("enable")
                                .executes(AnnouncementChannelCommand::enable)
                        )
                        .then(literal("disable")
                                .executes(AnnouncementChannelCommand::disable)
                        )
                        .then(literal("repost")
                                .then(literal("disable")
                                        .executes(AnnouncementChannelCommand::repostDisable)
                                )
                                .then(argument("repost-channel", channel())
                                        .executes(AnnouncementChannelCommand::repost)
                                )
                        )
                        .executes(AnnouncementChannelCommand::sendStatus)
                )
                .executes(AnnouncementChannelCommand::sendList)
        );
    }

    private static int sendList(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final TextChannel channel = source.getChannel();

        Collection<AnnouncementChannel> announcementChannels = bot.getSettings().getAnnouncementChannels();
        if (announcementChannels.isEmpty())
        {
            channel.sendMessage("На сервере не задано каналов для объявлений.").queue();
            LOGGER.debug("Send message that there is no announcement channels");
            return 894641188;
        }

        StringBuilder builder = new StringBuilder("Каналы для объявлений:");
        for (AnnouncementChannel ac : announcementChannels)
        {
            builder.append("\n• **").append(getChannelMention(ac.getChannelId())).append("**");
            long repostChannelId = ac.getRepostChannelId();
            if (repostChannelId != 0)
            {
                builder.append(" (репост в **").append(getChannelMention(repostChannelId)).append("**)");
            }
        }
        channel.sendMessage(builder.toString()).queue();
        LOGGER.debug("Send list of announcement channels");
        return 1;
    }

    private static int sendStatus(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long channel = context.getArgument("channel", Long.class);

        AnnouncementChannel announcementChannel = settings.getAnnouncementChannel(channel);
        if (announcementChannel == null)
        {
            SEND_NOT_ANNOUNCEMENT_CHANNEL.accept(commandChannel, channel);
            LOG_NOT_ANNOUNCEMENT_CHANNEL.accept(channel);
            return 115151151;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Канал **").append(getChannelMention(channel)).append("** является каналом для объявлений. Канал для репостов ");
        long repostChannelId = announcementChannel.getRepostChannelId();
        builder.append((repostChannelId == 0) ? "не подключен" : getChannelMention(repostChannelId));
        builder.append('.');
        commandChannel.sendMessage(builder.toString()).queue();
        LOGGER.debug("Send announcement channel {} status", channel);
        return 841115151;
    }

    private static int enable(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long channel = context.getArgument("channel", Long.class);

        if (settings.isAnnouncementChannel(channel))
        {
            commandChannel.sendMessage("Канал **" + getChannelMention(channel) + "** уже является каналом для объявлений.").queue();
            LOGGER.debug("Channel {} is already an announcement channel", channel);
            return 4425484;
        }

        settings.addAnnouncementChannel(new AnnouncementChannel(channel));
        commandChannel.sendMessage("Канал **" + getChannelMention(channel) + "** теперь является каналом для объявлений.").queue();
        LOGGER.debug("Channel {} is now an announcement channel", channel);

        return 463827277;
    }

    private static int disable(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long channel = context.getArgument("channel", Long.class);

        AnnouncementChannel announcementChannel = settings.getAnnouncementChannel(channel);
        if (announcementChannel == null)
        {
            commandChannel.sendMessage("Канал **" + getChannelMention(channel) + "** не является каналом для объявлений.").queue();
            LOGGER.debug("Channel {} is already not an announcement channel", channel);
            return 17484551;
        }

        settings.removeAnnouncementChannel(channel);
        commandChannel.sendMessage("Канал **" + getChannelMention(channel) + "** больше не является каналом для объявлений.").queue();
        LOGGER.debug("Channel {} is now not an announcement channel", channel);

        //Cleanup repost webhooks if existed
        long repostChannelId = announcementChannel.getRepostChannelId();
        if (repostChannelId == 0) return 44848118;

        Guild guild = commandChannel.getGuild();
        TextChannel repostChannel = guild.getTextChannelById(repostChannelId);
        if (repostChannel == null)
        {
            LOGGER.error("Channel {} is used as repost channel for announcement channel {} but doesn't exists", repostChannelId, channel);
            return 1715100417;
        }

        for (Webhook webhook : repostChannel.getWebhooks().complete())
        {
            if (webhook.getName().equals(AnnouncementChannel.REPOST_WEBHOOK_NAME))
            {
                bot.getWebhookClientCache().closeClient(webhook.getIdLong());
                webhook.delete().queue();
            }
        }
        return 415842214;
    }

    private static int repost(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long channel = context.getArgument("channel", Long.class);
        final long repostChannel = context.getArgument("repost-channel", Long.class);

        AnnouncementChannel announcementChannel = settings.getAnnouncementChannel(channel);
        if (announcementChannel == null)
        {
            commandChannel.sendMessage("Невозможно установить канал для репостов - канал **" + getChannelMention(channel) + "** не является каналом для объявлений.").queue();
            LOGGER.debug("Channel {} is not an announcement channel; can't set repost channel", channel);
            return 657616385;
        }
        announcementChannel.setRepostChannelId(repostChannel);
        commandChannel.sendMessage("Канал **" + getChannelMention(repostChannel) + "** установлен как канал для репостов из **" + getChannelMention(channel) + "**.").queue();
        LOGGER.debug("Channel {} is now the repost channel of announcement channel {}", repostChannel, channel);
        return 414441414;
    }

    private static int repostDisable(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long channel = context.getArgument("channel", Long.class);

        AnnouncementChannel announcementChannel = settings.getAnnouncementChannel(channel);
        if (announcementChannel == null)
        {
            commandChannel.sendMessage("Невозможно отключить канал для репостов - канал " + getChannelMention(channel) + " не является каналом для объявлений.").queue();
            LOGGER.debug("Channel {} is not an announcement channel; can't set repost channel", channel);
            return 657616385;
        }
        announcementChannel.setRepostChannelId(0);
        commandChannel.sendMessage("Канал для репостов из " + getChannelMention(channel) + "отключен.").queue();
        LOGGER.debug("Announcement channel {} now doesn't have a repost channel", channel);

        return 457572727;
    }

    private AnnouncementChannelCommand()
    {
    }
}
