package ubic.gemma.model.expression.experiment;

import java.util.*;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class FactorType implements java.io.Serializable, Comparable<FactorType> {
    public static final FactorType CONTINUOUS = new FactorType( "continuous" );
    public static final FactorType CATEGORICAL = new FactorType( "categorical" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 94783304160127784L;
    private static final Map<String, FactorType> values = new LinkedHashMap<>( 2, 1 );
    private static List<String> literals = new ArrayList<>( 2 );
    private static List<String> names = new ArrayList<>( 2 );
    private static List<FactorType> valueList = new ArrayList<>( 2 );

    static {
        FactorType.values.put( FactorType.CONTINUOUS.value, FactorType.CONTINUOUS );
        FactorType.valueList.add( FactorType.CONTINUOUS );
        FactorType.literals.add( FactorType.CONTINUOUS.value );
        FactorType.names.add( "CONTINUOUS" );
        FactorType.values.put( FactorType.CATEGORICAL.value, FactorType.CATEGORICAL );
        FactorType.valueList.add( FactorType.CATEGORICAL );
        FactorType.literals.add( FactorType.CATEGORICAL.value );
        FactorType.names.add( "CATEGORICAL" );
        FactorType.valueList = Collections.unmodifiableList( FactorType.valueList );
        FactorType.literals = Collections.unmodifiableList( FactorType.literals );
        FactorType.names = Collections.unmodifiableList( FactorType.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    FactorType() {
    }

    private FactorType( String value ) {
        this.value = value;
    }

    /**
     * Creates an instance of FactorType from <code>value</code>.
     *
     * @param value the value to create the FactorType from.
     * @return factor type
     */
    public static FactorType fromString( String value ) {
        final FactorType typeValue = FactorType.values.get( value );
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
    public static List<String> literals() {
        return FactorType.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return FactorType.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<FactorType> values() {
        return FactorType.valueList;
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( FactorType that ) {
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
        return ( this == object ) || ( object instanceof FactorType && ( ( FactorType ) object ).getValue()
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
     * regular deserialized object is always a newly constructed instance and will therefore never be an existing
     * reference; it is this <code>readResolve()</code> method which will intercept the deserialization process in order
     * to return the proper singleton reference.
     * This method is documented here:
     * <a href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">Java Object Serialization
     * Specification</a>
     *
     * @return object
     */
    private Object readResolve() {
        return FactorType.fromString( this.value );
    }
}