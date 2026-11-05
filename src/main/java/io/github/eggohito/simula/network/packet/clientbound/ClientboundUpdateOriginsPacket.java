package io.github.eggohito.simula.network.packet.clientbound;

import io.github.eggohito.simula.Simula;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.manager.OriginManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ClientboundUpdateOriginsPacket(Map<ResourceLocation, OriginHolder> origins, Map<ResourceLocation, List<OriginHolder>> tags) implements CustomPacketPayload {

	public static final Type<ClientboundUpdateOriginsPacket> TYPE = new Type<>(OriginManager.ID.withPath(path -> "clientbound/" + path + "/update"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUpdateOriginsPacket> CODEC = StreamCodec.ofMember(ClientboundUpdateOriginsPacket::send, ClientboundUpdateOriginsPacket::receive);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void send(RegistryFriendlyByteBuf buf) {

		buf.writeInt(origins().size());
		buf.writeInt(tags().size());

		for (var entry : origins().entrySet()) {

			ResourceLocation id = entry.getKey();
			ResourceLocation.STREAM_CODEC.encode(buf, id);

			try {
				OriginHolder.STREAM_CODEC.encode(buf, entry.getValue());
			}

			catch (Exception e) {
				Simula.LOGGER.error("Couldn't encode origin \"{}\" during the syncing process", id, e);
				throw e;
			}

		}

		for (var entry : tags().entrySet()) {

			ResourceLocation id = entry.getKey();
			List<OriginHolder> holders = entry.getValue();

			ResourceLocation.STREAM_CODEC.encode(buf, id);
			buf.writeInt(holders.size());

			for (var holder: holders) {
				ResourceLocation.STREAM_CODEC.encode(buf, holder.id());
			}

		}

	}

	private static ClientboundUpdateOriginsPacket receive(RegistryFriendlyByteBuf buf) {

		int originsCount = buf.readInt();
		int tagsCount = buf.readInt();

		Map<ResourceLocation, OriginHolder> origins = new Object2ObjectLinkedOpenHashMap<>(originsCount);
		Map<ResourceLocation, List<OriginHolder>> tags = new Object2ObjectLinkedOpenHashMap<>(tagsCount);

		for (int i = 0; i < originsCount; i++) {

			ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);

			try {
				origins.put(id, OriginHolder.STREAM_CODEC.decode(buf));
			}

			catch (Exception e) {
				Simula.LOGGER.error("Couldn't decode origin \"{}\" during the syncing process", id, e);
				throw e;
			}

		}

		for (int i = 0; i < tagsCount; i++) {

			ResourceLocation tagId = ResourceLocation.STREAM_CODEC.decode(buf);
			int count = buf.readInt();

			for (int j = 0; j < count; j++) {

				try {

					ResourceLocation originId = ResourceLocation.STREAM_CODEC.decode(buf);
					OriginHolder holder = Objects.requireNonNull(origins.get(originId), "Unknown origin: \"" + originId + "\"");

					tags
						.computeIfAbsent(tagId, k -> new ObjectArrayList<>())
						.add(holder);

				}

				catch (Exception e) {
					Simula.LOGGER.error("Couldn't decode origin tag \"{}\" during the syncing process", tagId, e);
					throw e;
				}

			}

		}

		return new ClientboundUpdateOriginsPacket(origins, tags);

	}

}
