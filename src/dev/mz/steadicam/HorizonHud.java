package dev.mz.steadicam;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public class HorizonHud implements HudElement {
	private static long noticeUntilNanos;
	private static Component notice;

	public static void showNotice(Component text, double seconds) {
		notice = text;
		noticeUntilNanos = System.nanoTime() + (long) (seconds * 1_000_000_000L);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor ex, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		SteadicamConfig c = SteadicamConfig.current;
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();

		if (c.enabled && !c.noticeShown) {
			c.noticeShown = true;
			SteadicamConfig.save(SteadicamMod.LOGGER);
			showNotice(Component.translatableWithFallback("steadicam.hud.first_run",
				"稳镜已开启：视角不再晃动。按 F7 可随时开关，或在 模组菜单→稳镜 恢复原设置"), 8.0);
		}
		drawNotice(mc, ex, w, h);
		if (!c.enabled || c.hudStyle == 0 || mc.gui.hud.isHidden()) {
			return;
		}
		Camera cam = mc.gameRenderer.mainCamera();
		if (cam.isPanoramicMode()) {
			return;
		}
		Vector3fc forward = cam.forwardVector();
		float horizontal = (float) Math.sqrt(forward.x() * forward.x() + forward.z() * forward.z());
		if (horizontal < 1.0E-4f) {
			return;
		}
		// A point 200 blocks ahead at eye height: projecting it through the camera matrix puts the
		// true horizon wherever it belongs, without assuming whether getFov() is vertical or horizontal.
		float scale = 200.0f / horizontal;
		Matrix4f viewProj = cam.getViewRotationProjectionMatrix(new Matrix4f());
		Vector4f clip = viewProj.transform(new Vector4f(forward.x() * scale, 0.0f, forward.z() * scale, 1.0f));
		if (clip.w <= 1.0E-3f) {
			return;
		}
		float screenY = (1.0f - clip.y / clip.w) * h * 0.5f;
		if (Float.isNaN(screenY) || Float.isInfinite(screenY) || screenY < 1.0f || screenY > h - 2.0f) {
			return;
		}
		int y = Math.round(screenY);
		int alpha = Math.max(0, Math.min(100, c.opacityPercent)) * 255 / 100;
		int line = (alpha << 24) | 0x00FFFFFF;
		int shadow = ((alpha / 3) << 24);
		int x1 = (int) (w * 0.31f);
		int x2 = (int) (w * 0.69f);
		ex.horizontalLine(x1, x2, y + 1, shadow);
		ex.horizontalLine(x1, x2, y, line);
		if (c.hudStyle >= 2) {
			int cx = w / 2;
			int cy = h / 2;
			ex.horizontalLine(cx - 4, cx + 4, cy, line);
			ex.verticalLine(cx, cy - 4, cy + 4, line);
		}
	}

	private void drawNotice(Minecraft mc, GuiGraphicsExtractor ex, int w, int h) {
		if (notice == null || System.nanoTime() >= noticeUntilNanos) {
			return;
		}
		Font font = mc.font;
		int y = 6;
		for (FormattedCharSequence line : font.split(notice, Math.max(60, w - 40))) {
			ex.text(font, line, (w - font.width(line)) / 2, y, 0xE0FFFFFF, true);
			y += font.lineHeight + 2;
		}
	}
}
