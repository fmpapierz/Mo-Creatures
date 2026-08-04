package drzhark.mocreatures.spawn;

import dev.architectury.event.events.common.TickEvent;
import drzhark.mocreatures.MoCreatures;
import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.entity.IMoCEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side mob-cap enforcer for Mo'Creatures.
 *
 * <p>The legacy 1.12.2 mod limited its own spawns through a bespoke {@code CustomSpawner}
 * (maxAnimals=90 / maxMobs=70 / maxWaterMobs=30 / maxAmbient=20). The Architectury port spawns
 * creatures through vanilla's natural-spawn pipeline, so those config caps had no teeth. This class
 * restores them by periodically counting loaded {@link IMoCEntity} instances per {@link ServerLevel},
 * bucketed by their {@link MobCategory}, and discarding the excess.</p>
 *
 * <p>Caps map to config fields as follows:</p>
 * <ul>
 *   <li>{@link MobCategory#CREATURE} &rarr; {@link MoCConfig#maxAnimals}</li>
 *   <li>{@link MobCategory#MONSTER} &rarr; {@link MoCConfig#maxMobs}</li>
 *   <li>{@link MobCategory#WATER_CREATURE} + {@link MobCategory#UNDERGROUND_WATER_CREATURE}
 *       &rarr; {@link MoCConfig#maxWaterMobs}</li>
 *   <li>{@link MobCategory#AMBIENT} &rarr; {@link MoCConfig#maxAmbient}</li>
 * </ul>
 *
 * <p>Enforcement is deliberately conservative: only <em>untamed</em>, non-persistent, un-named,
 * un-ridden creatures are ever removed, and within a category the ones farthest from any player are
 * culled first (so nearby, visible creatures are the last to go). Tamed / named / ridden creatures
 * and creatures whose {@code requiresCustomPersistence()} is set are never touched. If a config cap
 * is {@code <= 0} it is treated as "no limit" for that category.</p>
 *
 * <p>Register once from {@code MoCreatures.init()} via {@link #register()}.</p>
 */
public final class MoCMobCap {

    /** How many server-level ticks between cap sweeps. */
    private static final int INTERVAL_TICKS = 200;

    /**
     * Per-level throttle counters, so every {@link ServerLevel} is swept once per {@link #INTERVAL_TICKS}.
     *
     * <p>This was previously a single shared {@code static int} incremented on every level's tick. That is not
     * merely "spreading the sweeps out": with N levels ticking per server tick, the counter reaches
     * INTERVAL_TICKS on a fixed position in the rotation, so the sweep always lands on the same level and every
     * other level is never swept at all. A stock install has four levels (overworld, nether, end and the mod's
     * own {@code mocreatures:wyvern_lair}), and 200 mod 4 == 0, so the overworld — the one level that matters —
     * was never reached. Only accessed from the server thread, so a plain HashMap is safe.</p>
     */
    private static final java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>, Integer>
            TICK_COUNTERS = new java.util.HashMap<>();

    private MoCMobCap() {
    }

    /**
     * Hooks Architectury's per-{@link ServerLevel} post-tick event and enforces the config caps
     * every {@link #INTERVAL_TICKS} ticks. Call once during common init.
     */
    public static void register() {
        TickEvent.SERVER_LEVEL_POST.register(MoCMobCap::onLevelTick);
    }

    private static void onLevelTick(ServerLevel level) {
        // Throttle per level: each ServerLevel gets its own counter, so every level is swept once per interval.
        int ticks = TICK_COUNTERS.merge(level.dimension(), 1, Integer::sum);
        if (ticks < INTERVAL_TICKS) {
            return;
        }
        TICK_COUNTERS.put(level.dimension(), 0);

        MoCConfig cfg = MoCConfig.get();

        // Collect removable (untamed / non-persistent / un-named / un-ridden) Mo'Creatures entities
        // per category. Tamed / named / ridden / persistence-required creatures are excluded up front
        // so they can never be counted toward the cap or removed.
        List<Entity> creatures = null;
        List<Entity> monsters = null;
        List<Entity> water = null;
        List<Entity> ambient = null;

        for (Entity e : level.getAllEntities()) {
            if (!(e instanceof IMoCEntity moc)) {
                continue;
            }
            if (!isRemovable(e, moc)) {
                continue;
            }
            switch (e.getType().getCategory()) {
                case CREATURE -> (creatures != null ? creatures : (creatures = new ArrayList<>())).add(e);
                case MONSTER -> (monsters != null ? monsters : (monsters = new ArrayList<>())).add(e);
                case WATER_CREATURE, UNDERGROUND_WATER_CREATURE ->
                        (water != null ? water : (water = new ArrayList<>())).add(e);
                case AMBIENT -> (ambient != null ? ambient : (ambient = new ArrayList<>())).add(e);
                default -> {
                    // WATER_AMBIENT / MISC etc. are not managed by Mo'Creatures caps.
                }
            }
        }

        List<ServerPlayer> players = level.players();
        enforce(creatures, cfg.maxAnimals, players);
        enforce(monsters, cfg.maxMobs, players);
        enforce(water, cfg.maxWaterMobs, players);
        enforce(ambient, cfg.maxAmbient, players);

        // Legacy despawnVanillaAnimals: when Mo'Creatures manages spawns, thin out distant vanilla farm
        // animals so the world has room for Mo'Creatures fauna. Legacy gated this on a SEPARATE
        // DespawnVanilla toggle (both default true) in addition to modifyVanillaSpawns.
        if (cfg.modifyVanillaSpawns && cfg.despawnVanilla) {
            despawnVanillaAnimals(level, players);
        }
    }

    /**
     * Culls distant, unremarkable vanilla farm animals (cow/sheep/pig/chicken/squid/untamed wolf).
     *
     * <p>{@code discard()} is silent and permanent — no death, no drops, no message — and vanilla
     * {@code Animal.removeWhenFarAway} is hard-coded to {@code false}, so every removal here is one vanilla
     * would never have made. {@code level.getAllEntities()} reaches out to the whole loaded/tracked area, not
     * just the spawn radius, so an unguarded sweep wipes penned farms the moment the player walks to the far
     * edge of their own render distance. The guards below therefore restrict it to animals that carry no sign
     * of player investment: anything named, leashed, ridden, tamed, persistence-flagged, or bred/aged (an
     * {@code age} other than 0 means the animal has been fed, is a baby, or is in love) is left alone.</p>
     */
    private static void despawnVanillaAnimals(ServerLevel level, List<ServerPlayer> players) {
        if (players.isEmpty()) {
            return;
        }
        List<Entity> targets = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof Cow || e instanceof Sheep || e instanceof Pig
                    || e instanceof Chicken || e instanceof Squid || e instanceof Wolf) {
                targets.add(e);
            }
        }
        for (Entity e : targets) {
            if (!isCullableVanillaAnimal(e)) {
                continue;
            }
            double distSq = nearestPlayerDistSq(e, players);
            boolean cull = distSq > 16384.0D // >128 blocks: always
                    || (distSq >= 1024.0D && e instanceof LivingEntity le && le.tickCount > 600
                        && level.getRandom().nextInt(800) == 0); // 32-128 blocks: occasionally, if long-lived
            if (cull) {
                e.discard();
            }
        }
    }

    /** True only for a wild, unowned, uninvested vanilla animal that no player would miss. */
    private static boolean isCullableVanillaAnimal(Entity e) {
        // Named, ridden or riding, or tamed: someone's pet or mount.
        if (e.hasCustomName() || e.isVehicle() || e.isPassenger()
                || (e instanceof TamableAnimal ta && ta.isTame())) {
            return false;
        }
        if (e instanceof Mob mob) {
            // Persistence flag = spawn egg / spawner / name tag / anything vanilla marks as "keep".
            // requiresCustomPersistence covers leashed and passenger cases.
            if (mob.isPersistenceRequired() || mob.requiresCustomPersistence() || mob.isLeashed()) {
                return false;
            }
        }
        // Bred, fed, in love, or still a baby: a farm animal the player is actively raising.
        if (e instanceof Animal animal && animal.getAge() != 0) {
            return false;
        }
        return true;
    }

    /**
     * True if this Mo'Creatures entity may be culled to satisfy a cap: it must be untamed, not
     * ridden or a rider, not custom-named, and not flagged as requiring custom persistence.
     */
    private static boolean isRemovable(Entity e, IMoCEntity moc) {
        if (moc.getIsTamed()) {
            return false;
        }
        // Never cull a baby. Now that MoCAnimal no longer despawns on its own, this cap is the only thing
        // removing land animals — and a bred litter (kittens, foals, cubs) is the last thing a player wants
        // deleted. Babies grow into adults, so they become cullable on their own.
        if (!moc.getIsAdult()) {
            return false;
        }
        if (e.hasCustomName()) {
            return false;
        }
        if (e.isVehicle() || e.isPassenger()) {
            return false;
        }
        // Every Mo'Creatures entity extends Mob (via Animal / Monster), so these are always available;
        // guard the cast defensively regardless. isPersistenceRequired covers spawn-egg / spawner-placed /
        // name-tagged creatures, requiresCustomPersistence covers leashed and passengers. This matters more
        // now that MoCAnimal no longer despawns on its own: this cap is the only thing culling land animals.
        if (e instanceof Mob mob && (mob.requiresCustomPersistence() || mob.isPersistenceRequired())) {
            return false;
        }
        return true;
    }

    /**
     * If {@code candidates} exceeds {@code cap}, discards the excess, removing the entities farthest
     * from any player first. A {@code cap <= 0} means "unlimited" (no removal). A null / empty list
     * is a no-op.
     */
    private static void enforce(List<Entity> candidates, int cap, List<ServerPlayer> players) {
        if (candidates == null || cap <= 0) {
            return;
        }
        int excess = candidates.size() - cap;
        if (excess <= 0) {
            return;
        }
        // Sort farthest-from-any-player first so nearby (likely visible) creatures survive.
        candidates.sort((a, b) -> Double.compare(
                nearestPlayerDistSq(b, players), nearestPlayerDistSq(a, players)));
        int removed = 0;
        for (int i = 0; i < candidates.size() && removed < excess; i++) {
            candidates.get(i).discard();
            removed++;
        }
        if (removed > 0) {
            MoCreatures.LOGGER.debug("Mo'Creatures mob-cap: removed {} entities (cap {})", removed, cap);
        }
    }

    /** Squared distance to the nearest player, or {@link Double#MAX_VALUE} if there are none. */
    private static double nearestPlayerDistSq(Entity e, List<ServerPlayer> players) {
        if (players.isEmpty()) {
            return Double.MAX_VALUE;
        }
        Vec3 pos = e.position();
        double best = Double.MAX_VALUE;
        for (ServerPlayer p : players) {
            double d = p.distanceToSqr(pos);
            if (d < best) {
                best = d;
            }
        }
        return best;
    }
}
