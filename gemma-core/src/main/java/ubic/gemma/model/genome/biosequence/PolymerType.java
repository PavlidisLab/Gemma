package ubic.gemma.model.genome.biosequence;

import java.util.*;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class PolymerType implements java.io.Serializable, Comparable<PolymerType> {
    public static final PolymerType DNA = new PolymerType( "DNA" );
    public static final PolymerType RNA = new PolymerType( "RNA" );
    public static final PolymerType PROTEIN = new PolymerType( "PROTEIN" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 3644803022662521933L;
    private static final Map<String, PolymerType> values = new LinkedHashMap<>( 3, 1 );
    private static List<String> literals = new ArrayList<>( 3 );
    private static List<String> names = new ArrayList<>( 3 );
    private static List<PolymerType> valueList = new ArrayList<>( 3 );

    static {
        PolymerType.values.put( PolymerType.DNA.value, PolymerType.DNA );
        PolymerType.valueList.add( PolymerType.DNA );
        PolymerType.literals.add( PolymerType.DNA.value );
        PolymerType.names.add( "DNA" );
        PolymerType.values.put( PolymerType.RNA.value, PolymerType.RNA );
        PolymerType.valueList.add( PolymerType.RNA );
        PolymerType.literals.add( PolymerType.RNA.value );
        PolymerType.names.add( "RNA" );
        PolymerType.values.put( PolymerType.PROTEIN.value, PolymerType.PROTEIN );
        PolymerType.valueList.add( PolymerType.PROTEIN );
        PolymerType.literals.add( PolymerType.PROTEIN.value );
        PolymerType.names.add( "PROTEIN" );
        PolymerType.valueList = Collections.unmodifiableList( PolymerType.valueList );
        PolymerType.literals = Collections.unmodifiableList( PolymerType.literals );
        PolymerType.names = Collections.unmodifiableList( PolymerType.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    PolymerType() {
    }

    private PolymerType( String value ) {
        this.value = value;
    }

    /**
     * @param value the value to create the PolymerType from.
     * @return Creates an instance of PolymerType from <code>value</code>.
     */
    public static PolymerType fromString( String value ) {
        final PolymerType typeValue = PolymerType.values.get( value );
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
        return PolymerType.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return PolymerType.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<PolymerType> values() {
        return PolymerType.valueList;
    }

    @Override
    public int compareTo( PolymerType that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    /**
     * Gets the underlying value of this type safe enumeration.
     *
     * @return the underlying value.
     */
    public String getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    @Override
    public boolean equals( Object object ) {
        return ( this == object ) || ( object instanceof PolymerType && ( ( PolymerType ) object ).getValue()
                .equals( this.getValue() ) );
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
        return PolymerType.fromString( this.value );
    }
}