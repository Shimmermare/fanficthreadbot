package fanficthread.fanficthreadbot.listeners;

import fanficthread.fanficthreadbot.AnnouncementChannel;
import fanficthread.fanficthreadbot.BotSettings;
import fanficthread.fanficthreadbot.FanficThreadBot;
import fanficthread.fanficthreadbot.Util;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class AnnouncementChannelListener extends AbstractListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AnnouncementChannelListener.class);

    public AnnouncementChannelListener(FanficThreadBot bot)
    {
        super(bot);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event)
    {
        final Guild guild = event.getGuild();
        if (!bot.isLockedGuild(guild.getIdLong())) return;

        final BotSettings settings = bot.getSettings();
        final TextChannel channel = event.getChannel();
        final long channelId = channel.getIdLong();

        final AnnouncementChannel announcementChannel = settings.getAnnouncementChannel(channelId);
        if (announcementChannel == null) return;

        final Message message = event.getMessage();
        if (message.mentionsEveryone()) return;

        final long repostChannelId = announcementChannel.getRepostChannelId();
        if (repostChannelId != 0)
        {
            repost(guild, channel, repostChannelId, message);
        }

        message.delete().reason("Non-announcement in an announcement channel").queue();
        LOGGER.debug("Non-announcement message {} in announcement channel {} deleted", event.getMessageIdLong(), channelId);
    }

    private void repost(Guild guild, TextChannel announcementChannel, long repostChannelId, Message message)
    {
        final long announcementChannelId = announcementChannel.getIdLong();
        TextChannel repostChannel = guild.getTextChannelById(repostChannelId);
        if (repostChannel == null)
        {
            LOGGER.error("Channel {} is used as repost channel for announcement channel {} but doesn't exists", repostChannelId, announcementChannelId);
            return;
        }

        User user = message.getAuthor();
        Member member = guild.getMember(user);

        WebhookClient webhookClient = getRepostWebhookClient(repostChannel);

        WebhookMessageBuilder webhookMessageBuilder = new WebhookMessageBuilder();
        webhookMessageBuilder.setUsername(((member == null) ? user.getName() : member.getEffectiveName()));
        webhookMessageBuilder.setAvatarUrl(user.getEffectiveAvatarUrl());

        List<Message> previousMessages = announcementChannel.getHistoryBefore(message, 1).complete().getRetrievedHistory();
        if (previousMessages.size() > 0)
        {
            Message previousMessage = previousMessages.get(0);

            String content = "[>>" + previousMessage.getIdLong() + "](<"
                    + Util.getMessageLink(guild.getIdLong(), announcementChannelId, previousMessage.getIdLong())
                    + ">)\n" + message.getContentRaw();
            webhookMessageBuilder.setContent(content);
        } else {
            webhookMessageBuilder.setContent(message.getContentRaw());
        }

        webhookMessageBuilder.addEmbeds(message.getEmbeds());
        List<Message.Attachment> attachments = message.getAttachments();
        for (Message.Attachment attachment : attachments)
        {
            try
            {
                String name = attachment.getFileName();
                InputStream is = attachment.getInputStream();
                webhookMessageBuilder.addFile(name, is);
            } catch (IllegalArgumentException | IOException e)
            {
                LOGGER.error("Can't download and upload attachment", e);
            }
        }
        webhookClient.send(webhookMessageBuilder.build());

        LOGGER.debug("Message {} re-posted from an announcement channel {} with webhook {}", message.getIdLong(), announcementChannelId, webhookClient.getIdLong());
    }

    private WebhookClient getRepostWebhookClient(TextChannel channel)
    {
        final long channelId = channel.getIdLong();

        Optional<Webhook> webhookOptional = channel.getWebhooks().complete().stream()
                .filter((w) -> w.getName().equals(AnnouncementChannel.REPOST_WEBHOOK_NAME))
                .findFirst();
        Webhook webhook;
        if (!webhookOptional.isPresent())
        {
            webhook = channel.createWebhook(AnnouncementChannel.REPOST_WEBHOOK_NAME).complete();
            LOGGER.debug("Webhook {} for an announcement channel {} created", webhook.getIdLong(), channelId);
        } else
        {
            webhook = webhookOptional.get();
        }

        return bot.getWebhookClientCache().getOrCreateClient(webhook);
    }
}
