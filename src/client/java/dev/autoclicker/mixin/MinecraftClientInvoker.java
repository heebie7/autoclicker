package dev.autoclicker.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Opens up the private click plumbing of MinecraftClient.
 *
 * doAttack / doItemUse are exactly what vanilla calls when the real mouse button is
 * clicked, so going through them keeps every vanilla check (reach, cooldown-less swing,
 * server packets) instead of faking input at the GLFW level.
 */
@Mixin(MinecraftClient.class)
public interface MinecraftClientInvoker {

	@Invoker("doAttack")
	boolean invokeDoAttack();

	@Invoker("doItemUse")
	void invokeDoItemUse();

	@Accessor("attackCooldown")
	void setAttackCooldown(int cooldown);

	@Accessor("itemUseCooldown")
	void setItemUseCooldown(int cooldown);
}
