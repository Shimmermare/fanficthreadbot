package fanficthread.fanficthreadbot;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;

@JsonAdapter(AnnouncementChannel.JsonAdapter.class)
public class AnnouncementChannel
{
    public static final String REPOST_WEBHOOK_NAME = "AnnouncementChannelRepostHook";

    private final long channelId;
    private long repostChannelId;

    public AnnouncementChannel(long channelId)
    {
        this.channelId = channelId;
    }

    public AnnouncementChannel(long channelId, long repostChannelId)
    {
        this.channelId = channelId;
        this.repostChannelId = repostChannelId;
    }

    public long getChannelId()
    {
        return channelId;
    }

    public void setRepostChannelId(long repostChannelId)
    {
        this.repostChannelId = repostChannelId;
    }

    public long getRepostChannelId()
    {
        return repostChannelId;
    }

    public static class JsonAdapter implements JsonSerializer<AnnouncementChannel>, JsonDeserializer<AnnouncementChannel>
    {
        @Override
        public JsonElement serialize(AnnouncementChannel ac, Type type, JsonSerializationContext context)
        {
            JsonObject json = new JsonObject();

            json.addProperty("id", ac.getChannelId());
            json.addProperty("repost_id", ac.getRepostChannelId());

            return json;
        }

        @Override
        public AnnouncementChannel deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject json = jsonElement.getAsJsonObject();

            long id = json.get("id").getAsLong();
            long repostId = json.get("repost_id").getAsLong();

            return new AnnouncementChannel(id, repostId);
        }
    }
}
