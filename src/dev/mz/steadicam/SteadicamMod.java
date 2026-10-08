package dev.mz.steadicam;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SteadicamMod implements ClientModInitializer {
	public static final String MOD_ID = "steadicam";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	private static KeyMapping toggleKey;
	private static boolean pendingStartup;

	@Override
	public void onInitializeClient() {
		SteadicamConfig.load(LOGGER);

		try {
			toggleKey = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.steadicam.toggle", GLFW.GLFW_KEY_F7, CATEGORY));
		} catch (Throwable t) {
			toggleKey = null;
			LOGGER.warn("稳镜：快捷键注册失败，仍可用 模组菜单→稳镜 开关", t);
		}

		try {
			HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR,
				Identifier.fromNamespaceAndPath(MOD_ID, "horizon"), new HorizonHud());
		} catch (Throwable t) {
			LOGGER.warn("稳镜：地平线参考线注册失败，防眩晕设置不受影响", t);
		}

		ClientLifecycleEvents.CLIENT_STARTED.register(mc -> pendingStartup = true);
		ClientTickEvents.END_CLIENT_TICK.register(SteadicamMod::onEndTick);

		LOGGER.info("稳镜 Steadicam 已加载：开关={} 档位={} 参考线样式={}",
			SteadicamConfig.current.enabled, SteadicamConfig.current.tier, SteadicamConfig.current.hudStyle);
	}

	private static void onEndTick(Minecraft mc) {
		if (pendingStartup) {
			pendingStartup = false;
			if (SteadicamConfig.current.enabled) {
				Tiers.apply(mc, SteadicamConfig.tier());
			}
		}
		if (toggleKey != null && toggleKey.consumeClick()) {
			toggle(mc);
		}
	}

	private static void toggle(Minecraft mc) {
		if (SteadicamConfig.current.enabled) {
			Tiers.restore(mc);
			HorizonHud.showNotice(Component.translatableWithFallback("steadicam.hud.off",
				"稳镜：已关闭，原设置已逐项恢复"), 3.0);
		} else {
			Tiers.apply(mc, SteadicamConfig.tier());
			HorizonHud.showNotice(Component.translatableWithFallback("steadicam.hud.on",
				"稳镜：已开启，视角不再晃动"), 3.0);
		}
	}

	public static Screen createConfigScreen(Screen parent) {
		return new SteadicamConfigScreen(parent);
	}
}
