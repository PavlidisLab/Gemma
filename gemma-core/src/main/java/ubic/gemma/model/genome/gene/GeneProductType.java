package ubic.gemma.model.genome.gene;

import java.util.*;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class GeneProductType implements java.io.Serializable, Comparable<GeneProductType> {
    public static final GeneProductType PROTEIN = new GeneProductType( "PROTEIN" );
    public static final GeneProductType RNA = new GeneProductType( "RNA" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8731014426320800790L;
    private static final Map<String, GeneProductType> values = new LinkedHashMap<>( 2, 1 );
    private static List<String> literals = new ArrayList<>( 2 );
    private static List<String> names = new ArrayList<>( 2 );
    private static List<GeneProductType> valueList = new ArrayList<>( 2 );

    static {
        GeneProductType.values.put( GeneProductType.PROTEIN.value, GeneProductType.PROTEIN );
        GeneProductType.valueList.add( GeneProductType.PROTEIN );
        GeneProductType.literals.add( GeneProductType.PROTEIN.value );
        GeneProductType.names.add( "PROTEIN" );
        GeneProductType.values.put( GeneProductType.RNA.value, GeneProductType.RNA );
        GeneProductType.valueList.add( GeneProductType.RNA );
        GeneProductType.literals.add( GeneProductType.RNA.value );
        GeneProductType.names.add( "RNA" );
        GeneProductType.valueList = Collections.unmodifiableList( GeneProductType.valueList );
        GeneProductType.literals = Collections.unmodifiableList( GeneProductType.literals );
        GeneProductType.names = Collections.unmodifiableList( GeneProductType.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    GeneProductType() {
    }

    private GeneProductType( String value ) {
        this.value = value;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static GeneProductType fromString( String value ) {
        final GeneProductType typeValue = GeneProductType.values.get( value );
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
    public static List<String> literals() {
        return GeneProductType.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return GeneProductType.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<GeneProductType> values() {
        return GeneProductType.valueList;
    }

    @Override
    public int compareTo( GeneProductType that ) {
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
        return ( this == object ) || ( object instanceof GeneProductType && ( ( GeneProductType ) object ).getValue()
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
     * This method is documented here: <a href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">
     * Java Object Serialization Specification</a>
     *
     * @return object
     */
    private Object readResolve() {
        return GeneProductType.fromString( this.value );
    }
}