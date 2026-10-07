package io.github.rebootless.curvefix;

import com.simibubi.create.foundation.outliner.Outliner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.class_243;
import net.minecraft.class_2338;
import net.minecraft.class_310;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** F3 debug overlay: outlines every casing piece, primary and secondary ones in different colors. */
public final class CurveDebug {
    private CurveDebug() {}

    /** Outline colors (0xRRGGBB), set in build.sh. */
    public static final int PRIMARY_COLOR = Tunables.PRIMARY_COLOR;
    public static final int SECONDARY_COLOR = Tunables.SECONDARY_COLOR;

    /** Top face of the slab model in model space (blocks); the outline sits a hair above it. */
    private static final float[] CORNER_X = {0.0f, 1.0f, 1.0f, 0.0f};
    private static final float[] CORNER_Z = {0.0f, 0.0f, 1.0f, 1.0f};
    private static final float TOP_Y = 0.502f;

    private record Piece(double[] x, double[] y, double[] z, boolean primary) {}

    private record Slot(Object owner, int id, int edge) {}

    /** Pieces per owner (a track visual or a curve), world space. Weak so removed tracks disappear. */
    private static final Map<Object, List<Piece>> PIECES = new WeakHashMap<Object, List<Piece>>();

    /** True while the F3 debug screen is open. */
    public static boolean active() {
        return class_310.method_1551().field_1690.field_1866;
    }

    public static void reset(Object owner) {
        synchronized (PIECES) {
            PIECES.put(owner, new ArrayList<Piece>());
        }
    }

    public static void forget(Object owner) {
        synchronized (PIECES) {
            PIECES.remove(owner);
        }
    }

    /** Records a piece: base is the block position the pose is relative to. */
    public static void add(Object owner, class_2338 base, Matrix4f pose, int i) {
        double[] x = new double[4];
        double[] y = new double[4];
        double[] z = new double[4];
        for (int k = 0; k < 4; k++) {
            Vector3f v = new Vector3f(CORNER_X[k], TOP_Y, CORNER_Z[k]);
            pose.transformPosition(v);
            x[k] = base.method_10263() + v.x;
            y[k] = base.method_10264() + v.y;
            z[k] = base.method_10260() + v.z;
        }
        Piece piece = new Piece(x, y, z, !CurveCasing.secondary(i));
        synchronized (PIECES) {
            PIECES.computeIfAbsent(owner, key -> new ArrayList<Piece>()).add(piece);
        }
    }

    /** Called every client tick (see OutlinerMixin); the outlines fade out by themselves once F3 is closed. */
    public static void show(Outliner outliner) {
        if (!active()) return;
        synchronized (PIECES) {
            for (Map.Entry<Object, List<Piece>> entry : PIECES.entrySet()) {
                List<Piece> pieces = entry.getValue();
                for (int n = 0; n < pieces.size(); n++) {
                    Piece p = pieces.get(n);
                    int color = p.primary() ? PRIMARY_COLOR : SECONDARY_COLOR;
                    for (int k = 0; k < 4; k++) {
                        int j = (k + 1) & 3;
                        class_243 from = new class_243(p.x()[k], p.y()[k], p.z()[k]);
                        class_243 to = new class_243(p.x()[j], p.y()[j], p.z()[j]);
                        outliner.showLine(new Slot(entry.getKey(), n, k), from, to).colored(color);
                    }
                }
            }
        }
    }
}
