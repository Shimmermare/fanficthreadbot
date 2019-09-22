package fanficthread.fanficthreadbot;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.managers.GuildController;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;

@JsonAdapter(Narrator.JsonAdapter.class)
public class Narrator implements Comparable<Narrator>
{
    public static final Logger LOGGER = LoggerFactory.getLogger(Narrator.class);

    private final long id;
    //seconds
    private int time;

    public Narrator(long id)
    {
        this.id = id;
    }

    public Narrator(long id, int time)
    {
        this.id = id;
        this.time = time;
    }

    public long getId()
    {
        return id;
    }

    public void addTime(int time)
    {
        if (time < 0) throw new IllegalArgumentException("Can't add less than 0 seconds of time");
        this.time += time;
    }

    public int getTime()
    {
        return time;
    }

    public void setTime(int time)
    {
        if (time < 0) throw new IllegalArgumentException("Time can't be less than 0");
        this.time = time;
    }

    @Override
    public int compareTo(@NotNull Narrator o)
    {
        return Integer.compare(time, o.time) >> 8
                + Long.compare(id, o.id);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Narrator narrator = (Narrator) o;

        return id == narrator.id;

    }

    @Override
    public int hashCode()
    {
        return (int) (id ^ (id >>> 32));
    }

    public static class JsonAdapter implements JsonSerializer<Narrator>, JsonDeserializer<Narrator>
    {
        @Override
        public JsonElement serialize(Narrator narrator, Type type, JsonSerializationContext context)
        {
            JsonObject json = new JsonObject();

            json.addProperty("id", narrator.getId());
            json.addProperty("time", narrator.getTime());

            return json;
        }

        @Override
        public Narrator deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject json = jsonElement.getAsJsonObject();

            final long id = json.get("id").getAsLong();
            final int time = json.get("time").getAsInt();

            return new Narrator(id, time);
        }
    }
}
