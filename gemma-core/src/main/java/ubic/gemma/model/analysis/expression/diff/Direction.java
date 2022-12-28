package ubic.gemma.model.analysis.expression.diff;

/**
 * <p>
 * Represents the direction of a change e.g. in expression. "Either" is needed because a gene/probe could be changed in
 * two directions with respect to different conditions.
 * </p>
 */
public enum Direction {
    /**
     * Up
     */
    U,
    /**
     * Down
     */
    D,
    /**
     * Either direction (up or down).
     */
    E;
    /**
     * Aliases for readability.
     */
    public static final Direction
            UP = U,
            DOWN = D,
            EITHER = E;
}