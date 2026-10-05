package io.github.rebootless.curvefix;

import com.railwayteam.railways.mixin_interfaces.IHasTrackCasing;
import com.simibubi.create.content.trains.track.BezierConnection;

/** Side-independent rules used by the client rendering mixins. */
public final class CurveRules {
    private CurveRules() {}

    /** Flat curve (no height difference) with the default, non-alternate casing mode. */
    public static boolean flat(BezierConnection bc) {
        if (((IHasTrackCasing) bc).isAlternate()) return false;
        int h = Math.abs(bc.tePositions.getFirst().method_10264() - bc.tePositions.getSecond().method_10264());
        return h == 0;
    }
}
