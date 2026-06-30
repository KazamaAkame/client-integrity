package com.hktai.clientintegrity.rule;

import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

public interface IntegrityRule {
	String id();

	Optional<ModFinding> scan(FabricLoader loader);
}
