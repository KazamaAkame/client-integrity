package com.hktai.clientintegrity;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class ClientIntegrityCommand {
	private ClientIntegrityCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("integrity")
				.requires(ClientIntegrityCommand::canUse)
				.then(literal("status")
						.executes(context -> showAll(context.getSource()))
						.then(argument("player", EntityArgument.player())
								.executes(context -> showPlayer(
										context.getSource(),
										EntityArgument.getPlayer(context, "player")
								))))
				.then(literal("reload")
						.executes(context -> reload(context.getSource()))));
	}

	private static boolean canUse(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		return player == null || source.getServer().getPlayerList().isOp(player.nameAndId());
	}

	private static int showAll(CommandSourceStack source) {
		Collection<ClientIntegrityVerifier.CheckState> states = ClientIntegrityVerifier.states();
		source.sendSuccess(() -> Component.literal(
				"Client Integrity: enforce=" + ClientIntegrityConfig.enforce()
						+ ", timeoutTicks=" + ClientIntegrityConfig.timeoutTicks()
						+ ", responseTimeoutTicks=" + ClientIntegrityConfig.responseTimeoutTicks()
						+ ", bannedMods=" + String.join(",", ClientIntegrityConfig.bannedModIds())
		), false);

		if (states.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No players are currently tracked."), false);
			return 1;
		}

		for (ClientIntegrityVerifier.CheckState state : states) {
			source.sendSuccess(() -> Component.literal(formatState(state)), false);
		}
		return states.size();
	}

	private static int showPlayer(CommandSourceStack source, ServerPlayer player) {
		ClientIntegrityVerifier.CheckState state = ClientIntegrityVerifier.stateOf(player);
		if (state == null) {
			source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " is not currently tracked."), false);
			return 0;
		}

		source.sendSuccess(() -> Component.literal(formatState(state)), false);
		return 1;
	}

	private static int reload(CommandSourceStack source) {
		ClientIntegrityConfig.reload();
		ClientIntegrityVerifier.recheckOnlinePlayers(source.getServer());
		source.sendSuccess(() -> Component.literal("Client Integrity config reloaded and online players will be rechecked."), true);
		return 1;
	}

	private static String formatState(ClientIntegrityVerifier.CheckState state) {
		if (state.verified()) {
			return state.playerName() + ": verified (" + state.verifierVersion() + ")";
		}
		return state.playerName()
				+ ": pending, channelReady=" + state.channelReady()
				+ ", clientReady=" + state.clientReady()
				+ ", readyChecked=" + String.join(",", state.readyCheckedModIds())
				+ ", challengeSent=" + state.challengeSent()
				+ ", ageTicks=" + state.ageTicks();
	}
}
