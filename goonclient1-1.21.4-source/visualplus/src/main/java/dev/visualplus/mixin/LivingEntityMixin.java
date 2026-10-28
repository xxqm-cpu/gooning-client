package dev.visualplus.mixin;

import dev.visualplus.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    // Visual swing animation speed for your own player only
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void fcp$swing(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        double s = ModConfig.get().swingSpeed;
        if (s == 1.0) return;
        cir.setReturnValue(Math.max(1, (int) Math.round(cir.getReturnValueI() / s)));
    }
}
