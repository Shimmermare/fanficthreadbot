package fanficthread.fanficthreadbot.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fanficthread.fanficthreadbot.command.CommandSource;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fanficthread.fanficthreadbot.command.Commands.literal;

/**
 * "help" command.
 * Show all available commands for member, taken into consideration his permissions.
 */
public final class HelpCommand
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HelpCommand.class);

    public static void register(final CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(literal("help")
                .executes(HelpCommand::execute)
        );
    }

    private static int execute(CommandContext<CommandSource> context)
    {
        CommandSource source = context.getSource();
        Member member = source.getMember();
        TextChannel channel = source.getChannel();


        channel.sendMessage("**Список комманд:**").queue();
        /* Base user */
        StringBuilder baseBuilder = new StringBuilder();
        baseBuilder
                .append("\n• help -> список комманд.")
                .append("\n• achannel -> список каналов для объявлений.")
                .append("\n• top -> топ дикторов.");
        channel.sendMessage(baseBuilder.toString()).queue();

        /* Admin help */
        if (member.hasPermission(Permission.ADMINISTRATOR))
        {
            StringBuilder achannelBuilder = new StringBuilder();
            achannelBuilder
                    .append("\n• achannel -> display list of announcement channels")
                    .append("\n• achannel <#channel> -> display channel status")
                    .append("\n• achannel <#channel> enable -> make channel announcement only")
                    .append("\n• achannel <#channel> repost #repost-channel -> disable repost channel for an announcement channel")
                    .append("\n• achannel <#channel> repost disable -> set repost channel for an announcement channel")
                    .append("\n• achannel <#channel> disable -> make channel a normal text channel");
            channel.sendMessage(achannelBuilder.toString()).queue();

            StringBuilder narratorBuilder = new StringBuilder();
            narratorBuilder
                    .append("\n• narrator -> display AutoNarrator settings")
                    .append("\n• narrator enable -> enable AutoNarrator")
                    .append("\n• narrator disable -> disable AutoNarrator")
                    .append("\n• narrator clear -> clear all narrator user data")
                    .append("\n• narrator recorder <@narrator-recorder> -> set narrator recorder bot")
                    .append("\n• narrator role <@narrator-role> -> set narrator role")
                    .append("\n• narrator audience <narrator-min-audience -> set minimal audience")
                    .append("\n• narrator <@user> -> display user narrator status")
                    .append("\n• narrator <@user> time set <seconds> -> set user's narrator time")
                    .append("\n• narrator <@user> time add <seconds> -> add user narrator time")
                    .append("\n• narrator <@user> last <last-narration> -> set user last narration time");
            channel.sendMessage(narratorBuilder.toString()).queue();

            StringBuilder voteBuilder = new StringBuilder();
            voteBuilder
                    .append("\n• vote -> display MemberVote status")
                    .append("\n• vote enable -> enable MemberVote")
                    .append("\n• vote disable -> disable MemberVote")
                    .append("\n• vote channel <#vote-channel> -> set MemberVote channel")
                    .append("\n• vote reactions <:reaction-upvote:> <:reaction-downvote:> -> set MemberVote up- and down-vote reactions")
                    .append("\n• vote requirement <votes-required> -> set amount of votes required for MemberVote's poll completion")
                    .append("\n• vote timeout <poll-timeout> -> amount of seconds before poll is removed")
                    .append("\n• vote role <@member-role> -> set MemberVote main role")
                    .append("\n• vote additional -> list additional MemberVote roles")
                    .append("\n• vote additional clear -> clear additional MemberVote roles")
                    .append("\n• vote additional <@additional-role> -> display role's status as additional MemberVote role")
                    .append("\n• vote additional <@additional-role> add -> make role additional MemberVote role")
                    .append("\n• vote additional <@additional-role> delete -> remove role from additional MemberVote roles")
                    .append("\n• vote open all -> open MemberVote poll for all users without member role")
                    .append("\n• vote open <@user> -> open MemberVote poll for user")
                    .append("\n• vote cleanup - clean voting channel from non-poll messages");
            channel.sendMessage(voteBuilder.toString()).queue();
        }
        LOGGER.debug("Command help shown; channel:{}, user:{}", channel.getIdLong(), member.getUser().getIdLong());

        return 1;
    }

    private HelpCommand()
    {
    }
}
