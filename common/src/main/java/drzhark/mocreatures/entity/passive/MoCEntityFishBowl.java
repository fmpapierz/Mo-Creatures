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

    public MoCEntityFishBowl(EntityType<? extends MoCEntityFishBowl> type, Level level) {
        super(type, level);
        this.setNoAi(true);
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
     * Legacy {@code MoCEntityFishBowl.attackEntityFrom} returned {@code false} unconditionally: a placed bowl
     * is immune to every damage source (mobs, players, fire, lava, cactus, drowning, etc.) and can only be
     * removed by right-clicking it with a pickaxe. Overriding the server damage entry to return false
     * reproduces that — a bare MAX_HEALTH value would still let the bowl (and its captured fishy) be killed.
     * Mirrors {@link MoCEntityKittyBed#hurtServer}.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
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
    }
}
