package com.ma3auka.alaaggro.command;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.ai.AggroMarkerGoal;
import com.ma3auka.alaaggro.core.AggroConfig;
import com.ma3auka.alaaggro.core.AggroSettings;
import com.ma3auka.alaaggro.core.ConfigOption;
import com.ma3auka.alaaggro.core.ExemptRegistry;
import com.ma3auka.alaaggro.handler.AggroHandlers;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

/**
 * {@code /alaaggro} — the operator's controls. The whole command tree is loader-neutral Brigadier;
 * each loader only has to hand it a dispatcher.
 */
public final class AlaAggroCommand {

    private AlaAggroCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("alaaggro")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("reload").executes(AlaAggroCommand::reload))
                .then(Commands.literal("status").executes(AlaAggroCommand::status))
                .then(Commands.literal("info").executes(AlaAggroCommand::info))
                .then(Commands.literal("toggle").executes(AlaAggroCommand::toggle))
                .then(Commands.literal("set")
                        .then(doubleOption("damage", ConfigOption.DAMAGE_MULTIPLIER))
                        .then(doubleOption("speed", ConfigOption.SPEED_MULTIPLIER))
                        .then(boolOption("callforhelp", ConfigOption.CALL_FOR_HELP))
                        .then(boolOption("memory", ConfigOption.LONG_TERM_MEMORY))
                        .then(boolOption("villagers", ConfigOption.HOSTILE_VILLAGERS))
                        .then(boolOption("reactive", ConfigOption.REACTIVE_ONLY))
                        .then(boolOption("tamed", ConfigOption.EXCLUDE_TAMED))
                        .then(boolOption("babies", ConfigOption.EXCLUDE_BABIES))
                        .then(boolOption("named", ConfigOption.EXCLUDE_NAMED)))
                .then(Commands.literal("exempt")
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .executes(context -> exempt(context, true))))
                .then(Commands.literal("unexempt")
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .executes(context -> exempt(context, false))));

        dispatcher.register(root);
    }

    // -------------------------------------------------------------- builders

    private static LiteralArgumentBuilder<CommandSourceStack> doubleOption(String label, ConfigOption option) {
        return Commands.literal(label)
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(option.min, option.max))
                        .executes(context -> {
                            double value = DoubleArgumentType.getDouble(context, "value");
                            AggroConfig.set(option, value);
                            context.getSource().sendSuccess(
                                    () -> translate("commands.alaaggro.set.ok", label, format(value)), true);
                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> boolOption(String label, ConfigOption option) {
        return Commands.literal(label)
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean value = BoolArgumentType.getBool(context, "value");
                            AggroConfig.set(option, value);
                            context.getSource().sendSuccess(
                                    () -> translate("commands.alaaggro.set.ok", label, onOff(value)), true);
                            return 1;
                        }));
    }

    // -------------------------------------------------------------- handlers

    private static int reload(CommandContext<CommandSourceStack> context) {
        AggroConfig.reload();
        MinecraftServer server = context.getSource().getServer();
        // Honour the master switch: reloading while the mod is off must calm mobs, not re-aggro them.
        int affected = AggroConfig.get().enabled()
                ? AggroHandlers.applyToLoadedMobs(server)
                : AggroHandlers.pacifyLoadedMobs(server);
        context.getSource().sendSuccess(() -> translate("commands.alaaggro.reload.success", affected), true);
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> context) {
        boolean enabled = !AggroConfig.get().enabled();
        AggroConfig.set(ConfigOption.ENABLED, enabled);

        MinecraftServer server = context.getSource().getServer();
        if (enabled) {
            AggroHandlers.applyToLoadedMobs(server);
        } else {
            AggroHandlers.pacifyLoadedMobs(server);
        }
        context.getSource().sendSuccess(() -> translate("commands.alaaggro.toggle.set", onOff(enabled)), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        AggroSettings settings = AggroConfig.get();
        Map<MobCategory, Integer> counts = countAggroMobs(context.getSource().getServer());

        send(context, translate("commands.alaaggro.status.header",
                AlaAggro.version(), onOff(settings.enabled()), onOff(settings.reactiveOnly()))
                .withStyle(ChatFormatting.GOLD));
        send(context, translate("commands.alaaggro.status.numbers",
                format(settings.damageMultiplier()), format(settings.speedMultiplier()),
                format(settings.perCategorySpeedCap())));
        send(context, translate("commands.alaaggro.status.helpmem",
                onOff(settings.callForHelp()), settings.callForHelpRadius(), onOff(settings.longTermMemory())));
        send(context, translate("commands.alaaggro.status.protection",
                onOff(settings.hostileVillagers()), onOff(settings.excludeTamed()),
                onOff(settings.excludeBabies()), onOff(settings.excludeNamed())));
        send(context, translate("commands.alaaggro.status.lists",
                Integer.toString(settings.dimensionBlacklist().size()),
                Integer.toString(ExemptRegistry.view().size())));
        send(context, translate("commands.alaaggro.status.divider").withStyle(ChatFormatting.DARK_GRAY));
        for (Map.Entry<MobCategory, Integer> entry : counts.entrySet()) {
            send(context, Component.literal(entry.getKey().getName() + ": " + entry.getValue()));
        }
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        AggroSettings settings = AggroConfig.get();
        send(context, translate("commands.alaaggro.info.line",
                AlaAggro.version(), onOff(settings.enabled()), onOff(settings.reactiveOnly())));
        return 1;
    }

    private static int exempt(CommandContext<CommandSourceStack> context, boolean add)
            throws CommandSyntaxException {
        var profiles = GameProfileArgument.getGameProfiles(context, "targets");
        int affected = 0;
        for (var profile : profiles) {
            UUID id = profile.id();
            if (id == null) continue;
            boolean changed = add ? ExemptRegistry.add(id) : ExemptRegistry.remove(id);
            if (changed) affected++;
            String name = profile.name();
            String key = add ? "commands.alaaggro.exempt.added" : "commands.alaaggro.exempt.removed";
            context.getSource().sendSuccess(() -> translate(key, name), true);
        }
        return affected;
    }

    // --------------------------------------------------------------- helpers

    /** Counts mobs the mod has actually converted and which are currently hunting something. */
    private static Map<MobCategory, Integer> countAggroMobs(MinecraftServer server) {
        Map<MobCategory, Integer> counts = new TreeMap<>(
                (left, right) -> left.getName().compareTo(right.getName()));
        if (server == null) return counts;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob)) continue;
                if (mob.getTarget() == null) continue;
                if (AggroMarkerGoal.of(mob) == null) continue;
                counts.merge(mob.getType().getCategory(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static MutableComponent translate(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private static void send(CommandContext<CommandSourceStack> context, Component message) {
        context.getSource().sendSuccess(() -> message, false);
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
