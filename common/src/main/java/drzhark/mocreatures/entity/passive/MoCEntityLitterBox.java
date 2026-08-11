package drzhark.mocreatures.entity.passive;

import java.util.List;

import drzhark.mocreatures.entity.MoCAnimal;
import drzhark.mocreatures.entity.monster.MoCEntityOgre;
import drzhark.mocreatures.registry.MoCItems;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Port of the legacy {@code MoCEntityLitterBox}. A static piece of placeable furniture the kitty
 * uses. It has no AI and cannot be pushed. It carries a synched {@code used} flag: once a kitty uses
 * it the box becomes dirty (rendered with the "used" litter) and stays dirty until it is cleaned.
 * Faithful to the legacy entity it restores the signature behaviours of a dirty box:
 *
 * <ul>
 *   <li>while dirty it smoulders (smoke particles), lures every hostile mob within a 12x4x12 box
 *       onto itself (defusing creepers so they home in without exploding), and auto-cleans after
 *       ~5000 ticks;</li>
 *   <li>right-click with <b>sand</b> to scoop it clean (consuming one sand);</li>
 *   <li>right-click with any <b>pickaxe</b> to pick the box back up as its item;</li>
 *   <li>right-click empty-handed (or with anything else) to carry the box on your head / set it down.</li>
 * </ul>
 */
public class MoCEntityLitterBox extends MoCAnimal {

    private static final EntityDataAccessor<Boolean> USED =
            SynchedEntityData.defineId(MoCEntityLitterBox.class, EntityDataSerializers.BOOLEAN);

    /** Server-side festering timer; a dirty box auto-cleans once this passes 5000 (legacy littertime). */
    private int littertime;

    public MoCEntityLitterBox(EntityType<? extends MoCEntityLitterBox> type, Level level) {
        super(type, level);
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                // Legacy MoCEntityLitterBox.getMaxHealth() == 20 (matches the kitty bed's furniture toughness).
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
        builder.define(USED, false);
    }

    @Override
    public void selectType() {
        if (getTypeMoC() == 0) {
            setTypeMoC(1);
        }
    }

    @Override
    public Identifier getTexture() {
        return modelTexture("litterbox.png");
    }

    /** Whether the litter box is dirty and needs cleaning. */
    public boolean getUsed() {
        return this.entityData.get(USED);
    }

    public void setUsed(boolean used) {
        this.entityData.set(USED, used);
    }

    // ------------------------------------------------------------------------------------- tick

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        // Legacy MoCEntityLitterBox.onUpdate: while dirty the box smoulders, and the stench draws in
        // every hostile mob within a 12x4x12 box, pointing them at the litter box itself. Creepers are
        // defused (swell dir -1) so they home in without blowing the box apart.
        if (getUsed() && this.level() instanceof ServerLevel serverLevel) {
            this.littertime++;
            serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
            List<Monster> mobs = serverLevel.getEntitiesOfClass(Monster.class,
                    this.getBoundingBox().inflate(12.0D, 4.0D, 12.0D));
            for (Monster mob : mobs) {
                mob.setTarget(this);
                if (mob instanceof Creeper creeper) {
                    creeper.setSwellDir(-1);
                }
                // Legacy cleared the ogre's pendingSmashAttack so a lured ogre abandons its in-progress
                // smash and lumbers over to the box instead of pulverising where it stands.
                if (mob instanceof MoCEntityOgre ogre) {
                    ogre.resetSmash();
                }
            }
        }
        // Auto-clean once the litter has festered long enough (legacy littertime > 5000).
        if (this.littertime > 5000) {
            setUsed(false);
            this.littertime = 0;
        }
    }

    // ------------------------------------------------------------------------------- interaction

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        final boolean server = !this.level().isClientSide();

        // Any pickaxe -> pick the litter box back up as its item and remove it (legacy dropped
        // MoCreatures.litterbox, the same item that places it back down).
        if (stack.is(ItemTags.PICKAXES)) {
            if (server) {
                ItemStack drop = new ItemStack(MoCItems.KITTYLITTER.get());
                if (!player.addItem(drop)) {
                    player.drop(drop, false);
                }
                this.playSound(SoundEvents.ITEM_PICKUP, 0.2F,
                        ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                this.discard();
            }
            return InteractionResult.SUCCESS;
        }
        // Sand -> scoop the box clean, consuming one sand (legacy required Block.sand specifically; red
        // sand does not clean it).
        if (stack.is(Items.SAND)) {
            if (server) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                setUsed(false);
                this.littertime = 0;
            }
            return InteractionResult.SUCCESS;
        }
        // Otherwise: carry the litter box on the player's head, or set it back down (legacy mount toggle).
        if (server) {
            // Shared carry toggle. The furniture used the same "ride the player" idiom the pets did, and it
            // was broken for exactly the same reason: a player cannot be a vehicle on a 26.2 server.
            toggleCarry(player, false);
        }
        return InteractionResult.SUCCESS;
    }

    // ------------------------------------------------------------------------------------- damage

    /**
     * Legacy {@code MoCEntityLitterBox.attackEntityFrom} returned {@code false}: the box is
     * indestructible furniture and can only be removed by right-clicking it with a pickaxe. Overriding
     * the server damage entry to return false reproduces that (a bare MAX_HEALTH bump would still let it
     * be killed) — and, notably, keeps the lured-in hostile mobs from destroying the box they home in on.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    // ------------------------------------------------------------------------------ push behaviour

    // Static furniture isn't shoved around by other entities (legacy staticLitter, default on). Setting
    // staticLitter=false lets the litter box be pushed like a loose entity.
    @Override
    public boolean isPushable() {
        return !drzhark.mocreatures.config.MoCConfig.get().staticLitter;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    // ------------------------------------------------------------------------------- persistence

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Used", getUsed());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setUsed(input.getBooleanOr("Used", false));
    }
}
