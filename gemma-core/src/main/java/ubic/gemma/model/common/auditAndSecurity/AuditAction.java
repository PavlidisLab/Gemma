

package ubic.gemma.model.common.auditAndSecurity;

import java.util.*;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class AuditAction implements java.io.Serializable, Comparable<AuditAction> {
    public static final AuditAction CREATE = new AuditAction( "C" );
    public static final AuditAction READ = new AuditAction( "R" );
    public static final AuditAction UPDATE = new AuditAction( "U" );
    public static final AuditAction DELETE = new AuditAction( "D" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 4628458718626475444L;
    private static final Map<String, AuditAction> values = new LinkedHashMap<>( 4, 1 );
    private static List<String> literals = new ArrayList<>( 4 );
    private static List<String> names = new ArrayList<>( 4 );
    private static List<AuditAction> valueList = new ArrayList<>( 4 );

    static {
        AuditAction.values.put( AuditAction.CREATE.value, AuditAction.CREATE );
        AuditAction.valueList.add( AuditAction.CREATE );
        AuditAction.literals.add( AuditAction.CREATE.value );
        AuditAction.names.add( "CREATE" );
        AuditAction.values.put( AuditAction.READ.value, AuditAction.READ );
        AuditAction.valueList.add( AuditAction.READ );
        AuditAction.literals.add( AuditAction.READ.value );
        AuditAction.names.add( "READ" );
        AuditAction.values.put( AuditAction.UPDATE.value, AuditAction.UPDATE );
        AuditAction.valueList.add( AuditAction.UPDATE );
        AuditAction.literals.add( AuditAction.UPDATE.value );
        AuditAction.names.add( "UPDATE" );
        AuditAction.values.put( AuditAction.DELETE.value, AuditAction.DELETE );
        AuditAction.valueList.add( AuditAction.DELETE );
        AuditAction.literals.add( AuditAction.DELETE.value );
        AuditAction.names.add( "DELETE" );
        AuditAction.valueList = Collections.unmodifiableList( AuditAction.valueList );
        AuditAction.literals = Collections.unmodifiableList( AuditAction.literals );
        AuditAction.names = Collections.unmodifiableList( AuditAction.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    AuditAction() {
    }

    private AuditAction( String value ) {
        this.value = value;
    }

    public static AuditAction fromString( String value ) {
        final AuditAction typeValue = AuditAction.values.get( value );
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
        return AuditAction.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return AuditAction.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<AuditAction> values() {
        return AuditAction.valueList;
    }

    @Override
    public int compareTo( AuditAction that ) {
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
        return ( this == object ) || ( object instanceof AuditAction && ( ( AuditAction ) object ).getValue()
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
        return AuditAction.fromString( this.value );
    }
}