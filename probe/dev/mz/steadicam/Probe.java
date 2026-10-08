package dev.mz.steadicam;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class Probe implements ClientModInitializer {
	private static boolean done;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (done || mc.level == null) {
				return;
			}
			done = true;
			mc.setScreenAndShow(new SteadicamConfigScreen(null));
		});
	}
}
