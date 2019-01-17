package ubic.gemma.model.expression.arrayDesign;

import java.util.*;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class TechnologyType implements java.io.Serializable, Comparable<TechnologyType> {
    /**
     * <p>
     * Indicates this platform uses two channels and expression measurements are ratios.
     * </p>
     */
    public static final TechnologyType TWOCOLOR = new TechnologyType( "TWOCOLOR" );
    /**
     * <p>
     * Indicates this platform can be used in either a one- or two- channel mode.
     * </p>
     */
    public static final TechnologyType DUALMODE = new TechnologyType( "DUALMODE" );
    /**
     * <p>
     * Indicates this platform uses one channel and measurements are non-ratiometric (e.g. Affymetrix oligo arrays)
     * </p>
     */
    public static final TechnologyType ONECOLOR = new TechnologyType( "ONECOLOR" );

    /**
     * Indicate the platform is based on sequencing (e.g. Illumina short reads, SAGE, OxfordNanopore). However, once we
     * process RNA-seq data it ends up on a GENELIST platform.
     */
    public static final TechnologyType SEQUENCING = new TechnologyType( "SEQUENCING" );

    /**
     * Indicates that this "platform" is just a list of genes (we use this for RNA-seq)
     */
    public static final TechnologyType GENELIST = new TechnologyType( "GENELIST" );

    /** We don't know */
    public static final TechnologyType OTHER = new TechnologyType( "OTHER" );

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 994098639935513674L;
    private static final Map<String, TechnologyType> values = new LinkedHashMap<>( 4, 1 );
    private static List<String> literals = new ArrayList<>( 4 );
    private static List<String> names = new ArrayList<>( 4 );
    private static List<TechnologyType> valueList = new ArrayList<>( 4 );

    static {
        TechnologyType.values.put( TechnologyType.TWOCOLOR.value, TechnologyType.TWOCOLOR );
        TechnologyType.valueList.add( TechnologyType.TWOCOLOR );
        TechnologyType.literals.add( TechnologyType.TWOCOLOR.value );
        TechnologyType.names.add( "TWOCOLOR" );
        TechnologyType.values.put( TechnologyType.DUALMODE.value, TechnologyType.DUALMODE );
        TechnologyType.valueList.add( TechnologyType.DUALMODE );
        TechnologyType.literals.add( TechnologyType.DUALMODE.value );
        TechnologyType.names.add( "DUALMODE" );
        TechnologyType.values.put( TechnologyType.ONECOLOR.value, TechnologyType.ONECOLOR );
        TechnologyType.valueList.add( TechnologyType.ONECOLOR );
        TechnologyType.literals.add( TechnologyType.ONECOLOR.value );
        TechnologyType.names.add( "ONECOLOR" );
        TechnologyType.values.put( TechnologyType.GENELIST.value, TechnologyType.GENELIST );
        TechnologyType.valueList.add( TechnologyType.GENELIST );
        TechnologyType.literals.add( TechnologyType.GENELIST.value );
        TechnologyType.names.add( "GENELIST" );
        TechnologyType.values.put( TechnologyType.SEQUENCING.value, TechnologyType.SEQUENCING );
        TechnologyType.valueList.add( TechnologyType.SEQUENCING );
        TechnologyType.literals.add( TechnologyType.SEQUENCING.value );
        TechnologyType.names.add( "SEQUENCING" );

        TechnologyType.values.put( TechnologyType.OTHER.value, TechnologyType.OTHER );
        TechnologyType.valueList.add( TechnologyType.OTHER );
        TechnologyType.literals.add( TechnologyType.OTHER.value );
        TechnologyType.names.add( "OTHER" );

        TechnologyType.valueList = Collections.unmodifiableList( TechnologyType.valueList );
        TechnologyType.literals = Collections.unmodifiableList( TechnologyType.literals );
        TechnologyType.names = Collections.unmodifiableList( TechnologyType.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    TechnologyType() {
    }

    private TechnologyType( String value ) {
        this.value = value;
    }

    /**
     * Creates an instance of TechnologyType from <code>value</code>.
     *
     * @param  value the value to create the TechnologyType from.
     * @return       technology type
     */
    public static TechnologyType fromString( String value ) {
        final TechnologyType typeValue = TechnologyType.values.get( value );
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
        return TechnologyType.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     *         modified.
     */
    public static List<String> names() {
        return TechnologyType.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<TechnologyType> values() {
        return TechnologyType.valueList;
    }

    @Override
    public int compareTo( TechnologyType that ) {
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
        return ( this == object ) || ( object instanceof TechnologyType && ( ( TechnologyType ) object ).getValue()
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
        return TechnologyType.fromString( this.value );
    }
}