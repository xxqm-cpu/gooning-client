package dev.visualplus.mixin;

import dev.visualplus.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    // ESP: make other living entities glow on your screen only
    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    private void vp$glow(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || ModuleManager.ESP == null || !ModuleManager.ESP.enabled) return;
        Entity self = (Entity) (Object) this;
        if (self.getWorld().isClient && self instanceof LivingEntity && self != MinecraftClient.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}
