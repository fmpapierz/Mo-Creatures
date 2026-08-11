package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.config.MoCConfig;
import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Port of the legacy {@code MoCEntityElephant}, with its equipment progression restored: a tamed
 * adult is fitted with a harness, then either a garment+howdah (Indian) or a platform (mammoth), and
 * a chest set that grants a key and 18-slot storage (which grows to 36/45/54 slots with extra chest
 * sets / vanilla chests, mammoths only). A tusked elephant becomes a bulldozer while ridden, and the
 * whip cracks it into a trampling charge.
 */
public class MoCEntityElephant extends MoCAnimal {

    /** Equipment stage: 0 = bare, 1 = harness, 2 = garment (Indian), 3 = howdah / platform. */
    private static final EntityDataAccessor<Integer> ARMOR = SynchedEntityData.defineId(MoCEntityElephant.class, EntityDataSerializers.INT);
    /** Storage tier (legacy {@code getStorage}): 0 none, 1 chest (18), 2 double (36), 3 triple (45), 4 quad (54). */
    private static final EntityDataAccessor<Integer> STORAGE = SynchedEntityData.defineId(MoCEntityElephant.class, EntityDataSerializers.INT);
    /** Tusk armour tier: 0 none, 1 wood, 2 iron, 3 diamond. Drives both the model set and the bulldozer. */
    private static final EntityDataAccessor<Integer> TUSKS = SynchedEntityData.defineId(MoCEntityElephant.class, EntityDataSerializers.INT);
    /** Sustained-charge flag (mirrors legacy {@code sprintCounter} > 0): a whip-cracked elephant surges 1.5x and,
     *  while ridden, tramples nearby mobs — permanently, until unload. Synched so the client predicts the boost. */
    private static final EntityDataAccessor<Boolean> SPRINTING = SynchedEntityData.defineId(MoCEntityElephant.class, EntityDataSerializers.BOOLEAN);

    /** Ticks between successive bulldozer sweeps, so a tusked elephant doesn't tear through terrain every tick. */
    private static final int BULLDOZE_INTERVAL = 10;
    /** Counts down each tick; a bulldoze sweep fires (and resets it) when it reaches zero. */
    private int bulldozeCooldown;
    /** Accumulated tusk wear (legacy {@code tuskUses}); the set shatters once it passes its tier limit. */
    private int tuskUses;
    /** Charge state (legacy {@code sprintCounter}): set to 1 by the whip and — as in legacy — never incremented,
     *  so it stays at 1 and the 1.5x speed + trample run permanently while loaded. Transient (not persisted). */
    private int sprintCounter;
    /** Baby-taming progress (legacy {@code temper}): hand-feeding a wild baby raises this (cake +2, sugar lump +1);
     *  at 10 the baby tames and bonds to the feeder. Persisted so a partly-won baby keeps its progress. */
    // Temper lives on MoCAnimal now (one owner of the "Temper" NBT key).

    /** Up to 54 slots; the accessible slice (18/36/45/54) is chosen by the storage tier when the key is used. */
    private final SimpleContainer chest = new SimpleContainer(54);

    public MoCEntityElephant(EntityType<? extends MoCEntityElephant> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                // Base is the MAX across types (songhua mammoth 60); tick() lowers it per type so a loaded
                // mammoth's health is never clamped down before its type is known.
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D); // defensive melee: retaliates for 4 (legacy attackEntity)
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // The elephant is a defensive mob: it doesn't hunt, but retaliates (for 4 damage) against whatever
        // struck it (legacy attackEntity / attackEntityFrom). setTarget below keeps a tamed one from turning
        // on players; a wild one retaliates against anyone.
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        // Legacy attackEntityFrom only acquired the attacker as a target off Peaceful
        // (worldObj.difficultySetting > 0); on Peaceful a struck elephant does not retaliate.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return MoCEntityElephant.this.level().getDifficulty() != Difficulty.PEACEFUL
                        && super.canUse();
            }
        });
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // Legacy attackEntityFrom: a tamed elephant never turns on a player (it ignores its owner and other
        // players), but still retaliates against hostile mobs; a wild elephant retaliates against anyone.
        if (getIsTamed() && target instanceof Player) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ARMOR, 0);
        builder.define(STORAGE, 0);
        builder.define(TUSKS, 0);
        builder.define(SPRINTING, false);
    }

    /** Tusk armour tier: 0 none, 1 wood, 2 iron, 3 diamond. */
    public int getTusks() {
        return this.entityData.get(TUSKS);
    }

    public void setTusks(int v) {
        this.entityData.set(TUSKS, v);
    }

    public int getArmorStage() {
        return this.entityData.get(ARMOR);
    }

    public void setArmorStage(int v) {
        this.entityData.set(ARMOR, v);
    }

    /** Storage tier (0-4); 0 = no chest. */
    public int getStorage() {
        return this.entityData.get(STORAGE);
    }

    public void setStorage(int v) {
        this.entityData.set(STORAGE, v);
    }

    /** Whether the elephant carries any storage (drives the model's chest geometry via the render state). */
    public boolean hasChest() {
        return getStorage() > 0;
    }

    public SimpleContainer getChest() {
        return this.chest;
    }

    private void equip(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.level().playSound(null, blockPosition(), MoCSounds.ARMORPUT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean server = !this.level().isClientSide();
        // Riding is gated on the harness (not a vanilla saddle): a harnessed elephant carries a
        // driver, and a howdah / platform (stage 3) carries a second passenger behind the driver.
        if (getIsTamed() && getIsAdult() && stack.isEmpty() && getArmorStage() >= 1) {
            int seats = getArmorStage() >= 3 ? 2 : 1;
            if (getPassengers().size() < seats) {
                if (server) {
                    player.startRiding(this);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (getIsTamed() && getIsAdult() && !stack.isEmpty()) {
            int type = getTypeMoC();
            // Harness: first equipment stage. Works on ANY elephant.
            if (getArmorStage() == 0 && stack.is(MoCItems.ELEPHANTHARNESS.get())) {
                if (server) { setArmorStage(1); equip(player, stack); }
                return InteractionResult.SUCCESS;
            }
            // Garment on a harnessed INDIAN (type 2 only): dresses it into the decorated "pretty" type 5,
            // whose texture sheet carries the skirt/cabin art. African (type 1) and mammoths never accept it.
            if (getArmorStage() == 1 && type == 2 && stack.is(MoCItems.ELEPHANTGARMENT.get())) {
                if (server) {
                    setArmorStage(2);
                    setTypeMoC(5);
                    equip(player, stack);
                }
                return InteractionResult.SUCCESS;
            }
            // Howdah on a garment-wearing pretty Indian (stage 2, type 5): a 2-seat carriage. The garment
            // must already be fitted (legacy required getArmorType()==2 && getType()==5).
            if (getArmorStage() == 2 && type == 5 && stack.is(MoCItems.ELEPHANTHOWDAH.get())) {
                if (server) { setArmorStage(3); equip(player, stack); }
                return InteractionResult.SUCCESS;
            }
            // Platform on a harnessed Songhua mammoth (type 4 only, stage 1). The legacy model paints the
            // fort platform exclusively on the type-4 mammoth.
            if (getArmorStage() == 1 && type == 4 && stack.is(MoCItems.MAMMOTHPLATFORM.get())) {
                if (server) { setArmorStage(3); equip(player, stack); }
                return InteractionResult.SUCCESS;
            }
            // First chest set on a harnessed elephant: storage 0 -> 1 (18 slots), grants a key.
            if (getArmorStage() >= 1 && getStorage() == 0 && stack.is(MoCItems.ELEPHANTCHEST.get())) {
                if (server) {
                    setStorage(1);
                    equip(player, stack);
                    player.addItem(new ItemStack(MoCItems.KEY.get()));
                }
                return InteractionResult.SUCCESS;
            }
            // Second chest set: storage 1 -> 2 (double chest, 36 slots).
            if (getArmorStage() >= 1 && getStorage() == 1 && stack.is(MoCItems.ELEPHANTCHEST.get())) {
                if (server) { setStorage(2); equip(player, stack); }
                return InteractionResult.SUCCESS;
            }
            // Third storage: a vanilla chest on a small mammoth (type 3), storage 2 -> 3 (45 slots).
            if (getArmorStage() >= 1 && type == 3 && getStorage() == 2 && stack.is(Items.CHEST)) {
                if (server) { setStorage(3); equip(player, stack); }
                return InteractionResult.SUCCESS;
            }
            // Fourth storage: another vanilla chest on a small mammoth (type 3), storage 3 -> 4 (54 slots).
            if (getArmorStage() >= 1 && type == 3 && getStorage() == 3 && stack.is(Items.CHEST)) {
                if (server) { setStorage(4); equip(player, stack); }
                return InteractionResult.SUCCESS;
            }
            // Tusk armour sets: fit wood / iron / diamond tusks (tiers 1/2/3). A tusked elephant
            // becomes a bulldozer (breaks blocks in its path). Any tamed adult accepts them.
            if (stack.is(MoCItems.TUSKSWOOD.get())) {
                if (server) { fitTusks(player, stack, 1); }
                return InteractionResult.SUCCESS;
            }
            if (stack.is(MoCItems.TUSKSIRON.get())) {
                if (server) { fitTusks(player, stack, 2); }
                return InteractionResult.SUCCESS;
            }
            if (stack.is(MoCItems.TUSKSDIAMOND.get())) {
                if (server) { fitTusks(player, stack, 3); }
                return InteractionResult.SUCCESS;
            }
            // Any pickaxe (legacy accepted wood/stone/iron/gold/diamond) prises the tusk armour back off,
            // recovering its remaining durability.
            if (getTusks() > 0 && stack.is(ItemTags.PICKAXES)) {
                if (server && this.level() instanceof ServerLevel level) {
                    dropTusks(level);
                }
                return InteractionResult.SUCCESS;
            }
            // Key opens the elephant's chest (vanilla chest screen sized by the storage tier).
            if (getStorage() > 0 && stack.is(MoCItems.KEY.get())) {
                if (server && player instanceof ServerPlayer serverPlayer) {
                    openStorage(serverPlayer);
                }
                return InteractionResult.SUCCESS;
            }
        }
        // Baby taming (legacy interact temper mechanic, lines 397-432): a wild BABY elephant is won over by
        // hand-feeding — cake raises its temper by 2, a sugar lump by 1 — and each feed also fully heals it.
        // Once temper reaches 10 the baby tames and bonds to the feeder. Wild ADULTS stay untameable: this
        // path is gated on !getIsAdult() and never calls setAdult(true) (the port's MoCBehavior sets the
        // elephant's FEED tame to NONE, so a single feed no longer tames — this restores the graduated bond).
        if (!getIsTamed() && !getIsAdult() && !stack.isEmpty()) {
            int gain = 0;
            if (stack.is(Items.CAKE)) {
                gain = 2;
            } else if (stack.is(MoCItems.SUGARLUMP.get())) {
                gain = 1;
            }
            if (gain > 0) {
                if (server) {
                    setTemper(getTemper() + gain);
                    setHealth(getMaxHealth());
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    this.level().playSound(null, blockPosition(), MoCSounds.EATING.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                    // Legacy tameWithName enforced the per-player pet cap on every taming path.
                    if (getTemper() >= 10 && !exceedsTameCap(player)) {
                        setTamed(true);
                        setOwnerName(player.getName().getString());
                        // Legacy tameWithName prompted for a name the instant a creature was tamed.
                        drzhark.mocreatures.network.MoCNetwork.promptName(this, player);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        // Elephants ignore saddles (legacy has no saddle branch): swallow a saddle so it never reaches the
        // base rideNeedsSaddle path, which would otherwise consume it and let a harness-less elephant ride.
        if (stack.is(Items.SADDLE) || stack.is(MoCItems.HORSESADDLE.get())) {
            return InteractionResult.PASS;
        }
        return super.mobInteract(player, hand);
    }

    /** Opens the elephant's chest, sizing the screen (2/4/5/6 rows = 18/36/45/54 slots) to the storage tier. */
    private void openStorage(ServerPlayer serverPlayer) {
        int rows = switch (getStorage()) {
            case 1 -> 2;   // 18 slots
            case 2 -> 4;   // 36 slots
            case 3 -> 5;   // 45 slots
            default -> 6;  // 54 slots
        };
        net.minecraft.world.inventory.MenuType<?> menuType = switch (rows) {
            case 2 -> net.minecraft.world.inventory.MenuType.GENERIC_9x2;
            case 4 -> net.minecraft.world.inventory.MenuType.GENERIC_9x4;
            case 5 -> net.minecraft.world.inventory.MenuType.GENERIC_9x5;
            default -> net.minecraft.world.inventory.MenuType.GENERIC_9x6;
        };
        serverPlayer.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ChestMenu(menuType, id, inv, this.chest, rows), getDisplayName()));
    }

    @Override
    protected boolean canAddPassenger(net.minecraft.world.entity.Entity passenger) {
        // A howdah / platform (stage 3) seats two; otherwise the single vanilla rider. Seat positions
        // come from the entity type's passengerAttachments (anchored to the cabin pillow).
        int seats = getArmorStage() >= 3 ? 2 : 1;
        return getPassengers().size() < seats;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        // Per-type max health (legacy getMaxHealth switch), kept in sync as the type changes (e.g. an Indian
        // dressed with a garment becomes the type-5 pretty). Cheap: the common case is a no-op compare.
        double wantMaxHealth = maxHealthForType();
        AttributeInstance maxHealthAttr = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null && maxHealthAttr.getBaseValue() != wantMaxHealth) {
            maxHealthAttr.setBaseValue(wantMaxHealth);
            if (getHealth() > wantMaxHealth) {
                setHealth((float) wantMaxHealth);
            }
        }

        if (this.bulldozeCooldown > 0) {
            this.bulldozeCooldown--;
        }

        // Charge (legacy sprintCounter): the whip pins sprintCounter to 1 and legacy NEVER increments it, so the
        // 1.5x speed (getCustomSpeed) plus the per-tick trample persist for as long as the elephant stays loaded
        // — a permanent charge from a single whip crack (sprintCounter is transient, so it clears on unload).
        // While set and ridden, the elephant tramples nearby non-player mobs (legacy MoCTools.buckleMobsNotPlayers).
        if (this.sprintCounter > 0) {
            if (!this.entityData.get(SPRINTING)) {
                this.entityData.set(SPRINTING, true);
            }
            if (isVehicle()) {
                buckleMobsNotPlayers(level, 3.0D);
            }
        }

        // Bulldozer: a tusked elephant tears breakable blocks out of its path — but only while a player is
        // riding a tamed elephant (legacy onLivingUpdate gated on getIsTamed && riddenByEntity != null &&
        // getTusks() > 0). Also gated on the mob-griefing rule, ground contact, actual movement, and throttled.
        if (getTusks() > 0 && getIsTamed() && isVehicle() && this.bulldozeCooldown <= 0 && onGround()
                && MoCConfig.get().elephantBulldozer
                && level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            double dx = getX() - this.xo;
            double dz = getZ() - this.zo;
            if ((dx * dx + dz * dz) > 0.0025D) {
                int worn = bulldoze(level);
                checkTusks(worn); // wear the tusks by the (fractional) count of blocks ploughed
                this.bulldozeCooldown = BULLDOZE_INTERVAL;
            }
        }
    }

    /** Per-type maximum health (legacy {@code getMaxHealth}): Indian 30, mammoth 50, songhua 60, else 40. */
    private double maxHealthForType() {
        return switch (getTypeMoC()) {
            case 2 -> 30.0D; // Indian
            case 3 -> 50.0D; // mammoth
            case 4 -> 60.0D; // songhua mammoth
            default -> 40.0D; // African (1), pretty (5), and fallback
        };
    }

    /** Knocks back and grazes nearby non-player living entities (legacy {@code MoCTools.buckleMobsNotPlayers}). */
    private void buckleMobsNotPlayers(ServerLevel level, double dist) {
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(dist, 2.0D, dist),
                e -> e != this && !this.hasPassenger(e) && !(e instanceof Player))) {
            double dx = victim.getX() - this.getX();
            double dz = victim.getZ() - this.getZ();
            double d = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
            victim.push(dx / d * 0.6D, 0.6D, dz / d * 0.6D); // 0.6 horizontal + 0.6 up (legacy bigsmack)
            victim.hurtServer(level, this.damageSources().mobAttack(this), 2.0F); // 2 damage
        }
    }

    /**
     * Destroys breakable blocks in a single column directly in front of the elephant (legacy
     * {@code MoCTools.destroyBlocksInFront}), up to the mob's height (a taller column for mammoths). Only
     * blocks whose hardness does not exceed the tusk tier are torn out (wood 1 / iron 2 / diamond 3), so
     * wood tusks clear dirt/sand/leaves but never stone (1.5) or obsidian (50). Items drop with chance
     * 0.20 * tier, and only ~1/3 of the broken blocks count toward tusk wear.
     */
    private int bulldoze(ServerLevel level) {
        int strength = getTusks();
        if (strength == 0) {
            return 0;
        }
        // A point one block ahead of the elephant, along its facing.
        float yaw = getYRot() * ((float) Math.PI / 180.0F);
        double reach = (getBbWidth() * 0.5D) + 0.75D;
        double frontX = getX() - Math.sin(yaw) * reach;
        double frontZ = getZ() + Math.cos(yaw) * reach;

        int type = getTypeMoC();
        int height = (type == 3 || type == 4) ? 3 : 2;
        int baseX = net.minecraft.util.Mth.floor(frontX);
        int baseY = net.minecraft.util.Mth.floor(getY());
        int baseZ = net.minecraft.util.Mth.floor(frontZ);

        int worn = 0;
        for (int dy = 0; dy < height; dy++) {
            BlockPos pos = new BlockPos(baseX, baseY + dy, baseZ);
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            // Skip air and unbreakable blocks (hardness < 0), and any block harder than the tusk tier.
            if (!state.isAir() && hardness >= 0.0F && hardness <= strength) {
                boolean drop = this.random.nextFloat() < (0.20F * strength);
                level.destroyBlock(pos, drop, this);
                if (this.random.nextInt(3) == 0) {
                    worn++; // legacy only counts ~1/3 of broken blocks toward tusk wear
                }
            }
        }
        return worn;
    }

    /**
     * Fit a fresh or partly-worn tusk set (tier 1 wood / 2 iron / 3 diamond), dropping any set already
     * worn first and resuming the new item's remaining wear from its damage value (legacy behaviour).
     */
    private void fitTusks(Player player, ItemStack stack, int tier) {
        if (this.level() instanceof ServerLevel level) {
            dropTusks(level);                     // eject any currently-worn tusks first
            this.tuskUses = stack.getDamageValue(); // resume the new set's wear from its item damage
            setTusks(tier);
        }
        equip(player, stack); // consume one + play the equip sound
    }

    /** Accrue tusk wear as the elephant bulldozes; the set shatters once it passes its tier limit. */
    private void checkTusks(int dmg) {
        if (dmg <= 0 || getTusks() == 0) {
            return;
        }
        this.tuskUses += dmg;
        int limit = switch (getTusks()) {
            case 1 -> 59;
            case 2 -> 250;
            case 3 -> 1000;
            default -> Integer.MAX_VALUE;
        };
        if (this.tuskUses > limit) {
            // Worn out: the tusk set shatters (no item recovered).
            this.level().playSound(null, blockPosition(), MoCSounds.ARMOROFF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            setTusks(0);
            this.tuskUses = 0;
        }
    }

    /** Drop the currently-worn tusk set back as an item carrying its remaining wear, then bare the tusks. */
    private void dropTusks(ServerLevel level) {
        int tier = getTusks();
        if (tier == 0) {
            return;
        }
        Item item = switch (tier) {
            case 1 -> MoCItems.TUSKSWOOD.get();
            case 2 -> MoCItems.TUSKSIRON.get();
            case 3 -> MoCItems.TUSKSDIAMOND.get();
            default -> null;
        };
        if (item != null) {
            ItemStack drop = new ItemStack(item);
            drop.setDamageValue(Math.min(this.tuskUses, drop.getMaxDamage()));
            spawnAtLocation(level, drop);
            this.level().playSound(null, blockPosition(), MoCSounds.ARMOROFF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        setTusks(0);
        this.tuskUses = 0;
    }

    /**
     * Legacy {@code dropMyStuff}: release ALL worn gear — the harness, then the garment + howdah (non-mammoth) or
     * the mammoth platform, plus the tusk set — as items when the Scroll of Freedom frees the elephant, so none of
     * it is silently lost. The saddlebag chest units are released separately by the scroll's generic chest handling.
     */
    public void dropWornGear(ServerLevel level) {
        boolean droppedStage = false;
        if (getArmorStage() >= 1) {
            spawnAtLocation(level, new ItemStack(MoCItems.ELEPHANTHARNESS.get()));
            droppedStage = true;
        }
        if (getArmorStage() >= 3 && getTypeMoC() == 4) {
            // Mammoth path: harness -> platform (it never wore a garment).
            spawnAtLocation(level, new ItemStack(MoCItems.MAMMOTHPLATFORM.get()));
            droppedStage = true;
        } else {
            if (getArmorStage() >= 2) {
                spawnAtLocation(level, new ItemStack(MoCItems.ELEPHANTGARMENT.get()));
                droppedStage = true;
            }
            if (getArmorStage() >= 3) {
                spawnAtLocation(level, new ItemStack(MoCItems.ELEPHANTHOWDAH.get()));
                droppedStage = true;
            }
        }
        if (getArmorStage() > 0) {
            setArmorStage(0);
        }
        boolean hadTusks = getTusks() > 0;
        dropTusks(level); // releases the (partly-worn) tusk set, playing armoroff if it had any
        if (droppedStage && !hadTusks) {
            this.level().playSound(null, blockPosition(), MoCSounds.ARMOROFF.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, damageSource, hitByPlayer);
        dropTusks(level); // a slain elephant drops its (partly-worn) tusk armour
        // Drop the chest units back (legacy dropMyStuff): one chest set per storage tier 1/2, a vanilla chest
        // per tier 3/4.
        int storage = getStorage();
        if (storage >= 1) {
            spawnAtLocation(level, new ItemStack(MoCItems.ELEPHANTCHEST.get()));
        }
        if (storage >= 2) {
            spawnAtLocation(level, new ItemStack(MoCItems.ELEPHANTCHEST.get()));
        }
        if (storage >= 3) {
            spawnAtLocation(level, new ItemStack(Items.CHEST));
        }
        if (storage >= 4) {
            spawnAtLocation(level, new ItemStack(Items.CHEST));
        }
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            if (!s.isEmpty()) {
                spawnAtLocation(level, s);
            }
        }
        chest.clearContent();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("ArmorStage", getArmorStage());
        output.putInt("Storage", getStorage());
        output.putInt("Tusks", getTusks());
        output.putInt("TuskUses", this.tuskUses);
        ValueOutput.ValueOutputList items = output.childrenList("ChestItems");
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack s = chest.getItem(i);
            if (!s.isEmpty()) {
                ValueOutput child = items.addChild();
                child.putInt("Slot", i);
                child.store("Item", ItemStack.CODEC, s);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setArmorStage(input.getIntOr("ArmorStage", 0));
        setStorage(input.getIntOr("Storage", 0));
        setTusks(input.getIntOr("Tusks", 0));
        this.tuskUses = input.getIntOr("TuskUses", 0);
        chest.clearContent();
        for (ValueInput child : input.childrenListOrEmpty("ChestItems")) {
            int slot = child.getIntOr("Slot", -1);
            if (slot >= 0 && slot < chest.getContainerSize()) {
                child.read("Item", ItemStack.CODEC).ifPresent(s -> chest.setItem(slot, s));
            }
        }
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            // Biome-driven typing (legacy checkSpawningBiome): mammoths in cold biomes, African in deserts,
            // Indian in jungles, a coin-flip in plains/forest, and a 50/50 fallback everywhere else.
            var biome = this.level().getBiome(this.blockPosition());
            if (biome.value().getBaseTemperature() <= 0.05F) {
                setTypeMoC(3 + this.random.nextInt(2)); // mammoth (type 3) or songhua mammoth (type 4)
            } else if (biome.is(Biomes.DESERT)) {
                setTypeMoC(1); // African
            } else if (biome.is(BiomeTags.IS_JUNGLE)) {
                setTypeMoC(2); // Indian
            } else if (biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS) || biome.is(BiomeTags.IS_FOREST)) {
                setTypeMoC(1 + this.random.nextInt(2)); // African or Indian
            } else {
                setTypeMoC(this.random.nextInt(100) <= 50 ? 1 : 2); // 50/50 fallback
            }
            applyTypeHealth();
        }
    }

    /** Sets the max-health attribute base to the current type's value and fills the elephant (legacy spawn). */
    private void applyTypeHealth() {
        double max = maxHealthForType();
        AttributeInstance attr = getAttribute(Attributes.MAX_HEALTH);
        if (attr != null && attr.getBaseValue() != max) {
            attr.setBaseValue(max);
        }
        setHealth((float) getMaxHealth());
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        // Legacy fall(float f): the elephant softens falls. Damage is cut to ceil(f-3)/3 (integer division) and the
        // SAME reduced amount is dealt to each rider — e.g. a 10-block fall deals only 2. The vanilla multiplier is
        // ignored, exactly as legacy ignored it. By not calling super we also suppress vanilla's passenger propagation
        // (which would otherwise deal full fall damage to the rider), replacing it with the legacy reduced amount.
        int i = (int) Math.ceil(fallDistance - 3.0D);
        if (i > 0) {
            i /= 3;
            if (i > 0) {
                this.hurt(source, i);
                for (net.minecraft.world.entity.Entity passenger : getPassengers()) {
                    passenger.hurt(source, i);
                }
            }
        }
        return false;
    }

    // --------------------------------------------------------------------------- ridden speed (legacy getCustomSpeed)
    /** Per-type ridden pace (legacy {@code getCustomSpeed}): African 0.6, Indian/pretty 0.7, mammoths 0.5;
     *  a whip-cracked charge (SPRINTING) multiplies it by 1.5. */
    private double getCustomSpeed() {
        double tSpeed = switch (getTypeMoC()) {
            case 1 -> 0.6D;      // African
            case 2, 5 -> 0.7D;   // Indian / pretty
            default -> 0.5D;     // mammoths (3/4) and fallback
        };
        if (this.entityData.get(SPRINTING)) {
            tSpeed *= 1.5D;
        }
        return tSpeed;
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return (float) (this.getAttributeValue(Attributes.MOVEMENT_SPEED) * getCustomSpeed());
    }

    @Override
    public Identifier getTexture() {
        return switch (getTypeMoC()) {
            case 2 -> modelTexture("elephantindian.png");
            case 3 -> modelTexture("mammoth.png");
            case 4 -> modelTexture("mammothsonghua.png");
            case 5 -> modelTexture("elephantindianpretty.png");
            default -> modelTexture("elephantafrican.png");
        };
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        if (!getIsAdult() && getMoCAge() < 80) {
            return MoCSounds.ELEPHANTCALF.get();
        }
        return MoCSounds.ELEPHANTGRUNT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource damageSource) {
        return MoCSounds.ELEPHANTHURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return MoCSounds.ELEPHANTDYING.get();
    }

    // --------------------------------------------------------------- whip effect (legacy MoCItemWhip)
    /**
     * Whip crack on a ridden elephant (legacy {@code sprintCounter = 1}): kicks off a permanent charge — a
     * 1.5x speed boost plus per-tick trampling of nearby non-player mobs (see {@link #tick()}) that persists
     * until the elephant unloads, exactly as in legacy (sprintCounter is never incremented). No-op if already set.
     */
    public void whipCharge() {
        if (this.sprintCounter == 0) {
            this.sprintCounter = 1;
            this.entityData.set(SPRINTING, true);
        }
    }
}
