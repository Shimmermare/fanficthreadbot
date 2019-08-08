package fanficthread.fanficthreadbot.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fanficthread.fanficthreadbot.*;
import fanficthread.fanficthreadbot.command.CommandSource;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static fanficthread.fanficthreadbot.Util.*;
import static fanficthread.fanficthreadbot.command.Commands.argument;
import static fanficthread.fanficthreadbot.command.Commands.literal;
import static fanficthread.fanficthreadbot.command.argument.ChannelArgumentType.channel;
import static fanficthread.fanficthreadbot.command.argument.ReactionArgumentType.reaction;
import static fanficthread.fanficthreadbot.command.argument.RoleArgumentType.role;
import static fanficthread.fanficthreadbot.command.argument.UserArgumentType.user;

/**
 * Member Voting Command
 * <p>
 * Body syntax:
 * vote                         - display current status
 * vote enable                  - enable MemberVote
 * vote disable                 - disable MemberVote
 * vote channel #vote-channel   - set vote channel
 * vote reactions :reaction-upvote: :reaction-downvote: - set vote reactions
 * vote requirement votes-required - set amount of votes required
 * vote role @member-role       - set member role
 * vote additional - list additional roles
 * vote additional clear - clear additional roles
 * vote additional @additional-role - display current status
 * vote additional @additional-role add - make role additional
 * vote additional @additional-role delete - make role not-additional
 * <p>
 * Permission level: Admin only
 */
public final class MemberVoteCommand
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MemberVoteCommand.class);

    private MemberVoteCommand()
    {
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(literal("vote")
                .requires(source -> source.getMember().hasPermission(Permission.ADMINISTRATOR))
                .then(literal("enable")
                        .executes(MemberVoteCommand::executeEnable)
                )
                .then(literal("disable")
                        .executes(MemberVoteCommand::executeDisable)
                )
                .then(literal("channel")
                        .then(argument("vote-channel", channel())
                                .executes(MemberVoteCommand::executeChannel)
                        )
                )
                .then(literal("reactions")
                        .then(argument("reaction-upvote", reaction())
                                .then(argument("reaction-downvote", reaction())
                                        .executes(MemberVoteCommand::executeReactions)
                                )
                        )
                )
                .then(literal("requirement")
                        .then(argument("votes-required", integer(1, 100))
                                .executes(MemberVoteCommand::executeRequirement)
                        )
                )
                .then(literal("timeout")
                        .then(argument("poll-timeout", integer(1, Integer.MAX_VALUE))
                                .executes(MemberVoteCommand::executeTimeout)
                        )
                )
                .then(literal("role")
                        .then(argument("member-role", role())
                                .executes(MemberVoteCommand::executeRole)
                        )
                )
                .then(literal("additional")
                        .then(literal("clear")
                                .executes(MemberVoteCommand::executeAdditionalClear)
                        )
                        .then(argument("additional-role", role())
                                .then(literal("add")
                                        .executes(MemberVoteCommand::executeAdditionalAdd)
                                )
                                .then(literal("delete")
                                        .executes(MemberVoteCommand::executeAdditionalDelete)
                                )
                                .executes(MemberVoteCommand::executeAdditionalStatus)
                        )
                        .executes(MemberVoteCommand::executeAdditionalList)
                )
                .then(literal("open")
                        .then(literal("all")
                                .executes(MemberVoteCommand::executeOpenAll)
                        )
                        .then(argument("user", user())
                                .executes(MemberVoteCommand::executeOpen)
                        )
                )
                .then(literal("cleanup")
                        .executes(MemberVoteCommand::executeCleanup)
                )
                .executes(MemberVoteCommand::executeSendStatus)
        );
    }

    private static int executeCleanup(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final BotState state = bot.getState();
        final Guild guild = bot.getGuild();
        final TextChannel commandChannel = source.getChannel();

        final TextChannel pollChannel = guild.getTextChannelById(settings.getMemberVoteChannel());

        pollChannel.getHistoryBefore(pollChannel.getLatestMessageIdLong(), 100).complete().getRetrievedHistory().forEach(message ->
                {
                    if (state.getMemberPollByMessage(message.getIdLong()) == null)
                    {
                        message.delete().queue();
                    }
                }
        );

        commandChannel.sendMessage("Канал голосований очищен от лишних сообщений.").queue();
        LOGGER.debug("Member vote channel {} purged from non-poll messages", settings.getMemberVoteChannel());
        return 45678727;
    }

    private static int executeOpenAll(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final Guild guild = bot.getGuild();
        final TextChannel commandChannel = source.getChannel();

        final long memberRoleId = settings.getMemberRole();
        final Role memberRole = guild.getRoleById(memberRoleId);
        if (memberRole == null)
        {
            commandChannel.sendMessage("Роли участника " + getRoleMention(memberRoleId) + " не существует.").queue();
            LOGGER.debug("Member role {} doesn't exist", memberRoleId);
        }

        List<Long> users = guild.getMembers().stream()
                .filter(member -> !member.getUser().isBot())
                .filter(member -> !member.getRoles().contains(memberRole))
                .map(member -> member.getUser().getIdLong())
                .collect(Collectors.toList());

        if (users.isEmpty())
        {
            commandChannel.sendMessage("На сервере нет пользователей без роли " + getRoleMention(memberRoleId) + ".").queue();
            LOGGER.debug("Guild has no users without member role {}", memberRoleId);
            return 74892161;
        }

        for (long user : users)
            MemberPoll.create(bot, user, -1);

        StringBuilder builder = new StringBuilder("Голосование для пользователей ");
        List<String> userMentions = users.stream()
                .map(Util::getUserNicknameMention)
                .collect(Collectors.toList());
        builder.append(String.join(", ", userMentions));
        builder.append(" открыто.");

        commandChannel.sendMessage(builder.toString()).queue();
        LOGGER.debug("Member polls for users {} are open", users);
        return 787827114;
    }

    private static int executeOpen(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final BotState state = bot.getState();
        final TextChannel commandChannel = source.getChannel();
        final long user = context.getArgument("user", Long.class);

        if (!settings.isMemberVoteEnabled())
        {
            commandChannel.sendMessage("MemberVote отключен, невозможно создать голосование.").queue();
            LOGGER.debug("Tried to open poll for user {} but MemberVote is not enabled", user);
            return 5412352;
        }

        if (state.getMemberPollByUser(user) != null)
        {
            commandChannel.sendMessage("Голосование для " + getUserMention(user) + " уже открыто.").queue();
            LOGGER.debug("Tried to open poll for user {} but it's already open", user);
            return 1126848277;
        }

        Member member = bot.getGuild().getMemberById(user);
        if (member == null)
        {
            commandChannel.sendMessage("Пользователь " + getUserMention(user) + " не является участником сервера.").queue();
            LOGGER.debug("Tried to open poll for user {} but user is not a member", user);
            return 65527271;
        }

        List<Role> roles = member.getRoles();
        for (Role role : roles)
        {
            if (role.getIdLong() == settings.getMemberRole())
            {
                commandChannel.sendMessage("Пользователь " + getUserMention(user) + " уже имеет роль участника, голосование не требуется.").queue();
                LOGGER.debug("Tried to open poll for user {} but user already has member role {}", user, settings.getMemberRole());
                return 5267878;
            }
        }

        MemberPoll.create(bot, user, -1);

        commandChannel.sendMessage("Голосование для пользователя " + getUserMention(user) + " открыто.").queue();
        LOGGER.debug("Opened poll for user {}", user);
        return 2141245171;
    }

    private static int executeEnable(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();

        settings.setMemberVoteEnabled(true);
        commandChannel.sendMessage("Система демократичного посвящения участников включена.").queue();
        LOGGER.debug("Member vote is enabled");

        return 32984787;
    }

    private static int executeDisable(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();

        settings.setMemberVoteEnabled(false);
        commandChannel.sendMessage("Система демократичного посвящения участников отключена.").queue();
        LOGGER.debug("Member vote is disabled");

        return 125785714;
    }

    private static int executeChannel(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long channel = context.getArgument("vote-channel", Long.class);

        settings.setMemberVoteChannel(channel);
        commandChannel.sendMessage("Канал **" + getChannelMention(channel) + "** установлен как канал для посвящений участников.").queue();
        LOGGER.debug("Channel {} is set as member voting channel", channel);

        return 45758767;
    }

    private static int executeReactions(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long reactionUpvote = context.getArgument("reaction-upvote", Long.class);
        final long reactionDownvote = context.getArgument("reaction-downvote", Long.class);

        settings.setMemberVoteReactionUpvote(reactionUpvote);
        settings.setMemberVoteReactionDownvote(reactionDownvote);
        commandChannel.sendMessage("Реакции " + getReactionMention(reactionUpvote) + " и " + getReactionMention(reactionDownvote) + " установлены как +1 и -1 в голосовании.").queue();
        LOGGER.debug("Reactions {} and {} now are voting reactions", reactionUpvote, reactionDownvote);

        return 46464867;
    }

    private static int executeRequirement(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final int votesRequired = context.getArgument("votes-required", Integer.class);

        settings.setMemberVotesRequired(votesRequired);
        commandChannel.sendMessage("Для посвящения в голосовании теперь нужно " + votesRequired + " голосов.").queue();
        LOGGER.debug("Member poll now requires {} votes", votesRequired);

        return 45468774;
    }

    private static int executeTimeout(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final int timeout = context.getArgument("poll-timeout", Integer.class);

        settings.setMemberVoteTimeout(timeout);
        commandChannel.sendMessage("Максимальное время на голосование установлено как " + timeout + " секунд.").queue();
        LOGGER.debug("Member poll timeout set to {}", timeout);

        return 142728788;
    }

    private static int executeRole(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long memberRole = context.getArgument("member-role", Long.class);
        settings.setMemberRole(memberRole);
        commandChannel.sendMessage("Роль " + getRoleMention(memberRole) + " теперь является ролью участника.").queue();
        LOGGER.debug("Role {} is now the member role", memberRole);

        return 747141185;
    }

    private static int executeSendStatus(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();

        StringBuilder builder = new StringBuilder("Текущие настройки посвящения голосованием:");

        builder.append("\nСтатус: ").append((settings.isMemberVoteEnabled() ? "Включено" : "Выключено"));
        builder.append("\nКанал посвящений: ").append(getChannelMention(settings.getMemberVoteChannel()));
        builder.append("\nРеакции голосования: +1 = ").append(getReactionMention(settings.getMemberVoteReactionUpvote())).append(" -1 = ").append(getReactionMention(settings.getMemberVoteReactionDownvote()));
        builder.append("\nПорог голосов: ").append(settings.getMemberVotesRequired());
        builder.append("\nОтведённое время: ").append(settings.getMemberVoteTimeout()).append(" секунд");
        builder.append("\nРоль участника: ").append(getRoleMention(settings.getMemberRole()));
        builder.append("\nДополнительные роли: ");

        Set<Long> additionalRoles = settings.getMemberAdditionalRoles();
        if (additionalRoles.isEmpty())
            builder.append("нет");
        else
        {
            List<String> additionalRolesMentions = additionalRoles.stream()
                    .map(Util::getRoleMention)
                    .collect(Collectors.toList());
            builder.append(String.join(", ", additionalRolesMentions));
        }

        commandChannel.sendMessage(builder.toString()).queue();
        LOGGER.debug("Sent member vote status");

        return 554884111;
    }

    private static int executeAdditionalAdd(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long additionalRole = context.getArgument("additional-role", Long.class);

        if (settings.isMemberAdditionalRole(additionalRole))
        {
            commandChannel.sendMessage("Роль " + getRoleMention(additionalRole) + " уже является дополнительной.").queue();
            LOGGER.debug("Role {} is an additional role already, can't add it", additionalRole);
            return 11188114;
        }

        settings.addMemberAdditionalRole(additionalRole);
        commandChannel.sendMessage("Роль " + getRoleMention(additionalRole) + " установлена как дополнительная.").queue();
        LOGGER.debug("Role {} now is an additional role", additionalRole);
        return 211238711;
    }

    private static int executeAdditionalDelete(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long additionalRole = context.getArgument("additional-role", Long.class);

        if (!settings.isMemberAdditionalRole(additionalRole))
        {
            commandChannel.sendMessage("Роль " + getRoleMention(additionalRole) + " итак не является дополнительной.").queue();
            LOGGER.debug("Role {} is not an additional role, can't delete it", additionalRole);
            return 42427272;
        }

        settings.removeMemberAdditionalRole(additionalRole);
        commandChannel.sendMessage("Роль " + getRoleMention(additionalRole) + " больше не является как дополнительной.").queue();
        LOGGER.debug("Role {} now is not an additional role", additionalRole);

        return 214237371;
    }

    private static int executeAdditionalStatus(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long additionalRole = context.getArgument("additional-role", Long.class);

        String msg = "Роль " + getRoleMention(additionalRole) + (settings.isMemberAdditionalRole(additionalRole) ? "" : "не") + " является дополнительной.";
        commandChannel.sendMessage(msg).queue();
        LOGGER.debug("Sent status of a role {} as additional", additionalRole);

        return 786311889;
    }

    private static int executeAdditionalClear(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();

        settings.clearMemberAdditionalRoles();
        commandChannel.sendMessage("Очищены дополнительные роли.").queue();
        LOGGER.debug("Cleared additional roles");

        return 424517178;
    }

    private static int executeAdditionalList(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();

        Set<Long> additionalRoles = settings.getMemberAdditionalRoles();
        if (additionalRoles.isEmpty())
        {
            commandChannel.sendMessage("Дополнительных ролей не установлено.").queue();

        } else
        {
            StringBuilder builder = new StringBuilder("Дополнительные роли: ");
            for (long ar : additionalRoles)
            {
                builder.append("\n • ").append(getRoleMention(ar));
            }
            commandChannel.sendMessage(builder.toString()).queue();
        }
        LOGGER.debug("Sent list of additional roles");

        return 745237231;
    }
}
