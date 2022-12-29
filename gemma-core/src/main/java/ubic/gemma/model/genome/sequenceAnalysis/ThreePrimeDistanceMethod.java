package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.*;

public enum ThreePrimeDistanceMethod {
    LEFT,
    MIDDLE,
    /**
     * Signifies that the distance to the 3' end was measured from the right edge of the query.
     */
    RIGHT;
}