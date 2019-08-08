package fanficthread.fanficthreadbot.command.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fanficthread.fanficthreadbot.*;
import fanficthread.fanficthreadbot.command.CommandSource;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.LongArgumentType.longArg;
import static fanficthread.fanficthreadbot.Util.*;
import static fanficthread.fanficthreadbot.command.Commands.argument;
import static fanficthread.fanficthreadbot.command.Commands.literal;
import static fanficthread.fanficthreadbot.command.argument.RoleArgumentType.role;
import static fanficthread.fanficthreadbot.command.argument.UserArgumentType.user;

/**
 * AutoNarrator settings command
 * <p>
 * top - display narrator top
 * narrator - display current settings
 * narrator enable - enable AutoNarrator
 * narrator disable - disable AutoNarrator
 * narrator clear - clear all user data
 * narrator channel #narrator-channel - set narrator announcement channel
 * narrator role @narrator-role - set narrator role
 * narrator reward narrator-reward - set announcement reward
 * narrator charge narrator-charge - set hourly charge
 * narrator @user - display current user status
 * narrator @user current set (points) - set current user's points
 * narrator @user current give (points) - add or remove current user's points
 * narrator @user total set (points) - set user's total points
 * narrator @user total give (points) - add or remove user's total points
 */
public final class AutoNarratorCommand
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoNarratorCommand.class);

    private AutoNarratorCommand()
    {
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(literal("narrator")
                .requires(s -> s.getMember().hasPermission(Permission.ADMINISTRATOR))
                .then(literal("enable")
                        .executes(AutoNarratorCommand::executeEnable)
                )
                .then(literal("disable")
                        .executes(AutoNarratorCommand::executeDisable)
                )
                .then(literal("clear")
                        .executes(AutoNarratorCommand::executeClear)
                )
                .then(literal("recorder")
                        .then(argument("narrator-recorder", user())
                                .executes(AutoNarratorCommand::executeRecorder)
                        )
                )
                .then(literal("role")
                        .then(argument("narrator-role", role())
                                .executes(AutoNarratorCommand::executeRole)
                        )
                )
                .then(literal("audience")
                        .then(argument("narrator-min-audience", integer(0, Integer.MAX_VALUE))
                                .executes(AutoNarratorCommand::executeAudience)
                        )
                )
                .then(argument("user", user())
                        .then(literal("time")
                                .then(literal("set")
                                        .then(argument("seconds", integer())
                                                .executes(AutoNarratorCommand::executeTimeSet)
                                        )
                                )
                                .then(literal("add")
                                        .then(argument("seconds", integer(1, Integer.MAX_VALUE))
                                                .executes(AutoNarratorCommand::executeTimeAdd)
                                        )
                                )
                                .executes(AutoNarratorCommand::executeUserStatus)
                        )
                        .then(literal("last")
                                .then(argument("last-narration", longArg(0))
                                        .executes(AutoNarratorCommand::executeLastNarration)
                                )
                        )
                        .executes(AutoNarratorCommand::executeUserStatus)
                )
                .executes(AutoNarratorCommand::executeStatus)
        );

        dispatcher.register(literal("top")
                .then(literal("all")
                    .executes(context -> executeTop(context, 100))
                )
                .executes(context -> executeTop(context, 10))
        );
    }

    private static int executeEnable(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();

        if (settings.isNarratorEnabled())
        {
            commandChannel.sendMessage("Авто-диктор уже включён.").queue();
            LOGGER.debug("Tried to enable AutoNarrator but it's enabled already");
            return 771177133;
        }

        settings.setNarratorEnabled(true);
        commandChannel.sendMessage("Авто-диктор включён.").queue();
        LOGGER.debug("AutoNarrator enabled");

        return 125370108;
    }

    private static int executeDisable(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();

        if (!settings.isNarratorEnabled())
        {
            commandChannel.sendMessage("Авто-диктор уже выключен.").queue();
            LOGGER.debug("Tried to disable AutoNarrator but it's disabled already");
            return 209390451;
        }

        settings.setNarratorEnabled(false);
        commandChannel.sendMessage("Авто-диктор выключен.").queue();
        LOGGER.debug("AutoNarrator disabled");

        return 962213406;
    }

    private static int executeClear(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotState state = bot.getState();
        final TextChannel commandChannel = source.getChannel();

        bot.saveState();
        state.clearNarrators();

        commandChannel.sendMessage("Пользовательские данные авто-диктора очищены.").queue();
        LOGGER.debug("AutoNarrator userdata is cleared");

        return 465771965;
    }

    private static int executeRecorder(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long recorder = context.getArgument("narrator-recorder", Long.class);

        settings.setNarratorRecorder(recorder);
        commandChannel.sendMessage("Бот " + getUserMention(recorder) + " установлен как записывающий для дикторов.").queue();
        LOGGER.debug("Bot {} is set as AutoNarrator recorder", recorder);

        return 724812586;
    }

    private static int executeRole(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final long narratorRole = context.getArgument("narrator-role", Long.class);

        settings.setNarratorRole(narratorRole);
        commandChannel.sendMessage("Роль " + getRoleMention(narratorRole) + " установлена как роль диктора.").queue();
        LOGGER.debug("Role {} is set as AutoNarrator role", narratorRole);

        return 685827031;
    }

    private static int executeAudience(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();
        final int minAudience = context.getArgument("narrator-min-audience", Integer.class);

        settings.setNarratorMinAudience(minAudience);
        commandChannel.sendMessage("Минимальная аудитория для диктора установлена на " + minAudience + ".").queue();
        LOGGER.debug("AutoNarrator min audience is set to {}", minAudience);

        return 884635753;
    }

    private static int executeTimeSet(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotState state = bot.getState();
        final TextChannel commandChannel = source.getChannel();
        final long user = context.getArgument("user", Long.class);
        final int seconds = context.getArgument("seconds", Integer.class);

        Narrator narrator = state.getOrCreateNarrator(user);
        narrator.setTime(seconds);

        commandChannel.sendMessage("Время начитки " + getUserMention(user) + " установлено на " + seconds + " секунд.").queue();
        LOGGER.debug("AutoNarrator user {} time set to {}", user, seconds);

        return 609392665;
    }

    private static int executeTimeAdd(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotState state = bot.getState();
        final TextChannel commandChannel = source.getChannel();
        final long user = context.getArgument("user", Long.class);
        final int seconds = context.getArgument("seconds", Integer.class);

        Narrator narrator = state.getOrCreateNarrator(user);
        narrator.addTime(seconds);

        commandChannel.sendMessage(getUserMention(user) + " добавлено " + seconds + " секунд времени начитки. Итого пользователь \"начитал\" " + narrator.getTime() + " секунд.").queue();
        LOGGER.debug("AutoNarrator user {} added {} seconds to time", user, seconds);

        return 25100837;
    }

    private static int executeLastNarration(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotState state = bot.getState();
        final TextChannel commandChannel = source.getChannel();
        final long user = context.getArgument("user", Long.class);
        final long lastNarration = context.getArgument("last-narration", Long.class);

        Narrator narrator = state.getOrCreateNarrator(user);
        narrator.setLastNarration(lastNarration);
        narrator.checkRoles(bot);

        commandChannel.sendMessage("Дата последней активности диктора " + getUserMention(user) + " установлена как " + ZonedDateTime.ofInstant(Instant.ofEpochSecond(lastNarration), ZoneOffset.UTC)).queue();
        LOGGER.debug("Narrator's {} last narration set to {}", user, lastNarration);

        return 767868727;
    }

    private static int executeUserStatus(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotState state = bot.getState();
        final TextChannel commandChannel = source.getChannel();
        final long user = context.getArgument("user", Long.class);

        Narrator narrator = state.getOrCreateNarrator(user);
        StringBuilder builder = new StringBuilder("Текущий статус ").append(getUserMention(user)).append(":");
        builder.append("\nВремя начитки: ").append(narrator.getTime()).append(" секунд");

        Instant instant = Instant.ofEpochSecond(narrator.getLastNarration());
        builder.append("\nПоследняя начитка: ").append(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC));

        commandChannel.sendMessage(builder.toString()).queue();
        LOGGER.debug("Sent user {} status", user);

        return 981234476;
    }

    private static int executeStatus(CommandContext<CommandSource> context)
    {
        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotSettings settings = bot.getSettings();
        final TextChannel commandChannel = source.getChannel();

        StringBuilder builder = new StringBuilder("Текущие настройки авто-диктора:");
        builder.append("\nСтатус: ").append((settings.isNarratorEnabled() ? "Включен" : "Выключен"));
        builder.append("\nЗаписывающий-бот: ").append(getUserMention(settings.getNarratorRecorder()));
        builder.append("\nРоль: ").append(getRoleMention(settings.getNarratorRole()));
        builder.append("\nМин. аудитория: ").append(settings.getNarratorMinAudience());
        builder.append("\nРоль активна: ").append(settings.getNarratorActiveTime()).append(" секунд");

        commandChannel.sendMessage(builder.toString()).queue();
        LOGGER.debug("Sent current AutoNarrator status");

        return 226450415;
    }

    private static int executeTop(CommandContext<CommandSource> context, int max)
    {
        if (max < 1) throw new IllegalArgumentException("Can't show less than 1");

        final CommandSource source = context.getSource();
        final FanficThreadBot bot = source.getBot();
        final BotState state = bot.getState();
        final TextChannel commandChannel = source.getChannel();
        final Member member = source.getMember();
        final long userId = member.getUser().getIdLong();

        List<Narrator> narrators = new ArrayList<>(state.getNarrators());
        if (narrators.isEmpty())
        {
            commandChannel.sendMessage("Дикторов нет.").queue();
            LOGGER.debug("Tried to sent narrator top but no active narrators found");
            return 55772174;
        }

        narrators.sort((a, b) -> -Integer.compare(a.getTime(), b.getTime()));
        Narrator userNarrator = state.getNarrator(userId);
        int placeOfUser = userNarrator != null ? narrators.indexOf(userNarrator) : -1;

        Random random = new Random();
        final float hue = random.nextFloat();
        final float saturation = (random.nextInt(10000) + 7000) / 10000f;
        final Color color = Color.getHSBColor(hue, saturation, 1.0f);

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(color)
                .setAuthor("Топ дикторов гильдии")
                .setDescription("\n:trophy:  **|**  Диктор  **|**  Часов начитано  **|**  Последняя зачитка\n");

        String[] placeEmojis = new String[]
                {
                        ":first_place:",
                        ":second_place:",
                        ":third_place:",
                        ":four:",
                        ":five:",
                        ":six:",
                        ":seven:",
                        ":eight:",
                        ":nine:",
                        ":keycap_ten:"
                };
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

        for (int i = 0; i < narrators.size() && i < max; i++)
        {
            Narrator narrator = narrators.get(i);
            builder
                    .appendDescription("\n")
                    .appendDescription(max <= 10 ? placeEmojis[i] : Integer.toString(i + 1))
                    .appendDescription("  **|**  ")
                    .appendDescription(getUserNicknameMention(narrator.getId()))
                    .appendDescription("  **|**  ")
                    .appendDescription(String.format(Locale.US, "%.1f", narrator.getTime() / 3600.0f))
                    .appendDescription("  **|**  ")
                    .appendDescription(formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(narrator.getLastNarration()), ZoneOffset.UTC)));
            if (i == placeOfUser) builder.appendDescription(" <- **ты!**");
        }
        if (placeOfUser > max)
        {
            builder
                    .appendDescription("\n**• • •**\n")
                    .appendDescription(Integer.toString(placeOfUser))
                    .appendDescription("  **|**  ")
                    .appendDescription(getUserNicknameMention(userNarrator.getId()))
                    .appendDescription("  **|**  ")
                    .appendDescription(String.format(Locale.US, "%.1f", userNarrator.getTime() / 3600.0f))
                    .appendDescription("  **|**  ")
                    .appendDescription(formatter.format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(userNarrator.getLastNarration()), ZoneOffset.UTC)));
        }

        commandChannel.sendMessage(builder.build()).queue();
        LOGGER.debug("Sent narrator top");

        return 480121253;
    }
}
