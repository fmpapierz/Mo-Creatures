package drzhark.mocreatures.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Locale;

import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.entity.IMoCEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

/**
 * Cross-loader {@code /moc} admin command for the Mo'Creatures 26.2 Architectury port.
 *
 * <p>This is a focused modern (brigadier) equivalent of the legacy
 * {@code drzhark.mocreatures.command.CommandMoCreatures}. The legacy command edited a bespoke
 * {@code MoCProperties.cfg} through ~40 subcommands. The 26.2 port stores config in a mutable
 * {@link MoCConfig} singleton (fields {@code public}, non-final), so the surface here is deliberately
 * smaller and split into:
 * <ul>
 *   <li>{@code /moc reload}                       &ndash; re-read {@code mocreatures.properties}.</li>
 *   <li>{@code /moc spawn <entityId> [count]}     &ndash; summon Mo'Creatures mobs at the source.</li>
 *   <li>{@code /moc count}                        &ndash; count loaded Mo'Creatures entities.</li>
 *   <li>{@code /moc config <flag> <true|false>}   &ndash; view, or flip <em>and persist</em>, a
 *       boolean flag (via {@link MoCConfig#setFlag(String, boolean)} + {@link MoCConfig#save()}).</li>
 * </ul>
 *
 * <p>All nodes require permission level 2 (op), matching the legacy
 * {@code getRequiredPermissionLevel()}.
 */
public final class MoCCommand {

    private MoCCommand() {}

    /** Registers the whole {@code /moc} tree on the given dispatcher. */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("moc")
                // Permission level 2 (op / gamemaster) -- the 26.2 permission model replaced the
                // legacy int hasPermission(2) with a Permission-based check. COMMANDS_GAMEMASTER is
                // the level-2 equivalent used by vanilla command families like /gamemode.
                .requires(Commands.hasPermission(new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER)))
                // /moc reload
                .then(Commands.literal("reload")
                    .executes(MoCCommand::reload))
                // /moc count
                .then(Commands.literal("count")
                    .executes(MoCCommand::count))
                // /moc spawn <entityId> [count]
                .then(Commands.literal("spawn")
                    .then(Commands.argument("entityId", StringArgumentType.word())
                        .executes(ctx -> spawn(ctx, 1))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 128))
                            .executes(ctx -> spawn(ctx, IntegerArgumentType.getInteger(ctx, "count"))))))
                // /moc config <flag> <true|false>
                .then(Commands.literal("config")
                    .then(Commands.argument("flag", StringArgumentType.word())
                        .executes(MoCCommand::configShow)
                        .then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                            .executes(ctx -> configSet(ctx,
                                    com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "value"))))))
                // /moc setnumber <name> [value] -- runtime numeric tunable setter.
                // The legacy /moc set accepted ints AND doubles (e.g. /moc ogrestrength 3,
                // /moc zebrachance 5, /moc maxmobs 70); this restores that for every numeric field
                // (ints round from the double). Persists to mocreatures.properties via MoCConfig.
                .then(Commands.literal("setnumber")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(MoCCommand::numberShow)
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                            .executes(ctx -> numberSet(ctx,
                                    DoubleArgumentType.getDouble(ctx, "value"))))))
                // /moc spawnrate <entityId> <frequency|min|max> <value> -- runtime per-entity spawn tuning
                // (legacy /moc <entity> frequency|min|max <n>); persists to mocreatures.properties.
                .then(Commands.literal("spawnrate")
                    .then(Commands.argument("entityId", StringArgumentType.word())
                        .then(Commands.argument("field", StringArgumentType.word())
                            .then(Commands.argument("value", IntegerArgumentType.integer(0, 1000))
                                .executes(MoCCommand::spawnRate)))))
                // /moc biomegroup <entityId> add|remove <group> -- restrict an entity's natural spawns to
                // biome groups (legacy CustomSpawner biome groups); persists, applies on next world load.
                .then(Commands.literal("biomegroup")
                    .then(Commands.argument("entityId", StringArgumentType.word())
                        .then(Commands.literal("add")
                            .then(Commands.argument("group", StringArgumentType.word())
                                .executes(ctx -> biomeGroup(ctx, true))))
                        .then(Commands.literal("remove")
                            .then(Commands.argument("group", StringArgumentType.word())
                                .executes(ctx -> biomeGroup(ctx, false))))))
                // /moc list biomegroups
                .then(Commands.literal("list")
                    .then(Commands.literal("biomegroups")
                        .executes(MoCCommand::listBiomeGroups)))
        );
    }

    // ---------------------------------------------------------------------------------------------
    // /moc reload
    // ---------------------------------------------------------------------------------------------

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        MoCConfig.load();
        ctx.getSource().sendSuccess(() -> Component.literal("Mo'Creatures config reloaded."), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    // ---------------------------------------------------------------------------------------------
    // /moc count
    // ---------------------------------------------------------------------------------------------

    private static int count(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int n = 0;
        for (Entity e : level.getAllEntities()) {
            if (e instanceof IMoCEntity) {
                n++;
            }
        }
        final int total = n;
        ctx.getSource().sendSuccess(
                () -> Component.literal("Mo'Creatures entities loaded in this level: " + total), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    // ---------------------------------------------------------------------------------------------
    // /moc spawn <entityId> [count]
    // ---------------------------------------------------------------------------------------------

    private static int spawn(CommandContext<CommandSourceStack> ctx, int count) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        String id = StringArgumentType.getString(ctx, "entityId").toLowerCase(Locale.ROOT);

        Identifier rl = Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, id);
        // ENTITY_TYPE is a DefaultedRegistry: getValue() returns minecraft:pig for an unknown id,
        // so guard explicitly with containsKey to reject bad ids instead of silently spawning a pig.
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            src.sendFailure(Component.literal(
                    "Unknown Mo'Creatures entity id '" + id + "' (expected e.g. big_cat, wyvern, ogre)."));
            return 0;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(rl);
        ServerLevel level = src.getLevel();
        Vec3 pos = src.getPosition();
        BlockPos blockPos = BlockPos.containing(pos);

        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Entity e = type.spawn(level, blockPos, EntitySpawnReason.COMMAND);
            if (e != null) {
                spawned++;
            }
        }

        final int spawnedFinal = spawned;
        src.sendSuccess(
                () -> Component.literal("Spawned " + spawnedFinal + " x mocreatures:" + id + "."), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    // ---------------------------------------------------------------------------------------------
    // /moc spawnrate <entityId> <frequency|min|max> <value>
    // ---------------------------------------------------------------------------------------------

    private static int spawnRate(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "entityId").toLowerCase(Locale.ROOT);
        String field = StringArgumentType.getString(ctx, "field").toLowerCase(Locale.ROOT);
        int value = IntegerArgumentType.getInteger(ctx, "value");
        if (!field.equals("frequency") && !field.equals("min") && !field.equals("max")) {
            ctx.getSource().sendFailure(Component.literal("Field must be one of: frequency, min, max."));
            return 0;
        }
        Identifier rl = Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, id);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown Mo'Creatures entity id '" + id + "' (e.g. big_cat, wyvern, ostrich)."));
            return 0;
        }
        MoCConfig cfg = MoCConfig.get();
        cfg.setSpawnValue(id, field, value);
        cfg.save();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Set spawn." + id + "." + field + " = " + value
                + " (saved to mocreatures.properties; applies on next world load)."), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    // ---------------------------------------------------------------------------------------------
    // /moc biomegroup <entityId> add|remove <group>   +   /moc list biomegroups
    // ---------------------------------------------------------------------------------------------

    private static int biomeGroup(CommandContext<CommandSourceStack> ctx, boolean add) {
        String id = StringArgumentType.getString(ctx, "entityId").toLowerCase(Locale.ROOT);
        String group = StringArgumentType.getString(ctx, "group").toLowerCase(Locale.ROOT);
        Identifier rl = Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, id);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            ctx.getSource().sendFailure(Component.literal("Unknown Mo'Creatures entity id '" + id + "'."));
            return 0;
        }
        MoCConfig cfg = MoCConfig.get();
        boolean ok = add ? cfg.addBiomeGroup(id, group) : cfg.removeBiomeGroup(id, group);
        if (!ok) {
            ctx.getSource().sendFailure(Component.literal(
                    (add ? "Unknown biome group '" + group + "'. " : "Group '" + group + "' was not set on " + id + ". ")
                    + "Groups: " + String.join(", ", MoCConfig.biomeGroupNames())));
            return 0;
        }
        cfg.save();
        final java.util.List<String> now = cfg.biomeGroups(id);
        ctx.getSource().sendSuccess(() -> Component.literal(
                (add ? "Added '" + group + "' to " : "Removed '" + group + "' from ") + id
                + "; biome groups now: " + (now.isEmpty() ? "(all biomes)" : String.join(", ", now))
                + " (applies on next world load)."), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static int listBiomeGroups(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Mo'Creatures biome groups: " + String.join(", ", MoCConfig.biomeGroupNames())
                + ". Restrict spawns with /moc biomegroup <entity> add|remove <group>."), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    // ---------------------------------------------------------------------------------------------
    // /moc config <flag> [true|false]
    // ---------------------------------------------------------------------------------------------

    private static int configShow(CommandContext<CommandSourceStack> ctx) {
        String flag = StringArgumentType.getString(ctx, "flag");
        Boolean current = readFlag(flag);
        if (current == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown config flag '" + flag + "'. Supported: " + SUPPORTED_FLAGS));
            return 0;
        }
        final boolean value = current;
        ctx.getSource().sendSuccess(
                () -> Component.literal("mocreatures config " + flag + " = " + value), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static int configSet(CommandContext<CommandSourceStack> ctx, boolean value) {
        String flag = StringArgumentType.getString(ctx, "flag");
        MoCConfig cfg = MoCConfig.get();
        // Mutate the live field by name, then persist the whole file so the change survives restarts.
        boolean applied = cfg.setFlag(flag, value);
        if (!applied) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown config flag '" + flag + "'. Supported: " + SUPPORTED_FLAGS));
            return 0;
        }
        cfg.save();
        // Report the actually-persisted value (re-read via readFlag so the message can't drift).
        final boolean persisted = Boolean.TRUE.equals(readFlag(flag));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Set mocreatures config " + flag + " = " + persisted
                + " (saved to mocreatures.properties)."), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    // ---------------------------------------------------------------------------------------------
    // Config flag plumbing
    //
    // MoCConfig fields are now non-final and mutable at runtime. configSet writes the field via
    // MoCConfig.get().setFlag(name, value) then MoCConfig.get().save(), so changes are applied to
    // the live singleton (downstream MoCConfig.get().<field> readers see them immediately) AND
    // persisted to mocreatures.properties. readFlag below reflects the same live singleton for
    // /moc config <flag> (view) and for the post-save confirmation message.
    // ---------------------------------------------------------------------------------------------

    private static final String SUPPORTED_FLAGS =
            "elephantBulldozer, easyBreeding, easyHarvesting, useDefaultBiomeGroups, attackDolphins, "
            + "attackWolves, attackHorses, spawnPiranhas, modifyVanillaSpawns, despawnVanilla, enableOwnership, "
            + "enableResetOwnership, staticBed, staticLitter, animateTextures, destroyDrops, "
            + "destroyPassiveDrops, displayPetName, displayPetHealth, displayPetIcons, particleFX";

    /** Reads the current live value of a supported flag, or {@code null} if the name is unknown. */
    private static Boolean readFlag(String flag) {
        if (flag == null) {
            return null;
        }
        MoCConfig cfg = MoCConfig.get();
        switch (flag.toLowerCase(Locale.ROOT)) {
            case "elephantbulldozer":    return cfg.elephantBulldozer;
            case "easybreeding":         return cfg.easyBreeding;
            case "easyharvesting":       return cfg.easyHarvesting;
            case "usedefaultbiomegroups": return cfg.useDefaultBiomeGroups;
            case "attackdolphins":       return cfg.attackDolphins;
            case "attackwolves":         return cfg.attackWolves;
            case "attackhorses":         return cfg.attackHorses;
            case "spawnpiranhas":        return cfg.spawnPiranhas;
            case "modifyvanillaspawns":  return cfg.modifyVanillaSpawns;
            case "despawnvanilla":       return cfg.despawnVanilla;
            case "enableownership":      return cfg.enableOwnership;
            case "enableresetownership": return cfg.enableResetOwnership;
            case "staticbed":            return cfg.staticBed;
            case "staticlitter":         return cfg.staticLitter;
            case "animatetextures":      return cfg.animateTextures;
            case "destroydrops":         return cfg.destroyDrops;
            case "destroypassivedrops":  return cfg.destroyPassiveDrops;
            case "displaypetname":       return cfg.displayPetName;
            case "displaypethealth":     return cfg.displayPetHealth;
            case "displaypeticons":      return cfg.displayPetIcons;
            case "particlefx":           return cfg.particleFX;
            default:                     return null;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // /moc setnumber <name> [value]
    //
    // Restores the legacy numeric /moc set (ints AND doubles). MoCConfig.setNumber(name, double)
    // mutates the live field by name (rounding int-typed fields), matching the settings GUI; save()
    // then persists the whole file. readNumber below reflects the same live singleton for the view
    // form and the post-save confirmation message.
    // ---------------------------------------------------------------------------------------------

    private static final String SUPPORTED_NUMBERS =
            "ogreStrength, caveOgreStrength, fireOgreStrength, ogreAttackRange, caveOgreChance, "
            + "fireOgreChance, sharkStrength, zebraChance, wyvernEggDropChance, monsterEggDropChance, "
            + "maxAnimals, maxMobs, maxWaterMobs, maxAmbient, maxTamed, maxOPTamed";

    private static int numberShow(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        Double current = readNumber(name);
        if (current == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown numeric setting '" + name + "'. Supported: " + SUPPORTED_NUMBERS));
            return 0;
        }
        final double value = current;
        ctx.getSource().sendSuccess(
                () -> Component.literal("mocreatures config " + name + " = " + formatNumber(value)), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static int numberSet(CommandContext<CommandSourceStack> ctx, double value) {
        String name = StringArgumentType.getString(ctx, "name");
        MoCConfig cfg = MoCConfig.get();
        // Mutate the live field by name (int-typed fields round), then persist the whole file so
        // the change survives restarts. This is the same setter the settings GUI uses.
        boolean applied = cfg.setNumber(name, value);
        if (!applied) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown numeric setting '" + name + "'. Supported: " + SUPPORTED_NUMBERS));
            return 0;
        }
        cfg.save();
        // Report the actually-persisted value (re-read via readNumber so the message can't drift,
        // and so it reflects any rounding applied to an int-typed field).
        final Double persisted = readNumber(name);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Set mocreatures config " + name + " = "
                + (persisted == null ? formatNumber(value) : formatNumber(persisted))
                + " (saved to mocreatures.properties)."), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    /** Formats a numeric value: integer-valued doubles print without a trailing ".0". */
    private static String formatNumber(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    /** Reads the current live value of a supported numeric setting, or {@code null} if unknown. */
    private static Double readNumber(String name) {
        if (name == null) {
            return null;
        }
        MoCConfig cfg = MoCConfig.get();
        switch (name.toLowerCase(Locale.ROOT)) {
            case "ogrestrength":         return cfg.ogreStrength;
            case "caveogrestrength":     return cfg.caveOgreStrength;
            case "fireogrestrength":     return cfg.fireOgreStrength;
            case "ogreattackrange":      return (double) cfg.ogreAttackRange;
            case "caveogrechance":       return (double) cfg.caveOgreChance;
            case "fireogrechance":       return (double) cfg.fireOgreChance;
            case "sharkstrength":        return (double) cfg.sharkStrength;
            case "zebrachance":          return (double) cfg.zebraChance;
            case "wyverneggdropchance":  return (double) cfg.wyvernEggDropChance;
            case "monstereggdropchance": return (double) cfg.monsterEggDropChance;
            case "maxanimals":           return (double) cfg.maxAnimals;
            case "maxmobs":              return (double) cfg.maxMobs;
            case "maxwatermobs":         return (double) cfg.maxWaterMobs;
            case "maxambient":           return (double) cfg.maxAmbient;
            case "maxtamed":             return (double) cfg.maxTamed;
            case "maxoptamed":           return (double) cfg.maxOPTamed;
            default:                     return null;
        }
    }
}
