package ubic.gemma.model.common.measurement;

import java.util.List;
import java.util.Map;

public enum MeasurementKind {
    TIME,
    DISTANCE,
    TEMPERATURE,
    QUANTITY,
    MASS,
    VOLUME,
    /**
     * Concentration
     */
    CONC,
    OTHER,
    COUNT;
    /**
     * Alias for readability.
     */
    public static final MeasurementKind CONCENTRATION = CONC;
}