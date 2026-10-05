package io.github.rebootless.curvefix;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.railwayteam.railways.content.custom_tracks.casing.CasingRenderUtils;
import com.railwayteam.railways.mixin_interfaces.IHasTrackCasing;
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
 * Flat (no height difference) curves with the default (non-alternate) casing are drawn with the
 * straight-track slab model (ZO) placed along every curve segment, instead of 3px panels.
 */
public final class CurveCasing {
    private CurveCasing() {}

    /** Place a piece on every STEP-th segment (a segment is ~0.5 block). -Dcurvefix.step=N */
    public static final int STEP = Math.max(1, Integer.getInteger("curvefix.step", 2));
    /** How many pixels (1/16 block) to sink the casing relative to the track base. -Dcurvefix.drop=px */
    public static final float DROP = Float.parseFloat(System.getProperty("curvefix.drop", "1")) / 16.0f;
    /** Piece length multiplier; slightly >1 hides hairline seams. -Dcurvefix.overlap=f */
    public static final float OVERLAP = Float.parseFloat(System.getProperty("curvefix.overlap", "1.01"));

    /** Vertical offset: sunk by DROP, plus a tiny per-piece lift so neighbouring tops never z-fight. */
    public static float yOffset(int i) {
        return -DROP + (i % 4) * 0.001f;
    }

    /** Pieces are stretched along the curve so consecutive pieces meet without gaps or overlap. */
    public static float zScale(BezierConnection bc, int i, int count) {
        int span = Math.min(STEP, count - i + 1);
        return (float) (bc.getLength() / count * span * OVERLAP);
    }

    public static boolean flat(BezierConnection bc) {
        if (((IHasTrackCasing) bc).isAlternate()) return false;
        int h = Math.abs(bc.tePositions.getFirst().method_10264() - bc.tePositions.getSecond().method_10264());
        return h == 0;
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
            pose.translate((Vector3fc) new Vector3f(0.0f, yOffset(i), 0.0f));
            ((SuperByteBuffer) CachedBufferer.partial(model, state).mulPose(pose).mulNormal(segment.tieTransform.method_23762()).scale(1.001f, 1.001f, zScale(bc, i, count))).light(light).renderInto(ms, vb);
        }
        ms.method_22909();
        return true;
    }
}
