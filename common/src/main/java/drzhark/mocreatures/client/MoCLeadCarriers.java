package drzhark.mocreatures.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side registry of which players are currently lead-carrying a kitty
 * ({@code MoCEntityKitty.STATE_HELD_UPSIDE_DOWN}), consumed by the per-loader {@code HumanoidModelMixin}s
 * to keep the carrying arm from doing the walk swing.
 *
 * <p>The carried kitty is the sole writer: its CLIENT tick calls {@link #mark} every tick while it
 * dangles ({@code MoCEntityKitty.tick}), stamping the carrier's entity id with the current game time.
 * The reader is the player-model mixin, which runs per frame per rendered player: an id counts as
 * carrying while its stamp is at most {@link #STALE_AFTER_TICKS} ticks old. Entries therefore expire on
 * their own the moment the kitty is set down, unloaded, or its carrier logs off — no unmark path exists
 * to be forgotten — while a paused singleplayer client keeps the pose, because game time freezes with
 * the ticks that would refresh the stamp. Both writer and reader run on the client main thread, so the
 * plain map needs no synchronization.
 *
 * <p>Deliberately free of ALL Minecraft imports (ints, longs and a map only): it is referenced from
 * common entity code ({@code MoCEntityKitty.tick}), so even if a dedicated server's classloader touches
 * it, nothing client-only is dragged in — and the {@code isClientSide} guard around the one writer
 * keeps it empty there.
 */
public final class MoCLeadCarriers {

    /** How many ticks a stamp stays valid without being refreshed by the kitty's client tick. */
    private static final long STALE_AFTER_TICKS = 2L;

    /** Carrier entity id -> game time of the last tick a lead-carried kitty confirmed the carry. */
    private static final Map<Integer, Long> LAST_SEEN_AT = new HashMap<>();

    private MoCLeadCarriers() {
    }

    /** Stamps {@code carrierId} as lead-carrying a kitty at {@code gameTime}. Client tick path only. */
    public static void mark(int carrierId, long gameTime) {
        LAST_SEEN_AT.put(carrierId, gameTime);
        // Housekeeping for ids that are never queried again (carrier logged off mid-carry, world left):
        // only ever a handful of carriers exist at once, so any growth beyond that is stale baggage.
        if (LAST_SEEN_AT.size() > 16) {
            LAST_SEEN_AT.values().removeIf(seenAt -> Math.abs(gameTime - seenAt) > STALE_AFTER_TICKS);
        }
    }

    /**
     * Whether {@code carrierId} was stamped within the last {@link #STALE_AFTER_TICKS} ticks of
     * {@code gameTime}. The absolute-difference test also invalidates stamps left over from another
     * world's clock (rejoining a different server resets game time in either direction).
     */
    public static boolean isLeadCarrying(int carrierId, long gameTime) {
        Long seenAt = LAST_SEEN_AT.get(carrierId);
        if (seenAt == null) {
            return false;
        }
        if (Math.abs(gameTime - seenAt) > STALE_AFTER_TICKS) {
            LAST_SEEN_AT.remove(carrierId);
            return false;
        }
        return true;
    }
}
