package com.hktai.clientintegrity;

import com.hktai.clientintegrity.network.ChallengePayload;
import com.hktai.clientintegrity.network.ReadyPayload;
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
	private static final List<String> FALLBACK_CHECKED_MOD_IDS = List.of("seedcrackerx");

	@Override
	public void onInitializeClient() {
		ClientIntegrityNetworking.registerClientPayloads();
		startReadySender();
		ClientPlayNetworking.registerGlobalReceiver(ChallengePayload.TYPE, (payload, context) -> {
			context.responseSender().sendPacket(new ResponsePayload(
					ClientIntegrityMod.PROTOCOL_VERSION,
					payload.nonce(),
					verifierVersion(),
					scan(payload.bannedModIds())
			));
		});
	}

	private static void startReadySender() {
		Thread readyThread = new Thread(() -> {
			while (true) {
				try {
					if (ClientPlayNetworking.canSend(ReadyPayload.TYPE)) {
						ClientPlayNetworking.send(readyPayload());
					}
				} catch (IllegalStateException ignored) {
					// The client is not in a server connection yet.
				}

				try {
					Thread.sleep(2000L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}, "Client Integrity Ready Sender");
		readyThread.setDaemon(true);
		readyThread.start();
	}

	private static ReadyPayload readyPayload() {
		return new ReadyPayload(
				ClientIntegrityMod.PROTOCOL_VERSION,
				verifierVersion(),
				FALLBACK_CHECKED_MOD_IDS,
				scan(FALLBACK_CHECKED_MOD_IDS)
		);
	}

	private static List<ModFinding> scan(List<String> bannedModIds) {
		return bannedModIds.stream()
				.map(BannedModRule::new)
				.map(rule -> rule.scan(FabricLoader.getInstance()))
				.flatMap(Optional::stream)
				.toList();
	}

	private static String verifierVersion() {
		return FabricLoader.getInstance()
				.getModContainer(ClientIntegrityMod.MOD_ID)
				.map(ModContainer::getMetadata)
				.map(metadata -> metadata.getVersion().getFriendlyString())
				.orElse("unknown");
	}
}
