package fanficthread.fanficthreadbot.listeners;

import fanficthread.fanficthreadbot.FanficThreadBot;
import net.dv8tion.jda.core.events.DisconnectEvent;

public class BotListener extends AbstractListener
{
    public BotListener(FanficThreadBot bot)
    {
        super(bot);
    }

    @Override
    public void onDisconnect(DisconnectEvent event)
    {
        bot.save();
    }
}
