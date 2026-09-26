package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.IMoCEntity;
import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCEntities;
import drzhark.mocreatures.registry.MoCItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Port of the legacy {@code MoCEntityEgg}: a laid egg that sits on the ground and, after a while, hatches into a
 * baby of its egg type. Ostriches/turtles/snakes/komodos/scorpions lay these; the thrown {@code mocegg} spawns them.
 *
 * <p>An egg carries a <em>species</em> ({@link #getTypeMoC()}, 1-8) and an optional <em>variant</em> coat/sub-type
 * ({@code 0} = random). The legacy composite egg-type id (1-54: fishy 1-10, shark 11, snakes 21-28, ostrich 30-32,
 * komodo 33, scorpions 41-45, wyverns 50-54) is decoded into (species, variant) by {@link #setEggType(int)}.
 */
public class MoCEntityEgg extends MoCAnimal {

    public static final int TYPE_OSTRICH = 1;
    public static final int TYPE_TURTLE = 2;
    public static final int TYPE_SNAKE = 3;
    public static final int TYPE_KOMODO = 4;
    public static final int TYPE_SCORPION = 5;
    public static final int TYPE_WYVERN = 6;
    public static final int TYPE_FISHY = 7;
    public static final int TYPE_SHARK = 8;
    /** Manticore egg (legacy composite ids 62-65) — the only legacy source of a tameable pet manticore. */
    public static final int TYPE_MANTICORE = 9;

    /**
     * Legacy {@code tCounter}: incubation progress, raised on a 1-in-20 roll per tick while the egg is in its
     * hatching environment (aquatic eggs in water, land eggs out of it). The egg hatches at {@link #HATCH_AT}.
     */
    private int hatchTimer;
    /** Legacy 12.0.5 hatched at {@code tCounter >= 30} ({@code MoCEntityEgg.onUpdate:134/215}): ~600 ticks, ~30 s. */
    private static final int HATCH_AT = 30;
    /** Legacy {@code lCounter}: the unattended-lifetime counter that eventually removes a forgotten egg. */
    private int lifeCounter;
    /** The coat / sub-variant the hatchling should take (0 = random via {@code selectType}). */
    private int variant;
    /**
     * True only for the "stolen" ostrich egg (legacy composite id 31), which hatches a <em>tamed</em>, owner-named
     * ostrich. Wild ostrich eggs (30) and Nether/fire ostrich eggs (32) hatch untamed, matching legacy.
     */
    private boolean stolen;

    public MoCEntityEgg(EntityType<? extends MoCEntityEgg> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)   // legacy MoCEntityEgg.getMaxHealth() == 10
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void registerGoals() {
        // No AI — the egg simply rests and hatches.
    }

    /**
     * Every egg renders at the same size, as it did in 12.0.5 and 5.1.5. Legacy {@code getSize()} asked for 1.7x on
     * an ostrich egg (types 30/31) and {@code MoCRenderEgg} scaled by it, but the egg type was a plain field that
     * never reached the client, so the client always read 100: nobody ever saw a bigger egg.
     */
    @Override
    public float getSizeFactor() {
        return 1.0F;
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(TYPE_OSTRICH);
        }
    }

    public int getVariant() {
        return this.variant;
    }

    public void setVariant(int variant) {
        this.variant = variant;
    }

    /**
     * Decodes a legacy composite egg-type id (1-54) into this egg's species + coat variant. Fish/shark and the
     * per-variant snake/scorpion/wyvern sub-types are all reachable this way.
     */
    public void setEggType(int eggType) {
        if (eggType >= 1 && eggType <= 10) {          // fishy variants 1-10
            setTypeMoC(TYPE_FISHY);
            this.variant = eggType;
        } else if (eggType == 11) {                   // shark
            setTypeMoC(TYPE_SHARK);
            this.variant = 1;
        } else if (eggType >= 21 && eggType <= 28) {  // snake variants 1-8
            setTypeMoC(TYPE_SNAKE);
            this.variant = eggType - 20;
        } else if (eggType == 29) {                   // turtle (port addition; legacy had no turtle egg id)
            setTypeMoC(TYPE_TURTLE);
            this.variant = 0;
        } else if (eggType == 33) {                   // komodo
            setTypeMoC(TYPE_KOMODO);
            this.variant = 0;
        } else if (eggType >= 41 && eggType <= 45) {  // scorpion variants 1-5
            setTypeMoC(TYPE_SCORPION);
            this.variant = eggType - 40;
        } else if (eggType >= 62 && eggType <= 65) {  // manticore coats 1-4 (legacy "62 - 65")
            setTypeMoC(TYPE_MANTICORE);
            this.variant = eggType - 61;
        } else if (eggType >= 50 && eggType <= 54) {  // wyvern variants 1-5
            setTypeMoC(TYPE_WYVERN);
            this.variant = eggType - 49;
        } else if (eggType == 32) {                   // Nether / fire ostrich egg -> hatches the fire ostrich (type 5)
            setTypeMoC(TYPE_OSTRICH);
            this.variant = 5;
            this.stolen = false;
        } else if (eggType == 31) {                   // stolen ostrich egg -> hatches a TAMED ostrich
            setTypeMoC(TYPE_OSTRICH);
            this.variant = 0;
            this.stolen = true;
        } else {                                      // wild ostrich (30) and any fallthrough -> untamed
            setTypeMoC(TYPE_OSTRICH);
            this.variant = 0;
            this.stolen = false;
        }
    }

    /**
     * Re-encodes this egg's (species, variant) back into a legacy composite egg-type id (1-54), for putting the egg
     * back into item form when a player walks over it. Mirrors {@link #setEggType(int)} and the legacy pickup remap
     * of a wild ostrich egg (30) to a stolen ostrich egg (31).
     */
    public int getEggType() {
        return switch (getTypeMoC()) {
            case TYPE_SHARK -> 11;
            case TYPE_SNAKE -> 20 + Math.max(1, this.variant);
            case TYPE_TURTLE -> 29;
            case TYPE_KOMODO -> 33;
            case TYPE_SCORPION -> 40 + Math.max(1, this.variant);
            case TYPE_WYVERN -> 49 + Math.max(1, this.variant);
            case TYPE_MANTICORE -> 61 + Math.max(1, this.variant);
            case TYPE_OSTRICH -> this.variant == 5 ? 32 : 31; // legacy onCollideWithPlayer remaps 30 -> 31
            // A blank (spoiled) fishy egg has no variant yet, so it round-trips back to item form as a
            // Spoiled Egg rather than silently becoming a Blue Fish Egg.
            default -> this.variant >= 1 && this.variant <= 10 ? this.variant : 0; // fishy 1-10, 0 = spoiled
        };
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("egg.png");
    }

    /**
     * Legacy {@code canBreatheUnderwater() == true}: aquatic fishy/shark eggs incubate fully submerged and must not
     * drown (they need ~50 s underwater to hatch), so the egg never takes vanilla drown damage.
     */
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        // Legacy lCounter (MoCEntityEgg.onUpdate:132-146): a separate counter that ticks on a 1-in-20 roll
        // regardless of environment; once past 500, an egg with no player within 24 blocks is removed. Without
        // it an egg that never meets its hatching condition — a spoiled egg dropped on land, an aquatic egg
        // left out of water — would litter the world forever, which matters now that MoCAnimal no longer
        // despawns. Not persisted, exactly as legacy left it: the timer restarts when the chunk reloads.
        if (this.random.nextInt(20) == 0 && ++this.lifeCounter > 500
                && level.getNearestPlayer(this, 24.0D) == null) {
            discard();
            return;
        }
        // Incubation environment, per 12.0.5 MoCEntityEgg.onUpdate:124/202: aquatic eggs (fishy/shark) progress
        // only while in water, every land egg only while OUT of water — anywhere, no other condition. This used
        // to follow 5.1.5, which also demanded a torch / glowstone / lit lamp / jack-o'-lantern within 4 blocks
        // (MoCTools.isNearTorch) before a land egg would progress at all. Nothing in the game says so, so a
        // placed egg — the manticore egg, which only exists in 12.0.5 — simply sat there until it was cleaned
        // up. 12.0.5, the version the manticore comes from, dropped that requirement, and so does the port.
        boolean aquatic = getTypeMoC() == TYPE_FISHY || getTypeMoC() == TYPE_SHARK;
        boolean ready = aquatic == this.isInWater();
        if (ready && this.random.nextInt(20) == 0) {
            this.hatchTimer++;
            // Legacy: every fifth step the egg rocks — a little hop you can see — and on the fifth it warns
            // the nearest player that it is about to hatch (NotifyEggHatching).
            if (this.hatchTimer % 5 == 0) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.2D, 0.0D));
                // Ship the position on the next tracker pass (mc262-ref ServerEntity:123): a client only replays
                // a mob's synced positions, so a velocity-only sync (hurtMarked) would never show the hop.
                this.needsSync = true;
            }
            if (this.hatchTimer == 5) {
                notifyHatching(level);
            }
            if (this.hatchTimer >= HATCH_AT) {
                hatch(level);
            }
        }
    }

    /**
     * Legacy {@code NotifyEggHatching} ({@code MoCEntityEgg:317-323}): tells the nearest player within 24 blocks
     * that the egg is about to hatch and where. The hatchling is tamed to whoever is nearest at the moment it
     * hatches, so this is the cue to stay close.
     */
    private void notifyHatching(ServerLevel level) {
        Player player = level.getNearestPlayer(this, 24.0D);
        if (player != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                    "message.mocreatures.egg_hatching", this.getBlockX(), this.getBlockY(), this.getBlockZ()));
        }
    }

    /**
     * Legacy {@code onCollideWithPlayer}: once the egg has rested for a while, a player walking into it collects it
     * back into item form (a {@code mocegg} carrying this egg's composite EggType subtype), plays a pop and vanishes.
     * The grace period is legacy's own {@code lCounter > 10} ({@code :91}) — about ten seconds on its 1-in-20 roll. This used to
     * be {@code tickCount > 10}, half a second, and an egg placed with the item lands at the player's feet, so a
     * player who placed an egg and stood still to watch it had it snatched straight back into their inventory.
     */
    @Override
    public void playerTouch(Player player) {
        if (this.level().isClientSide() || this.lifeCounter <= 10) {
            return;
        }
        ItemStack stack = drzhark.mocreatures.item.MoCThrownEggItem.createEgg(getEggType());
        if (player.addItem(stack)) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL,
                    0.2F, ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            this.discard();
        }
    }

    private void hatch(ServerLevel level) {
        // Legacy soft cap: refuse to hatch an ostrich egg while more than 20 ostriches already exist in this
        // world; reset the incubation counter and try again later (legacy set tCounter=0, lCounter=500).
        if (getTypeMoC() == TYPE_OSTRICH) {
            int ostriches = 0;
            for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
                if (e instanceof MoCEntityOstrich) {
                    ostriches++;
                }
            }
            if (ostriches > 20) {
                this.hatchTimer = 0;
                return;
            }
        }
        Mob baby = switch (getTypeMoC()) {
            case TYPE_TURTLE -> new MoCEntityTurtle(MoCEntities.TURTLE.get(), level);
            case TYPE_SNAKE -> new MoCEntitySnake(MoCEntities.SNAKE.get(), level);
            case TYPE_KOMODO -> new MoCEntityKomodo(MoCEntities.KOMODO.get(), level);
            case TYPE_SCORPION -> new MoCEntityPetScorpion(MoCEntities.PET_SCORPION.get(), level);
            case TYPE_WYVERN -> new MoCEntityWyvern(MoCEntities.WYVERN.get(), level);
            case TYPE_FISHY -> new MoCEntityFishy(MoCEntities.FISHY.get(), level);
            case TYPE_SHARK -> new MoCEntityShark(MoCEntities.SHARK.get(), level);
            case TYPE_MANTICORE -> new MoCEntityManticorePet(MoCEntities.MANTICORE_PET.get(), level);
            default -> new MoCEntityOstrich(MoCEntities.OSTRICH.get(), level);
        };
        baby.setPos(this.getX(), this.getY(), this.getZ());
        baby.setYRot(this.getYRot());
        if (baby instanceof IMoCEntity moc) {
            if (getTypeMoC() == TYPE_OSTRICH) {
                // Legacy: overworld ostrich eggs hatch the plain type-1 ostrich; a Nether-incubated egg or the
                // type-32 (fire) egg hatches the fire ostrich (type 5). Never randomised.
                moc.setTypeMoC((this.variant == 5 || level.dimension() == net.minecraft.world.level.Level.NETHER) ? 5 : 1);
            } else if (this.variant > 0) {
                moc.setTypeMoC(this.variant); // deterministic coat/variant...
            }
            moc.selectType();                 // ...selectType is a no-op once the type is set, else random
            moc.setAdult(false);
            // Legacy per-species hatch age (setEdad): snake 50; fishy/shark/komodo/wyvern 30; ostrich 35.
            // Scorpion called only setAdult(false) with no setEdad, so keep its constructor default.
            if (getTypeMoC() != TYPE_SCORPION) {
                moc.setMoCAge(switch (getTypeMoC()) {
                    case TYPE_SNAKE -> 50;
                    case TYPE_FISHY, TYPE_SHARK, TYPE_KOMODO, TYPE_WYVERN, TYPE_MANTICORE -> 30;
                    default -> 35; // ostrich, turtle
                });
            }
            // Legacy tameWithName: every hatch tames + owner-names the baby to the nearest player within 24 blocks,
            // EXCEPT wild/Nether ostrich eggs (only the stolen ostrich egg, id 31, hatches a tamed ostrich).
            if (getTypeMoC() != TYPE_OSTRICH || this.stolen) {
                Player owner = level.getNearestPlayer(this, 24.0D);
                // Legacy tameWithName enforced the per-player pet cap on every taming path, hatches included.
                if (owner != null
                        && !drzhark.mocreatures.entity.MoCAnimal.exceedsTameCap(baby, owner)) {
                    moc.setTamed(true);
                    moc.setOwnerName(owner.getName().getString());
                    // Legacy tameWithName prompted for a name the instant a creature was tamed.
                    drzhark.mocreatures.network.MoCNetwork.promptName(moc, owner);
                }
            }
        }
        // Land babies use the vanilla baby-age render shrink; aquatic (WaterAnimal) fish/shark aren't AgeableMob.
        if (baby instanceof AgeableMob ageable) {
            ageable.setAge(-24000);
        }
        baby.setHealth(baby.getMaxHealth());
        level.addFreshEntity(baby);
        // Legacy played "mob.chickenplop" (the chicken egg-plop) on hatch, at a slightly randomised pitch.
        level.playSound(null, this.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 1.0F,
                ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F) + 1.0F);
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("HatchTimer", this.hatchTimer);
        output.putInt("EggVariant", this.variant);
        output.putBoolean("EggStolen", this.stolen);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.hatchTimer = input.getIntOr("HatchTimer", 0);
        this.variant = input.getIntOr("EggVariant", 0);
        this.stolen = input.getBooleanOr("EggStolen", false);
    }
}
