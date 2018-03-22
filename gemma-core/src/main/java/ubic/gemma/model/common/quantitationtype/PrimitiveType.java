/*
 * The gemma-core project
 *
 * Copyright (c) 2017 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.model.common.quantitationtype;

import java.io.Serializable;
import java.util.*;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class PrimitiveType implements Serializable, Comparable<PrimitiveType> {
    public static final PrimitiveType DOUBLE = new PrimitiveType( "DOUBLE" );
    public static final PrimitiveType INT = new PrimitiveType( "INT" );
    public static final PrimitiveType LONG = new PrimitiveType( "LONG" );
    public static final PrimitiveType CHAR = new PrimitiveType( "CHAR" );
    public static final PrimitiveType BOOLEAN = new PrimitiveType( "BOOLEAN" );
    public static final PrimitiveType STRING = new PrimitiveType( "STRING" );
    public static final PrimitiveType INTARRAY = new PrimitiveType( "INTARRAY" );
    public static final PrimitiveType DOUBLEARRAY = new PrimitiveType( "DOUBLEARRAY" );
    public static final PrimitiveType CHARARRAY = new PrimitiveType( "CHARARRAY" );
    public static final PrimitiveType BOOLEANARRAY = new PrimitiveType( "BOOLEANARRAY" );
    public static final PrimitiveType STRINGARRAY = new PrimitiveType( "STRINGARRAY" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8068644810546069278L;
    private static final Map<String, PrimitiveType> values = new LinkedHashMap<>( 11, 1 );

    @SuppressWarnings("UnusedAssignment") // Not redundant, using static initialization
    private static List<String> literals = new ArrayList<>( 11 );
    @SuppressWarnings("UnusedAssignment") // Not redundant, using static initialization
    private static List<String> names = new ArrayList<>( 11 );
    @SuppressWarnings("UnusedAssignment") // Not redundant, using static initialization
    private static List<PrimitiveType> valueList = new ArrayList<>( 11 );

    static {
        PrimitiveType.values.put( PrimitiveType.DOUBLE.value, PrimitiveType.DOUBLE );
        PrimitiveType.valueList.add( PrimitiveType.DOUBLE );
        PrimitiveType.literals.add( PrimitiveType.DOUBLE.value );
        PrimitiveType.names.add( "DOUBLE" );
        PrimitiveType.values.put( PrimitiveType.INT.value, PrimitiveType.INT );
        PrimitiveType.valueList.add( PrimitiveType.INT );
        PrimitiveType.literals.add( PrimitiveType.INT.value );
        PrimitiveType.names.add( "INT" );
        PrimitiveType.values.put( PrimitiveType.LONG.value, PrimitiveType.LONG );
        PrimitiveType.valueList.add( PrimitiveType.LONG );
        PrimitiveType.literals.add( PrimitiveType.LONG.value );
        PrimitiveType.names.add( "LONG" );
        PrimitiveType.values.put( PrimitiveType.CHAR.value, PrimitiveType.CHAR );
        PrimitiveType.valueList.add( PrimitiveType.CHAR );
        PrimitiveType.literals.add( PrimitiveType.CHAR.value );
        PrimitiveType.names.add( "CHAR" );
        PrimitiveType.values.put( PrimitiveType.BOOLEAN.value, PrimitiveType.BOOLEAN );
        PrimitiveType.valueList.add( PrimitiveType.BOOLEAN );
        PrimitiveType.literals.add( PrimitiveType.BOOLEAN.value );
        PrimitiveType.names.add( "BOOLEAN" );
        PrimitiveType.values.put( PrimitiveType.STRING.value, PrimitiveType.STRING );
        PrimitiveType.valueList.add( PrimitiveType.STRING );
        PrimitiveType.literals.add( PrimitiveType.STRING.value );
        PrimitiveType.names.add( "STRING" );
        PrimitiveType.values.put( PrimitiveType.INTARRAY.value, PrimitiveType.INTARRAY );
        PrimitiveType.valueList.add( PrimitiveType.INTARRAY );
        PrimitiveType.literals.add( PrimitiveType.INTARRAY.value );
        PrimitiveType.names.add( "INTARRAY" );
        PrimitiveType.values.put( PrimitiveType.DOUBLEARRAY.value, PrimitiveType.DOUBLEARRAY );
        PrimitiveType.valueList.add( PrimitiveType.DOUBLEARRAY );
        PrimitiveType.literals.add( PrimitiveType.DOUBLEARRAY.value );
        PrimitiveType.names.add( "DOUBLEARRAY" );
        PrimitiveType.values.put( PrimitiveType.CHARARRAY.value, PrimitiveType.CHARARRAY );
        PrimitiveType.valueList.add( PrimitiveType.CHARARRAY );
        PrimitiveType.literals.add( PrimitiveType.CHARARRAY.value );
        PrimitiveType.names.add( "CHARARRAY" );
        PrimitiveType.values.put( PrimitiveType.BOOLEANARRAY.value, PrimitiveType.BOOLEANARRAY );
        PrimitiveType.valueList.add( PrimitiveType.BOOLEANARRAY );
        PrimitiveType.literals.add( PrimitiveType.BOOLEANARRAY.value );
        PrimitiveType.names.add( "BOOLEANARRAY" );
        PrimitiveType.values.put( PrimitiveType.STRINGARRAY.value, PrimitiveType.STRINGARRAY );
        PrimitiveType.valueList.add( PrimitiveType.STRINGARRAY );
        PrimitiveType.literals.add( PrimitiveType.STRINGARRAY.value );
        PrimitiveType.names.add( "STRINGARRAY" );
        PrimitiveType.valueList = Collections.unmodifiableList( PrimitiveType.valueList );
        PrimitiveType.literals = Collections.unmodifiableList( PrimitiveType.literals );
        PrimitiveType.names = Collections.unmodifiableList( PrimitiveType.names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    PrimitiveType() {
    }

    private PrimitiveType( String value ) {
        this.value = value;
    }

    /**
     * Creates an instance of PrimitiveType from <code>value</code>.
     *
     * @param value the value to create the PrimitiveType from.
     * @return primitive type
     */
    public static PrimitiveType fromString( String value ) {
        final PrimitiveType typeValue = PrimitiveType.values.get( value );
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
        return PrimitiveType.literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return PrimitiveType.names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<PrimitiveType> values() {
        return PrimitiveType.valueList;
    }

    @Override
    public int compareTo( PrimitiveType that ) {
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
        return ( this == object ) || ( object instanceof PrimitiveType && ( ( PrimitiveType ) object ).getValue()
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
     * This method is documented here: <a
     * href="http://java.sun.com/j2se/1.3/docs/guide/serialization/spec/input.doc6.html">Java Object Serialization
     * Specification</a>
     *
     * @return object
     */
    private Object readResolve() {
        return PrimitiveType.fromString( this.value );
    }
}