package ubic.gemma.core.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Rounding of floating-point payloads on their way into JSON.
 * <p>
 * Gemma's expression data descends from 16-bit measurements, and these are values that get plotted, not
 * values anything recomputes from. The digits this drops are also incompressible, which makes them costliest
 * exactly where the payload is largest: measured on {@code gemma2}, {@code /datasets/1/mean-variance} goes
 * from 883 KB to 346 KB decompressed, and from 382.6 KB to 101.0 KB gzipped. Say which of the two you mean
 * whenever you quote a size here — conflating them has already produced one wrong conclusion.
 * <p>
 * Significant digits, not decimal places: expression values, p-values and t-statistics span orders of
 * magnitude, and a p-value of 1e-300 has to survive. That rules out the {@code Math.round(v * 1000) / 1000}
 * shape used for the bounded [-1, 1] sample-correlation matrix.
 * <p>
 * This is the JSON counterpart of {@link TsvUtils#format(double)}, which emits the same four significant
 * digits for every file writer. File output does not route through here.
 *
 * @author paul
 */
public final class RoundingUtils {

    /**
     * Significant digits kept in JSON responses.
     */
    public static final int JSON_SIGNIFICANT_DIGITS = 4;

    private static final MathContext JSON_PRECISION = new MathContext( JSON_SIGNIFICANT_DIGITS, RoundingMode.HALF_UP );

    private RoundingUtils() {
    }

    /**
     * Round to {@link #JSON_SIGNIFICANT_DIGITS} significant digits.
     * <p>
     * NaN and the infinities are returned untouched: they have no {@link BigDecimal} representation, and the
     * 0.0 that a naive rounding puts in their place reads as a measured zero rather than "not known". Signed
     * zero keeps its sign.
     */
    public static double round( double v ) {
        if ( !Double.isFinite( v ) || v == 0.0 ) {
            return v;
        }
        // valueOf() goes through Double.toString(), i.e. the shortest decimal that round-trips to v. Rounding
        // that instead of the exact binary expansion keeps the BigDecimal to at most 17 digits, so a subnormal
        // like 1e-300 does not build a 1000-digit unscaled value on every element of a bulk array.
        return BigDecimal.valueOf( v ).round( JSON_PRECISION ).doubleValue();
    }

    /**
     * Null-tolerant {@link #round(double)}, for boxed value-object fields.
     */
    @Nullable
    public static Double round( @Nullable Double v ) {
        return v != null ? round( v.doubleValue() ) : null;
    }

    /**
     * Round every element into a new array.
     * <p>
     * Copies rather than rounding in place: the arrays handed to value objects are frequently the loaded
     * entity's or the cached matrix's own backing store, and rounding those corrupts what the next reader
     * sees.
     */
    @Nullable
    public static double[] roundedCopy( @Nullable double[] src ) {
        if ( src == null ) {
            return null;
        }
        double[] out = new double[src.length];
        for ( int i = 0; i < src.length; i++ ) {
            out[i] = round( src[i] );
        }
        return out;
    }

    /**
     * Row-by-row {@link #roundedCopy(double[])}; see it for why this copies.
     */
    @Nullable
    public static double[][] roundedCopy( @Nullable double[][] src ) {
        if ( src == null ) {
            return null;
        }
        double[][] out = new double[src.length][];
        for ( int i = 0; i < src.length; i++ ) {
            out[i] = roundedCopy( src[i] );
        }
        return out;
    }

    /**
     * Applies {@link #round(double)} at serialization time, leaving the in-memory value exact.
     * <p>
     * Use this rather than rounding in a value object's constructor wherever the field is also read by
     * application code — the differential-expression endpoints rank rows by corrected p-value, and rounding
     * before the sort can reorder near-ties.
     * <p>
     * Opt in per field with {@code @JsonSerialize(using = RoundingUtils.SignificantDigitsSerializer.class)}.
     * It is deliberately not registered on the shared {@code ObjectMapper}: that would also round GEEQ scores
     * and thresholds.
     */
    public static class SignificantDigitsSerializer extends JsonSerializer<Double> {

        @Override
        public void serialize( Double value, JsonGenerator gen, SerializerProvider serializers ) throws IOException {
            gen.writeNumber( round( value.doubleValue() ) );
        }
    }
}
