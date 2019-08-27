package fanficthread.fanficthreadbot.listeners;

import fanficthread.fanficthreadbot.BotSettings;
import fanficthread.fanficthreadbot.FanficThreadBot;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SpoilerListener extends AbstractListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SpoilerListener.class);

    public SpoilerListener(FanficThreadBot bot)
    {
        super(bot);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event)
    {
        final BotSettings settings = bot.getSettings();
        if (!settings.isSpoilerEnabled()) return;
        final TextChannel channel = event.getChannel();
        if (settings.isInSpoilerWhitelist(channel.getIdLong())) return;
        final User user = event.getAuthor();
        if (user.isBot() || user.isFake()) return;
        final Message message = event.getMessage();

        Message.Attachment spoiledImage = null;
        for (Message.Attachment attachment : message.getAttachments())
        {
            String fileName = attachment.getFileName();
            if (attachment.isImage() && !fileName.startsWith("SPOILER_") && fileName.contains("explicit"))
            {
                spoiledImage = attachment;
                break;
            }
        }
        if (spoiledImage == null) return;

        final Member member = event.getGuild().getMember(user);

        WebhookClient webhookClient = bot.getBotWebhookCache().getClient(channel);
        WebhookMessageBuilder builder = new WebhookMessageBuilder();

        builder.setUsername(member == null ? user.getName() : member.getEffectiveName());
        builder.setAvatarUrl(user.getEffectiveAvatarUrl());
        builder.setContent(message.getContentRaw());
        try
        {
            builder.addFile("SPOILER_" + spoiledImage.getFileName(), spoiledImage.getInputStream());
        } catch (IOException e)
        {
            LOGGER.error("Failed to download an attached image (message: {}/{})", channel.getIdLong(), message.getIdLong(), e);
            return;
        }

        webhookClient.send(builder.build()).thenRun(() -> message.delete().queue());
        LOGGER.debug("Reposted spoiled image. Original message: {}", message.getIdLong());
    }
}
