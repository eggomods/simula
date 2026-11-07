package io.github.eggohito.simula.attachment.entity;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.mixin.access.EntityAccessor;
import io.github.eggohito.simula.layer.LayerHolder;
import io.github.eggohito.simula.layer.manager.LayerManager;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.manager.OriginManager;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Map;

public record OriginsAttachment(ImmutableMap<LayerHolder, OriginHolder> origins, boolean hadOriginsBefore) {

	public static final Codec<OriginsAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Entry.CODEC.listOf().comapFlatMap(Entry::unpackAll, Entry::packAll).fieldOf("origins").forGetter(OriginsAttachment::origins),
		Codec.BOOL.optionalFieldOf("had_origins_before", false).forGetter(OriginsAttachment::hadOriginsBefore)
	).apply(instance, OriginsAttachment::new));

	public static final StreamCodec<ByteBuf, OriginsAttachment> STREAM_CODEC = StreamCodec.composite(
		Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(entries -> Entry.unpackAll(entries).getOrThrow(), Entry::packAll), OriginsAttachment::origins,
		ByteBufCodecs.BOOL, OriginsAttachment::hadOriginsBefore,
		OriginsAttachment::new
	);

	public static void onChanged(Entity entity, OriginsAttachment oldValue, OriginsAttachment newValue) {

		//  If there is no old value and a new value, it means the attachment was initialized
		if (oldValue == null && newValue != null && !((EntityAccessor) entity).isFirstTick()) {
			//  TODO: add power type for origin chosen callback
		}

	}

	record Entry(ResourceLocation layer, ResourceLocation origin) {

		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("layer").forGetter(Entry::layer),
			ResourceLocation.CODEC.fieldOf("origin").forGetter(Entry::layer)
		).apply(instance, Entry::new));

		public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, Entry::layer,
			ResourceLocation.STREAM_CODEC, Entry::origin,
			Entry::new
		);

		private static List<Entry> packAll(Map<LayerHolder, OriginHolder> origins) {

			List<Entry> entries = new ObjectArrayList<>();
			origins.forEach((layer, origin) -> entries.add(new Entry(layer.id(), origin.id())));

			return entries;

		}

		//  TODO:   Pass the identity as the result since decoding/encoding data attachments in FAPI in version
		//          26.1.x now promotes its partial result
		private static DataResult<ImmutableMap<LayerHolder, OriginHolder>> unpackAll(List<Entry> entries) {

			Map<LayerHolder, OriginHolder> origins = new Object2ObjectLinkedOpenHashMap<>();
			DataResult<Unit> identity = DataResult.success(Unit.INSTANCE);

			for (var entry : entries) {

				DataResult<LayerHolder> layerResult = LayerManager.getInstance().getAsResult(entry.layer());
				identity = identity.apply2stable((unit, ignored) -> unit, layerResult);

				if (layerResult.isError()) {
					continue;
				}

				DataResult<OriginHolder> originResult = OriginManager.getInstance().getAsResult(entry.origin());
				identity = identity.apply2stable((unit, ignored) -> unit, originResult);

				if (originResult.isError()) {
					continue;
				}

				origins.put(layerResult.getOrThrow(), originResult.getOrThrow());

			}

			return DataResult.success(ImmutableMap.copyOf(origins));

		}

	}

}
