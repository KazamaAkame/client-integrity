package com.hktai.clientintegrity.rule;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.util.Optional;

public record BannedModRule(String id) implements IntegrityRule {
	@Override
	public Optional<ModFinding> scan(FabricLoader loader) {
		return loader.getModContainer(id)
				.map(container -> {
					ModMetadata metadata = container.getMetadata();
					return new ModFinding(metadata.getId(), metadata.getName(), metadata.getVersion().getFriendlyString());
				});
	}
}
