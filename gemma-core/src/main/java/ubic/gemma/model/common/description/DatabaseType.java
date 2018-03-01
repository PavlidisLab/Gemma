

package ubic.gemma.model.common.description;

import java.util.*;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Consistency, possible external use
public class DatabaseType implements java.io.Serializable, Comparable<DatabaseType> {
    public static final DatabaseType ONTOLOGY = new DatabaseType( "ONTOLOGY" );
    public static final DatabaseType SEQUENCE = new DatabaseType( "SEQUENCE" );
    @SuppressWarnings("WeakerAccess") // All constants should have the same access level
    public static final DatabaseType LITERATURE = new DatabaseType( "LITERATURE" );
    public static final DatabaseType EXPRESSION = new DatabaseType( "EXPRESSION" );
    /**
     * Represents a genome database such as Golden Path or Ensembl
     */
    public static final DatabaseType GENOME = new DatabaseType( "GENOME" );
    public static final DatabaseType OTHER = new DatabaseType( "OTHER" );
    public static final DatabaseType PROTEIN = new DatabaseType( "PROTEIN" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 3701187744112944950L;
    private static final Map<String, DatabaseType> values = new LinkedHashMap<>( 7, 1 );
    @SuppressWarnings("UnusedAssignment") // NOT REDUNDANT! WE USE STATIC INITIALISATION
    private static List<String> literals = new ArrayList<>( 7 );
    @SuppressWarnings("UnusedAssignment") // NOT REDUNDANT! WE USE STATIC INITIALISATION
    private static List<String> names = new ArrayList<>( 7 );
    @SuppressWarnings("UnusedAssignment") // NOT REDUNDANT! WE USE STATIC INITIALISATION
    private static List<DatabaseType> valueList = new ArrayList<>( 7 );

    static {
        DatabaseType.values.put( DatabaseType.ONTOLOGY.value, DatabaseType.ONTOLOGY );
        DatabaseType.valueList.add( DatabaseType.ONTOLOGY );
        DatabaseType.literals.add( DatabaseType.ONTOLOGY.value );
        DatabaseType.names.add( "ONTOLOGY" );
        DatabaseType.values.put( DatabaseType.SEQUENCE.value, DatabaseType.SEQUENCE );
        DatabaseType.valueList.add( DatabaseType.SEQUENCE );
        DatabaseType.literals.add( DatabaseType.SEQUENCE.value );
        DatabaseType.names.add( "SEQUENCE" );
        DatabaseType.values.put( DatabaseType.LITERATURE.value, DatabaseType.LITERATURE );
        DatabaseType.valueList.add( DatabaseType.LITERATURE );
        DatabaseType.literals.add( DatabaseType.LITERATURE.value );
        DatabaseType.names.add( "LITERATURE" );
        DatabaseType.values.put( DatabaseType.EXPRESSION.value, DatabaseType.EXPRESSION );
        DatabaseType.valueList.add( DatabaseType.EXPRESSION );
        DatabaseType.literals.add( DatabaseType.EXPRESSION.value );
        DatabaseType.names.add( "EXPRESSION" );
        DatabaseType.values.put( DatabaseType.GENOME.value, DatabaseType.GENOME );
        DatabaseType.valueList.add( DatabaseType.GENOME );
        DatabaseType.literals.add( DatabaseType.GENOME.value );
        DatabaseType.names.add( "GENOME" );
        DatabaseType.values.put( DatabaseType.OTHER.value, DatabaseType.OTHER );
        DatabaseType.valueList.add( DatabaseType.OTHER );
        DatabaseType.literals.add( DatabaseType.OTHER.value );
        DatabaseType.names.add( "OTHER" );
        DatabaseType.values.put( DatabaseType.PROTEIN.value, DatabaseType.PROTEIN );
        DatabaseType.valueList.add( DatabaseType.PROTEIN );
        DatabaseType.literals.add( DatabaseType.PROTEIN.value );
        DatabaseType.names.add( "PROTEIN" );
        DatabaseType.valueList = Collections.unmodifiableList( DatabaseType.valueList );
        DatabaseType.literals = Collections.unmodifiableList( DatabaseType.literals );
        DatabaseType.names = Collections.unmodifiableList( DatabaseType.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    DatabaseType() {
    }

    private DatabaseType( String value ) {
        this.value = value;
    }

    public static DatabaseType fromString( String value ) {
        final DatabaseType typeValue = DatabaseType.values.get( value );
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
        return DatabaseType.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return DatabaseType.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<DatabaseType> values() {
        return DatabaseType.valueList;
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( DatabaseType that ) {
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
        return ( this == object ) || ( object instanceof DatabaseType && ( ( DatabaseType ) object ).getValue()
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
     */
    private Object readResolve() {
        return DatabaseType.fromString( this.value );
    }
}