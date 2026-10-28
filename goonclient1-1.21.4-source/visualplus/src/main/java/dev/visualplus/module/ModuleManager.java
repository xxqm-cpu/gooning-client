package dev.visualplus.module;

import dev.visualplus.ModConfig;
import dev.visualplus.mixin.MinecraftClientAccessor;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> ALL = new ArrayList<>();
    public static Module ESP;

    private ModuleManager() {}

    public static void init() {
        ALL.add(new Module("Sprint", "Always sprint while moving forward") {
            @Override public void onTick() {
                ClientPlayerEntity p = mc().player;
                if (p.input.movementForward > 0 && !p.isSneaking() && !p.horizontalCollision
                        && !p.isUsingItem() && p.getHungerManager().getFoodLevel() > 6) {
                    p.setSprinting(true);
                }
            }
        });

        ALL.add(new Module("Fullbright", "See in the dark (client-side night vision)") {
            @Override public void onTick() {
                mc().player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false));
            }
            @Override public void onDisable() {
                if (mc().player != null) mc().player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        });

        ESP = new Module("ESP", "Glowing outline on living entities") {};
        ALL.add(ESP);

        ALL.add(new Module("Fly", "Creative-style flight. Servers will usually rubberband or kick you") {
            @Override public void onTick() {
                var ab = mc().player.getAbilities();
                ab.allowFlying = true;
                ab.flying = true;
                ab.setFlySpeed(0.05f * (float) ModConfig.get().flySpeed);
            }
            @Override public void onDisable() {
                ClientPlayerEntity p = mc().player;
                if (p == null) return;
                var ab = p.getAbilities();
                ab.setFlySpeed(0.05f);
                if (!p.isCreative() && !p.isSpectator()) { ab.flying = false; ab.allowFlying = false; }
            }
        });

        ALL.add(new Module("NoFall", "Tells the server you're on the ground when falling") {
            @Override public void onTick() {
                ClientPlayerEntity p = mc().player;
                if (p.fallDistance > 2.5f && !p.isOnGround()) {
                    p.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, false));
                }
            }
        });

        ALL.add(new Module("FastPlace", "No delay between placing blocks / using items") {
            @Override public void onTick() {
                ((MinecraftClientAccessor) mc()).setItemUseCooldown(0);
            }
        });

        // Every module gets its own keybind (unbound by default) in Options > Controls > VisualPlus
        for (Module m : ALL) {
            m.key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.visualplus.module." + m.name.toLowerCase(),
                    InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.visualplus"));
            if (ModConfig.get().enabledModules.contains(m.name)) m.enabled = true; // onTick re-applies state
        }
    }

    public static void tick(MinecraftClient mc) {
        for (Module m : ALL) {
            while (m.key.wasPressed()) {
                if (mc.currentScreen == null) m.toggle();
            }
            if (m.enabled && mc.player != null && mc.world != null) m.onTick();
        }
    }
}
