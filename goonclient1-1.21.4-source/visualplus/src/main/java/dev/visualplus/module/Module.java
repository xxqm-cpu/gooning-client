package dev.visualplus.module;

import dev.visualplus.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public abstract class Module {
    public final String name, description;
    public boolean enabled;
    public KeyBinding key;

    protected Module(String name, String description) {
        this.name = name;
        this.description = description;
    }

    protected MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public void onEnable() {}
    public void onDisable() {}
    /** Called every client tick while enabled and in a world. Must be safe to call repeatedly. */
    public void onTick() {}

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        if (e) onEnable(); else onDisable();
        ModConfig.get().setModule(name, e);
        ModConfig.save();
    }
}
