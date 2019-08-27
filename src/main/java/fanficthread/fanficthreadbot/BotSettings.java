package fanficthread.fanficthreadbot;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the settings of the bot.
 * Thread-safe.
 * <p>
 * Settings:
 * • announcementChannels - active {@link AnnouncementChannel}s.
 * <p>
 * • memberVoteEnabled - is MemberVote module enabled.
 * • memberVoteChannel - MemberVote poll text channel.
 * • memberVoteReactionUpvote - MemberVote vote 'up' reaction. Can be a custom reaction only.
 * • memberVoteReactionDownvote - MemberVote vote ''down' reaction. Can be a custom reaction only.
 * • memberVotesRequired - MemberVote poll score required to close a poll. 1 upvote = +1, 1 downvote = -1.
 * • memberVoteTimeout - MemberVote time in seconds before poll will be closed without winning. Default is 2 weeks.
 * • memberRole - MemberVote member role.
 * • memberAdditionalRoles - MemberVote additional member roles.
 * <p>
 * • narratorEnabled - is AutoNarrator module enabled.
 * • narratorRecorder - recording bot id
 * • narratorRole - narrator role id
 * • narratorMinAudience - minimal voice channel audience to start getting narrator time.
 * • narratorActiveTime - narrator role on-user time. Default is 1 week.
 */
@JsonAdapter(BotSettings.JsonAdapter.class)
public class BotSettings
{
    private Map<Long, AnnouncementChannel> announcementChannels = new ConcurrentHashMap<>();

    private boolean memberVoteEnabled;
    private long memberVoteChannel;
    private long memberVoteReactionUpvote;
    private long memberVoteReactionDownvote;
    private int memberVotesRequired = 5;
    private int memberVoteTimeout = 1184400;
    private long memberRole;
    private Set<Long> memberAdditionalRoles = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private boolean narratorEnabled;
    private long narratorRecorder;
    private long narratorRole;
    private int narratorMinAudience = 5;
    private int narratorActiveTime = 604800;

    private boolean spoilerEnabled;
    private Set<Long> spolierWhitelist = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public BotSettings()
    {
    }

    public boolean isAnnouncementChannel(long snowflake)
    {
        return announcementChannels.containsKey(snowflake);
    }

    public AnnouncementChannel getAnnouncementChannel(long id)
    {
        return announcementChannels.get(id);
    }

    public void addAnnouncementChannel(AnnouncementChannel channel)
    {
        if (channel == null) throw new IllegalArgumentException("Announcement channel can't be null");
        announcementChannels.put(channel.getChannelId(), channel);
    }

    public void removeAnnouncementChannel(long channelID)
    {
        announcementChannels.remove(channelID);
    }

    public Collection<AnnouncementChannel> getAnnouncementChannels()
    {
        return announcementChannels.values();
    }

    public void clearAnnouncementChannels()
    {
        announcementChannels.clear();
    }

    public boolean isMemberVoteEnabled()
    {
        return memberVoteEnabled;
    }

    public void setMemberVoteEnabled(boolean memberVoteEnabled)
    {
        this.memberVoteEnabled = memberVoteEnabled;
    }

    public long getMemberVoteChannel()
    {
        return memberVoteChannel;
    }

    public void setMemberVoteChannel(long memberVoteChannel)
    {
        this.memberVoteChannel = memberVoteChannel;
    }

    public long getMemberVoteReactionUpvote()
    {
        return memberVoteReactionUpvote;
    }

    public void setMemberVoteReactionUpvote(long memberVoteReactionUpvote)
    {
        this.memberVoteReactionUpvote = memberVoteReactionUpvote;
    }

    public long getMemberVoteReactionDownvote()
    {
        return memberVoteReactionDownvote;
    }

    public void setMemberVoteReactionDownvote(long memberVoteReactionDownvote)
    {
        this.memberVoteReactionDownvote = memberVoteReactionDownvote;
    }

    public int getMemberVotesRequired()
    {
        return memberVotesRequired;
    }

    public void setMemberVotesRequired(int memberVoteRequired)
    {
        this.memberVotesRequired = memberVoteRequired;
    }

    public int getMemberVoteTimeout()
    {
        return memberVoteTimeout;
    }

    public void setMemberVoteTimeout(int memberVoteTimeout)
    {
        this.memberVoteTimeout = memberVoteTimeout;
    }

    public long getMemberRole()
    {
        return memberRole;
    }

    public void setMemberRole(long memberRole)
    {
        this.memberRole = memberRole;
    }

    public void addMemberAdditionalRole(long role)
    {
        memberAdditionalRoles.add(role);
    }

    public void removeMemberAdditionalRole(long role)
    {
        memberAdditionalRoles.remove(role);
    }

    public Set<Long> getMemberAdditionalRoles()
    {
        return memberAdditionalRoles;
    }

    public boolean isMemberAdditionalRole(long role)
    {
        return memberAdditionalRoles.contains(role);
    }

    public void clearMemberAdditionalRoles()
    {
        memberAdditionalRoles.clear();
    }

    public boolean isNarratorEnabled()
    {
        return narratorEnabled;
    }

    public void setNarratorEnabled(boolean narratorEnabled)
    {
        this.narratorEnabled = narratorEnabled;
    }

    public long getNarratorRecorder()
    {
        return narratorRecorder;
    }

    public void setNarratorRecorder(long narratorRecorder)
    {
        this.narratorRecorder = narratorRecorder;
    }

    public long getNarratorRole()
    {
        return narratorRole;
    }

    public void setNarratorRole(long narratorRole)
    {

        this.narratorRole = narratorRole;
    }

    public int getNarratorMinAudience()
    {
        return narratorMinAudience;
    }

    public void setNarratorMinAudience(int narratorMinAudience)
    {
        if (narratorMinAudience < 0)
            throw new IllegalArgumentException("Narrator min audience can't be lower than 0");
        this.narratorMinAudience = narratorMinAudience;
    }

    public int getNarratorActiveTime()
    {
        return narratorActiveTime;
    }

    public void setNarratorActiveTime(int narratorActiveTime)
    {
        if (narratorActiveTime < 0)
            throw new IllegalArgumentException("Narrator active time can't be lower than 0");
        this.narratorActiveTime = narratorActiveTime;
    }

    /**
     * Set BotSettings state from json
     *
     * @param gson        gson instance
     * @param jsonElement json object
     */
    public void setFromJson(Gson gson, JsonElement jsonElement)
    {

    }

    public boolean isSpoilerEnabled()
    {
        return spoilerEnabled;
    }

    public void setSpoilerEnabled(boolean spoilerEnabled)
    {
        this.spoilerEnabled = spoilerEnabled;
    }

    public Set<Long> getSpolierWhitelist()
    {
        return spolierWhitelist;
    }

    public boolean isInSpoilerWhitelist(long id)
    {
        return spolierWhitelist.contains(id);
    }

    public void addToSpoilerWhitelist(long id)
    {
        spolierWhitelist.add(id);
    }

    public void removeFromSpoilerWhitelist(long id)
    {
        spolierWhitelist.remove(id);
    }

    public static class JsonAdapter implements JsonSerializer<BotSettings>, JsonDeserializer<BotSettings>
    {
        private static final String NAME_ANNOUNCEMENT_CHANNELS_ARRAY = "announcement_channels";

        private static final String NAME_MEMBER_VOTE_OBJECT = "member_vote";
        private static final String NAME_MEMBER_VOTE_OBJECT_ENABLED = "enabled";
        private static final String NAME_MEMBER_VOTE_OBJECT_CHANNEL = "channel";
        private static final String NAME_MEMBER_VOTE_OBJECT_REACTION_UPVOTE = "reaction_upvote";
        private static final String NAME_MEMBER_VOTE_OBJECT_REACTION_DOWNVOTE = "reaction_downvote";
        private static final String NAME_MEMBER_VOTE_OBJECT_VOTES_REQUIRED = "votes_required";
        private static final String NAME_MEMBER_VOTE_OBJECT_TIMEOUT = "timeout";
        private static final String NAME_MEMBER_VOTE_OBJECT_ROLE = "role";
        private static final String NAME_MEMBER_VOTE_OBJECT_ADDITIONAL_ROLES_ARRAY = "additional_roles";

        private static final String NAME_MEMBER_NARRATOR_OBJECT = "narrator";
        private static final String NAME_MEMBER_NARRATOR_OBJECT_ENABLED = "enabled";
        private static final String NAME_MEMBER_NARRATOR_OBJECT_RECORDER = "recorder";
        private static final String NAME_MEMBER_NARRATOR_OBJECT_ROLE = "role";
        private static final String NAME_MEMBER_NARRATOR_OBJECT_MIN_AUDIENCE = "min_audience";
        private static final String NAME_MEMBER_NARRATOR_OBJECT_ACTIVE_TIME = "active_time";

        private static final String NAME_SPOILER_ENABLED = "spoiler_enabled";
        private static final String NAME_SPOILER_WHITELIST = "spoiler_whitelist";

        @Override
        public JsonElement serialize(BotSettings obj, Type type, JsonSerializationContext context)
        {
            JsonObject json = new JsonObject();

            JsonArray announcementChannels = new JsonArray();
            for (AnnouncementChannel ac : obj.getAnnouncementChannels())
                announcementChannels.add(context.serialize(ac, AnnouncementChannel.class));
            json.add(NAME_ANNOUNCEMENT_CHANNELS_ARRAY, announcementChannels);

            JsonObject memberVote = new JsonObject();
            memberVote.addProperty(NAME_MEMBER_VOTE_OBJECT_ENABLED, obj.isMemberVoteEnabled());
            memberVote.addProperty(NAME_MEMBER_VOTE_OBJECT_CHANNEL, obj.getMemberVoteChannel());
            memberVote.addProperty(NAME_MEMBER_VOTE_OBJECT_REACTION_UPVOTE, obj.getMemberVoteReactionUpvote());
            memberVote.addProperty(NAME_MEMBER_VOTE_OBJECT_REACTION_DOWNVOTE, obj.getMemberVoteReactionDownvote());
            memberVote.addProperty(NAME_MEMBER_VOTE_OBJECT_VOTES_REQUIRED, obj.getMemberVotesRequired());
            memberVote.addProperty(NAME_MEMBER_VOTE_OBJECT_TIMEOUT, obj.getMemberVoteTimeout());
            memberVote.addProperty(NAME_MEMBER_VOTE_OBJECT_ROLE, obj.getMemberRole());
            JsonArray additionalRoles = new JsonArray();
            for (long ar : obj.getMemberAdditionalRoles()) additionalRoles.add(ar);
            memberVote.add(NAME_MEMBER_VOTE_OBJECT_ADDITIONAL_ROLES_ARRAY, additionalRoles);
            json.add(NAME_MEMBER_VOTE_OBJECT, memberVote);

            JsonObject narrator = new JsonObject();
            narrator.addProperty(NAME_MEMBER_NARRATOR_OBJECT_ENABLED, obj.isNarratorEnabled());
            narrator.addProperty(NAME_MEMBER_NARRATOR_OBJECT_RECORDER, obj.getNarratorRecorder());
            narrator.addProperty(NAME_MEMBER_NARRATOR_OBJECT_ROLE, obj.getNarratorRole());
            narrator.addProperty(NAME_MEMBER_NARRATOR_OBJECT_MIN_AUDIENCE, obj.getNarratorMinAudience());
            narrator.addProperty(NAME_MEMBER_NARRATOR_OBJECT_ACTIVE_TIME, obj.getNarratorActiveTime());
            json.add(NAME_MEMBER_NARRATOR_OBJECT, narrator);

            json.addProperty(NAME_SPOILER_ENABLED, obj.isSpoilerEnabled());
            JsonArray spoilerWhitelist = new JsonArray();
            for (long id : obj.getSpolierWhitelist())
            {
                spoilerWhitelist.add(id);
            }
            json.add(NAME_SPOILER_WHITELIST, spoilerWhitelist);

            return json;
        }

        @Override
        public BotSettings deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject json = jsonElement.getAsJsonObject();
            BotSettings obj = new BotSettings();

            JsonArray announcementChannels = json.getAsJsonArray(NAME_ANNOUNCEMENT_CHANNELS_ARRAY);
            if (announcementChannels != null)
            {
                AnnouncementChannel[] announcementChannelsArr = context.deserialize(announcementChannels, AnnouncementChannel[].class);
                if (announcementChannelsArr != null)
                {
                    for (AnnouncementChannel ac : announcementChannelsArr) obj.addAnnouncementChannel(ac);
                }
            }

            JsonObject memberVote = json.getAsJsonObject(NAME_MEMBER_VOTE_OBJECT);
            if (memberVote != null)
            {
                JsonElement enabled = memberVote.get(NAME_MEMBER_VOTE_OBJECT_ENABLED);
                if (enabled != null) obj.setMemberVoteEnabled(enabled.getAsBoolean());
                JsonElement channel = memberVote.get(NAME_MEMBER_VOTE_OBJECT_CHANNEL);
                if (channel != null) obj.setMemberVoteChannel(channel.getAsLong());
                JsonElement reactionUpvote = memberVote.get(NAME_MEMBER_VOTE_OBJECT_REACTION_UPVOTE);
                if (reactionUpvote != null) obj.setMemberVoteReactionUpvote(reactionUpvote.getAsLong());
                JsonElement reactionDownvote = memberVote.get(NAME_MEMBER_VOTE_OBJECT_REACTION_DOWNVOTE);
                if (reactionDownvote != null) obj.setMemberVoteReactionDownvote(reactionDownvote.getAsLong());
                JsonElement votesRequired = memberVote.get(NAME_MEMBER_VOTE_OBJECT_VOTES_REQUIRED);
                if (votesRequired != null) obj.setMemberVotesRequired(votesRequired.getAsInt());
                JsonElement timeout = memberVote.get(NAME_MEMBER_VOTE_OBJECT_TIMEOUT);
                if (timeout != null) obj.setMemberVoteTimeout(timeout.getAsInt());
                JsonElement role = memberVote.get(NAME_MEMBER_VOTE_OBJECT_ROLE);
                if (role != null) obj.setMemberRole(role.getAsLong());

                JsonArray additionalRoles = memberVote.getAsJsonArray(NAME_MEMBER_VOTE_OBJECT_ADDITIONAL_ROLES_ARRAY);
                if (additionalRoles != null)
                {
                    long[] additionalRolesArr = context.deserialize(additionalRoles, long[].class);
                    if (additionalRolesArr != null)
                    {
                        for (long ar : additionalRolesArr) obj.addMemberAdditionalRole(ar);
                    }
                }
            }

            JsonObject narrator = json.getAsJsonObject(NAME_MEMBER_NARRATOR_OBJECT);
            if (narrator != null)
            {
                JsonElement enabled = narrator.get(NAME_MEMBER_NARRATOR_OBJECT_ENABLED);
                if (enabled != null) obj.setNarratorEnabled(enabled.getAsBoolean());
                JsonElement recorder = narrator.get(NAME_MEMBER_NARRATOR_OBJECT_RECORDER);
                if (recorder != null) obj.setNarratorRecorder(recorder.getAsLong());
                JsonElement role = narrator.get(NAME_MEMBER_NARRATOR_OBJECT_ROLE);
                if (role != null) obj.setNarratorRole(role.getAsLong());
                JsonElement minAudience = narrator.get(NAME_MEMBER_NARRATOR_OBJECT_MIN_AUDIENCE);
                if (minAudience != null) obj.setNarratorMinAudience(minAudience.getAsInt());
                JsonElement activeTime = narrator.get(NAME_MEMBER_NARRATOR_OBJECT_ACTIVE_TIME);
                if (activeTime != null) obj.setNarratorActiveTime(activeTime.getAsInt());
            }

            JsonElement spoilerEnabledJson = json.get(NAME_SPOILER_ENABLED);
            if (spoilerEnabledJson != null)
            {
                obj.setSpoilerEnabled(spoilerEnabledJson.getAsBoolean());
            }
            JsonArray spoilerWhitelist = json.getAsJsonArray(NAME_SPOILER_WHITELIST);
            if (spoilerWhitelist != null)
            {
                long[] spoilerWhitelistArr = context.deserialize(spoilerWhitelist, long[].class);
                if (spoilerWhitelistArr != null)
                {
                    for (long id : spoilerWhitelistArr) obj.addToSpoilerWhitelist(id);
                }
            }

            return obj;
        }
    }
}
