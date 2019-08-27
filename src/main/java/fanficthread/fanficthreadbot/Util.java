package fanficthread.fanficthreadbot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Util
{
    public static final String MESSAGE_LINK_FORMAT = "https://discordapp.com/channels/%1$d/%2$d/%3$d";

    public static final String USER_MENTION_REGEX = "<@(\\d+)>";
    public static final Pattern USER_MENTION_PATTERN = Pattern.compile(USER_MENTION_REGEX);

    public static final String ROLE_MENTION_REGEX = "<@&(\\d+)>";
    public static final Pattern ROLE_MENTION_PATTERN = Pattern.compile(ROLE_MENTION_REGEX);

    public static final String CHANNEL_MENTION_REGEX = "<#(\\d+)>";
    public static final Pattern CHANNEL_MENTION_PATTERN = Pattern.compile(CHANNEL_MENTION_REGEX);

    public static final String EMOJI_NAME_REGEX = "<:([0-9A-Za-z_]{1,32}):>";
    public static final Pattern EMOJI_NAME_PATTERN = Pattern.compile(EMOJI_NAME_REGEX);

    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private Util()
    {
    }

    public static String getMessageLink(long guild, long channel, long message)
    {
        return String.format(MESSAGE_LINK_FORMAT, guild, channel, message);
    }

    public static String formatSeconds(int timeInSeconds)
    {
        int secondsLeft = timeInSeconds % 3600 % 60;
        int minutes = (int) Math.floor(timeInSeconds % 3600 / 60);
        int hours = (int) Math.floor(timeInSeconds / 3600);

        String HH = hours < 10 ? "0" + hours : String.valueOf(hours);
        String MM = minutes < 10 ? "0" + minutes : String.valueOf(minutes);
        String SS = secondsLeft < 10 ? "0" + secondsLeft : String.valueOf(secondsLeft);

        return HH + ":" + MM + ":" + SS;
    }

    /**
     * Find all user mentions in string using {@link Util#USER_MENTION_REGEX} regex.
     *
     * @param str string to search within
     * @return array of mentioned user id's in order
     */
    public static long[] findUserMentions(String str)
    {
        return findLongs(USER_MENTION_PATTERN, str);
    }

    /**
     * Find all role mentions in string using {@link Util#ROLE_MENTION_REGEX} regex.
     *
     * @param str string to search within
     * @return array of mentioned roles id's in order
     */
    public static long[] findRoleMentions(String str)
    {
        return findLongs(ROLE_MENTION_PATTERN, str);
    }

    /**
     * Find all channel mentions in string using {@link Util#CHANNEL_MENTION_REGEX} regex.
     *
     * @param str string to search within
     * @return array of mentioned channels id's in order
     */
    public static long[] findChannelMentions(String str)
    {
        return findLongs(CHANNEL_MENTION_PATTERN, str);
    }

    private static long[] findLongs(Pattern pattern, String str)
    {
        Matcher matcher = pattern.matcher(str);
        if (!matcher.matches()) return EMPTY_LONG_ARRAY;

        int count = matcher.groupCount();
        long[] longs = new long[count];
        for (int i = 0; i < count; i++)
        {
            //Since regex captures only numbers NumberFormatException is unexpected
            longs[i] = Long.parseLong(matcher.group(i + 1));
        }
        return longs;
    }

    /**
     * Find first user mention in string using {@link Util#USER_MENTION_REGEX} regex.
     *
     * @param str string to search within
     * @return mentioned user id or 0
     */
    public static long findUserMention(String str)
    {
        return findLong(USER_MENTION_PATTERN, str);
    }

    /**
     * Find first role mention in string using {@link Util#ROLE_MENTION_REGEX} regex.
     *
     * @param str string to search within
     * @return mentioned role id or 0
     */
    public static long findRoleMention(String str)
    {
        return findLong(ROLE_MENTION_PATTERN, str);
    }

    /**
     * Find first channel mention in string using {@link Util#CHANNEL_MENTION_REGEX} regex.
     *
     * @param str string to search within
     * @return mentioned channel id or 0
     */
    public static long findChannelMention(String str)
    {
        return findLong(CHANNEL_MENTION_PATTERN, str);
    }

    private static long findLong(Pattern pattern, String str)
    {
        Matcher matcher = pattern.matcher(str);
        if (!matcher.matches()) return 0;
        return Long.parseLong(matcher.group(1));
    }

    /**
     * Find all emojis in string using {@link Util#EMOJI_NAME_REGEX} regex.
     *
     * @param str string to search within
     * @return array of emojis in order
     */
    public static String[] findEmojis(String str)
    {
        Matcher matcher = EMOJI_NAME_PATTERN.matcher(str);
        if (!matcher.matches()) return EMPTY_STRING_ARRAY;

        int count = matcher.groupCount();
        String[] emojis = new String[count];
        for (int i = 0; i < count; i++)
        {
            //Since regex captures only numbers NumberFormatException is unexpected
            emojis[i] = matcher.group(i + 1);
        }
        return emojis;
    }

    public static String getUserMention(long id)
    {
        return "<@" + id + ">";
    }

    public static String getUserNicknameMention(long id)
    {
        return "<@!" + id + ">";
    }

    public static String getRoleMention(long id)
    {
        return "<@&" + id + ">";
    }

    public static String getChannelMention(long id)
    {
        return "<#" + id + ">";
    }

    public static String getReactionMention(long id)
    {
        return getReactionMention(null, id);
    }

    public static String getReactionMention(String name, long id)
    {
        return "<:" + name + ":" + id + ">";
    }

    public static String getEmojiText(String name)
    {
        return ":" + name + ":";
    }
}
