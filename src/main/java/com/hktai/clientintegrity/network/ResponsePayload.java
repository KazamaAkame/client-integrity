package com.hktai.clientintegrity.network;

import com.hktai.clientintegrity.ClientIntegrityMod;
import com.hktai.clientintegrity.rule.ModFinding;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record ResponsePayload(int protocolVersion, long nonce, String verifierVersion, List<ModFinding> findings) implements CustomPacketPayload {
	public static final Type<ResponsePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ClientIntegrityMod.MOD_ID, "response"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ResponsePayload> CODEC = CustomPacketPayload.codec(ResponsePayload::write, ResponsePayload::read);

	private static ResponsePayload read(RegistryFriendlyByteBuf buffer) {
		int protocolVersion = buffer.readVarInt();
		long nonce = buffer.readLong();
		String verifierVersion = buffer.readUtf(128);
		int size = buffer.readVarInt();
		List<ModFinding> findings = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			findings.add(ModFinding.read(buffer));
		}
		return new ResponsePayload(protocolVersion, nonce, verifierVersion, findings);
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeVarInt(protocolVersion);
		buffer.writeLong(nonce);
		buffer.writeUtf(verifierVersion, 128);
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
