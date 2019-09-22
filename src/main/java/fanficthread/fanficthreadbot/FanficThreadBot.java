package fanficthread.fanficthreadbot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import fanficthread.fanficthreadbot.command.CommandSource;
import fanficthread.fanficthreadbot.command.impl.*;
import fanficthread.fanficthreadbot.listeners.*;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Discord bot for one weird server.
 */
public class FanficThreadBot implements Runnable
{
    private static final int SAVE_COOLDOWN_MS = 10000;
    private static final Path SETTINGS_FILE_NAME = Paths.get("bot_settings.json");
    private static final Path STATE_FILE_NAME = Paths.get("bot_state.json");

    private static final Logger LOGGER = LoggerFactory.getLogger(FanficThreadBot.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ScheduledExecutorService WEBHOOK_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private final BotArgs args;
    private CommandDispatcher<CommandSource> commandDispatcher;
    private BotWebhookCache botWebhookCache;

    //effectively non-final
    private BotSettings settings;
    private BotState state;
    private long lastSaveMillis;

    private JDA jda;
    private Guild guild;
    private long botUserId;

    public FanficThreadBot(BotArgs args)
    {
        this.args = args;
    }

    public static void main(String[] args)
    {
        BotArgs botArgs;
        try
        {
            botArgs = BotArgs.parseArgs(args);
        } catch (IllegalArgumentException e)
        {
            LOGGER.error("Program argument parsing exception", e);
            return;
        }
        new FanficThreadBot(botArgs).run();
    }

    @Override
    public void run()
    {
        commandDispatcher = new CommandDispatcher<>();
        BotCommand.register(commandDispatcher);
        HelpCommand.register(commandDispatcher);
        AnnouncementChannelCommand.register(commandDispatcher);
        MemberVoteCommand.register(commandDispatcher);
        NarratorCommand.register(commandDispatcher);
        SpoilerCommand.register(commandDispatcher);

        botWebhookCache = new BotWebhookCache(this, WEBHOOK_EXECUTOR);

        load(true);

        try
        {
            jda = new JDABuilder(args.token)
                    .addEventListener(
                            new CommandListener(this),
                            new AnnouncementChannelListener(this),
                            new NarratorListener(this),
                            new MemberVoteListener(this),
                            new SpoilerListener(this)
                    )
                    .build().awaitReady();
        } catch (LoginException e)
        {
            LOGGER.error("Failed to login bot", e);
            return;
        } catch (InterruptedException e)
        {
            LOGGER.error("Interrupted bot login thread", e);
            return;
        }

        botUserId = jda.getSelfUser().getIdLong();
        guild = jda.getGuildById(args.guild);

        startHourlyTicker();

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            LOGGER.debug("Shutdown hook executed");
            save();
        }, "Shutdown-hook-thread"));

        try {
            System.in.read();
            System.exit(0);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void shutdown(int status)
    {

        forceSave();
        jda.shutdown();
        System.exit(status);
    }

    private void startHourlyTicker()
    {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        LocalDateTime nextHourStart = now.withSecond(0).withMinute(0).plusHours(1);
        long initialDelay = nextHourStart.toEpochSecond(ZoneOffset.UTC) - now.toEpochSecond(ZoneOffset.UTC);
        EXECUTOR.scheduleWithFixedDelay(this::doEveryHour, initialDelay, 3600L, TimeUnit.SECONDS);
        //EXECUTOR.scheduleWithFixedDelay(this::doEveryHour, 60, 30, TimeUnit.SECONDS);
    }

    private void doEveryHour()
    {
        LOGGER.debug("Hourly tick");

        if (settings.isMemberVoteEnabled())
        {
            long now = Instant.now().getEpochSecond();
            TextChannel channel = guild.getTextChannelById(settings.getMemberVoteChannel());
            if (channel == null)
            {
                LOGGER.error("Voting channel {} does not exist", settings.getMemberVoteChannel());
            } else
            {
                for (MemberPoll poll : state.getMemberPolls())
                {
                    if (now - poll.getTimestampCreated() > settings.getMemberVoteTimeout())
                    {
                        Message message = channel.getMessageById(poll.getMessageId()).complete();
                        if (message == null)
                        {
                            LOGGER.error("Poll message {} doesn't exist", poll.getMessageId());
                            continue;
                        }
                        message.delete().queue();
                        state.removeMemberPoll(poll);
                        LOGGER.debug("Removed member poll {}/{} because it is older than the limit of {}", poll.getUserId(), poll.getMessageId(), settings.getMemberVoteTimeout());
                    }
                }
            }
        }
        save();
    }

    public void save()
    {
        final long nowMillis = System.currentTimeMillis();
        final long fromLastSave = nowMillis - lastSaveMillis;
        if (fromLastSave < SAVE_COOLDOWN_MS)
        {
            LOGGER.debug("Saving cancelled, only {} ms passed since last save and delay is {} ms", fromLastSave, SAVE_COOLDOWN_MS);
            return;
        }
        saveSettings();
        saveState();
        lastSaveMillis = nowMillis;
    }

    public void forceSave()
    {
        saveSettings();
        saveState();
    }

    public void saveSettings()
    {
        try
        {
            Files.write(SETTINGS_FILE_NAME, GSON.toJson(settings).getBytes());
        } catch (IOException e)
        {
            LOGGER.error("Failed to save settings to file", e);
        } catch (RuntimeException e)
        {
            LOGGER.error("Failed to serialize settings to json", e);
        }
    }

    public void saveState()
    {
        try
        {
            Files.write(STATE_FILE_NAME, GSON.toJson(state).getBytes());
        } catch (IOException e)
        {
            LOGGER.error("Failed to save state to file", e);
        } catch (RuntimeException e)
        {
            LOGGER.error("Failed to serialize state to json", e);
        }
    }

    public void load(boolean orElseCreate)
    {
        loadSettings(orElseCreate);
        loadState(orElseCreate);
    }

    public void loadSettings(boolean orElseCreate)
    {
        if (!Files.exists(SETTINGS_FILE_NAME))
        {
            LOGGER.info("Can't load bot settings: no file found");
        } else
        {
            try
            {
                settings = GSON.fromJson(new String(Files.readAllBytes(SETTINGS_FILE_NAME)), BotSettings.class);
                return;
            } catch (IOException e)
            {
                LOGGER.error("Failed to read bot settings file", e);
                shutdown(-100);
            } catch (RuntimeException e)
            {
                LOGGER.error("Failed to parse bot settings from json", e);
                shutdown(-100);
            }
        }
        if (orElseCreate) settings = new BotSettings();
    }

    public void loadState(boolean orElseCreate)
    {
        if (!Files.exists(STATE_FILE_NAME))
        {
            LOGGER.info("Can't load bot state: no file found");
        } else
        {
            try
            {
                state = GSON.fromJson(new String(Files.readAllBytes(STATE_FILE_NAME)), BotState.class);
                return;
            } catch (IOException e)
            {
                LOGGER.error("Failed to read bot state file", e);
                shutdown(-101);
            } catch (RuntimeException e)
            {
                LOGGER.error("Failed to parse bot state from json", e);
                shutdown(-101);
            }
        }
        if (orElseCreate) state = new BotState();
    }

    public CommandDispatcher<CommandSource> getCommandDispatcher()
    {
        return commandDispatcher;
    }

    public BotWebhookCache getBotWebhookCache()
    {
        return botWebhookCache;
    }

    /**
     * Get current Bot Settings.
     * The returned instance is valid only at the moment of time and should not be cached.
     *
     * @return current bot settings
     * @see BotSettings
     */
    public BotSettings getSettings()
    {
        return settings;
    }

    void setSettings(BotSettings settings)
    {
        this.settings = settings;
    }

    /**
     * Get current Bot State.
     * The returned instance is valid only at the moment of time and should not be cached.
     *
     * @return current bot state
     * @see BotState
     */
    public BotState getState()
    {
        return state;
    }

    void setState(BotState state)
    {
        this.state = state;
    }

    public JDA getJDA()
    {
        return jda;
    }

    public Guild getGuild()
    {
        return guild;
    }

    public long getBotUserId()
    {
        return botUserId;
    }

    public boolean isLockedGuild(long id)
    {
        return args.guild == id;
    }

    private static class BotArgs
    {
        private static final String ARG_TOKEN = "token";
        private static final String ARG_GUILD = "guild";

        private String token;
        private long guild;

        BotArgs()
        {
        }

        static BotArgs parseArgs(String[] args) throws IllegalArgumentException
        {
            final OptionParser optionParser = new OptionParser();
            OptionSpec<String> tokenSpec = optionParser.accepts(ARG_TOKEN).withRequiredArg().ofType(String.class).required();
            OptionSpec<Long> guildSpec = optionParser.accepts(ARG_GUILD).withRequiredArg().ofType(Long.class).required();

            final OptionSet optionSet = optionParser.parse(args);

            BotArgs botArgs = new BotArgs();
            botArgs.token = optionSet.valueOf(tokenSpec);
            botArgs.guild = optionSet.valueOf(guildSpec);
            return botArgs;
        }

        public String getToken()
        {
            return token;
        }

        public long getGuild()
        {
            return guild;
        }
    }
}
