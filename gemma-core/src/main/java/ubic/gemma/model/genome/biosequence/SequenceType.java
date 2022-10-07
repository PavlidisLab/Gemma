package ubic.gemma.model.genome.biosequence;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.*;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class SequenceType implements java.io.Serializable, Comparable<SequenceType> {
    /**
     * Represents the target sequence provided by the manufacturer.
     */
    public static final SequenceType AFFY_TARGET = new SequenceType( "AFFY_TARGET" );
    /**
     * Represents a single probe sequence for Affymetrix reporters
     */
    public static final SequenceType AFFY_PROBE = new SequenceType( "AFFY_PROBE" );
    public static final SequenceType EST = new SequenceType( "EST" );
    public static final SequenceType mRNA = new SequenceType( "mRNA" );
    public static final SequenceType REFSEQ = new SequenceType( "REFSEQ" );
    public static final SequenceType BAC = new SequenceType( "BAC" );
    public static final SequenceType WHOLE_GENOME = new SequenceType( "WHOLE_GENOME" );
    public static final SequenceType WHOLE_CHROMOSOME = new SequenceType( "WHOLE_CHROMOSOME" );
    /**
     * Generic DNA sequence of any other type not representable by another value
     */
    public static final SequenceType DNA = new SequenceType( "DNA" );

    public static final SequenceType OTHER = new SequenceType( "OTHER" );

    public static final SequenceType ORF = new SequenceType( "ORF" );
    /**
     * Represents Affymetrix probe sequences that have been "collapsed" or combined into a single sequence.
     */
    public static final SequenceType AFFY_COLLAPSED = new SequenceType( "AFFY_COLLAPSED" );
    /**
     * Represents a (synthetic) oligonucleotide.
     */
    public static final SequenceType OLIGO = new SequenceType( "OLIGO" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -514928613005922741L;
    private static final Map<String, SequenceType> values = new LinkedHashMap<>( 13, 1 );
    private static List<String> literals = new ArrayList<>( 13 );
    private static List<String> names = new ArrayList<>( 13 );
    private static List<SequenceType> valueList = new ArrayList<>( 13 );

    static {
        SequenceType.values.put( SequenceType.AFFY_TARGET.value, SequenceType.AFFY_TARGET );
        SequenceType.valueList.add( SequenceType.AFFY_TARGET );
        SequenceType.literals.add( SequenceType.AFFY_TARGET.value );
        SequenceType.names.add( "AFFY_TARGET" );
        SequenceType.values.put( SequenceType.AFFY_PROBE.value, SequenceType.AFFY_PROBE );
        SequenceType.valueList.add( SequenceType.AFFY_PROBE );
        SequenceType.literals.add( SequenceType.AFFY_PROBE.value );
        SequenceType.names.add( "AFFY_PROBE" );
        SequenceType.values.put( SequenceType.EST.value, SequenceType.EST );
        SequenceType.valueList.add( SequenceType.EST );
        SequenceType.literals.add( SequenceType.EST.value );
        SequenceType.names.add( "EST" );
        SequenceType.values.put( SequenceType.mRNA.value, SequenceType.mRNA );
        SequenceType.valueList.add( SequenceType.mRNA );
        SequenceType.literals.add( SequenceType.mRNA.value );
        SequenceType.names.add( "mRNA" );
        SequenceType.values.put( SequenceType.REFSEQ.value, SequenceType.REFSEQ );
        SequenceType.valueList.add( SequenceType.REFSEQ );
        SequenceType.literals.add( SequenceType.REFSEQ.value );
        SequenceType.names.add( "REFSEQ" );
        SequenceType.values.put( SequenceType.BAC.value, SequenceType.BAC );
        SequenceType.valueList.add( SequenceType.BAC );
        SequenceType.literals.add( SequenceType.BAC.value );
        SequenceType.names.add( "BAC" );
        SequenceType.values.put( SequenceType.WHOLE_GENOME.value, SequenceType.WHOLE_GENOME );
        SequenceType.valueList.add( SequenceType.WHOLE_GENOME );
        SequenceType.literals.add( SequenceType.WHOLE_GENOME.value );
        SequenceType.names.add( "WHOLE_GENOME" );
        SequenceType.values.put( SequenceType.WHOLE_CHROMOSOME.value, SequenceType.WHOLE_CHROMOSOME );
        SequenceType.valueList.add( SequenceType.WHOLE_CHROMOSOME );
        SequenceType.literals.add( SequenceType.WHOLE_CHROMOSOME.value );
        SequenceType.names.add( "WHOLE_CHROMOSOME" );
        SequenceType.values.put( SequenceType.DNA.value, SequenceType.DNA );
        SequenceType.valueList.add( SequenceType.DNA );
        SequenceType.literals.add( SequenceType.DNA.value );
        SequenceType.names.add( "DNA" );
        SequenceType.values.put( SequenceType.OTHER.value, SequenceType.OTHER );
        SequenceType.valueList.add( SequenceType.OTHER );
        SequenceType.literals.add( SequenceType.OTHER.value );
        SequenceType.names.add( "OTHER" );
        SequenceType.values.put( SequenceType.ORF.value, SequenceType.ORF );
        SequenceType.valueList.add( SequenceType.ORF );
        SequenceType.literals.add( SequenceType.ORF.value );
        SequenceType.names.add( "ORF" );
        SequenceType.values.put( SequenceType.AFFY_COLLAPSED.value, SequenceType.AFFY_COLLAPSED );
        SequenceType.valueList.add( SequenceType.AFFY_COLLAPSED );
        SequenceType.literals.add( SequenceType.AFFY_COLLAPSED.value );
        SequenceType.names.add( "AFFY_COLLAPSED" );
        SequenceType.values.put( SequenceType.OLIGO.value, SequenceType.OLIGO );
        SequenceType.valueList.add( SequenceType.OLIGO );
        SequenceType.literals.add( SequenceType.OLIGO.value );
        SequenceType.names.add( "OLIGO" );
        SequenceType.valueList = Collections.unmodifiableList( SequenceType.valueList );
        SequenceType.literals = Collections.unmodifiableList( SequenceType.literals );
        SequenceType.names = Collections.unmodifiableList( SequenceType.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    SequenceType() {
    }

    private SequenceType( String value ) {
        this.value = value;
    }

    /**
     * @param value the value to create the SequenceType from.
     * @return Creates an instance of SequenceType from <code>value</code>.
     */
    public static SequenceType fromString( String value ) {
        final SequenceType typeValue = SequenceType.values.get( value );
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
        return SequenceType.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return SequenceType.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<SequenceType> values() {
        return SequenceType.valueList;
    }

    @Override
    public int compareTo( SequenceType that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    /**
     * Gets the underlying value of this type safe enumeration.
     *
     * @return the underlying value.
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    @JsonValue
    public String getValue() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    @Override
    public boolean equals( Object object ) {
        return ( this == object ) || ( object instanceof SequenceType && ( ( SequenceType ) object ).getValue()
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
        return SequenceType.fromString( this.value );
    }
}