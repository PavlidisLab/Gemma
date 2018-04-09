package ubic.gemma.model.common.quantitationtype;

import java.util.*;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class StandardQuantitationType implements java.io.Serializable, Comparable<StandardQuantitationType> {
    public static final StandardQuantitationType PRESENTABSENT = new StandardQuantitationType( "PRESENTABSENT" );
    public static final StandardQuantitationType FAILED = new StandardQuantitationType( "FAILED" );
    /**
     * Referring to a measured or derived "amount", indicating the relative or absolute level of something. Typically an
     * expression level or expression ratio. This is intentionally very generic.
     */
    public static final StandardQuantitationType AMOUNT = new StandardQuantitationType( "AMOUNT" );
    public static final StandardQuantitationType CONFIDENCEINDICATOR = new StandardQuantitationType(
            "CONFIDENCEINDICATOR" );
    public static final StandardQuantitationType CORRELATION = new StandardQuantitationType( "CORRELATION" );
    /**
     * Indicates value is a count, such as the number of sequencing reads.
     */
    public static final StandardQuantitationType COUNT = new StandardQuantitationType( "COUNT" );
    /**
     * Used to represent a value for a spatial coordinate
     */
    public static final StandardQuantitationType COORDINATE = new StandardQuantitationType( "COORDINATE" );
    /**
     * Standard deviations from the mean
     */
    public static final StandardQuantitationType ZSCORE = new StandardQuantitationType( "ZSCORE" );
    public static final StandardQuantitationType OTHER = new StandardQuantitationType( "OTHER" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4967992559215681852L;
    private static final Map<String, StandardQuantitationType> values = new LinkedHashMap<>( 9, 1 );

    private static List<String> literals = new ArrayList<>( 9 );
    private static List<String> names = new ArrayList<>( 9 );
    private static List<StandardQuantitationType> valueList = new ArrayList<>( 9 );

    static {
        StandardQuantitationType.values
                .put( StandardQuantitationType.PRESENTABSENT.value, StandardQuantitationType.PRESENTABSENT );
        StandardQuantitationType.valueList.add( StandardQuantitationType.PRESENTABSENT );
        StandardQuantitationType.literals.add( StandardQuantitationType.PRESENTABSENT.value );
        StandardQuantitationType.names.add( "PRESENTABSENT" );
        StandardQuantitationType.values.put( StandardQuantitationType.FAILED.value, StandardQuantitationType.FAILED );
        StandardQuantitationType.valueList.add( StandardQuantitationType.FAILED );
        StandardQuantitationType.literals.add( StandardQuantitationType.FAILED.value );
        StandardQuantitationType.names.add( "FAILED" );
        StandardQuantitationType.values.put( StandardQuantitationType.AMOUNT.value, StandardQuantitationType.AMOUNT );
        StandardQuantitationType.valueList.add( StandardQuantitationType.AMOUNT );
        StandardQuantitationType.literals.add( StandardQuantitationType.AMOUNT.value );
        StandardQuantitationType.names.add( "AMOUNT" );
        StandardQuantitationType.values.put( StandardQuantitationType.CONFIDENCEINDICATOR.value,
                StandardQuantitationType.CONFIDENCEINDICATOR );
        StandardQuantitationType.valueList.add( StandardQuantitationType.CONFIDENCEINDICATOR );
        StandardQuantitationType.literals.add( StandardQuantitationType.CONFIDENCEINDICATOR.value );
        StandardQuantitationType.names.add( "CONFIDENCEINDICATOR" );
        StandardQuantitationType.values
                .put( StandardQuantitationType.CORRELATION.value, StandardQuantitationType.CORRELATION );
        StandardQuantitationType.valueList.add( StandardQuantitationType.CORRELATION );
        StandardQuantitationType.literals.add( StandardQuantitationType.CORRELATION.value );
        StandardQuantitationType.names.add( "CORRELATION" );
        StandardQuantitationType.values.put( StandardQuantitationType.COUNT.value, StandardQuantitationType.COUNT );
        StandardQuantitationType.valueList.add( StandardQuantitationType.COUNT );
        StandardQuantitationType.literals.add( StandardQuantitationType.COUNT.value );
        StandardQuantitationType.names.add( "COUNT" );
        StandardQuantitationType.values
                .put( StandardQuantitationType.COORDINATE.value, StandardQuantitationType.COORDINATE );
        StandardQuantitationType.valueList.add( StandardQuantitationType.COORDINATE );
        StandardQuantitationType.literals.add( StandardQuantitationType.COORDINATE.value );
        StandardQuantitationType.names.add( "COORDINATE" );
        StandardQuantitationType.values.put( StandardQuantitationType.ZSCORE.value, StandardQuantitationType.ZSCORE );
        StandardQuantitationType.valueList.add( StandardQuantitationType.ZSCORE );
        StandardQuantitationType.literals.add( StandardQuantitationType.ZSCORE.value );
        StandardQuantitationType.names.add( "ZSCORE" );
        StandardQuantitationType.values.put( StandardQuantitationType.OTHER.value, StandardQuantitationType.OTHER );
        StandardQuantitationType.valueList.add( StandardQuantitationType.OTHER );
        StandardQuantitationType.literals.add( StandardQuantitationType.OTHER.value );
        StandardQuantitationType.names.add( "OTHER" );
        StandardQuantitationType.valueList = Collections.unmodifiableList( StandardQuantitationType.valueList );
        StandardQuantitationType.literals = Collections.unmodifiableList( StandardQuantitationType.literals );
        StandardQuantitationType.names = Collections.unmodifiableList( StandardQuantitationType.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    StandardQuantitationType() {
    }

    private StandardQuantitationType( String value ) {
        this.value = value;
    }

    /**
     * Creates an instance of StandardQuantitationType from <code>value</code>.
     *
     * @param value the value to create the StandardQuantitationType from.
     * @return standard QT
     */
    public static StandardQuantitationType fromString( String value ) {
        final StandardQuantitationType typeValue = StandardQuantitationType.values.get( value );
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
        return StandardQuantitationType.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return StandardQuantitationType.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<StandardQuantitationType> values() {
        return StandardQuantitationType.valueList;
    }

    @Override
    public int compareTo( StandardQuantitationType that ) {
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
        return ( this == object ) || ( object instanceof StandardQuantitationType
                && ( ( StandardQuantitationType ) object ).getValue().equals( this.getValue() ) );
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
     * This method is documented here: <a
     * href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">Java Object Serialization
     * Specification</a>
     *
     * @return object
     */
    private Object readResolve() {
        return StandardQuantitationType.fromString( this.value );
    }
}