package com.hktai.clientintegrity.network;

import com.hktai.clientintegrity.ClientIntegrityMod;
import com.hktai.clientintegrity.rule.ModFinding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record ReadyPayload(int protocolVersion, String verifierVersion, List<String> checkedModIds, List<ModFinding> findings) implements CustomPacketPayload {
	public static final Type<ReadyPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ClientIntegrityMod.MOD_ID, "ready"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ReadyPayload> CODEC = CustomPacketPayload.codec(ReadyPayload::write, ReadyPayload::read);

	private static ReadyPayload read(RegistryFriendlyByteBuf buffer) {
		int protocolVersion = buffer.readVarInt();
		String verifierVersion = buffer.readUtf(128);
		int checkedSize = buffer.readVarInt();
		List<String> checkedModIds = new ArrayList<>(checkedSize);
		for (int i = 0; i < checkedSize; i++) {
			checkedModIds.add(buffer.readUtf(128));
		}

		int findingSize = buffer.readVarInt();
		List<ModFinding> findings = new ArrayList<>(findingSize);
		for (int i = 0; i < findingSize; i++) {
			findings.add(ModFinding.read(buffer));
		}

		return new ReadyPayload(protocolVersion, verifierVersion, checkedModIds, findings);
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeVarInt(protocolVersion);
		buffer.writeUtf(verifierVersion, 128);
		buffer.writeVarInt(checkedModIds.size());
		for (String modId : checkedModIds) {
			buffer.writeUtf(modId, 128);
		}

		buffer.writeVarInt(findings.size());
		for (ModFinding finding : findings) {
			finding.write(buffer);
		}
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
