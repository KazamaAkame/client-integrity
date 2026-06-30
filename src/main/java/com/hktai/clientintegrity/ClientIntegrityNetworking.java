package com.hktai.clientintegrity;

import com.hktai.clientintegrity.network.ChallengePayload;
import com.hktai.clientintegrity.network.ResponsePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ClientIntegrityNetworking {
	private ClientIntegrityNetworking() {
	}

	public static void registerServerReceiver() {
		registerServerPayloads();
		ServerPlayNetworking.registerGlobalReceiver(ResponsePayload.TYPE, (payload, context) ->
				ClientIntegrityVerifier.onResponse(context.player(), payload));
	}

	public static void registerServerPayloads() {
		PayloadTypeRegistry.clientboundPlay().register(ChallengePayload.TYPE, ChallengePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ResponsePayload.TYPE, ResponsePayload.CODEC);
	}

	public static void registerClientPayloads() {
		registerServerPayloads();
	}
}
