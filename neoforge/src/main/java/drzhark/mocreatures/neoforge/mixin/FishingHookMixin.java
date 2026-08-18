package drzhark.mocreatures.neoforge.mixin;

import drzhark.mocreatures.util.FishingHookAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes vanilla's private {@code FishingHook.setHookedEntity} ({@code FishingHook.java}:292) through
 * {@link FishingHookAccess}, so a Mo'Creatures fish can bite a bobber — the modern stand-in for legacy
 * {@code MoCEntityAquatic.getFished()}:672-686 writing {@code fishHook.caughtEntity = this}, which has no
 * public 26.2 equivalent. Consumed by {@code MoCAquatic.customServerAiStep}. The Fabric counterpart is
 * {@code drzhark.mocreatures.fabric.mixin.FishingHookMixin}.
 */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin implements FishingHookAccess {

    @Shadow
    protected abstract void setHookedEntity(@Nullable Entity hookedIn);

    @Override
    public void moc$setHookedEntity(@Nullable Entity entity) {
        this.setHookedEntity(entity);
    }
}
