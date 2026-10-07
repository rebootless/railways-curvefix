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

    /**
     * Height (px, 1/16 block) of the slab top above the straight-track casing top; 0 = flush.
     * Default from build.sh; runtime override: -Dcurvefix.height=px
     */
    public static final float HEIGHT = Float.parseFloat(System.getProperty("curvefix.height", String.valueOf(Tunables.HEIGHT_PX)));
    /** The curve origin sits 15/16 px above the straight-track origin, so pieces are sunk by 15/16 - HEIGHT. */
    private static final float SINK = (0.9375f - HEIGHT) / 16.0f;
    /** Height offset (blocks) of primary (main) pieces only. -Dcurvefix.primarylayer=f */
    public static final float PRIMARY_LAYER = Float.parseFloat(System.getProperty("curvefix.primarylayer", String.valueOf(Tunables.PRIMARY_LAYER)));
    /** Height offset (blocks) of secondary (gap-filling) pieces only. -Dcurvefix.secondarylayer=f */
    public static final float SECONDARY_LAYER = Float.parseFloat(System.getProperty("curvefix.secondarylayer", String.valueOf(Tunables.SECONDARY_LAYER)));
    /** Extra height on every other primary piece (anti-z-fighting among primaries). -Dcurvefix.primaryzfight=f */
    public static final float PRIMARY_ZFIGHT = Float.parseFloat(System.getProperty("curvefix.primaryzfight", String.valueOf(Tunables.PRIMARY_ZFIGHT)));
    /** Extra height on every other secondary piece (anti-z-fighting among secondaries). -Dcurvefix.secondaryzfight=f */
    public static final float SECONDARY_ZFIGHT = Float.parseFloat(System.getProperty("curvefix.secondaryzfight", String.valueOf(Tunables.SECONDARY_ZFIGHT)));

    /** Width and length scale of the lower, gap-filling pieces (around the model center); 1 = full size. -Dcurvefix.secondaryscale=f */
    public static final float SECONDARY_SCALE = Float.parseFloat(System.getProperty("curvefix.secondaryscale", String.valueOf(Tunables.SECONDARY_SCALE)));

    /** Rotation (degrees) of the lower, gap-filling pieces around the vertical axis through their center; positive = clockwise from above. -Dcurvefix.secondaryrotation=deg */
    public static final float SECONDARY_ROTATION = (float) Math.toRadians(Float.parseFloat(System.getProperty("curvefix.secondaryrotation", String.valueOf(Tunables.SECONDARY_ROTATION_DEG))));
    /** Rotation (degrees) of the main pieces around the vertical axis through their center; positive = clockwise from above. -Dcurvefix.primaryrotation=deg */
    public static final float PRIMARY_ROTATION = (float) Math.toRadians(Float.parseFloat(System.getProperty("curvefix.primaryrotation", String.valueOf(Tunables.PRIMARY_ROTATION_DEG))));

    /** Center of the straight-track slab model (zo, zo_wide, zo_narrow) in width and length: 8 px each. */
    private static final float MODEL_CENTER_X = 0.5f;
    private static final float MODEL_CENTER_Z = 0.5f;

    /**
     * Positions a piece.
     * Secondary (even, gap-fillers): SECONDARY_LAYER + SECONDARY_ZFIGHT, SECONDARY_SCALE, SECONDARY_ROTATION.
     * Primary (odd, main): PRIMARY_LAYER + PRIMARY_ZFIGHT, PRIMARY_ROTATION, full scale.
     * SECONDARY_* only to secondary; PRIMARY_* only to primary.
     * ZFIGHT alternates within the same role so neighbouring pieces of one role never share the same Y.
     */
    public static void place(Matrix4f pose, BezierConnection bc, int i, int count) {
        boolean filler = secondary(i);
        float y = -SINK + (filler ? SECONDARY_LAYER : PRIMARY_LAYER);
        // every other piece of the same role gets the z-fight offset
        if (((i / 2) & 1) == 1) {
            y += filler ? SECONDARY_ZFIGHT : PRIMARY_ZFIGHT;
        }
        pose.translate((Vector3fc) new Vector3f(0.0f, y, zShift(bc, i, count)));
        float angle = filler ? SECONDARY_ROTATION : PRIMARY_ROTATION;
        float scale = filler ? SECONDARY_SCALE : 1.0f;
        if (angle != 0.0f || scale != 1.0f) {
            pose.translate((Vector3fc) new Vector3f(MODEL_CENTER_X, 0.0f, MODEL_CENTER_Z));
            pose.rotateY(-angle);
            pose.scale(scale, 1.0f, scale);
            pose.translate((Vector3fc) new Vector3f(-MODEL_CENTER_X, 0.0f, -MODEL_CENTER_Z));
        }
    }

    /** Secondary pieces are the lower, gap-filling ones (even indices); the rest are primary. */
    public static boolean secondary(int i) {
        return (i & 1) == 0;
    }

    /**
     * Pieces 1..count sit on the curve segments. i = 0 and i = count + 1 are extra lower pieces hanging over the
     * start and the end of the curve; the end one only exists when the last regular piece is not already a lower one.
     */
    public static boolean skip(int i, int count) {
        return count < 1 || (i == count + 1 && (i & 1) != 0);
    }

    /** Segment whose transform a piece uses; the extra pieces borrow the first and the last one. */
    public static int src(int i, int count) {
        return Math.min(Math.max(i, 1), count);
    }

    /** Pieces are 1 block long; the last one is pulled back so it ends exactly at the end of the curve. */
    public static float zShift(BezierConnection bc, int i, int count) {
        double length = bc.getLength();
        double seg = length / count;
        if (i == 0) return (float) -seg;
        if (i == count + 1) return (float) (2.0 * seg - 1.0);
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
        boolean debug = CurveDebug.active();
        if (debug) CurveDebug.reset(bc);
        for (int i = 0; i <= count + 1; i++) {
            if (skip(i, count)) continue;
            BezierConnection.SegmentAngles segment = segments[src(i, count)];
            int light = class_761.method_23794((class_1920) level, (class_2338) segment.lightPosition.method_10081((class_2382) tePosition));
            Matrix4f pose = MathUtils.copy(segment.tieTransform.method_23761());
            place(pose, bc, i, count);
            if (debug) CurveDebug.add(bc, tePosition, pose, i);
            ((SuperByteBuffer) CachedBufferer.partial(model, state).mulPose(pose).mulNormal(segment.tieTransform.method_23762())).light(light).renderInto(ms, vb);
        }
        ms.method_22909();
        return true;
    }
}
