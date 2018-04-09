package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.*;

public class ThreePrimeDistanceMethod implements java.io.Serializable, Comparable<ThreePrimeDistanceMethod> {
    public static final ThreePrimeDistanceMethod LEFT = new ThreePrimeDistanceMethod( "LEFT" );
    public static final ThreePrimeDistanceMethod MIDDLE = new ThreePrimeDistanceMethod( "MIDDLE" );
    /**
     * Signifies that the distance to the 3' end was measured from the right edge of the query.
     */
    public static final ThreePrimeDistanceMethod RIGHT = new ThreePrimeDistanceMethod( "RIGHT" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8474258815612159402L;
    private static final Map<String, ThreePrimeDistanceMethod> values = new LinkedHashMap<>( 3, 1 );
    private static List<String> literals = new ArrayList<>( 3 );
    private static List<String> names = new ArrayList<>( 3 );
    private static List<ThreePrimeDistanceMethod> valueList = new ArrayList<>( 3 );

    static {
        ThreePrimeDistanceMethod.values.put( ThreePrimeDistanceMethod.LEFT.value, ThreePrimeDistanceMethod.LEFT );
        ThreePrimeDistanceMethod.valueList.add( ThreePrimeDistanceMethod.LEFT );
        ThreePrimeDistanceMethod.literals.add( ThreePrimeDistanceMethod.LEFT.value );
        ThreePrimeDistanceMethod.names.add( "LEFT" );
        ThreePrimeDistanceMethod.values.put( ThreePrimeDistanceMethod.MIDDLE.value, ThreePrimeDistanceMethod.MIDDLE );
        ThreePrimeDistanceMethod.valueList.add( ThreePrimeDistanceMethod.MIDDLE );
        ThreePrimeDistanceMethod.literals.add( ThreePrimeDistanceMethod.MIDDLE.value );
        ThreePrimeDistanceMethod.names.add( "MIDDLE" );
        ThreePrimeDistanceMethod.values.put( ThreePrimeDistanceMethod.RIGHT.value, ThreePrimeDistanceMethod.RIGHT );
        ThreePrimeDistanceMethod.valueList.add( ThreePrimeDistanceMethod.RIGHT );
        ThreePrimeDistanceMethod.literals.add( ThreePrimeDistanceMethod.RIGHT.value );
        ThreePrimeDistanceMethod.names.add( "RIGHT" );
        ThreePrimeDistanceMethod.valueList = Collections.unmodifiableList( ThreePrimeDistanceMethod.valueList );
        ThreePrimeDistanceMethod.literals = Collections.unmodifiableList( ThreePrimeDistanceMethod.literals );
        ThreePrimeDistanceMethod.names = Collections.unmodifiableList( ThreePrimeDistanceMethod.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    ThreePrimeDistanceMethod() {
    }

    private ThreePrimeDistanceMethod( String value ) {
        this.value = value;
    }

    /**
     * @param value the value to create the ThreePrimeDistanceMethod from.
     * @return Creates an instance of ThreePrimeDistanceMethod from <code>value</code>.
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static ThreePrimeDistanceMethod fromString( String value ) {
        final ThreePrimeDistanceMethod typeValue = ThreePrimeDistanceMethod.values.get( value );
        if ( typeValue == null ) {
            /*
             * Customization to permit database values to change before code does. Previously this would throw an
             * exception.
             */
            return null;
        }
        return typeValue;
    }

    /**
     * Returns an unmodifiable list containing the literals that are known by this enumeration.
     *
     * @return A List containing the actual literals defined by this enumeration, this list can not be modified.
     */
    @SuppressWarnings("unused") // Possible external use
    public static List<String> literals() {
        return ThreePrimeDistanceMethod.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    @SuppressWarnings("unused") // Possible external use
    public static List<String> names() {
        return ThreePrimeDistanceMethod.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    @SuppressWarnings("unused") // Possible external use
    public static List<ThreePrimeDistanceMethod> values() {
        return ThreePrimeDistanceMethod.valueList;
    }

    @Override
    public int compareTo( ThreePrimeDistanceMethod that ) {
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

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    @Override
    public boolean equals( Object object ) {
        return ( this == object ) || ( object instanceof ThreePrimeDistanceMethod
                && ( ( ThreePrimeDistanceMethod ) object ).getValue().equals( this.getValue() ) );
    }

    @Override
    public String toString() {
        return String.valueOf( value );
    }

    /**
     * This method allows the deserialization of an instance of this enumeration type to return the actual instance that
     * will be the singleton for the JVM in which the current thread is running.
     * Doing this will allow users to safely use the equality operator <code>==</code> for enumerations because a
     * regular deserialized object is always a newly constructed instance and will therefore never be an existing
     * reference; it is this <code>readResolve()</code> method which will intercept the deserialization process in order
     * to return the proper singleton reference.
     * This method is documented here:
     * <a href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">
     * Java Object Serialization Specification</a>
     *
     * @return object
     */
    private Object readResolve() {
        return ThreePrimeDistanceMethod.fromString( this.value );
    }
}