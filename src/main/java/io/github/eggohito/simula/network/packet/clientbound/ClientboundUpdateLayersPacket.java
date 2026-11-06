package io.github.eggohito.simula.network.packet.clientbound;

import io.github.eggohito.simula.Simula;
import io.github.eggohito.simula.layer.LayerHolder;
import io.github.eggohito.simula.layer.manager.LayerManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record ClientboundUpdateLayersPacket(Map<ResourceLocation, LayerHolder> layers) implements CustomPacketPayload {

	public static final Type<ClientboundUpdateLayersPacket> TYPE = new Type<>(LayerManager.ID.withPath(path -> "clientbound/" + path + "/update"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUpdateLayersPacket> CODEC = StreamCodec.ofMember(ClientboundUpdateLayersPacket::send, ClientboundUpdateLayersPacket::receive);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void send(RegistryFriendlyByteBuf buf) {

		buf.writeInt(layers().size());

		for (var entry : layers().values()) {

			ResourceLocation.STREAM_CODEC.encode(buf, entry.id());

			try {
				LayerHolder.STREAM_CODEC.encode(buf, entry);
			}

			catch (Exception e) {
				Simula.LOGGER.error("Could not encode layer \"{}\" during the syncing process", entry.id(), e);
				throw e;
			}

		}

	}

	private static ClientboundUpdateLayersPacket receive(RegistryFriendlyByteBuf buf) {

		int count = buf.readInt();
		Map<ResourceLocation, LayerHolder> layers = new Object2ObjectLinkedOpenHashMap<>(count);

		for (int i = 0; i < count; i++) {

			ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);

			try {
				layers.put(id, LayerHolder.STREAM_CODEC.decode(buf));
			}

			catch (Exception e) {
				Simula.LOGGER.error("Could not decode layer \"{}\" during the syncing process", id, e);
				throw e;
			}

		}

		return new ClientboundUpdateLayersPacket(layers);

	}

}
