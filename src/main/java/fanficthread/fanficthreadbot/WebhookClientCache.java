package fanficthread.fanficthreadbot;

import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.webhook.WebhookClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class WebhookClientCache
{
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private Map<Long, WebhookClient> activeClients = new ConcurrentHashMap<>();

    public WebhookClientCache()
    {
    }

    public WebhookClient getOrCreateClient(Webhook webhook)
    {
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
}
