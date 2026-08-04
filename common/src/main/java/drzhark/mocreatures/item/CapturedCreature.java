package drzhark.mocreatures.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The data-component payload stored inside a full Mo'Creatures amulet — the captured creature's
 * entity-type id plus the key Mo'Creatures state (coat/sub-type, tamed flag, owner) plus preserved
 * vitals (health, MoCAge, custom name, adult/baby flag) so it can be released faithfully. (Full
 * entity NBT is intentionally not stored; species + variant + ownership + health/age/name/adult are
 * preserved.)
 *
 * <p>Legacy parity: MoCTools.dropAmulet stored {@code nbtt.setBoolean("Adult", entity.getIsAdult())}
 * and MoCItemAmulet.onItemRightClick restored it via {@code storedCreature.setAdult(adult)}, so a
 * captured juvenile is released as a juvenile rather than snapping to adult (MoCAnimal's ADULT
 * synched data defaults to true).</p>
 */
public record CapturedCreature(String typeId, int variant, boolean tamed, String owner,
                               float health, int age, String customName, int armor, boolean saddled,
                               boolean adult) {

    /**
     * Backward-compatible constructor for callers that pre-date the adult/baby flag. Defaults
     * {@code adult} to {@code true} (matching MoCAnimal's ADULT synched-data default) so existing
     * amulet code compiles unchanged; the capture path should prefer the canonical 10-arg
     * constructor and pass the creature's real {@code getIsAdult()} state.
     */
    public CapturedCreature(String typeId, int variant, boolean tamed, String owner,
                            float health, int age, String customName, int armor, boolean saddled) {
        this(typeId, variant, tamed, owner, health, age, customName, armor, saddled, true);
    }

    public static final Codec<CapturedCreature> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("type").forGetter(CapturedCreature::typeId),
            Codec.INT.fieldOf("variant").forGetter(CapturedCreature::variant),
            Codec.BOOL.fieldOf("tamed").forGetter(CapturedCreature::tamed),
            Codec.STRING.fieldOf("owner").forGetter(CapturedCreature::owner),
            Codec.FLOAT.fieldOf("health").forGetter(CapturedCreature::health),
            Codec.INT.fieldOf("age").forGetter(CapturedCreature::age),
            Codec.STRING.fieldOf("customName").forGetter(CapturedCreature::customName),
            Codec.INT.optionalFieldOf("armor", 0).forGetter(CapturedCreature::armor),
            Codec.BOOL.optionalFieldOf("saddled", false).forGetter(CapturedCreature::saddled),
            Codec.BOOL.optionalFieldOf("adult", true).forGetter(CapturedCreature::adult)
    ).apply(i, CapturedCreature::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CapturedCreature> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CapturedCreature::typeId,
            ByteBufCodecs.VAR_INT, CapturedCreature::variant,
            ByteBufCodecs.BOOL, CapturedCreature::tamed,
            ByteBufCodecs.STRING_UTF8, CapturedCreature::owner,
            ByteBufCodecs.FLOAT, CapturedCreature::health,
            ByteBufCodecs.VAR_INT, CapturedCreature::age,
            ByteBufCodecs.STRING_UTF8, CapturedCreature::customName,
            ByteBufCodecs.VAR_INT, CapturedCreature::armor,
            ByteBufCodecs.BOOL, CapturedCreature::saddled,
            ByteBufCodecs.BOOL, CapturedCreature::adult,
            CapturedCreature::new);
}
