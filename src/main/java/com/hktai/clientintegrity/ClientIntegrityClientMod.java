package com.hktai.clientintegrity;

import com.hktai.clientintegrity.network.ChallengePayload;
import com.hktai.clientintegrity.network.ResponsePayload;
import com.hktai.clientintegrity.rule.BannedModRule;
import com.hktai.clientintegrity.rule.ModFinding;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.List;
import java.util.Optional;

public class ClientIntegrityClientMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientIntegrityNetworking.registerClientPayloads();
		ClientPlayNetworking.registerGlobalReceiver(ChallengePayload.TYPE, (payload, context) -> {
			List<ModFinding> findings = payload.bannedModIds().stream()
					.map(BannedModRule::new)
					.map(rule -> rule.scan(FabricLoader.getInstance()))
					.flatMap(Optional::stream)
					.toList();

			context.responseSender().sendPacket(new ResponsePayload(
					ClientIntegrityMod.PROTOCOL_VERSION,
					payload.nonce(),
					verifierVersion(),
					findings
			));
		});
	}

	private static String verifierVersion() {
		return FabricLoader.getInstance()
				.getModContainer(ClientIntegrityMod.MOD_ID)
				.map(ModContainer::getMetadata)
				.map(metadata -> metadata.getVersion().getFriendlyString())
				.orElse("unknown");
	}
}
