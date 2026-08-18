package drzhark.mocreatures.util;

import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Duck interface stitched onto vanilla {@link net.minecraft.world.entity.projectile.FishingHook} by the
 * per-loader {@code FishingHookMixin}s. Legacy {@code MoCEntityAquatic.getFished()}:672-686 hooked a fish
 * onto a bobber by writing {@code fishHook.caughtEntity = this} directly; that field's 26.2 equivalent
 * ({@code hookedIn}) only has a public getter ({@code FishingHook.getHookedIn()}:546) while its setter is
 * private ({@code setHookedEntity}:292), so {@code MoCAquatic}'s bite logic reaches the setter through this
 * accessor instead.
 */
public interface FishingHookAccess {

    /** Hooks {@code entity} onto this bobber ({@code null} unhooks it) via the private {@code setHookedEntity}. */
    void moc$setHookedEntity(@Nullable Entity entity);
}
