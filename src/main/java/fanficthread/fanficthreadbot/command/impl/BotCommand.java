package fanficthread.fanficthreadbot.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fanficthread.fanficthreadbot.BotSettings;
import fanficthread.fanficthreadbot.FanficThreadBot;
import fanficthread.fanficthreadbot.command.CommandSource;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.managers.GuildController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static fanficthread.fanficthreadbot.command.Commands.argument;
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
                .then(literal("kicknonusers")
                        .executes(BotCommand::executeKickNonUsers)
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

    private static int executeKickNonUsers(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final Guild guild = bot.getGuild();
        final GuildController controller = guild.getController();
        final TextChannel commandChannel = source.getChannel();
        final long memberRoleId = settings.getMemberRole();

        List<Member> members = guild.getMembers();
        List<Long> kicked = new ArrayList<>();
        for (Member member : members)
        {
            if (!member.getUser().isBot() && member.getRoles().stream().noneMatch(r -> r.getIdLong() == memberRoleId)
            )
            {
                kicked.add(member.getUser().getIdLong());
                controller.kick(member, "Non-user kick").queue();
            }
        }

        if (!kicked.isEmpty())
        {
            LOGGER.info("Kicked non-user members: {}", kicked);
        }
        commandChannel.sendMessage("Было кикнуто " + kicked.size() + " не-участников.").queue();

        return 44741151;
    }
}
