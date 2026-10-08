package dev.mz.steadicam;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Locale;

public class SteadicamConfigScreen extends Screen {
	private static final Tiers.Tier[] TIERS = Tiers.Tier.values();
	private static final int[] OPACITIES = {20, 35, 50, 70};

	private final Screen parent;
	private Button tierButton;
	private Button hudButton;
	private Button opacityButton;

	public SteadicamConfigScreen(Screen parent) {
		super(Component.translatableWithFallback("steadicam.menu.title", "稳镜（抗3D眩晕）"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		SteadicamConfig c = SteadicamConfig.current;
		int w = 220;
		int x = (this.width - w) / 2;
		int y = 46;

		this.addRenderableWidget(OptionInstance.createBoolean("steadicam.option.enabled", c.enabled, enabled -> {
			if (this.minecraft == null) {
				return;
			}
			if (Boolean.TRUE.equals(enabled)) {
				Tiers.apply(this.minecraft, SteadicamConfig.tier());
			} else {
				Tiers.restore(this.minecraft);
			}
		}).createButton(this.minecraft.options, x, y, w));
		y += 24;

		this.tierButton = this.addRenderableWidget(Button.builder(this.tierCaption(), b -> this.cycleTier())
			.bounds(x, y, w, 20)
			.tooltip(Tooltip.create(Component.translatableWithFallback("steadicam.tooltip.tier",
				"弱：只关视角摇晃、受伤歪屏、FOV 缩放\n标准：再加屏幕扭曲、暗角、黑暗效果\n最强：再加帧率上限 120、附魔光效、区块淡入、矿车翻转")))
			.build());
		y += 24;

		this.hudButton = this.addRenderableWidget(Button.builder(this.hudCaption(), b -> this.cycleHudStyle())
			.bounds(x, y, w, 20)
			.tooltip(Tooltip.create(Component.translatableWithFallback("steadicam.tooltip.hud",
				"一条不动的水平线，给大脑当\"地平线\"锚点\nF1 隐藏 HUD 时一起隐藏")))
			.build());
		y += 24;

		this.opacityButton = this.addRenderableWidget(Button.builder(this.opacityCaption(), b -> this.cycleOpacity())
			.bounds(x, y, w, 20)
			.build());
		y += 28;

		this.addRenderableWidget(Button.builder(
				Component.translatableWithFallback("steadicam.button.apply", "重新应用当前档位"),
				b -> {
					Tiers.apply(this.minecraft, SteadicamConfig.tier());
					this.rebuildWidgets();
				})
			.bounds(x, y, w, 20).build());
		y += 24;

		this.addRenderableWidget(Button.builder(
				Component.translatableWithFallback("steadicam.button.restore", "恢复原设置并关闭"),
				b -> {
					Tiers.restore(this.minecraft);
					this.rebuildWidgets();
				})
			.bounds(x, y, w, 20).build());
		y += 24;

		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
			.bounds(x, y, w, 20).build());
		this.updateLabels();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		Component advice = Component.translatableWithFallback("steadicam.advice",
			"另外这些要她自己关（本 mod 不碰）：着色器里的景深和动态模糊、鼠标灵敏度、用窗口化而不是全屏、离屏幕远一点");
		List<FormattedCharSequence> lines = this.font.split(advice, this.width - 60);
		int y = this.height - 12 - lines.size() * (this.font.lineHeight + 1);
		for (FormattedCharSequence line : lines) {
			g.text(this.font, line, (this.width - this.font.width(line)) / 2, y, 0xA0A0A0);
			y += this.font.lineHeight + 1;
		}
	}

	private void cycleTier() {
		Tiers.Tier now = Tiers.Tier.of(SteadicamConfig.current.tier);
		Tiers.Tier next = TIERS[(now.ordinal() + 1) % TIERS.length];
		Tiers.setTier(this.minecraft, next);
		this.updateLabels();
	}

	private void cycleHudStyle() {
		SteadicamConfig c = SteadicamConfig.current;
		c.hudStyle = (c.hudStyle + 1) % 3;
		SteadicamConfig.save(SteadicamMod.LOGGER);
		this.updateLabels();
	}

	private void cycleOpacity() {
		SteadicamConfig c = SteadicamConfig.current;
		int index = 1;
		for (int i = 0; i < OPACITIES.length; i++) {
			if (OPACITIES[i] == c.opacityPercent) {
				index = (i + 1) % OPACITIES.length;
				break;
			}
		}
		c.opacityPercent = OPACITIES[index];
		SteadicamConfig.save(SteadicamMod.LOGGER);
		this.updateLabels();
	}

	private void updateLabels() {
		if (this.tierButton != null) {
			this.tierButton.setMessage(this.tierCaption());
		}
		if (this.hudButton != null) {
			this.hudButton.setMessage(this.hudCaption());
		}
		if (this.opacityButton != null) {
			this.opacityButton.setMessage(this.opacityCaption());
		}
	}

	private Component tierCaption() {
		return Component.translatableWithFallback("steadicam.option.tier", "防眩晕档位")
			.append(": ")
			.append(Component.translatableWithFallback("steadicam.tier."
				+ SteadicamConfig.current.tier.toLowerCase(Locale.ROOT), tierFallback()));
	}

	private String tierFallback() {
		return switch (Tiers.Tier.of(SteadicamConfig.current.tier)) {
			case WEAK -> "弱";
			case STRONG -> "最强";
			default -> "标准";
		};
	}

	private Component hudCaption() {
		int style = SteadicamConfig.current.hudStyle;
		return Component.translatableWithFallback("steadicam.option.hud", "参考线")
			.append(": ")
			.append(Component.translatableWithFallback("steadicam.hud.style." + style,
				switch (style) {
					case 0 -> "关";
					case 2 -> "地平线 + 中心锚点";
					default -> "只画地平线";
				}));
	}

	private Component opacityCaption() {
		return Component.translatableWithFallback("steadicam.option.opacity", "线条浓度")
			.append(": ")
			.append(Component.literal(SteadicamConfig.current.opacityPercent + "%"));
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreenAndShow(this.parent);
		}
	}
}
