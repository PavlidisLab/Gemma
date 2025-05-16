package ubic.gemma.core.analysis.preprocess.detect;

import lombok.Value;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Exception raised when suspicious values are detected in an {@link ExpressionDataMatrix}.
 * @author poirigui
 */
public class SuspiciousValuesForQuantitationException extends QuantitationMismatchException {

    private final List<SuspiciousValueResult> suspiciousValues;

    public SuspiciousValuesForQuantitationException( QuantitationType qt, String message, List<SuspiciousValueResult> lintResult ) {
        super( qt, message );
        this.suspiciousValues = lintResult;
    }

    public List<SuspiciousValueResult> getSuspiciousValues() {
        return suspiciousValues;
    }

    @Value
    public static class SuspiciousValueResult {
        /**
         * Affected row or -1 if all rows are affected.
         */
        int row;
        /**
         * Affected row name, if known.
         */
        @Nullable
        String rowName;
        /**
         * Affected column or -1 if all columns are affected.
         */
        int column;
        /**
         * Affected column name, if known.
         */
        @Nullable
        String columnName;
        /**
         * Message explaining why the value is suspicious.
         * <p>
         * Use {@link #toString()} to render the full message with coordinates.
         */
        String message;

        @Override
        public String toString() {
            String s = message;
            if ( row != -1 ) {
                s += " at row " + ( row + 1 );
                if ( rowName != null ) {
                    s += " (" + rowName + ")";
                }
            }
            if ( column != -1 ) {
                s += " at column " + ( column + 1 );
                if ( columnName != null ) {
                    s += " (" + columnName + ")";
                }
            }
            return s;
        }
    }
}
