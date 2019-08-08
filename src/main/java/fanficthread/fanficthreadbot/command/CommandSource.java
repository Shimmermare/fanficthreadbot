package fanficthread.fanficthreadbot.command;

import fanficthread.fanficthreadbot.FanficThreadBot;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

public class CommandSource
{
    private final FanficThreadBot bot;
    private final TextChannel channel;
    private final Member member;

    public CommandSource(FanficThreadBot bot, TextChannel channel, Member member)
    {
        this.bot = bot;
        this.channel = channel;
        this.member = member;
    }

    public FanficThreadBot getBot()
    {
        return bot;
    }

    public TextChannel getChannel()
    {
        return channel;
    }

    public Member getMember()
    {
        return member;
    }
}
