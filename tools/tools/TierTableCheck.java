package tools;

import com.google.gson.Gson;
import dev.mz.steadicam.SteadicamConfig;
import dev.mz.steadicam.SteadicamOption;
import dev.mz.steadicam.Tiers;

import java.util.ArrayList;
import java.util.List;

// Not shipped in the jar: proves the tier tables and the JSON number round-trip,
// which is where an Integer option restored as Double would blow up at runtime.
public class TierTableCheck {
	private static final List<String> failures = new ArrayList<>();

	public static void main(String[] args) {
		checkTable(Tiers.Tier.WEAK, new String[]{"bobView", "damageTiltStrength", "fovEffectScale"});
		checkTable(Tiers.Tier.STANDARD, new String[]{"bobView", "damageTiltStrength", "fovEffectScale",
			"screenEffectScale", "darknessEffectScale", "vignette"});
		checkTable(Tiers.Tier.STRONG, new String[]{"bobView", "damageTiltStrength", "fovEffectScale",
			"screenEffectScale", "darknessEffectScale", "vignette", "hideLightningFlash", "glintStrength",
			"chunkSectionFadeInTime", "rotateWithMinecart", "framerateLimit"});

		SteadicamConfig c = new SteadicamConfig();
		for (SteadicamOption o : SteadicamOption.values()) {
			c.baseline.put(o.jsonKey, o.coerce(sample(o)));
		}
		c.appliedKeys.add("framerateLimit");
		c.appliedKeys.add("vignette");
		String json = new Gson().toJson(c);
		SteadicamConfig back = new Gson().fromJson(json, SteadicamConfig.class);
		SteadicamConfig.current = back;
		expect("framerateLimit stays Integer",
			SteadicamConfig.baselineOf(SteadicamOption.FPS_LIMIT) instanceof Integer,
			SteadicamConfig.baselineOf(SteadicamOption.FPS_LIMIT));
		expect("vignette stays Boolean",
			SteadicamConfig.baselineOf(SteadicamOption.VIGNETTE) instanceof Boolean,
			SteadicamConfig.baselineOf(SteadicamOption.VIGNETTE));
		expect("damageTiltStrength stays Double",
			SteadicamConfig.baselineOf(SteadicamOption.DAMAGE_TILT) instanceof Double,
			SteadicamConfig.baselineOf(SteadicamOption.DAMAGE_TILT));
		expect("tier string falls back to STANDARD",
			Tiers.Tier.of("garbage") == Tiers.Tier.STANDARD, Tiers.Tier.of("garbage"));
		expect("unknown json key maps to null",
			SteadicamOption.byJsonKey("nope") == null, SteadicamOption.byJsonKey("nope"));

		if (failures.isEmpty()) {
			System.out.println("PASS: 档位表与类型往返全部正确");
		} else {
			failures.forEach(f -> System.out.println("FAIL: " + f));
			System.exit(1);
		}
	}

	private static Object sample(SteadicamOption o) {
		return switch (o.kind()) {
			case BOOL -> Boolean.TRUE;
			case DOUBLE -> 0.75d;
			case INT -> 260;
		};
	}

	private static void checkTable(Tiers.Tier tier, String[] expectedKeys) {
		List<String> keys = new ArrayList<>(Tiers.valuesOf(tier).keySet().stream().map(o -> o.jsonKey).toList());
		expect(tier + " keys", keys.equals(List.of(expectedKeys)), keys);
	}

	private static void expect(String what, boolean ok, Object actual) {
		if (!ok) {
			failures.add(what + " -> " + actual);
		} else {
			System.out.println("  ok  " + what);
		}
	}
}
