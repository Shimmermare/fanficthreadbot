package fanficthread.fanficthreadbot.listeners;

import fanficthread.fanficthreadbot.BotSettings;
import fanficthread.fanficthreadbot.BotState;
import fanficthread.fanficthreadbot.FanficThreadBot;
import fanficthread.fanficthreadbot.MemberPoll;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.core.managers.GuildController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fanficthread.fanficthreadbot.Util.getUserMention;

/**
 * On GuildMemberJoin - check and create poll
 * On GuildMemberRoleRemove - check and create poll
 * On GuildMemberLeave - check and delete poll
 * On GenericGuildMessageReaction - check poll status and grant role
 */
public class MemberVoteListener extends AbstractListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MemberVoteListener.class);

    public MemberVoteListener(FanficThreadBot bot)
    {
        super(bot);
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event)
    {
        checkAndCreatePoll(event.getMember());
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event)
    {
        checkAndCreatePoll(event.getMember());
    }

    private void checkAndCreatePoll(Member member)
    {
        final BotSettings settings = bot.getSettings();
        final BotState state = bot.getState();
        final long userId = member.getUser().getIdLong();

        if (!settings.isMemberVoteEnabled()) return;
        if (state.getMemberPollByUser(userId) != null) return;

        for (Role role : member.getRoles())
        {
            if (role.getIdLong() == settings.getMemberRole()) return;
        }

        long userAccountTimestamp = ((userId >> 22) + 1420070400000L) / 1000;
        long memberJoinTimestamp = member.getJoinDate().toEpochSecond();
        long secondsBetween = memberJoinTimestamp - userAccountTimestamp;
        float nospaceChance = 1.0F - ((secondsBetween + 1) / 172800.0F);

        try
        {
            MemberPoll.create(bot, userId, (int) (nospaceChance * 100));
        } catch (RuntimeException e)
        {
            LOGGER.error("Failed to create member poll", e);
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event)
    {
        checkAndDeletePoll(event.getUser().getIdLong());
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event)
    {
        final BotSettings settings = bot.getSettings();
        final BotState state = bot.getState();
        if (!settings.isMemberVoteEnabled()) return;
        final TextChannel channel = event.getChannel();
        if (channel.getIdLong() != settings.getMemberVoteChannel()) return;
        MemberPoll poll = state.getMemberPollByMessage(event.getMessageIdLong());
        if (poll == null) return;

        state.removeMemberPoll(poll);
        LOGGER.debug("Manually deleted poll message {} for user {}; poll removed", event.getMessageIdLong(), poll.getUserId());
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event)
    {
        final BotSettings settings = bot.getSettings();
        if (!settings.isMemberVoteEnabled()) return;
        if (event.getRoles().stream().noneMatch(role -> role.getIdLong() == settings.getMemberRole())) return;
        final BotState state = bot.getState();
        final MemberPoll poll = state.getMemberPollByUser(event.getMember().getUser().getIdLong());
        if (poll == null) return;
        final Guild guild = bot.getGuild();
        final TextChannel channel = guild.getTextChannelById(settings.getMemberVoteChannel());

        channel.deleteMessageById(poll.getMessageId()).queue();
        state.removeMemberPoll(poll);
        LOGGER.debug("Deleted poll u:{}/m:{} because user was manually given member role {}", poll.getUserId(), poll.getMessageId(), settings.getMemberRole());
    }

    private void checkAndDeletePoll(final long userId)
    {
        final BotSettings settings = bot.getSettings();
        final BotState state = bot.getState();
        if (!settings.isMemberVoteEnabled()) return;

        MemberPoll poll = state.getMemberPollByUser(userId);
        if (poll == null) return;

        state.removeMemberPoll(poll);

        final TextChannel voteChannel = bot.getGuild().getTextChannelById(settings.getMemberVoteChannel());
        if (voteChannel == null)
        {
            LOGGER.error("MemberVote channel {} doesn't exists", voteChannel);
            return;
        }

        final Message message = voteChannel.getMessageById(poll.getMessageId()).complete();
        if (message == null)
        {
            LOGGER.error("MemberVote poll message {} doesn't exists", poll.getMessageId());
            return;
        }

        message.delete().queue();
    }

    @Override
    public void onGenericGuildMessageReaction(GenericGuildMessageReactionEvent event)
    {
        checkPollStatus(event.getMessageIdLong());
    }

    private void checkPollStatus(long messageId)
    {
        final BotSettings settings = bot.getSettings();
        final BotState state = bot.getState();
        final Guild guild = bot.getGuild();

        if (!settings.isMemberVoteEnabled()) return;

        final MemberPoll poll = state.getMemberPollByMessage(messageId);
        if (poll == null)
        {
            LOGGER.warn("Someone added reaction to message {} in MemberVote channel, but message isn't poll somehow.", messageId);
            return;
        }

        final TextChannel voteChannel = guild.getTextChannelById(settings.getMemberVoteChannel());
        if (voteChannel == null)
        {
            LOGGER.error("MemberVote channel {} doesn't exists", voteChannel);
            return;
        }

        final Message message = voteChannel.getMessageById(messageId).complete();
        if (message == null)
        {
            LOGGER.error("MemberVote poll message {} doesn't exists", poll.getMessageId());
            return;
        }


        final int score = countScore(message);
        if (score < settings.getMemberVotesRequired())
        {
            LOGGER.debug("MemberVote score {} counted from poll user {} message {}; not enough for completion", score, poll.getUserId(), poll.getMessageId());
            return;
        }

        state.removeMemberPoll(poll);

        final long userId = poll.getUserId();
        Member member = guild.getMemberById(userId);
        if (member == null)
        {
            LOGGER.error("MemberVote user {} is not a member", userId);
            return;
        }

        Set<Long> rolesToAddIds = new HashSet<>(settings.getMemberAdditionalRoles());
        rolesToAddIds.add(settings.getMemberRole());

        Set<Role> rolesToAdd = rolesToAddIds.stream().map(guild::getRoleById).collect(Collectors.toSet());

        GuildController controller = bot.getGuild().getController();
        controller.addRolesToMember(member, rolesToAdd).queue();

        message.editMessage("Голосование закончено, " + getUserMention(userId) + " теперь участник. Это сообщение будет удалено через несколько секунд.").queue();
        message.delete().queueAfter(10L, TimeUnit.SECONDS);

        LOGGER.debug("MemberVote poll finished, granted roles to user {}", userId);
    }

    private int countScore(Message message)
    {
        final BotSettings settings = bot.getSettings();
        final Guild guild = bot.getGuild();
        final Predicate<User> isLegitVoter = u -> !u.isBot() && guild.isMember(u);

        int score = 0;
        for (MessageReaction reaction : message.getReactions())
        {
            long reactionId;
            try
            {
                reactionId = reaction.getReactionEmote().getIdLong();
            } catch (IllegalStateException e)
            {
                reactionId = 0L;
            }

            if (reactionId == settings.getMemberVoteReactionUpvote())
            {
                score += reaction.getUsers().complete().stream().filter(isLegitVoter).count();
            } else if (reactionId == settings.getMemberVoteReactionDownvote())
            {
                score -= reaction.getUsers().complete().stream().filter(isLegitVoter).count();
            } else
            {
                reaction.getUsers().queue(users -> users.forEach(u -> reaction.removeReaction(u).queue()));
                LOGGER.debug("Non-voting reaction {} removed from message {}", reactionId, message.getIdLong());
            }
        }
        return score;
    }
}
