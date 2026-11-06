package io.github.eggohito.simula.mixin.access.kaya;

import com.google.common.collect.ImmutableList;
import io.github.eggohito.kaya.util.tag.TagLike;
import net.minecraft.tags.TagEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//  FIXME: Remove once the 'entries' method is exposed in Kaya
@Mixin(TagLike.class)
public interface TagLikeAccessor {

	@Accessor
	ImmutableList<TagEntry> getEntries();

}
