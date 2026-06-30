package com.hktai.clientintegrity;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class ClientIntegrityMod implements DedicatedServerModInitializer {
	public static final String MOD_ID = "client_integrity";
	public static final int PROTOCOL_VERSION = 1;

	@Override
	public void onInitializeServer() {
		ClientIntegrityConfig.load();
		ClientIntegrityNetworking.registerServerReceiver();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ClientIntegrityCommand.register(dispatcher));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ClientIntegrityVerifier.onJoin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ClientIntegrityVerifier.onDisconnect(handler.player));
		ServerTickEvents.END_SERVER_TICK.register(ClientIntegrityVerifier::tick);
	}
}
