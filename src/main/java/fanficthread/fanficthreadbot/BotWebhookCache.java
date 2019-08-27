package fanficthread.fanficthreadbot;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.webhook.WebhookClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

public final class BotWebhookCache
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BotWebhookCache.class);

    private static final String BOT_HOOK_NAME = "FTBotHook";

    private final FanficThreadBot bot;
    private final ScheduledExecutorService executorService;

    private Map<Long, WebhookClient> activeClients = new ConcurrentHashMap<>();

    public BotWebhookCache(FanficThreadBot bot, ScheduledExecutorService executorService)
    {
        this.bot = bot;
        this.executorService = executorService;
    }

    public Webhook get(long channelId)
    {
        TextChannel channel = bot.getGuild().getTextChannelById(channelId);
        if (channel == null)
        {
            throw new IllegalArgumentException("No such text channel: " + channelId);
        }
        return get(channel);
    }

    public Webhook get(TextChannel channel)
    {
        Optional<Webhook> optionalWebhook = channel.getWebhooks().complete().stream()
                .filter(w -> w.getName().equals(BOT_HOOK_NAME)).findFirst();
        if (optionalWebhook.isPresent())
        {
            return optionalWebhook.get();
        }

        LOGGER.debug("Bot hook for channel {} was requested but wasn't found, a new one is created", channel.getIdLong());
        return channel.createWebhook(BOT_HOOK_NAME).complete();
    }

    public WebhookClient getClient(long channelId)
    {
        TextChannel channel = bot.getGuild().getTextChannelById(channelId);
        if (channel == null)
        {
            throw new IllegalArgumentException("No such text channel: " + channelId);
        }
        return getClient(channel);
    }

    public WebhookClient getClient(TextChannel channel)
    {
        Webhook webhook = get(channel);
        return activeClients.computeIfAbsent(webhook.getIdLong(), (id) ->
                webhook.newClient()
                        .setExecutorService(executorService)
                        .build()
        );
    }

    public void closeClient(long webhookId)
    {
        activeClients.computeIfPresent(webhookId, (id, client) ->
        {
            client.close();
            return null;
        });
    }

    public void closeAllClients()
    {
        activeClients.forEach((hookId, client) -> client.close());
        activeClients.clear();
    }
}
