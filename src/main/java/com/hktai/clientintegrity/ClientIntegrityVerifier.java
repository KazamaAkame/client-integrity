package com.hktai.clientintegrity;

import com.hktai.clientintegrity.network.ChallengePayload;
import com.hktai.clientintegrity.network.ReadyPayload;
import com.hktai.clientintegrity.network.ResponsePayload;
import com.hktai.clientintegrity.rule.ModFinding;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientIntegrityVerifier {
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Map<UUID, CheckState> STATES = new ConcurrentHashMap<>();

	private ClientIntegrityVerifier() {
	}

	public static void onJoin(ServerPlayer player) {
		if (!ClientIntegrityConfig.enforce()) {
			STATES.put(player.getUUID(), CheckState.verified(player.getScoreboardName(), "enforcement disabled"));
			return;
		}

		STATES.put(player.getUUID(), CheckState.pending(player.getScoreboardName(), RANDOM.nextLong()));
		ClientIntegrityLog.info(player.getScoreboardName() + " is pending client integrity verification.");
	}

	public static void onDisconnect(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	public static void tick(MinecraftServer server) {
		if (!ClientIntegrityConfig.enforce()) {
			return;
		}

		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			CheckState state = STATES.get(player.getUUID());
			if (state == null || state.verified()) {
				continue;
			}

			state.tick();
			boolean canSendChallenge = ServerPlayNetworking.canSend(player, ChallengePayload.TYPE);
			state.markChannelReady(canSendChallenge);

			if (!state.challengeSent() && canSendChallenge) {
				ServerPlayNetworking.send(player, new ChallengePayload(
						ClientIntegrityMod.PROTOCOL_VERSION,
						state.nonce(),
						ClientIntegrityConfig.bannedModIds()
				));
				state.markChallengeSent();
				ClientIntegrityLog.info("Sent client integrity challenge to " + player.getScoreboardName() + ".");
			}

			if (!state.verified() && !state.challengeSent() && state.ageTicks() >= ClientIntegrityConfig.timeoutTicks()) {
				if (state.clientReady()) {
					kick(player, "Client Integrity verifier could not complete the server challenge. Please update the verifier mod.",
							"client ready payload did not cover configured banned mods: " + String.join(",", ClientIntegrityConfig.bannedModIds()));
				} else {
					kick(player, "This server requires the Client Integrity verifier mod.",
							"client never advertised the client_integrity:challenge channel or sent a ready payload");
				}
			} else if (!state.verified() && state.challengeSent()
					&& state.ticksSinceChallenge() >= ClientIntegrityConfig.responseTimeoutTicks()) {
				kick(player, "Client Integrity verifier did not respond. Please restart Minecraft and try again.",
						"challenge was sent but no valid response arrived");
			}
		}
	}

	public static void onReady(ServerPlayer player, ReadyPayload payload) {
		if (!ClientIntegrityConfig.enforce()) {
			return;
		}

		CheckState state = STATES.computeIfAbsent(player.getUUID(), uuid -> CheckState.pending(player.getScoreboardName(), RANDOM.nextLong()));
		if (state.verified()) {
			return;
		}

		ClientIntegrityLog.info("Received client integrity ready from " + player.getScoreboardName()
				+ " using verifier " + payload.verifierVersion()
				+ " after checking " + String.join(",", payload.checkedModIds()) + ".");

		if (payload.protocolVersion() != ClientIntegrityMod.PROTOCOL_VERSION) {
			kick(player, "Client integrity verification failed. Please update your verifier mod.", "ready protocol mismatch");
			return;
		}

		if (!payload.findings().isEmpty()) {
			kick(player, blockedMessage(payload.findings()), "ready blocked findings: " + describeFindings(payload.findings()));
			return;
		}

		state.markClientReady(payload.verifierVersion(), payload.checkedModIds());
		if (readyCoversConfiguredBannedMods(payload.checkedModIds())) {
			state.markVerified(payload.verifierVersion() + " ready");
			ClientIntegrityLog.info(player.getScoreboardName() + " passed client integrity ready verification with verifier "
					+ payload.verifierVersion() + ".");
		}
	}

	public static void onResponse(ServerPlayer player, ResponsePayload payload) {
		CheckState state = STATES.get(player.getUUID());
		if (state == null) {
			return;
		}

		ClientIntegrityLog.info("Received client integrity response from " + player.getScoreboardName()
				+ " using verifier " + payload.verifierVersion() + ".");

		if (payload.protocolVersion() != ClientIntegrityMod.PROTOCOL_VERSION || payload.nonce() != state.nonce()) {
			kick(player, "Client integrity verification failed. Please update your verifier mod.", "protocol or nonce mismatch");
			return;
		}

		if (!payload.findings().isEmpty()) {
			kick(player, blockedMessage(payload.findings()), "blocked findings: " + describeFindings(payload.findings()));
			return;
		}

		state.markVerified(payload.verifierVersion());
		if (ClientIntegrityConfig.logPassedPlayers()) {
			ClientIntegrityLog.info(player.getScoreboardName() + " passed client integrity verification with verifier " + payload.verifierVersion() + ".");
		}
	}

	public static Collection<CheckState> states() {
		List<CheckState> result = new ArrayList<>(STATES.values());
		result.sort(Comparator.comparing(CheckState::playerName));
		return result;
	}

	public static void recheckOnlinePlayers(MinecraftServer server) {
		STATES.clear();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			onJoin(player);
		}
	}

	public static CheckState stateOf(ServerPlayer player) {
		return STATES.get(player.getUUID());
	}

	private static String blockedMessage(List<ModFinding> findings) {
		String blocked = findings.stream()
				.map(ModFinding::modId)
				.distinct()
				.sorted()
				.reduce((left, right) -> left + ", " + right)
				.orElse("blocked client mod");
		return "Blocked client mod detected: " + blocked + ". Remove it before joining.";
	}

	private static String describeFindings(List<ModFinding> findings) {
		return findings.stream()
				.map(finding -> finding.modId() + " " + finding.version())
				.distinct()
				.sorted()
				.reduce((left, right) -> left + ", " + right)
				.orElse("none");
	}

	private static boolean readyCoversConfiguredBannedMods(List<String> checkedModIds) {
		return checkedModIds.containsAll(ClientIntegrityConfig.bannedModIds());
	}

	private static void kick(ServerPlayer player, String message, String detail) {
		ClientIntegrityLog.warn("Kicking " + player.getScoreboardName() + ": " + message + " (" + detail + ")");
		player.connection.disconnect(Component.literal(message));
		STATES.remove(player.getUUID());
	}

	public static final class CheckState {
		private final String playerName;
		private final long nonce;
		private int ageTicks;
		private int challengeSentAtTicks = -1;
		private boolean challengeSent;
		private boolean channelReady;
		private boolean clientReady;
		private boolean verified;
		private String verifierVersion;
		private List<String> readyCheckedModIds = List.of();

		private CheckState(String playerName, long nonce, boolean verified, String verifierVersion) {
			this.playerName = playerName;
			this.nonce = nonce;
			this.verified = verified;
			this.verifierVersion = verifierVersion;
		}

		public static CheckState pending(String playerName, long nonce) {
			return new CheckState(playerName, nonce, false, "");
		}

		public static CheckState verified(String playerName, String verifierVersion) {
			return new CheckState(playerName, 0L, true, verifierVersion);
		}

		public String playerName() {
			return playerName;
		}

		public long nonce() {
			return nonce;
		}

		public int ageTicks() {
			return ageTicks;
		}

		public boolean challengeSent() {
			return challengeSent;
		}

		public int ticksSinceChallenge() {
			return challengeSentAtTicks < 0 ? 0 : ageTicks - challengeSentAtTicks;
		}

		public boolean channelReady() {
			return channelReady;
		}

		public boolean clientReady() {
			return clientReady;
		}

		public List<String> readyCheckedModIds() {
			return readyCheckedModIds;
		}

		public boolean verified() {
			return verified;
		}

		public String verifierVersion() {
			return verifierVersion;
		}

		private void tick() {
			ageTicks++;
		}

		private void markChallengeSent() {
			challengeSent = true;
			challengeSentAtTicks = ageTicks;
		}

		private void markChannelReady(boolean channelReady) {
			this.channelReady = this.channelReady || channelReady;
		}

		private void markClientReady(String verifierVersion, List<String> checkedModIds) {
			clientReady = true;
			this.verifierVersion = verifierVersion;
			readyCheckedModIds = List.copyOf(checkedModIds);
		}

		private void markVerified(String verifierVersion) {
			verified = true;
			this.verifierVersion = verifierVersion;
		}
	}
}
