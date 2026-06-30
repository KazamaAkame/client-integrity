package com.hktai.clientintegrity.network;

import com.hktai.clientintegrity.ClientIntegrityMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record ChallengePayload(int protocolVersion, long nonce, List<String> bannedModIds) implements CustomPacketPayload {
	public static final Type<ChallengePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ClientIntegrityMod.MOD_ID, "challenge"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ChallengePayload> CODEC = CustomPacketPayload.codec(ChallengePayload::write, ChallengePayload::read);

	private static ChallengePayload read(RegistryFriendlyByteBuf buffer) {
		int protocolVersion = buffer.readVarInt();
		long nonce = buffer.readLong();
		int size = buffer.readVarInt();
		List<String> bannedModIds = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			bannedModIds.add(buffer.readUtf(128));
		}
		return new ChallengePayload(protocolVersion, nonce, bannedModIds);
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeVarInt(protocolVersion);
		buffer.writeLong(nonce);
		buffer.writeVarInt(bannedModIds.size());
		for (String modId : bannedModIds) {
			buffer.writeUtf(modId, 128);
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
