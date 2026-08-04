package drzhark.mocreatures.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server packet: name a tamed creature. Carries the target entity's network id and the
 * chosen name (entered in {@code MoCNameScreen}).
 */
public record MoCNamePayload(int entityId, String name) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MoCNamePayload> TYPE = new CustomPacketPayload.Type<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("mocreatures", "name_creature"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoCNamePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MoCNamePayload::entityId,
            ByteBufCodecs.STRING_UTF8, MoCNamePayload::name,
            MoCNamePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
