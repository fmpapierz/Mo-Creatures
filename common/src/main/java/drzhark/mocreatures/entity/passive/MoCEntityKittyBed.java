package drzhark.mocreatures.entity.passive;

import drzhark.mocreatures.registry.MoCItems;
import drzhark.mocreatures.registry.MoCSounds;
import drzhark.mocreatures.entity.MoCAnimal;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Port of the legacy {@code MoCEntityKittyBed}. A placeable piece of furniture — a bed the kitty
 * sleeps in and feeds from. It has no AI and cannot be pushed; it simply sits where the player placed
 * it. Faithful to the legacy entity it restores the milk/pet-food feeding, kitty-healing-while-nestled
 * and pickaxe-pickup mechanics:
 *
 * <ul>
 *   <li>Right-click with a <b>milk bucket</b> to fill the bed with milk (the bucket is emptied);</li>
 *   <li>Right-click with <b>pet food</b> to fill the bed with food;</li>
 *   <li>Right-click with any <b>pickaxe</b> to pick the bed back up as its item (setDead);</li>
 *   <li>Right-click empty-handed (or with anything else) to carry the bed on your head / set it down;</li>
 *   <li>while a kitty is nestled in a stocked bed it heals to full, slowly draining the supply.</li>
 * </ul>
 */
public class MoCEntityKittyBed extends MoCAnimal {

    /** Legacy datawatcher 16: the bed currently holds milk. */
    private static final EntityDataAccessor<Boolean> HAS_MILK =
            SynchedEntityData.defineId(MoCEntityKittyBed.class, EntityDataSerializers.BOOLEAN);
    /** Legacy datawatcher 15: the bed currently holds pet food. */
    private static final EntityDataAccessor<Boolean> HAS_FOOD =
            SynchedEntityData.defineId(MoCEntityKittyBed.class, EntityDataSerializers.BOOLEAN);
    /** Legacy datawatcher 18: dye/sheet colour, round-tripped through the kittybed item on pickup. */
    private static final EntityDataAccessor<Integer> SHEET_COLOUR =
            SynchedEntityData.defineId(MoCEntityKittyBed.class, EntityDataSerializers.INT);

    /** Server-side supply counter: rises +0.003/tick while a kitty feeds, emptying the bed past 2.0. */
    private float milkLevel;

    public MoCEntityKittyBed(EntityType<? extends MoCEntityKittyBed> type, Level level) {
        super(type, level);
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                // Legacy MoCEntityKittyBed.getMaxHealth() == 20.
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /** Static furniture: no goals at all. */
    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_MILK, false);
        builder.define(HAS_FOOD, false);
        builder.define(SHEET_COLOUR, 0);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(1);
        }
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("fullkittybed.png");
    }

    // ------------------------------------------------------------------------------ synched state

    public boolean getHasMilk() {
        return this.entityData.get(HAS_MILK);
    }

    public void setHasMilk(boolean hasMilk) {
        this.entityData.set(HAS_MILK, hasMilk);
    }

    public boolean getHasFood() {
        return this.entityData.get(HAS_FOOD);
    }

    public void setHasFood(boolean hasFood) {
        this.entityData.set(HAS_FOOD, hasFood);
    }

    public int getSheetColour() {
        return this.entityData.get(SHEET_COLOUR);
    }

    public void setSheetColour(int colour) {
        this.entityData.set(SHEET_COLOUR, colour);
    }

    /**
     * Maps the bed's sheet colour back to the item it should be picked up as. Colour 0 (plain) → the base
     * {@code kittybed}; 1..16 → the matching dyed kittybed (index = DyeColor id + 1). Legacy round-tripped the
     * colour through the single damage-keyed item; the 26.2 split uses one item per colour.
     */
    private static net.minecraft.world.item.Item bedItemForColour(int colour) {
        return switch (colour) {
            case 1 -> MoCItems.KITTYBED_WHITE.get();
            case 2 -> MoCItems.KITTYBED_ORANGE.get();
            case 3 -> MoCItems.KITTYBED_MAGENTA.get();
            case 4 -> MoCItems.KITTYBED_LIGHT_BLUE.get();
            case 5 -> MoCItems.KITTYBED_YELLOW.get();
            case 6 -> MoCItems.KITTYBED_LIME.get();
            case 7 -> MoCItems.KITTYBED_PINK.get();
            case 8 -> MoCItems.KITTYBED_GRAY.get();
            case 9 -> MoCItems.KITTYBED_SILVER.get();
            case 10 -> MoCItems.KITTYBED_CYAN.get();
            case 11 -> MoCItems.KITTYBED_PURPLE.get();
            case 12 -> MoCItems.KITTYBED_BLUE.get();
            case 13 -> MoCItems.KITTYBED_BROWN.get();
            case 14 -> MoCItems.KITTYBED_GREEN.get();
            case 15 -> MoCItems.KITTYBED_RED.get();
            case 16 -> MoCItems.KITTYBED_BLACK.get();
            default -> MoCItems.KITTYBED.get();
        };
    }

    // ------------------------------------------------------------------------------- interaction

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        final boolean server = !this.level().isClientSide();

        // Milk bucket -> load milk and hand back an empty bucket (legacy: bucketMilk -> bucketEmpty).
        if (stack.is(Items.MILK_BUCKET)) {
            if (server) {
                player.setItemInHand(hand,
                        ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                setHasMilk(true);
                setHasFood(false);
                this.playSound(MoCSounds.POURINGMILK.get(), 1.0F,
                        1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
            return InteractionResult.SUCCESS;
        }
        // Pet food -> load food (legacy only if the bed is not already stocked with food).
        if (stack.is(MoCItems.PETFOOD.get()) && !getHasFood()) {
            if (server) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                setHasMilk(false);
                setHasFood(true);
                this.playSound(MoCSounds.POURINGFOOD.get(), 1.0F,
                        1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            }
            return InteractionResult.SUCCESS;
        }
        // Any pickaxe -> pick the bed back up as its item (carrying its sheet colour) and remove it.
        if (stack.is(ItemTags.PICKAXES)) {
            if (server) {
                ItemStack drop = new ItemStack(bedItemForColour(getSheetColour()));
                if (!player.addItem(drop)) {
                    player.drop(drop, false);
                }
                this.playSound(SoundEvents.ITEM_PICKUP, 0.2F,
                        ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                this.discard();
            }
            return InteractionResult.SUCCESS;
        }
        // Otherwise: carry the bed on the player's head, or set it back down (legacy mount toggle).
        if (server) {
            this.setYRot(player.getYRot());
            if (this.isPassenger()) {
                this.stopRiding();
            } else if (!this.isVehicle() && !player.isPassenger()) {
                this.startRiding(player);
            }
            this.playSound(SoundEvents.CHICKEN_EGG, 1.0F,
                    (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------------------------- tick

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        // While a kitty is nestled in a stocked bed it heals to full and slowly drains the supply
        // (legacy MoCEntityKitty state 4 set health = maxHealth; the bed drained milklevel in onUpdate).
        if ((getHasMilk() || getHasFood()) && this.isVehicle()) {
            Entity passenger = this.getFirstPassenger();
            if (passenger instanceof MoCEntityKitty kitty) {
                kitty.heal(kitty.getMaxHealth());
            }
            milkLevel += 0.003F;
            if (milkLevel > 2.0F) {
                milkLevel = 0.0F;
                setHasMilk(false);
                setHasFood(false);
            }
        }
    }

    // ------------------------------------------------------------------------------------- damage

    /**
     * Legacy {@code MoCEntityKittyBed.attackEntityFrom} returned {@code false}: the bed is indestructible
     * furniture and can only be removed by right-clicking it with a pickaxe. Overriding the server damage
     * entry to return false reproduces that (a bare MAX_HEALTH bump would still let it be killed).
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    // ------------------------------------------------------------------------------ push behaviour

    // Static furniture isn't shoved around by other entities (legacy staticBed, default on). Setting
    // staticBed=false lets the bed be pushed like a loose entity.
    @Override
    public boolean isPushable() {
        return !drzhark.mocreatures.config.MoCConfig.get().staticBed;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    // ------------------------------------------------------------------------------- persistence

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("HasMilk", getHasMilk());
        output.putBoolean("HasFood", getHasFood());
        output.putInt("SheetColour", getSheetColour());
        output.putFloat("MilkLevel", milkLevel);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setHasMilk(input.getBooleanOr("HasMilk", false));
        setHasFood(input.getBooleanOr("HasFood", false));
        setSheetColour(input.getIntOr("SheetColour", 0));
        milkLevel = input.getFloatOr("MilkLevel", 0.0F);
    }
}
