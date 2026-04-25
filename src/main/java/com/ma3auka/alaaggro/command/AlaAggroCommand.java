package com.ma3auka.alaaggro.command;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.ma3auka.alaaggro.AlaAggro;
import com.ma3auka.alaaggro.config.AlaAggroConfig;
import com.ma3auka.alaaggro.event.MobAggroEventHandler;
import com.ma3auka.alaaggro.util.AggroConfigCache;
import com.ma3auka.alaaggro.util.BossGuard;
import com.ma3auka.alaaggro.util.ExemptRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = AlaAggro.MODID)
public final class AlaAggroCommand {
    private AlaAggroCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("alaaggro")
                .requires(s -> s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("reload").executes(AlaAggroCommand::cmdReload))
                .then(Commands.literal("status").executes(AlaAggroCommand::cmdStatus))
                .then(Commands.literal("info").executes(AlaAggroCommand::cmdInfo))
                .then(Commands.literal("toggle").executes(AlaAggroCommand::cmdToggle))
                .then(Commands.literal("set")
                        .then(Commands.literal("damage")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.1, 10.0))
                                        .executes(ctx -> setDouble(ctx, AlaAggroConfig.DAMAGE_MULTIPLIER, "damage",
                                                DoubleArgumentType.getDouble(ctx, "value")))))
                        .then(Commands.literal("speed")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.1, 3.0))
                                        .executes(ctx -> setDouble(ctx, AlaAggroConfig.SPEED_MULTIPLIER, "speed",
                                                DoubleArgumentType.getDouble(ctx, "value")))))
                        .then(Commands.literal("callforhelp")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> setBool(ctx, AlaAggroConfig.CALL_FOR_HELP, "callforhelp",
                                                BoolArgumentType.getBool(ctx, "value")))))
                        .then(Commands.literal("memory")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> setBool(ctx, AlaAggroConfig.LONG_TERM_MEMORY, "memory",
                                                BoolArgumentType.getBool(ctx, "value")))))
                        .then(Commands.literal("villagers")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> setBool(ctx, AlaAggroConfig.HOSTILE_VILLAGERS, "villagers",
                                                BoolArgumentType.getBool(ctx, "value")))))
                        .then(Commands.literal("reactive")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> setBool(ctx, AlaAggroConfig.REACTIVE_ONLY, "reactive",
                                                BoolArgumentType.getBool(ctx, "value"))))))
                .then(Commands.literal("exempt")
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .executes(ctx -> exempt(ctx, true))))
                .then(Commands.literal("unexempt")
                        .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                                .executes(ctx -> exempt(ctx, false))));

        d.register(root);
    }

    // ---------- handlers ----------

    private static int cmdReload(CommandContext<CommandSourceStack> ctx) {
        AggroConfigCache.rebuild();
        AggroConfigCache.Snapshot s = AggroConfigCache.get();
        int affected = applyToLoadedMobs(ctx.getSource().getServer(), s);
        ctx.getSource().sendSuccess(() -> tr("commands.alaaggro.reload.success", affected), true);
        return 1;
    }

    private static int cmdStatus(CommandContext<CommandSourceStack> ctx) {
        AggroConfigCache.Snapshot s = AggroConfigCache.get();
        Map<MobCategory, Integer> stats = countActiveByCategory(ctx.getSource().getServer());

        send(ctx, tr("commands.alaaggro.status.header",
                modVersion(), bool(s.enabled()), bool(s.reactiveOnly()))
                .withStyle(ChatFormatting.GOLD));
        send(ctx, tr("commands.alaaggro.status.numbers",
                fmt(s.damageMultiplier()), fmt(s.speedMultiplier()), fmt(s.perCategorySpeedCap())));
        send(ctx, tr("commands.alaaggro.status.helpmem",
                bool(s.callForHelp()), s.callForHelpRadius(), bool(s.longTermMemory())));
        send(ctx, tr("commands.alaaggro.status.villagepanic",
                bool(s.hostileVillagers()), bool(s.removePanicGoal())));
        send(ctx, tr("commands.alaaggro.status.lists",
                Integer.toString(s.dimensionBlacklist().size()),
                Integer.toString(ExemptRegistry.view().size())));
        send(ctx, tr("commands.alaaggro.status.divider").withStyle(ChatFormatting.DARK_GRAY));
        for (Map.Entry<MobCategory, Integer> e : stats.entrySet()) {
            send(ctx, Component.literal(e.getKey().getName() + ": " + e.getValue()));
        }
        return 1;
    }

    private static int cmdInfo(CommandContext<CommandSourceStack> ctx) {
        AggroConfigCache.Snapshot s = AggroConfigCache.get();
        send(ctx, tr("commands.alaaggro.info.line",
                modVersion(), bool(s.enabled()), bool(s.reactiveOnly())));
        return 1;
    }

    private static int cmdToggle(CommandContext<CommandSourceStack> ctx) {
        boolean newVal = !AlaAggroConfig.ENABLED.get();
        AlaAggroConfig.ENABLED.set(newVal);
        AggroConfigCache.rebuild();
        ctx.getSource().sendSuccess(() -> tr("commands.alaaggro.toggle.set", bool(newVal)), true);
        return 1;
    }

    private static int setDouble(CommandContext<CommandSourceStack> ctx,
                                 ModConfigSpec.DoubleValue cfg, String label, double value) {
        cfg.set(value);
        AggroConfigCache.rebuild();
        ctx.getSource().sendSuccess(() -> tr("commands.alaaggro.set.ok", label, fmt(value)), true);
        return 1;
    }

    private static int setBool(CommandContext<CommandSourceStack> ctx,
                               ModConfigSpec.BooleanValue cfg, String label, boolean value) {
        cfg.set(value);
        AggroConfigCache.rebuild();
        ctx.getSource().sendSuccess(() -> tr("commands.alaaggro.set.ok", label, bool(value)), true);
        return 1;
    }

    private static int exempt(CommandContext<CommandSourceStack> ctx, boolean add) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var profiles = GameProfileArgument.getGameProfiles(ctx, "targets");
        int affected = 0;
        for (var profile : profiles) {
            UUID id = profile.id();
            if (id == null) continue;
            boolean changed = add ? ExemptRegistry.add(id) : ExemptRegistry.remove(id);
            if (changed) affected++;
            String name = profile.name();
            if (add) {
                ctx.getSource().sendSuccess(() -> tr("commands.alaaggro.exempt.added", name), true);
            } else {
                ctx.getSource().sendSuccess(() -> tr("commands.alaaggro.exempt.removed", name), true);
            }
        }
        return affected;
    }

    // ---------- helpers ----------

    private static int applyToLoadedMobs(MinecraftServer server, AggroConfigCache.Snapshot s) {
        if (server == null) return 0;
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob)) continue;
                if (BossGuard.isBoss(mob)) continue;
                MobAggroEventHandler.injectAggro(mob, s);
                count++;
            }
        }
        return count;
    }

    private static Map<MobCategory, Integer> countActiveByCategory(MinecraftServer server) {
        Map<MobCategory, Integer> out = new TreeMap<>((a, b) -> a.getName().compareTo(b.getName()));
        if (server == null) return out;
        for (ServerLevel level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (!(entity instanceof Mob mob)) continue;
                if (BossGuard.isBoss(mob)) continue;
                if (mob.getTarget() == null) continue;
                out.merge(mob.getType().getCategory(), 1, Integer::sum);
            }
        }
        return out;
    }

    private static MutableComponent tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private static void send(CommandContext<CommandSourceStack> ctx, Component msg) {
        ctx.getSource().sendSuccess(() -> msg, false);
    }

    private static String bool(boolean b) {
        return b ? "ON" : "OFF";
    }

    private static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.2f", d);
    }

    private static String modVersion() {
        var container = net.neoforged.fml.ModList.get().getModContainerById(AlaAggro.MODID);
        return container.map(c -> c.getModInfo().getVersion().toString()).orElse("?");
    }
}
