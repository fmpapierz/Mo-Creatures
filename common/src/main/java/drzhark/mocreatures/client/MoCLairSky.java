package drzhark.mocreatures.client;

import drzhark.mocreatures.MoCreatures;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Client-side special-effects descriptor for the Wyvern Lair dimension
 * ({@code mocreatures:wyvern_lair}) — the 26.2 successor to the legacy
 * {@code MoCSkyRenderer} / {@code WorldProviderWyvernEnd} twin-suns sky.
 *
 * <h2>Why this is a plain descriptor, not a {@code DimensionSpecialEffects} subclass</h2>
 * The task brief asked for a {@code net.minecraft.client.renderer.DimensionSpecialEffects}
 * subclass "or the 26.2 equivalent". Verified against the merged 26.2 client jar
 * ({@code javap}): <b>{@code DimensionSpecialEffects} no longer exists in MC 26.2</b>, and
 * neither does any {@code net.minecraft.client.renderer.Dimension*Effects} class. The whole
 * per-dimension client visual model was reworked into two data-driven mechanisms:
 * <ul>
 *   <li>{@code net.minecraft.world.level.dimension.DimensionType$Skybox} — a fixed enum
 *       ({@code NONE} / {@code OVERWORLD} / {@code END}) chosen in the dimension-type JSON,
 *       which decides whether the vanilla void, overworld, or end skybox draws; and</li>
 *   <li>{@code net.minecraft.world.attribute.EnvironmentAttributeMap} /
 *       {@code net.minecraft.world.attribute.EnvironmentAttributes} — fog colour, sky colour,
 *       cloud colour, cloud height, ambient-light colour, sun/moon angles, star brightness,
 *       etc., all set from the dimension-type JSON {@code attributes} block.</li>
 * </ul>
 *
 * <p>Consequently there is <b>no common cross-loader class</b> a mod can subclass to override
 * the sky the way {@code DimensionSpecialEffects} used to allow. The only code hooks that
 * override sky drawing in 26.2 are loader-specific and use loader-specific render contexts,
 * so they cannot live in {@code common}:
 * <ul>
 *   <li><b>Fabric</b>: {@code net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry
 *       #registerSkyRenderer(ResourceKey&lt;Level&gt;, SkyRenderer)} — the {@code SkyRenderer}
 *       functional interface renders through a {@code WorldRenderContext}. (Note: this Fabric API
 *       build also still exposes {@code registerDimensionEffects(Identifier, class_5294)}, but its
 *       {@code class_5294} intermediary type has no official-mapped counterpart in the 26.2 dev jar,
 *       so it is not usable from this unobfuscated multiloader project.)</li>
 *   <li><b>NeoForge</b>: {@code net.neoforged.neoforge.client.event
 *       .RegisterCustomEnvironmentEffectRendererEvent#registerSkyboxRenderer(Identifier,
 *       CustomSkyboxRenderer)} — the {@code CustomSkyboxRenderer} functional interface renders
 *       through {@code LevelRenderState} / {@code SkyRenderState} / a {@code Matrix4fc}.</li>
 * </ul>
 *
 * <h2>What this class delivers</h2>
 * A distinctive, <b>void-less</b> Lair sky is already achieved purely from data: the
 * {@code data/mocreatures/dimension_type/wyvern_lair.json} sets {@code has_skylight: true}
 * (no {@code skybox} field → defaults to the overworld skybox, so there is a proper sun/moon/star
 * sky and no End-style void) plus a full custom colour palette in its {@code attributes} block
 * (custom {@code fog_color}, {@code sky_color}, {@code cloud_color}, {@code cloud_height},
 * {@code ambient_light_color}). The constants below <b>mirror that JSON</b> so the loader-specific
 * skybox renderers — which DO draw the twin-suns quad (Fabric SkyRendererMixin, NeoForge
 * MoCLairSkyboxRenderer) — reuse the exact same palette without drift.
 *
 * <p>Both loader entry points reference {@link #WYVERN_LAIR} and {@link #EFFECTS} when wiring up
 * their skybox renderer (see the per-loader wiring snippets returned to the orchestrator).</p>
 *
 * @see #WYVERN_LAIR
 * @see #TWIN_SUNS_TEXTURE
 */
public final class MoCLairSky {

    private MoCLairSky() {}

    /**
     * The Wyvern Lair level key. Mirrors {@code MoCStaffPortalItem.WYVERN_LAIR}; duplicated here so
     * client-only code does not have to reach into the item class (which pulls in server-side teleport
     * logic). Loader skybox renderers gate on this key to only affect the Lair.
     */
    public static final ResourceKey<Level> WYVERN_LAIR = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "wyvern_lair"));

    /**
     * Identifier the loader skybox renderers register under. Fabric's
     * {@code registerDimensionEffects} and NeoForge's {@code registerSkyboxRenderer} both key by a
     * plain {@link Identifier} (the dimension-type JSON's effects/skybox reference), not by the level
     * key, so this is the id both loaders share.
     */
    public static final Identifier EFFECTS =
            Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "wyvern_lair");

    /**
     * The shipped twin-suns sky texture ({@code assets/mocreatures/textures/misc/twinsuns.png}). It IS
     * rendered in-Lair by the loader-specific skybox renderers that consume this constant:
     * {@code fabric/.../mixin/SkyRendererMixin} (Fabric) and
     * {@code neoforge/.../client/MoCLairSkyboxRenderer} (NeoForge) both blit it onto a celestial quad via
     * the 26.2 render pipeline (confirmed in live-client sessions on both loaders). This common class stays a
     * plain descriptor — cross-loader code can't subclass the (removed) DimensionSpecialEffects, so the actual
     * draw lives per-loader as explained in the class javadoc.
     */
    public static final Identifier TWIN_SUNS_TEXTURE =
            Identifier.fromNamespaceAndPath(MoCreatures.MOD_ID, "textures/misc/twinsuns.png");

    // --- Palette: kept in lock-step with data/mocreatures/dimension_type/wyvern_lair.json ---
    // Values are 0xAARRGGBB where alpha is meaningful (cloud) and 0xRRGGBB otherwise.

    /** Sky colour — matches JSON {@code minecraft:visual/sky_color} "#86dcb6". */
    public static final int SKY_COLOR = 0x86DCB6;
    /** Fog colour — matches JSON {@code minecraft:visual/fog_color} "#9fe6c4". */
    public static final int FOG_COLOR = 0x9FE6C4;
    /** Cloud colour (ARGB) — matches JSON {@code minecraft:visual/cloud_color} "#ccffffff". */
    public static final int CLOUD_COLOR = 0xCCFFFFFF;
    /** Ambient-light tint — matches JSON {@code minecraft:visual/ambient_light_color} "#0a140f". */
    public static final int AMBIENT_LIGHT_COLOR = 0x0A140F;
    /** Cloud height — matches JSON {@code minecraft:visual/cloud_height} 192.0. */
    public static final float CLOUD_HEIGHT = 192.0F;

    /**
     * True if the given level key is the Wyvern Lair. Loader skybox renderers call this to decide
     * whether to apply the Lair sky. Null-safe.
     *
     * @param dimension the client level's dimension key (may be {@code null})
     * @return {@code true} iff {@code dimension} equals {@link #WYVERN_LAIR}
     */
    public static boolean isLair(ResourceKey<Level> dimension) {
        return WYVERN_LAIR.equals(dimension);
    }
}
