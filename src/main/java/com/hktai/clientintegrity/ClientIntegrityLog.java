package com.hktai.clientintegrity;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class ClientIntegrityLog {
	private static final Logger LOGGER = LogUtils.getLogger();

	private ClientIntegrityLog() {
	}

	public static void info(String message) {
		LOGGER.info("[Client Integrity] {}", message);
	}

	public static void warn(String message) {
		LOGGER.warn("[Client Integrity] {}", message);
	}
}
