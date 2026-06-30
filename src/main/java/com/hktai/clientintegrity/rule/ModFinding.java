package com.hktai.clientintegrity.rule;

import net.minecraft.network.FriendlyByteBuf;

public record ModFinding(String modId, String name, String version) {
	public static ModFinding read(FriendlyByteBuf buffer) {
		return new ModFinding(
				buffer.readUtf(128),
				buffer.readUtf(128),
				buffer.readUtf(128)
		);
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeUtf(modId, 128);
		buffer.writeUtf(name, 128);
		buffer.writeUtf(version, 128);
	}
}
