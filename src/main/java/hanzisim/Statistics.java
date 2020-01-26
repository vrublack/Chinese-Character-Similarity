package hanzisim;

import java.util.Collection;
import java.util.Map;

public class Statistics {
    public static float calculateVariance(final Collection<Float> values) {
        final int n = values.size();
        float avg = 0;
        for (Float val : values) {
            avg += val;
        }
        avg /= n;
        float variance = 0;
        for (Float val : values) {
            final float residual = (avg - val);
            variance += residual * residual;
        }
        variance /= n;

        return variance;
    }
}
