package com.hktai.clientintegrity;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public final class ClientIntegrityConfig {
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("client-integrity.properties");
	private static final List<String> DEFAULT_BANNED_MODS = List.of("seedcrackerx");

	private static boolean enforce = true;
	private static int timeoutTicks = 300;
	private static int responseTimeoutTicks = 200;
	private static List<String> bannedModIds = DEFAULT_BANNED_MODS;
	private static boolean logPassedPlayers = false;

	private ClientIntegrityConfig() {
	}

	public static void load() {
		Properties properties = new Properties();

		if (Files.exists(CONFIG_PATH)) {
			try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
				properties.load(reader);
			} catch (IOException e) {
				throw new IllegalStateException("Failed to load client integrity config", e);
			}
		}

		enforce = Boolean.parseBoolean(properties.getProperty("enforce", Boolean.toString(enforce)));
		timeoutTicks = parsePositiveInt(properties.getProperty("timeoutTicks"), timeoutTicks);
		responseTimeoutTicks = parsePositiveInt(properties.getProperty("responseTimeoutTicks"), responseTimeoutTicks);
		bannedModIds = parseCsv(properties.getProperty("bannedMods"), DEFAULT_BANNED_MODS);
		logPassedPlayers = Boolean.parseBoolean(properties.getProperty("logPassedPlayers", Boolean.toString(logPassedPlayers)));

		save(properties);
	}

	public static boolean reload() {
		load();
		return true;
	}

	public static boolean enforce() {
		return enforce;
	}

	public static int timeoutTicks() {
		return timeoutTicks;
	}

	public static int responseTimeoutTicks() {
		return responseTimeoutTicks;
	}

	public static List<String> bannedModIds() {
		return bannedModIds;
	}

	public static boolean logPassedPlayers() {
		return logPassedPlayers;
	}

	private static int parsePositiveInt(String raw, int fallback) {
		if (raw == null || raw.isBlank()) {
			return fallback;
		}

		try {
			int parsed = Integer.parseInt(raw.trim());
			return Math.max(1, parsed);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static List<String> parseCsv(String raw, List<String> fallback) {
		if (raw == null || raw.isBlank()) {
			return fallback;
		}

		List<String> parsed = Arrays.stream(raw.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.distinct()
				.toList();
		return parsed.isEmpty() ? fallback : parsed;
	}

	private static void save(Properties existing) {
		Properties properties = new Properties();
		properties.putAll(existing);
		properties.setProperty("enforce", Boolean.toString(enforce));
		properties.setProperty("timeoutTicks", Integer.toString(timeoutTicks));
		properties.setProperty("responseTimeoutTicks", Integer.toString(responseTimeoutTicks));
		properties.setProperty("bannedMods", String.join(",", bannedModIds));
		properties.setProperty("logPassedPlayers", Boolean.toString(logPassedPlayers));

		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				properties.store(writer, "Client Integrity config");
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to save client integrity config", e);
		}
	}
}
