package drzhark.mocreatures.spawn;

import dev.architectury.event.events.common.TickEvent;
import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.entity.IMoCEntity;
import drzhark.mocreatures.entity.MoCAquatic;
import drzhark.mocreatures.registry.MoCSpawns;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mo'Creatures' own spawn pool for its sea life (fish, sharks, rays, jellyfish, dolphins), used unless the config puts
 * them in vanilla's pools ({@code aquaticPool}, {@link MoCSpawns#aquaticsInOwnPool}).
 *
 * <p>In the own pool the sea creatures are in no biome spawn list and register with the MISC mob category, which
 * vanilla's natural spawner neither spawns nor counts against any mob cap. So they never take a spawn from vanilla's or
 * another mod's sea mobs, and these never take one from them (a player report, 2026-10-12: in an ocean the Mo'Creatures
 * fish were 74% of the sea-creature pool and filled its cap of 5, and Alex's Mobs' sea mobs barely spawned). Instead,
 * once a second, this keeps up to {@code aquaticsPerPlayer} wild ones within {@value #NEAR} blocks of each player:
 * one group per player per run, in water {@value #MIN_DISTANCE}-{@value #MAX_DISTANCE} blocks away, picked by the same
 * per-biome weights and group sizes as before ({@code spawn.<id>} settings included), under the same placement rules.
 * They despawn like any sea creature once no player is near, and {@code maxWaterMobs} still caps a whole dimension.</p>
 */
public final class MoCAquaticSpawner {

    private static final int INTERVAL_TICKS = 20;
    /** Radius around a player in which its wild sea creatures are counted. */
    private static final int NEAR = 64;
    /** Groups start this far from every player (vanilla spawns nothing closer than 24), and at most this far. */
    private static final int MIN_DISTANCE = 24;
    private static final int MAX_DISTANCE = 56;

    private MoCAquaticSpawner() {
    }

    public static void register() {
        TickEvent.SERVER_LEVEL_POST.register(MoCAquaticSpawner::onLevelTick);
    }

    private static void onLevelTick(ServerLevel level) {
        if (level.getGameTime() % INTERVAL_TICKS != 0 || !MoCSpawns.aquaticsInOwnPool() || level.players().isEmpty()
                || !level.getGameRules().get(GameRules.SPAWN_MOBS)) {
            return;
        }
        MoCConfig cfg = MoCConfig.get();
        if (cfg.aquaticsPerPlayer <= 0) {
            return;
        }
        List<Entity> wild = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof MoCAquatic && isWild(e)) {
                wild.add(e);
            }
        }
        int roomInLevel = cfg.maxWaterMobs > 0 ? cfg.maxWaterMobs - wild.size() : Integer.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            if (roomInLevel <= 0) {
                return;
            }
            if (player.isSpectator()) {
                continue;
            }
            int near = 0;
            for (Entity e : wild) {
                if (e.distanceToSqr(player) < NEAR * NEAR) {
                    near++;
                }
            }
            int room = Math.min(cfg.aquaticsPerPlayer - near, roomInLevel);
            if (room > 0) {
                roomInLevel -= spawnGroup(level, player, cfg, room);
            }
        }
    }

    /** A sea creature this pool keeps count of: untamed, not kept (named, leashed, spawn egg...), not a pet's baby. */
    private static boolean isWild(Entity e) {
        return e instanceof IMoCEntity moc && !moc.getIsTamed() && e instanceof Mob mob
                && !mob.isPersistenceRequired() && !mob.requiresCustomPersistence();
    }

    /** One try at a group of one species in water near {@code player}; returns how many spawned. */
    private static int spawnGroup(ServerLevel level, ServerPlayer player, MoCConfig cfg, int room) {
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        // the water column there: from the sea floor up to the surface
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (surface - floor < 2) {
            return 0;
        }
        BlockPos pos = new BlockPos(x, floor + random.nextInt(surface - floor), z);
        if (!level.isPositionEntityTicking(pos) || !level.getFluidState(pos).is(FluidTags.WATER)
                || tooCloseToAPlayer(level, pos)) {
            return 0;
        }
        Holder<Biome> biome = level.getBiome(pos);
        if (!biome.is(BiomeTags.IS_OVERWORLD)) {
            return 0;
        }
        Set<String> types = MoCBiomeTypes.of(
                biome.unwrapKey().map(net.minecraft.resources.ResourceKey::identifier).orElse(null), biome::is);

        // the species, by its weight in this biome (0 where it does not spawn or is switched off)
        int total = 0;
        int[] weights = new int[MoCSpawns.AQUATICS.size()];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = Math.max(0, cfg.spawnFrequency(id(MoCSpawns.AQUATICS.get(i).get()), types));
            total += weights[i];
        }
        if (total <= 0) {
            return 0;
        }
        int roll = random.nextInt(total);
        int pick = 0;
        while (roll >= weights[pick]) {
            roll -= weights[pick++];
        }
        EntityType<?> type = MoCSpawns.AQUATICS.get(pick).get();
        String id = id(type);
        int min = Math.max(1, cfg.spawnMin(id, types));
        int max = Math.max(min, cfg.spawnMax(id, types));
        int count = Math.min(room, min + random.nextInt(max - min + 1));

        int spawned = 0;
        for (int i = 0; i < count * 3 && spawned < count; i++) {
            BlockPos at = i == 0 ? pos : pos.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
            if (!level.getFluidState(at).is(FluidTags.WATER)
                    || !SpawnPlacements.isSpawnPositionOk(type, level, at)
                    || !SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, at, random)) {
                continue;
            }
            Entity entity = type.create(level, EntitySpawnReason.NATURAL);
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            mob.snapTo(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            if (!mob.checkSpawnRules(level, EntitySpawnReason.NATURAL) || !mob.checkSpawnObstruction(level)) {
                mob.discard();
                continue;
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.NATURAL, null);
            level.addFreshEntityWithPassengers(mob);
            spawned++;
        }
        return spawned;
    }

    private static boolean tooCloseToAPlayer(ServerLevel level, BlockPos pos) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D)
                    < MIN_DISTANCE * MIN_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    private static String id(EntityType<?> type) {
        return EntityType.getKey(type).getPath();
    }
}
