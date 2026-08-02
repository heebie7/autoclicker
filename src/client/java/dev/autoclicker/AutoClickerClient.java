package dev.autoclicker;

import dev.autoclicker.mixin.MinecraftClientInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Autoclicker - hold a key, the game clicks for you at 10 clicks per second.
 *
 * Two separate binds (left/attack and right/use), both UNBOUND by default so nothing
 * collides with existing controls. Set them in Options -> Controls -> Key Binds,
 * category "Autoclicker".
 *
 * 10 cps = one click every 2 game ticks (20 tps / 2). Vanilla keeps its own short
 * cooldowns after an attack / item use, so we zero those out right before clicking -
 * otherwise the game would silently eat half of the clicks.
 */
public class AutoClickerClient implements ClientModInitializer {

	/** 20 ticks per second / 2 ticks per click = 10 clicks per second. */
	private static final int TICKS_PER_CLICK = 2;

	private static KeyBinding attackKey;
	private static KeyBinding useKey;

	private int ticksSinceClick;

	@Override
	public void onInitializeClient() {
		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("autoclicker", "main"));

		attackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.autoclicker.attack",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				category
		));

		useKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.autoclicker.use",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				category
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void onClientTick(MinecraftClient client) {
		// no world, or a GUI is open -> stay out of the way
		if (client.player == null || client.currentScreen != null) {
			ticksSinceClick = 0;
			return;
		}

		boolean autoAttack = attackKey.isPressed();
		boolean autoUse = useKey.isPressed();

		if (!autoAttack && !autoUse) {
			ticksSinceClick = 0;
			return;
		}

		if (++ticksSinceClick < TICKS_PER_CLICK) {
			return;
		}

		ticksSinceClick = 0;

		MinecraftClientInvoker invoker = (MinecraftClientInvoker) client;

		if (autoAttack) {
			invoker.setAttackCooldown(0);
			invoker.invokeDoAttack();
		}

		if (autoUse) {
			invoker.setItemUseCooldown(0);
			invoker.invokeDoItemUse();
		}
	}
}
