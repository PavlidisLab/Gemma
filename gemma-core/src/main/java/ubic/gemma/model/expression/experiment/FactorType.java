package ubic.gemma.model.expression.experiment;

import java.util.*;

public enum FactorType {
    continuous,
    categorical;

    /**
     * Aliases for consistency.
     */
    public static final FactorType
            CONTINUOUS = continuous,
            CATEGORICAL = categorical;

    @Override
    public String toString() {
        return super.toString().toUpperCase();
    }
}