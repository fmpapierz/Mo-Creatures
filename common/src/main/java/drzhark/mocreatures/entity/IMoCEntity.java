package drzhark.mocreatures.entity;

import net.minecraft.resources.Identifier;

/**
 * Common contract shared by every Mo'Creatures entity, mirroring the responsibilities of the
 * legacy {@code drzhark.mocreatures.entity.MoCIMoCreature} interface but adapted to the
 * Minecraft 26.2 / NeoForge entity model.
 *
 * <p>Mo'Creatures entities carry a small amount of shared synchronized state on top of vanilla:
 * a <em>type</em> (texture / sub-species selector), a tamed flag, an owner name, an adult flag and
 * an integer "age" counter. The base implementations live in {@link MoCAnimal} (passive) and
 * {@link MoCMob} (hostile).
 */
public interface IMoCEntity {

    /** Picks the sub-type (texture variant) for this entity when it first spawns. */
    void selectType();

    /** @return the current sub-type / texture variant index (1-based; 0 = unset). */
    int getTypeMoC();

    void setTypeMoC(int type);

    boolean getIsTamed();

    void setTamed(boolean tamed);

    boolean getIsAdult();

    void setAdult(boolean adult);

    /** @return the networked integer age (legacy "edad"), typically 0-100. */
    int getMoCAge();

    void setMoCAge(int age);

    String getOwnerName();

    void setOwnerName(String name);

    /** @return the texture to render this entity with, based on its current type. */
    Identifier getTexture();

    /** @return true if this entity should run extra per-tick logic on the client as well. */
    default boolean forceUpdates() {
        return false;
    }

    /**
     * Per-species render size multiplier applied on top of the age-based growth curve (legacy
     * {@code getSizeFactor}). Defaults to 1.0; species like the crab (0.7x) and donkey (0.9x) override it.
     */
    default float getSizeFactor() {
        return 1.0F;
    }

    /** Legacy Jump keybind: request a jump on the next tick (implemented by rideable creatures). */
    default void makeEntityJump() {
    }

    /** Legacy Dismount keybind: eject the rider from this mount (server-side). */
    default void dismountMoCEntity(net.minecraft.server.level.ServerLevel level) {
    }
}
