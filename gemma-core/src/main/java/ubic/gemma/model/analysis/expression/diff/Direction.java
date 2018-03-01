package ubic.gemma.model.analysis.expression.diff;

import java.util.List;

/**
 * <p>
 * Represents the direction of a change e.g. in expression. "Either" is needed because a gene/probe could be changed in
 * two directions with respect to different conditions.
 * </p>
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class Direction implements java.io.Serializable, Comparable<Direction> {
    public static final Direction UP = new Direction( "U" );
    public static final Direction DOWN = new Direction( "D" );
    public static final Direction EITHER = new Direction( "E" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 3327245550772860236L;
    private static final java.util.Map<String, Direction> values = new java.util.LinkedHashMap<>( 3, 1 );
    @SuppressWarnings("UnusedAssignment") // Not redundant, using static initialization
    private static List<String> literals = new java.util.ArrayList<>( 3 );
    @SuppressWarnings("UnusedAssignment") // Not redundant, using static initialization
    private static List<String> names = new java.util.ArrayList<>( 3 );
    @SuppressWarnings("UnusedAssignment") // Not redundant, using static initialization
    private static List<Direction> valueList = new java.util.ArrayList<>( 3 );

    static {
        Direction.values.put( Direction.UP.value, Direction.UP );
        Direction.valueList.add( Direction.UP );
        Direction.literals.add( Direction.UP.value );
        Direction.names.add( "UP" );
        Direction.values.put( Direction.DOWN.value, Direction.DOWN );
        Direction.valueList.add( Direction.DOWN );
        Direction.literals.add( Direction.DOWN.value );
        Direction.names.add( "DOWN" );
        Direction.values.put( Direction.EITHER.value, Direction.EITHER );
        Direction.valueList.add( Direction.EITHER );
        Direction.literals.add( Direction.EITHER.value );
        Direction.names.add( "EITHER" );
        Direction.valueList = java.util.Collections.unmodifiableList( Direction.valueList );
        Direction.literals = java.util.Collections.unmodifiableList( Direction.literals );
        Direction.names = java.util.Collections.unmodifiableList( Direction.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    Direction() {
    }

    private Direction( String value ) {
        this.value = value;
    }

    /**
     * Creates an instance of Direction from <code>value</code>.
     *
     * @param value the value to create the Direction from.
     * @return new instance of Direction
     */
    public static Direction fromString( String value ) {
        final Direction typeValue = Direction.values.get( value );
        if ( typeValue == null ) {
            /*
             * Customization to permit database values to change before code does. Previously this would throw an
             * exception.
             */
            // throw new IllegalArgumentException("invalid value '" + value + "', possible values are: " + literals);
            return null;
        }
        return typeValue;
    }

    /**
     * Returns an unmodifiable list containing the literals that are known by this enumeration.
     *
     * @return A List containing the actual literals defined by this enumeration, this list can not be modified.
     */
    public static java.util.List<String> literals() {
        return Direction.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static java.util.List<String> names() {
        return Direction.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static java.util.List<Direction> values() {
        return Direction.valueList;
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( Direction that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    /**
     * Gets the underlying value of this type safe enumeration.
     *
     * @return the underlying value.
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public String getValue() {
        return this.value;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals( Object object ) {
        return ( this == object ) || ( object instanceof Direction && ( ( Direction ) object ).getValue()
                .equals( this.getValue() ) );
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return String.valueOf( value );
    }

    /**
     * This method allows the deserialization of an instance of this enumeration type to return the actual instance that
     * will be the singleton for the JVM in which the current thread is running.
     * Doing this will allow users to safely use the equality operator <code>==</code> for enumerations because a
     * regular de-serialized object is always a newly constructed instance and will therefore never be an existing
     * reference; it is this <code>readResolve()</code> method which will intercept the deserialization process in order
     * to return the proper singleton reference.
     * This method is documented here: <a
     * href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">Java Object Serialization
     * Specification</a>
     */
    private Object readResolve() {
        return Direction.fromString( this.value );
    }
}