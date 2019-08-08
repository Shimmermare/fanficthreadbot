package fanficthread.fanficthreadbot.listeners;

import fanficthread.fanficthreadbot.FanficThreadBot;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

abstract class AbstractListener extends ListenerAdapter
{
    protected final FanficThreadBot bot;

    public AbstractListener(FanficThreadBot bot)
    {
        this.bot = bot;
    }
}
