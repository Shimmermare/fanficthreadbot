package fanficthread.fanficthreadbot.listeners;

import fanficthread.fanficthreadbot.BotSettings;
import fanficthread.fanficthreadbot.FanficThreadBot;
import fanficthread.fanficthreadbot.Narrator;
import net.dv8tion.jda.core.audio.hooks.ConnectionListener;
import net.dv8tion.jda.core.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AutoNarratorListener extends AbstractListener implements ConnectionListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoNarratorListener.class);

    public AutoNarratorListener(FanficThreadBot bot)
    {
        super(bot);
    }

    private long currentChannelId;
    private int channelUserCount;

    private long reconnect5SecLock;

    private Map<Long, Long> userSpeakSessions = new ConcurrentHashMap<>();

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event)
    {
        final User user = event.getMember().getUser();
        final long userId = user.getIdLong();
        checkNarratorRole(userId);

        final VoiceChannel channel = event.getChannelJoined();

        if (currentChannelId != 0)
        {
            if (currentChannelId == channel.getIdLong() && !user.isBot())
            {
                channelUserCount++;
                LOGGER.debug("Channel user count incremented to {}", channelUserCount);
            }
            return;
        }

        final BotSettings settings = bot.getSettings();
        if (!settings.isNarratorEnabled()) return;
        if (userId != settings.getNarratorRecorder()) return;
        if (Instant.now().getEpochSecond() - reconnect5SecLock < 5) return;

        final AudioManager manager = bot.getGuild().getAudioManager();

        manager.openAudioConnection(channel);
        manager.setConnectionListener(this);

        channelUserCount = (int) channel.getMembers().stream().filter(m -> !m.getUser().isBot()).count();
        currentChannelId = channel.getIdLong();
        LOGGER.debug("Audio connection to {} established, {} current users", currentChannelId, channelUserCount);
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event)
    {
        final User user = event.getMember().getUser();
        final long userId = user.getIdLong();
        checkNarratorRole(userId);

        if (currentChannelId == 0 || currentChannelId != event.getChannelLeft().getIdLong()) return;
        final BotSettings settings = bot.getSettings();
        if (!settings.isNarratorEnabled()) return;

        if (userId != settings.getNarratorRecorder())
        {
            if (!user.isBot())
            {
                userSpeakSessions.remove(userId);
                channelUserCount--;
                LOGGER.debug("Channel user count decremented to {}", channelUserCount);
            }
            return;
        }

        final AudioManager manager = bot.getGuild().getAudioManager();
        manager.closeAudioConnection();
        LOGGER.debug("Audio connection to {} closed", currentChannelId);

        currentChannelId = 0;
        channelUserCount = 0;
        userSpeakSessions.clear();

        reconnect5SecLock = Instant.now().getEpochSecond();
    }

    private void checkNarratorRole(long userId)
    {
        Narrator narrator = bot.getState().getNarrator(userId);
        if (narrator != null)
        {
            narrator.checkRoles(bot);
            LOGGER.debug("Checked narrator role of {}", narrator.getId());
        }
    }

    @Override
    public void onPing(long l)
    {

    }

    @Override
    public void onStatusChange(ConnectionStatus connectionStatus)
    {

    }

    @Override
    public void onUserSpeaking(User user, boolean b)
    {
        if (channelUserCount < bot.getSettings().getNarratorMinAudience()) return;
        if (user.isBot()) return;

        final long id = user.getIdLong();
        if (b)
        {
            userSpeakSessions.computeIfAbsent(id, u -> Instant.now().getEpochSecond());
        } else
        {

            long startedTalking = Optional.ofNullable(userSpeakSessions.remove(id)).orElse(0L);
            if (startedTalking == 0) return;
            long endedTalking = Instant.now().getEpochSecond();

            int talkedTimeSec = (int) (endedTalking - startedTalking);

            Narrator narrator = bot.getState().getOrCreateNarrator(id);
            narrator.narrated(Instant.now().getEpochSecond(), talkedTimeSec);
            LOGGER.debug("Narrator {} narrated for " + talkedTimeSec + " seconds", id);
        }
    }
}
