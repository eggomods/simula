package io.github.eggohito.simula.origin.manager;

import io.github.eggohito.kaya.util.manager.ContentAndTagManager;
import io.github.eggohito.kaya.util.manager.KayaManagers;
import io.github.eggohito.simula.Simula;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.util.TagValidator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

@ApiStatus.NonExtendable
public interface OriginManager extends ContentAndTagManager<ResourceLocation, OriginHolder> {

	ResourceLocation ID = Simula.id("manager/origin");

	Supplier<OriginManager> DEFERRED_INSTANCE = KayaManagers.deferredPhysicalSideSpecific(OriginManager.class, ServerOriginManager::new);

	TagEntry.Lookup<OriginHolder> TAG_LOOKUP = new TagEntry.Lookup<>() {

		@Override
		public @Nullable OriginHolder element(ResourceLocation id, boolean required) {
			return getInstance().getAsResult(id).result().orElse(null);
		}

		@Override
		public @Nullable Collection<OriginHolder> tag(ResourceLocation id) {
			return getInstance().getTagAsResult(id).result().orElse(null);
		}

		@Override
		public String toString() {
			return "Origin manager";
		}

	};

	TagValidator<OriginHolder> TAG_VALIDATOR = new TagValidator<>(TAG_LOOKUP);

	@ApiStatus.Internal
	void init();

	static OriginManager getInstance() {
		return DEFERRED_INSTANCE.get();
	}

}
