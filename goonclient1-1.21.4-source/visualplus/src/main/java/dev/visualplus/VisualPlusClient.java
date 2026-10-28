package dev.visualplus;

import dev.visualplus.module.ModuleManager;
import dev.visualplus.screen.ModulesScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class VisualPlusClient implements ClientModInitializer {
    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        // Shows up in Options > Controls > VisualPlus and can be rebound there.
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visualplus.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.visualplus"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new ModulesScreen(null));
            }
            ModuleManager.tick(mc);
            GlintManager.tick(mc);
        });

        ModuleManager.init();
        HudRenderCallback.EVENT.register(SpotifyHud::render);
        HudRenderCallback.EVENT.register(ModuleHud::render);
        AuroraRenderer.register();
        SpotifyClient.start();

        // Re-apply the tinted glint after resource reloads (F3+T etc.)
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() { return Identifier.of("visualplus", "glint_refresh"); }

                    @Override
                    public void reload(ResourceManager manager) { GlintManager.markDirty(); }
                });
    }
}
