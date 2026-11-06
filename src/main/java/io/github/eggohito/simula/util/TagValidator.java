package io.github.eggohito.simula.util;

import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.util.Unit;
import org.apache.commons.lang3.function.Consumers;

import java.util.List;
import java.util.stream.Collectors;

public record TagValidator<T>(TagEntry.Lookup<T> lookup) {

	public DataResult<Unit> validateEntry(TagEntry entry) {
		return entry.build(lookup, Consumers.nop())
			? DataResult.success(Unit.INSTANCE)
			: DataResult.error(() -> "Missing reference: \"" + entry + "\"");
	}

	public DataResult<Unit> validateFile(TagFile file) {

		List<TagEntry> missing = new ObjectArrayList<>();

		for (var entry : file.entries()) {

			if (!entry.build(lookup, Consumers.nop())) {
				missing.add(entry);
			}

		}

		return missing.isEmpty()
			? DataResult.success(Unit.INSTANCE)
			: DataResult.error(() -> lookup() + " is missing the following references: [" + missing.stream().map(TagEntry::toString).map(str -> "\"" + str + "\"").collect(Collectors.joining(", ")) + "]");

	}

}
