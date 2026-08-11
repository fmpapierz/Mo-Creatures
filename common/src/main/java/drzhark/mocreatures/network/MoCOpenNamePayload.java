package drzhark.mocreatures.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client packet: open the naming screen for a creature the player has just tamed. Legacy
 * {@code MoCTools.tameWithName} sent this on EVERY successful tame, whatever the route — feeding, a
 * medallion, picking a creature up, breaking in a mount, hatching an egg or releasing a fish bowl — so
 * naming a new pet was part of taming it rather than a separate medallion right-click.
 */
public record MoCOpenNamePayload(int entityId, String currentName) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MoCOpenNamePayload> TYPE = new CustomPacketPayload.Type<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("mocreatures", "open_name_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MoCOpenNamePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MoCOpenNamePayload::entityId,
            ByteBufCodecs.STRING_UTF8, MoCOpenNamePayload::currentName,
            MoCOpenNamePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
