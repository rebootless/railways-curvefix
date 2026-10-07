package io.github.rebootless.curvefix.mixin;

import com.simibubi.create.foundation.outliner.Outliner;
import io.github.rebootless.curvefix.CurveDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Outliner.class, remap = false)
public abstract class OutlinerMixin {
    /** Keeps the F3 debug outlines of the casing pieces alive; Create renders them. */
    @Inject(method = "tickOutlines", at = @At("HEAD"), require = 0)
    private void curvefix$debug(CallbackInfo ci) {
        CurveDebug.show((Outliner) (Object) this);
    }
}
