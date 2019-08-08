package fanficthread.fanficthreadbot;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.lang.reflect.Type;

import static fanficthread.fanficthreadbot.Util.getReactionMention;
import static fanficthread.fanficthreadbot.Util.getUserMention;

@JsonAdapter(MemberPoll.JsonAdapter.class)
public class MemberPoll
{
    private final long messageId;
    private final long userId;
    private final long timestampCreated;

    public MemberPoll(long messageId, long userId, long timestampCreated)
    {
        this.messageId = messageId;
        this.userId = userId;
        this.timestampCreated = timestampCreated;
    }

    public long getMessageId()
    {
        return messageId;
    }

    public long getUserId()
    {
        return userId;
    }

    public long getTimestampCreated()
    {
        return timestampCreated;
    }

    public static MemberPoll create(FanficThreadBot bot, long userId, int nospaceChance)
    {
        final BotSettings settings = bot.getSettings();
        final BotState state = bot.getState();
        final Guild guild = bot.getGuild();

        if (state.getMemberPollByUser(userId) != null)
            throw new IllegalArgumentException("Poll is already up for user " + userId);

        final TextChannel channel = bot.getGuild().getTextChannelById(settings.getMemberVoteChannel());
        if (channel == null)
            throw new IllegalStateException("MemberVote channel " + settings.getMemberVoteChannel() + " does not exists");

        StringBuilder builder = new StringBuilder();
        builder.append("Посвятить в участники ").append(getUserMention(userId)).append('?');

        if (nospaceChance > 50)
            builder.append("\n**Внимание! С шансом ").append(nospaceChance).append("% этот аккаунт пренадлежит беспробелу!**");

        builder.append("\n").append(getReactionMention(settings.getMemberVoteReactionUpvote())).append(" - да");
        builder.append("\n").append(getReactionMention(settings.getMemberVoteReactionDownvote())).append(" - нет");

        Message message = channel.sendMessage(builder.toString()).complete();

        Emote emoteUp = guild.getEmoteById(settings.getMemberVoteReactionUpvote());
        Emote emoteDown = guild.getEmoteById(settings.getMemberVoteReactionDownvote());
        message.addReaction(emoteUp).queue((v) -> message.addReaction(emoteDown).queue());

        MemberPoll memberPoll = new MemberPoll(message.getIdLong(), userId, message.getCreationTime().toEpochSecond());
        state.addMemberPoll(memberPoll);
        return memberPoll;
    }

    public static class JsonAdapter implements JsonSerializer<MemberPoll>, JsonDeserializer<MemberPoll>
    {
        @Override
        public JsonElement serialize(MemberPoll poll, Type type, JsonSerializationContext context)
        {
            JsonObject json = new JsonObject();

            json.addProperty("message_id", poll.getMessageId());
            json.addProperty("user_id", poll.getUserId());
            json.addProperty("timestamp_created", poll.getTimestampCreated());

            return json;
        }

        @Override
        public MemberPoll deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject json = jsonElement.getAsJsonObject();

            final long messageId = json.get("message_id").getAsLong();
            final long userId = json.get("user_id").getAsLong();
            final long timestampCreated = json.get("timestamp_created").getAsLong();

            return new MemberPoll(messageId, userId, timestampCreated);
        }
    }
}
