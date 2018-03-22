package ubic.gemma.model.common.measurement;

import java.util.List;
import java.util.Map;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class MeasurementKind implements java.io.Serializable, Comparable<MeasurementKind> {

    public static final MeasurementKind TIME = new MeasurementKind( "TIME" );
    public static final MeasurementKind DISTANCE = new MeasurementKind( "DISTANCE" );
    public static final MeasurementKind TEMPERATURE = new MeasurementKind( "TEMPERATURE" );
    public static final MeasurementKind QUANTITY = new MeasurementKind( "QUANTITY" );
    public static final MeasurementKind MASS = new MeasurementKind( "MASS" );
    public static final MeasurementKind VOLUME = new MeasurementKind( "VOLUME" );
    public static final MeasurementKind CONCENTRATION = new MeasurementKind( "CONC" );
    public static final MeasurementKind OTHER = new MeasurementKind( "OTHER" );
    public static final MeasurementKind COUNT = new MeasurementKind( "COUNT" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 7640193836825779342L;
    private static final Map<String, MeasurementKind> values = new java.util.LinkedHashMap<>( 9, 1 );

    private static List<String> literals = new java.util.ArrayList<>( 9 );
    private static List<String> names = new java.util.ArrayList<>( 9 );
    private static List<MeasurementKind> valueList = new java.util.ArrayList<>( 9 );

    static {
        MeasurementKind.values.put( MeasurementKind.TIME.value, MeasurementKind.TIME );
        MeasurementKind.valueList.add( MeasurementKind.TIME );
        MeasurementKind.literals.add( MeasurementKind.TIME.value );
        MeasurementKind.names.add( "TIME" );
        MeasurementKind.values.put( MeasurementKind.DISTANCE.value, MeasurementKind.DISTANCE );
        MeasurementKind.valueList.add( MeasurementKind.DISTANCE );
        MeasurementKind.literals.add( MeasurementKind.DISTANCE.value );
        MeasurementKind.names.add( "DISTANCE" );
        MeasurementKind.values.put( MeasurementKind.TEMPERATURE.value, MeasurementKind.TEMPERATURE );
        MeasurementKind.valueList.add( MeasurementKind.TEMPERATURE );
        MeasurementKind.literals.add( MeasurementKind.TEMPERATURE.value );
        MeasurementKind.names.add( "TEMPERATURE" );
        MeasurementKind.values.put( MeasurementKind.QUANTITY.value, MeasurementKind.QUANTITY );
        MeasurementKind.valueList.add( MeasurementKind.QUANTITY );
        MeasurementKind.literals.add( MeasurementKind.QUANTITY.value );
        MeasurementKind.names.add( "QUANTITY" );
        MeasurementKind.values.put( MeasurementKind.MASS.value, MeasurementKind.MASS );
        MeasurementKind.valueList.add( MeasurementKind.MASS );
        MeasurementKind.literals.add( MeasurementKind.MASS.value );
        MeasurementKind.names.add( "MASS" );
        MeasurementKind.values.put( MeasurementKind.VOLUME.value, MeasurementKind.VOLUME );
        MeasurementKind.valueList.add( MeasurementKind.VOLUME );
        MeasurementKind.literals.add( MeasurementKind.VOLUME.value );
        MeasurementKind.names.add( "VOLUME" );
        MeasurementKind.values.put( MeasurementKind.CONCENTRATION.value, MeasurementKind.CONCENTRATION );
        MeasurementKind.valueList.add( MeasurementKind.CONCENTRATION );
        MeasurementKind.literals.add( MeasurementKind.CONCENTRATION.value );
        MeasurementKind.names.add( "CONCENTRATION" );
        MeasurementKind.values.put( MeasurementKind.OTHER.value, MeasurementKind.OTHER );
        MeasurementKind.valueList.add( MeasurementKind.OTHER );
        MeasurementKind.literals.add( MeasurementKind.OTHER.value );
        MeasurementKind.names.add( "OTHER" );
        MeasurementKind.values.put( MeasurementKind.COUNT.value, MeasurementKind.COUNT );
        MeasurementKind.valueList.add( MeasurementKind.COUNT );
        MeasurementKind.literals.add( MeasurementKind.COUNT.value );
        MeasurementKind.names.add( "COUNT" );
        MeasurementKind.valueList = java.util.Collections.unmodifiableList( MeasurementKind.valueList );
        MeasurementKind.literals = java.util.Collections.unmodifiableList( MeasurementKind.literals );
        MeasurementKind.names = java.util.Collections.unmodifiableList( MeasurementKind.names );
    }

    private String value;

    protected MeasurementKind() {
    }

    private MeasurementKind( String value ) {
        this.value = value;
    }

    /**
     * Creates an instance of MeasurementKind from <code>value</code>.
     *
     * @param value the value to create the MeasurementKind from.
     * @return measurement kind
     */
    public static MeasurementKind fromString( String value ) {
        final MeasurementKind typeValue = MeasurementKind.values.get( value );
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
        return MeasurementKind.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static java.util.List<String> names() {
        return MeasurementKind.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static java.util.List<MeasurementKind> values() {
        return MeasurementKind.valueList;
    }

    @Override
    public int compareTo( MeasurementKind that ) {
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
        return ( this == object ) || ( object instanceof MeasurementKind && ( ( MeasurementKind ) object ).getValue()
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
     * <a href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">Java Object Serialization
     * Specification</a>
     *
     * @return object
     */
    private Object readResolve() {
        return MeasurementKind.fromString( this.value );
    }
}