package fanficthread.fanficthreadbot;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the state of the bot aka everything that's not settings.
 * Thread-safe.
 */
@JsonAdapter(BotState.JsonAdapter.class)
public class BotState
{
    private Map<Long, Narrator> narrators = new ConcurrentHashMap<>();
    private Map<Long, MemberPoll> memberPollsByMessage = new ConcurrentHashMap<>();
    private Map<Long, MemberPoll> memberPollsByUser = new ConcurrentHashMap<>();

    public BotState()
    {
    }

    public void addNarrator(Narrator narrator)
    {
        narrators.put(narrator.getId(), narrator);
    }

    public void removeNarrator(Narrator narrator)
    {
        narrators.remove(narrator.getId());
    }

    public Narrator getNarrator(long user)
    {
        return narrators.get(user);
    }

    public Narrator getOrCreateNarrator(long user)
    {
        return narrators.computeIfAbsent(user, Narrator::new);
    }

    public Collection<Narrator> getNarrators()
    {
        return narrators.values();
    }

    public void clearNarrators()
    {
        narrators.clear();
    }

    public void addMemberPoll(MemberPoll poll)
    {
        memberPollsByMessage.put(poll.getMessageId(), poll);
        memberPollsByUser.put(poll.getUserId(), poll);
    }

    public void removeMemberPoll(MemberPoll poll)
    {
        memberPollsByMessage.remove(poll.getMessageId());
        memberPollsByUser.remove(poll.getUserId());
    }

    public MemberPoll getMemberPollByMessage(long msgId)
    {
        return memberPollsByMessage.get(msgId);
    }

    public MemberPoll getMemberPollByUser(long userId)
    {
        return memberPollsByUser.get(userId);
    }

    public Collection<MemberPoll> getMemberPolls()
    {
        return memberPollsByMessage.values();
    }

    public void clearMemberPolls()
    {
        memberPollsByMessage.clear();
        memberPollsByUser.clear();
    }

    public static class JsonAdapter implements JsonSerializer<BotState>, JsonDeserializer<BotState>
    {
        private static final String NAME_NARRATORS_ARRAY = "narrators";
        private static final String NAME_MEMBER_POLLS_ARRAY = "member_polls";

        @Override
        public JsonElement serialize(BotState obj, Type type, JsonSerializationContext context)
        {
            JsonObject json = new JsonObject();

            JsonArray narratorArray = new JsonArray();
            for (Narrator narrator : obj.getNarrators())
            {
                narratorArray.add(context.serialize(narrator, Narrator.class));
            }
            json.add(NAME_NARRATORS_ARRAY, narratorArray);

            JsonArray memberPollsArray = new JsonArray();
            for (MemberPoll memberPoll : obj.getMemberPolls())
            {
                memberPollsArray.add(context.serialize(memberPoll, MemberPoll.class));
            }
            json.add(NAME_MEMBER_POLLS_ARRAY, memberPollsArray);

            return json;
        }

        @Override
        public BotState deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject json = jsonElement.getAsJsonObject();
            BotState obj = new BotState();

            JsonArray narratorArray = json.getAsJsonArray(NAME_NARRATORS_ARRAY);
            Narrator[] narratorsDes = context.deserialize(narratorArray, Narrator[].class);
            if (narratorsDes != null)
            {
                for (Narrator narrator : narratorsDes) obj.addNarrator(narrator);
            }

            JsonArray memberPollsArray = json.getAsJsonArray(NAME_MEMBER_POLLS_ARRAY);
            MemberPoll[] memberPollsDes = context.deserialize(memberPollsArray, MemberPoll[].class);
            if (memberPollsDes != null)
            {
                for (MemberPoll memberPoll : memberPollsDes) obj.addMemberPoll(memberPoll);
            }
            return obj;
        }
    }
}
