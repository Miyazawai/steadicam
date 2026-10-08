package dev.mz.steadicam;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;

public enum SteadicamOption {
	BOB_VIEW("bobView", Kind.BOOL),
	DAMAGE_TILT("damageTiltStrength", Kind.DOUBLE),
	FOV_EFFECT("fovEffectScale", Kind.DOUBLE),
	SCREEN_EFFECT("screenEffectScale", Kind.DOUBLE),
	DARKNESS_EFFECT("darknessEffectScale", Kind.DOUBLE),
	VIGNETTE("vignette", Kind.BOOL),
	HIDE_LIGHTNING("hideLightningFlash", Kind.BOOL),
	GLINT_STRENGTH("glintStrength", Kind.DOUBLE),
	CHUNK_FADE("chunkSectionFadeInTime", Kind.DOUBLE),
	MINECART_ROTATE("rotateWithMinecart", Kind.BOOL),
	FPS_LIMIT("framerateLimit", Kind.INT);

	public enum Kind { BOOL, DOUBLE, INT }

	public final String jsonKey;
	private final Kind kind;

	SteadicamOption(String jsonKey, Kind kind) {
		this.jsonKey = jsonKey;
		this.kind = kind;
	}

	public Kind kind() {
		return this.kind;
	}

	public static SteadicamOption byJsonKey(String key) {
		for (SteadicamOption o : values()) {
			if (o.jsonKey.equals(key)) {
				return o;
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private OptionInstance raw(Options options) {
		return switch (this) {
			case BOB_VIEW -> options.bobView();
			case DAMAGE_TILT -> options.damageTiltStrength();
			case FOV_EFFECT -> options.fovEffectScale();
			case SCREEN_EFFECT -> options.screenEffectScale();
			case DARKNESS_EFFECT -> options.darknessEffectScale();
			case VIGNETTE -> options.vignette();
			case HIDE_LIGHTNING -> options.hideLightningFlash();
			case GLINT_STRENGTH -> options.glintStrength();
			case CHUNK_FADE -> options.chunkSectionFadeInTime();
			case MINECART_ROTATE -> options.rotateWithMinecart();
			case FPS_LIMIT -> options.framerateLimit();
		};
	}

	public Object get(Options options) {
		return this.raw(options).get();
	}

	@SuppressWarnings("unchecked")
	public void set(Options options, Object value) {
		this.raw(options).set(value);
	}

	// Gson reads every JSON number as Double; re-narrow to this row's declared type.
	public Object coerce(Object fromJson) {
		if (fromJson == null) {
			return null;
		}
		return switch (this.kind) {
			case BOOL -> fromJson instanceof Boolean b ? b : Boolean.parseBoolean(fromJson.toString());
			case DOUBLE -> fromJson instanceof Number n ? n.doubleValue() : Double.parseDouble(fromJson.toString());
			case INT -> fromJson instanceof Number n ? n.intValue() : (int) Double.parseDouble(fromJson.toString());
		};
	}
}
