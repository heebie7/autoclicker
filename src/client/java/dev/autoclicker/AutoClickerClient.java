package dev.autoclicker;

import dev.autoclicker.mixin.MinecraftClientInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Autoclicker - hold a key, the game clicks for you at 10 clicks per second.
 *
 * Two separate binds (left/attack and right/use), both UNBOUND by default so nothing
 * collides with existing controls. Set them in Options -> Controls -> Key Binds,
 * category "Autoclicker".
 *
 * Timing rules (v1.1.0, after the "it eats the first click" report):
 *   - the FIRST click fires on the very tick the key goes down, no warm-up delay
 *   - a tap too short to survive until the next tick still produces exactly one click,
 *     because vanilla counts key presses in KeyBinding.timesPressed even if the key is
 *     already released again by the time we look
 *   - while held: one click every 2 ticks = 10 per second
 *
 * With a GUI open the mod clicks whatever is under the cursor (slots, buttons, trades).
 * Vanilla stops updating keybind state while a screen is up, so there we read the bound
 * key straight from GLFW instead.
 */
public class AutoClickerClient implements ClientModInitializer {

	/** 20 ticks per second / 2 ticks per click = 10 clicks per second. */
	private static final int TICKS_PER_CLICK = 2;

	private Clicker attack;
	private Clicker use;

	@Override
	public void onInitializeClient() {
		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("autoclicker", "main"));

		attack = new Clicker(KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.autoclicker.attack",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				category
		)));

		use = new Clicker(KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.autoclicker.use",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				category
		)));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void onClientTick(MinecraftClient client) {
		if (client.player == null) {
			attack.reset();
			use.reset();
			return;
		}

		boolean clickAttack = attack.pollShouldClick(client);
		boolean clickUse = use.pollShouldClick(client);

		if (!clickAttack && !clickUse) {
			return;
		}

		Screen screen = client.currentScreen;

		if (screen != null) {
			// typing into a text box (chat, creative search, anvil) - a bound letter key is
			// meant as text there, not as a click
			if (screen.getFocused() instanceof TextFieldWidget) {
				return;
			}

			if (clickAttack) {
				clickScreen(client, screen, GLFW.GLFW_MOUSE_BUTTON_LEFT);
			}

			if (clickUse) {
				clickScreen(client, screen, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
			}

			return;
		}

		MinecraftClientInvoker invoker = (MinecraftClientInvoker) client;

		if (clickAttack) {
			invoker.setAttackCooldown(0);
			invoker.invokeDoAttack();
		}

		if (clickUse) {
			invoker.setItemUseCooldown(0);
			invoker.invokeDoItemUse();
		}
	}

	/** Feeds a full press+release of a mouse button to the screen at the current cursor position. */
	private static void clickScreen(MinecraftClient client, Screen screen, int button) {
		Window window = client.getWindow();

		if (window.getWidth() == 0 || window.getHeight() == 0) {
			return;
		}

		double x = client.mouse.getX() * window.getScaledWidth() / window.getWidth();
		double y = client.mouse.getY() * window.getScaledHeight() / window.getHeight();

		Click click = new Click(x, y, new MouseInput(button, 0));
		screen.mouseClicked(click, false);
		screen.mouseReleased(click);
	}

	/** One bind's worth of state: is it down, and when is the next click due. */
	private static final class Clicker {

		private final KeyBinding key;
		private int ticksUntilNext;

		private Clicker(KeyBinding key) {
			this.key = key;
		}

		private void reset() {
			ticksUntilNext = 0;
		}

		private boolean pollShouldClick(MinecraftClient client) {
			// drain the press counter: catches a tap that started AND ended between two ticks
			boolean pressedThisTick = false;

			while (key.wasPressed()) {
				pressedThisTick = true;
			}

			boolean down = key.isPressed() || isPhysicallyDown(client);

			if (!down && !pressedThisTick) {
				ticksUntilNext = 0;
				return false;
			}

			// a fresh press always clicks right now, whatever the schedule said
			if (!pressedThisTick && ticksUntilNext > 0) {
				ticksUntilNext--;
				return false;
			}

			ticksUntilNext = TICKS_PER_CLICK - 1;
			return true;
		}

		/**
		 * Asks GLFW directly whether the bound key is held. Needed because vanilla freezes
		 * keybind state while a screen is open, which is exactly when we still want to click.
		 */
		private boolean isPhysicallyDown(MinecraftClient client) {
			InputUtil.Key bound = KeyBindingHelper.getBoundKeyOf(key);
			int code = bound.getCode();

			if (code == GLFW.GLFW_KEY_UNKNOWN) {
				return false;
			}

			if (bound.getCategory() == InputUtil.Type.MOUSE) {
				return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), code) == GLFW.GLFW_PRESS;
			}

			return InputUtil.isKeyPressed(client.getWindow(), code);
		}
	}
}
