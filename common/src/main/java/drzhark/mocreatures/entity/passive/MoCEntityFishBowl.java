package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.item.MoCFishBowlItem;
import drzhark.mocreatures.registry.MoCItems;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Port of the legacy {@code MoCEntityFishBowl}. A placeable fish bowl — static, no-AI furniture that
 * holds a single captured fishy (type 1-10) and shows it swimming inside the glass. Modelled on the
 * lightweight no-AI {@link MoCEntityKittyBed} pattern (extends {@link MoCAnimal}, no goals, unpushable,
 * non-despawning). Right-click with a pickaxe to pick the bowl back up as its matching item.
 *
 * <p>The legacy "carry the bowl on your head" mounting behaviour is intentionally not ported.
 */
public class MoCEntityFishBowl extends MoCAnimal {

    /** The fishy type held in the bowl (1-10), separate from {@link MoCAnimal}'s shared TYPE field. */
    private static final EntityDataAccessor<Integer> BOWL_TYPE =
            SynchedEntityData.defineId(MoCEntityFishBowl.class, EntityDataSerializers.INT);

    /** Legacy cosmetic swim state: the current turn angle (degrees, 0-360) and whether the fish is moving. */
    private int swimRotation;
    private boolean swimming;

    /**
     * No {@code setNoAi(true)}, although the bowl has no goals: in 26.2 a NoAI mob never runs {@code travel} —
     * the only place gravity is applied ({@code LivingEntity.aiStep}, mc262-ref LivingEntity.java:3110, gated on
     * {@code isEffectiveAi()} = {@code !isNoAi()}, Mob.java:1301-1302) — so a bowl placed on the side of a block
     * hung in mid-air forever. Legacy was an ordinary entity that fell and then held still (see {@link #move}).
     * Same fix as {@link MoCEntityKittyBed}.
     */
    public MoCEntityFishBowl(EntityType<? extends MoCEntityFishBowl> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOWL_TYPE, 1);
    }

    /** Static furniture: no goals at all. */
    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        // Clear any grass or flower the bowl came to rest in, so it doesn't grow up through the glass.
        if (this.level() instanceof ServerLevel serverLevel && this.onGround()) {
            breakPlantsInside(serverLevel);
        }
        // Faithful legacy swim: ~every 80 ticks toggle whether the fish is swimming; while swimming, turn
        // 0-9 degrees per tick and wrap at 360. Client and server tick independently, but only the client's
        // value is ever rendered, so no syncing is needed for this purely cosmetic circling.
        if (this.random.nextInt(80) == 0) {
            this.swimming = !this.swimming;
        }
        if (this.swimming) {
            this.swimRotation += this.random.nextInt(10);
            if (this.swimRotation > 360) {
                this.swimRotation = 0;
            }
        }
    }

    /** Current fish swim turn angle in degrees (0-360); drives the in-bowl fish circling animation. */
    public int getSwimRotation() {
        return this.swimRotation;
    }

    @Override
    public void selectType() {
        // no-op: the bowl type is set explicitly by the placing item.
    }

    public int getBowlType() {
        return this.entityData.get(BOWL_TYPE);
    }

    public void setType(int type) {
        this.entityData.set(BOWL_TYPE, type);
    }

    /**
     * Pins every yaw (body, head, and their previous-tick values) to {@code yaw} so the placed bowl renders
     * at exactly its facing with no interpolation wobble. Called by the placing item right after spawn.
     */
    public void lockRotation(float yaw) {
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setYBodyRot(yaw);
        this.yBodyRotO = yaw;
        this.setYHeadRot(yaw);
        this.yHeadRotO = yaw;
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("fishbowl.png");
    }

    // Static furniture must not be shoved around by other entities.
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    /**
     * Legacy {@code MoCEntityFishBowl.moveEntity} (5.1.5 :242-251) ran the move only while the bowl was carried
     * or airborne, so it fell into place and then ignored every shove. Same gate as
     * {@link MoCEntityKittyBed#move}: a grounded bowl keeps only the DOWNWARD part of its own motion (so it still
     * drops if its floor is removed), and the discarded momentum is cleared so a current can't bank it.
     */
    @Override
    public void move(net.minecraft.world.entity.MoverType type, net.minecraft.world.phys.Vec3 delta) {
        if (type == net.minecraft.world.entity.MoverType.SELF && this.onGround()) {
            this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
            delta = new net.minecraft.world.phys.Vec3(0.0D, Math.min(0.0D, delta.y), 0.0D);
        }
        super.move(type, delta);
    }

    /** Legacy {@code fall()} was empty: a dropped bowl makes no fall sound or damage roll. */
    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    /** Legacy {@code canBreatheUnderwater() == true}: a bowl set down in water does not drown. */
    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    /**
     * Legacy {@code MoCEntityFishBowl.attackEntityFrom} returned {@code false} unconditionally: no mob, arrow,
     * fire, lava, cactus or drowning can destroy a placed bowl (or the fish in it). The one exception here is
     * damage tagged {@code BYPASSES_INVULNERABILITY} — {@code /kill} and the void — which the blanket
     * {@code false} used to block too ({@code LivingEntity.kill} IS {@code hurtServer(genericKill, MAX_VALUE)},
     * mc262-ref LivingEntity.java:317-319), leaving a bowl that nothing could remove. Players break it through
     * {@link #skipAttackInteraction}. Mirrors {@link MoCEntityKittyBed#hurtServer}.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                && super.hurtServer(level, source, amount);
    }

    /**
     * A player's hit breaks the bowl and drops it as its item WITH its contents (the matching empty / water /
     * fish bowl, {@link MoCFishBowlItem#toItemStack}), or simply removes it in creative — the way vanilla
     * paintings and item frames break. Legacy's only removal was the pickaxe right-click (still in
     * {@link #mobInteract}); punching did nothing. This hook rather than {@link #hurtServer} so a sword sweep or a
     * mob never breaks it (Player.attack consults it only for the entity actually struck), and spawn protection
     * is honoured through {@code mayInteract}. Mirrors {@link MoCEntityKittyBed#skipAttackInteraction}.
     */
    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!(attacker instanceof Player player)) {
            return false;
        }
        if (this.level() instanceof ServerLevel serverLevel && !this.isRemoved()
                && serverLevel.mayInteract(player, this.blockPosition())) {
            if (!player.hasInfiniteMaterials()) {
                this.spawnAtLocation(serverLevel, MoCFishBowlItem.toItemStack(getBowlType()));
            }
            this.playSound(SoundEvents.ITEM_PICKUP, 0.2F,
                    ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            this.discard();
        }
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Right-click with any pickaxe to pick the bowl back up as its matching item.
        if (stack.is(ItemTags.PICKAXES)) {
            if (!this.level().isClientSide()) {
                player.addItem(MoCFishBowlItem.toItemStack(getBowlType()));
                this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP,
                        SoundSource.NEUTRAL, 0.2F,
                        ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                this.discard();
            }
            return InteractionResult.SUCCESS;
        }
        // Legacy: right-click a stocked bowl (type 1-10) with an empty or water bowl to extract the fish
        // as a portable filled fishbowl item — consumes one held bowl and empties the placed bowl (type 0).
        if (getBowlType() > 0 && getBowlType() < 11
                && (stack.getItem() == MoCItems.FISHBOWL_EMPTY.get()
                        || stack.getItem() == MoCItems.FISHBOWL_WATER.get())) {
            if (!this.level().isClientSide()) {
                stack.shrink(1);
                player.addItem(MoCFishBowlItem.toItemStack(getBowlType()));
                setType(0);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("BowlType", getBowlType());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setType(input.getIntOr("BowlType", 1));
        // Bowls placed by earlier builds were saved with NoAI:1b (Mob.addAdditionalSaveData writes it) and would
        // reload frozen in mid-air. The bowl has no AI to disable, so the flag only ever meant "no gravity".
        this.setNoAi(false);
    }
}
