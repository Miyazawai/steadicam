package dev.mz.steadicam;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Tiers {
	public enum Tier {
		WEAK,
		STANDARD,
		STRONG;

		public static Tier of(String name) {
			if (name == null) {
				return STANDARD;
			}
			try {
				return valueOf(name.toUpperCase(java.util.Locale.ROOT));
			} catch (IllegalArgumentException e) {
				return STANDARD;
			}
		}
	}

	private Tiers() {
	}

	public static Map<SteadicamOption, Object> valuesOf(Tier tier) {
		Map<SteadicamOption, Object> map = new LinkedHashMap<>();
		map.put(SteadicamOption.BOB_VIEW, false);
		map.put(SteadicamOption.DAMAGE_TILT, 0.0);
		map.put(SteadicamOption.FOV_EFFECT, 0.0);
		if (tier != Tier.WEAK) {
			map.put(SteadicamOption.SCREEN_EFFECT, 0.0);
			map.put(SteadicamOption.DARKNESS_EFFECT, 0.0);
			map.put(SteadicamOption.VIGNETTE, false);
		}
		if (tier == Tier.STRONG) {
			map.put(SteadicamOption.HIDE_LIGHTNING, true);
			map.put(SteadicamOption.GLINT_STRENGTH, 0.0);
			map.put(SteadicamOption.CHUNK_FADE, 0.0);
			map.put(SteadicamOption.MINECART_ROTATE, false);
			map.put(SteadicamOption.FPS_LIMIT, 120);
		}
		return map;
	}

	public static void apply(Minecraft mc, Tier tier) {
		SteadicamConfig c = SteadicamConfig.current;
		if (!c.baselineCaptured) {
			for (SteadicamOption o : SteadicamOption.values()) {
				c.baseline.put(o.jsonKey, o.get(mc.options));
			}
			c.baselineCaptured = true;
			SteadicamConfig.backupOptionsFileOnce(SteadicamMod.LOGGER);
		}
		List<String> keys = new ArrayList<>();
		for (Map.Entry<SteadicamOption, Object> e : valuesOf(tier).entrySet()) {
			e.getKey().set(mc.options, e.getValue());
			keys.add(e.getKey().jsonKey);
		}
		c.appliedKeys = keys;
		c.tier = tier.name();
		c.enabled = true;
		mc.options.save();
		SteadicamConfig.save(SteadicamMod.LOGGER);
		SteadicamMod.LOGGER.info("稳镜：已开启（档位 {}，改动 {} 项）", tier, keys.size());
	}

	public static void restore(Minecraft mc) {
		SteadicamConfig c = SteadicamConfig.current;
		int reverted = revertApplied(mc);
		c.enabled = false;
		mc.options.save();
		SteadicamConfig.save(SteadicamMod.LOGGER);
		SteadicamMod.LOGGER.info("稳镜：已关闭并逐项还原 {} 项原设置", reverted);
	}

	public static void setTier(Minecraft mc, Tier tier) {
		SteadicamConfig c = SteadicamConfig.current;
		if (!c.enabled) {
			c.tier = tier.name();
			SteadicamConfig.save(SteadicamMod.LOGGER);
			return;
		}
		revertApplied(mc);
		apply(mc, tier);
	}

	// Only the keys we actually wrote go back; her own unrelated edits stay untouched.
	private static int revertApplied(Minecraft mc) {
		SteadicamConfig c = SteadicamConfig.current;
		int n = 0;
		for (String key : new ArrayList<>(c.appliedKeys)) {
			SteadicamOption o = SteadicamOption.byJsonKey(key);
			Object value = o == null ? null : SteadicamConfig.baselineOf(o);
			if (value != null) {
				o.set(mc.options, value);
				n++;
			}
		}
		c.appliedKeys.clear();
		return n;
	}
}
