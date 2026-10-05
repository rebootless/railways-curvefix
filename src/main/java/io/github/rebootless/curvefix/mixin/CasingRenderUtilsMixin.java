package io.github.rebootless.curvefix.mixin;

import com.jozufozu.flywheel.core.PartialModel;
import com.railwayteam.railways.content.custom_tracks.casing.CasingRenderUtils;
import com.simibubi.create.content.trains.track.BezierConnection;
import io.github.rebootless.curvefix.CurveCasing;
import net.minecraft.class_1937;
import net.minecraft.class_2680;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CasingRenderUtils.class, remap = false)
public abstract class CasingRenderUtilsMixin {
    @Inject(method = "renderBezierCasings", at = @At("HEAD"), cancellable = true)
    private static void curvefix$flatCurve(class_4587 ms, class_1937 level, PartialModel texturedPartial, class_2680 state,
                                           class_4588 vb, BezierConnection bc, CallbackInfo ci) {
        if (CurveCasing.flat(bc) && CurveCasing.render(ms, level, state, vb, bc)) ci.cancel();
    }
}
