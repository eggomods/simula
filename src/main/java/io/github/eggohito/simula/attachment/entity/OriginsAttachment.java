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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;

import java.util.Map;

public record OriginsAttachment(ImmutableMap<LayerHolder, OriginHolder> origins, boolean hadOriginsBefore) {

	private static final Codec<Map<ResourceLocation, ResourceLocation>> ID_CODEC_MAP = Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC);
	private static final StreamCodec<ByteBuf, Map<ResourceLocation, ResourceLocation>> ID_STREAM_CODEC_MAP = ByteBufCodecs.map(Object2ObjectLinkedOpenHashMap::new, ResourceLocation.STREAM_CODEC, ResourceLocation.STREAM_CODEC);

	public static final Codec<OriginsAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ID_CODEC_MAP.comapFlatMap(OriginsAttachment::unpackOrigins, OriginsAttachment::packOrigins).fieldOf("origins").forGetter(OriginsAttachment::origins),
		Codec.BOOL.optionalFieldOf("had_origins_before", false).forGetter(OriginsAttachment::hadOriginsBefore)
	).apply(instance, OriginsAttachment::new));

	public static final StreamCodec<ByteBuf, OriginsAttachment> STREAM_CODEC = StreamCodec.composite(
		ID_STREAM_CODEC_MAP.map(map -> unpackOrigins(map).getOrThrow(), OriginsAttachment::packOrigins), OriginsAttachment::origins,
		ByteBufCodecs.BOOL, OriginsAttachment::hadOriginsBefore,
		OriginsAttachment::new
	);

	public static void onChanged(Entity entity, OriginsAttachment oldValue, OriginsAttachment newValue) {

		//  If there is no old value and a new value, it means the attachment was initialized
		if (oldValue == null && newValue != null && !((EntityAccessor) entity).isFirstTick()) {
			//  TODO: add power type for origin chosen callback
		}

	}

	private static ImmutableMap<ResourceLocation, ResourceLocation> packOrigins(ImmutableMap<LayerHolder, OriginHolder> origins) {

		ImmutableMap.Builder<ResourceLocation, ResourceLocation> builder = ImmutableMap.builder();
		origins.forEach((layer, origin) -> builder.put(layer.id(), origin.id()));

		return builder.build();

	}

	//  TODO:   Pass the identity as the result since decoding/encoding data attachments in FAPI in version
	//          26.1.x now promotes its partial result
	private static DataResult<ImmutableMap<LayerHolder, OriginHolder>> unpackOrigins(Map<ResourceLocation, ResourceLocation> map) {

		Map<LayerHolder, OriginHolder> origins = new Object2ObjectLinkedOpenHashMap<>();
		DataResult<Unit> identity = DataResult.success(Unit.INSTANCE);

		for (var entry : map.entrySet()) {

			ResourceLocation layerId = entry.getKey();
			DataResult<LayerHolder> layerResult = LayerManager.getInstance().getAsResult(layerId);

			identity = identity.apply2stable((unit, ignored) -> unit, layerResult);

			if (layerResult.isError()) {
				continue;
			}

			ResourceLocation originId = entry.getValue();
			DataResult<OriginHolder> originResult = OriginManager.getInstance().getAsResult(originId);

			identity = identity.apply2stable((unit, ignored) -> unit, originResult);

			if (originResult.isError()) {
				continue;
			}

			origins.put(layerResult.getOrThrow(), originResult.getOrThrow());

		}

		return DataResult.success(ImmutableMap.copyOf(origins));

	}

}
