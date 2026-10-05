package io.github.rebootless.curvefix.mixin;

import com.jozufozu.flywheel.api.Material;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.materials.model.ModelData;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.railwayteam.railways.content.custom_tracks.casing.CasingRenderUtils;
import com.railwayteam.railways.mixin_interfaces.IHasTrackCasing;
import com.railwayteam.railways.util.MathUtils;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackInstance;
import io.github.rebootless.curvefix.CurveCasing;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1920;
import net.minecraft.class_1921;
import net.minecraft.class_2338;
import net.minecraft.class_2382;
import net.minecraft.class_2482;
import net.minecraft.class_4587;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TrackInstance.class, remap = false, priority = 1500)
public abstract class TrackInstanceCurveMixin extends BlockEntityInstance<TrackBlockEntity> {
    @Unique
    private final List<ModelData> curvefix$models = new ArrayList<ModelData>();
    @Unique
    private final List<class_2338> curvefix$lightPos = new ArrayList<class_2338>();

    private TrackInstanceCurveMixin(MaterialManager materialManager, TrackBlockEntity blockEntity) {
        super(materialManager, blockEntity);
    }

    /** Make Steam 'n' Rails skip its own (panel) casing for flat curves; we draw those ourselves. */
    @Redirect(method = "railways$makeCasingData",
            at = @At(value = "INVOKE", target = "Lcom/railwayteam/railways/mixin_interfaces/IHasTrackCasing;getTrackCasing()Lnet/minecraft/class_2482;"),
            require = 0)
    private class_2482 curvefix$hideFlatCurve(IHasTrackCasing self) {
        if (self instanceof BezierConnection && CurveCasing.flat((BezierConnection) self)) return null;
        return self.getTrackCasing();
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void curvefix$update(CallbackInfo ci) {
        this.curvefix$clear();
        Material mat = this.materialManager.cutout(class_1921.method_23579()).material(Materials.TRANSFORMED);
        class_4587 ms = new class_4587();
        ((TransformStack) TransformStack.cast(ms).translate((class_2382) this.getInstancePosition())).nudge((int) this.pos.method_10063());
        for (BezierConnection bc : this.blockEntity.getConnections().values()) {
            if (!bc.isPrimary() || !CurveCasing.flat(bc)) continue;
            class_2482 casing = ((IHasTrackCasing) bc).getTrackCasing();
            PartialModel raw = CurveCasing.rawModel(bc);
            if (casing == null || raw == null) continue;
            BezierConnection.SegmentAngles[] segments = bc.getBakedSegments();
            int count = segments.length - 1;
            for (int i = 1; i < segments.length; i += CurveCasing.STEP) {
                BezierConnection.SegmentAngles segment = segments[i];
                Matrix4f pose = MathUtils.copy(segment.tieTransform.method_23761());
                pose.translate((Vector3fc) new Vector3f(0.0f, CurveCasing.yOffset(i), 0.0f));
                ModelData inst = CasingRenderUtils.makeCasingInstance(raw, casing, mat);
                inst.setTransform(ms).mulPose(pose).mulNormal(segment.tieTransform.method_23762()).scale(1.001f, 1.001f, CurveCasing.zScale(bc, i, count));
                class_2338 rel = segment.lightPosition.method_10081((class_2382) this.pos);
                inst.updateLight((class_1920) this.world, rel);
                this.curvefix$models.add(inst);
                this.curvefix$lightPos.add(rel);
            }
        }
    }

    @Inject(method = "updateLight", at = @At("HEAD"))
    private void curvefix$updateLight(CallbackInfo ci) {
        for (int i = 0; i < this.curvefix$models.size(); ++i) {
            this.curvefix$models.get(i).updateLight((class_1920) this.world, this.curvefix$lightPos.get(i));
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void curvefix$remove(CallbackInfo ci) {
        this.curvefix$clear();
    }

    @Unique
    private void curvefix$clear() {
        for (ModelData m : this.curvefix$models) m.delete();
        this.curvefix$models.clear();
        this.curvefix$lightPos.clear();
    }
}
