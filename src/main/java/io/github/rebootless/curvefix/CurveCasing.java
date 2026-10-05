package io.github.rebootless.curvefix;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.railwayteam.railways.content.custom_tracks.casing.CasingRenderUtils;
import com.railwayteam.railways.registry.CRBlockPartials;
import com.railwayteam.railways.util.MathUtils;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackShape;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import net.minecraft.class_1920;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2382;
import net.minecraft.class_2482;
import net.minecraft.class_2680;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_761;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Client side. Flat curves with the default casing are drawn with the straight-track slab model (ZO),
 * one piece per curve segment, 1:1 (no scaling, so textures stay pixel-perfect).
 */
public final class CurveCasing {
    private CurveCasing() {}

    /** One piece per STEP segments (a segment is ~0.5 block). -Dcurvefix.step=N */
    public static final int STEP = Math.max(1, Integer.getInteger("curvefix.step", 1));
    /**
     * Pixels (1/16 block) the pieces are sunk. The curve origin sits 15/16 px above the straight-track
     * origin, so 0.9375 would put the slab top exactly level with straight casing; the default is
     * 0.25 px higher than that. -Dcurvefix.drop=px
     */
    public static final float DROP = Float.parseFloat(System.getProperty("curvefix.drop", "0.6875")) / 16.0f;
    /** Height difference (blocks) between alternating pieces; the lower layer fills seams without z-fighting. -Dcurvefix.layer=f */
    public static final float LAYER = Float.parseFloat(System.getProperty("curvefix.layer", "0.001"));

    /** Every second piece sits one LAYER lower, so overlapping pieces never z-fight. */
    public static float yOffset(int i) {
        return -DROP + ((i & 1) == 0 ? 0.0f : LAYER);
    }

    /** Pieces are 1 block long; the last one is pulled back so it ends exactly at the end of the curve. */
    public static float zShift(BezierConnection bc, int i, int count) {
        double length = bc.getLength();
        if (length < 1.0) return 0.0f;
        double start = (i - 1) * (length / count);
        return (float) Math.min(0.0, (length - 1.0) - start);
    }

    /** Straight-track slab model for this curve's track type, or null. */
    public static PartialModel rawModel(BezierConnection bc) {
        CRBlockPartials.TrackCasingSpec spec = CRBlockPartials.TRACK_CASINGS.get(TrackShape.ZO);
        if (spec == null) return null;
        CRBlockPartials.TrackCasingSpec s = spec.getFor(bc.getMaterial().trackType);
        return s == null ? null : s.model;
    }

    /** Non-instanced path (replaces CasingRenderUtils.renderBezierCasings for flat curves). */
    public static boolean render(class_4587 ms, class_1937 level, class_2680 state, class_4588 vb, BezierConnection bc) {
        PartialModel raw = rawModel(bc);
        if (raw == null) return false;
        PartialModel model = CasingRenderUtils.reTexture(raw, (class_2482) state.method_26204());
        ms.method_22903();
        class_2338 tePosition = (class_2338) bc.tePositions.getFirst();
        BezierConnection.SegmentAngles[] segments = bc.getBakedSegments();
        TransformStack.cast(ms).nudge((int) tePosition.method_10063());
        int count = segments.length - 1;
        for (int i = 1; i < segments.length; i += STEP) {
            BezierConnection.SegmentAngles segment = segments[i];
            int light = class_761.method_23794((class_1920) level, (class_2338) segment.lightPosition.method_10081((class_2382) tePosition));
            Matrix4f pose = MathUtils.copy(segment.tieTransform.method_23761());
            pose.translate((Vector3fc) new Vector3f(0.0f, yOffset(i), zShift(bc, i, count)));
            ((SuperByteBuffer) CachedBufferer.partial(model, state).mulPose(pose).mulNormal(segment.tieTransform.method_23762())).light(light).renderInto(ms, vb);
        }
        ms.method_22909();
        return true;
    }
}
