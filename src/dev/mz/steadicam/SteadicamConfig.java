package dev.mz.steadicam;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SteadicamConfig {
	public static final int SCHEMA = 1;

	public int schema = SCHEMA;
	public boolean enabled = true;
	public String tier = Tiers.Tier.STANDARD.name();
	public int hudStyle = 2;
	public int opacityPercent = 35;
	public boolean baselineCaptured = false;
	public Map<String, Object> baseline = new LinkedHashMap<>();
	public List<String> appliedKeys = new ArrayList<>();
	public boolean noticeShown = false;

	public static SteadicamConfig current = new SteadicamConfig();

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	public static Tiers.Tier tier() {
		return Tiers.Tier.of(current.tier);
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("steadicam.json");
	}

	public static void load(Logger log) {
		SteadicamConfig fresh = new SteadicamConfig();
		Path path = file();
		if (Files.exists(path)) {
			try {
				SteadicamConfig read = GSON.fromJson(Files.readString(path), SteadicamConfig.class);
				if (read != null) {
					if (read.baseline == null) {
						read.baseline = new LinkedHashMap<>();
					}
					if (read.appliedKeys == null) {
						read.appliedKeys = new ArrayList<>();
					}
					read.schema = SCHEMA;
					fresh = read;
				}
			} catch (Exception e) {
				log.warn("稳镜：配置文件读不了，本次先用默认值（未覆盖原文件）", e);
			}
		}
		current = fresh;
	}

	public static void save(Logger log) {
		Path path = file();
		Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(tmp, GSON.toJson(current));
			try {
				Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			log.warn("稳镜：配置文件写不了", e);
		}
	}

	public static Object baselineOf(SteadicamOption option) {
		return option.coerce(current.baseline.get(option.jsonKey));
	}

	// Whole-file copy taken before we first touch options.txt, as a last-resort fallback.
	public static void backupOptionsFileOnce(Logger log) {
		try {
			Minecraft mc = Minecraft.getInstance();
			Path options = mc.gameDirectory.toPath().resolve("options.txt");
			if (!Files.exists(options)) {
				return;
			}
			Path dir = FabricLoader.getInstance().getConfigDir().resolve("steadicam");
			Files.createDirectories(dir);
			Path bak = dir.resolve("options.txt.bak");
			if (!Files.exists(bak)) {
				Files.copy(options, bak);
				log.info("稳镜：已备份原 options.txt 到 {}", bak);
			}
		} catch (IOException e) {
			log.warn("稳镜：options.txt 备份失败（不影响还原，逐项 baseline 已记录）", e);
		}
	}
}
