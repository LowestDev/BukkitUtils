package me.lowestdev.player;

import org.bukkit.Bukkit;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import me.lowestdev.player.PremiumChecker.Result;

public final class PlayerUtils {

	private static final Map<String, Result> RESULT_CACHE = new ConcurrentHashMap<>();
	private static final Map<String, UUID> UUID_CACHE = new ConcurrentHashMap<>();

	private PlayerUtils() {
	}

	/*
	 * ========================= Public API =========================
	 */

	public static Result check(String username) {
		String name = username.toLowerCase(Locale.ROOT);
		return RESULT_CACHE.computeIfAbsent(name, PlayerUtils::queryStatus);
	}

	/**
	 * Only valid when Result == PREMIUM
	 */
	public static UUID fetchOnlineUUID(String username) {
		String name = username.toLowerCase(Locale.ROOT);
		return UUID_CACHE.computeIfAbsent(name, PlayerUtils::queryOnlineUUID);
	}

	/*
	 * ========================= Mojang logic =========================
	 */

	private static Result queryStatus(String username) {
		try {
			URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setRequestMethod("GET");
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(3000);

			int code = conn.getResponseCode();
			if (code == 200)
				return Result.PREMIUM;

			if (code == 204 || code == 404)
				return Result.CRACKED;

			return Result.UNKNOWN;

		} catch (Exception e) {
			Bukkit.getLogger().log(Level.WARNING, "[PremiumChecker] Mojang unreachable, fallback engaged.");
			return Result.UNKNOWN;
		}
	}

	private static UUID queryOnlineUUID(String username) {
		try {
			URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setRequestMethod("GET");
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(3000);

			if (conn.getResponseCode() != 200)
				return null;

			try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {

				JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				String rawUUID = json.get("id").getAsString();

				return UUID.fromString(
						rawUUID.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
			}

		} catch (Exception e) {
			return null;
		}
	}
}
