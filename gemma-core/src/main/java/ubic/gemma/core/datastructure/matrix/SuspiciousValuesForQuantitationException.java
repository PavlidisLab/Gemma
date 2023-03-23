package ubic.gemma.core.datastructure.matrix;

import lombok.Value;

import java.util.List;

public class SuspiciousValuesForQuantitationException extends QuantitationMismatchException {

    private final List<SuspiciousValueResult> lintResults;

    public SuspiciousValuesForQuantitationException( String message, List<SuspiciousValueResult> lintResult ) {
        super( message );
        this.lintResults = lintResult;
    }

    public List<SuspiciousValueResult> getLintResults() {
        return lintResults;
    }

    @Value
    public static class SuspiciousValueResult {
        /**
         * Affected row or -1 if all rows are affected.
         */
        int row;
        /**
         * Affected column or -1 if all columns are affected.
         */
        int column;
        String message;

        @Override
        public String toString() {
            String s = message;
            if ( row != -1 ) {
                s += " at row " + ( row + 1 );
            }
            if ( column != -1 ) {
                s += " at column " + ( column + 1 );
            }
            return s;
        }
    }
}
