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

import java.util.*;

public class ScaleType implements java.io.Serializable, Comparable<ScaleType> {
    /**
     * This is effectively the opposite of "log-transformed" (or any other transformation)
     */
    public static final ScaleType LINEAR = new ScaleType( "LINEAR" );
    public static final ScaleType LN = new ScaleType( "LN" );
    public static final ScaleType LOG2 = new ScaleType( "LOG2" );
    public static final ScaleType LOG10 = new ScaleType( "LOG10" );
    public static final ScaleType LOGBASEUNKNOWN = new ScaleType( "LOGBASEUNKNOWN" );
    /**
     * Deprecated, do not use.
     */
    public static final ScaleType FOLDCHANGE = new ScaleType( "FOLDCHANGE" );
    public static final ScaleType OTHER = new ScaleType( "OTHER" );
    /**
     * An unscaled measurement is one that has no inherent scale; e.g., a categorial value.
     */
    public static final ScaleType UNSCALED = new ScaleType( "UNSCALED" );
    /**
     * Constrained to be a value between 0 and 100.
     */
    public static final ScaleType PERCENT = new ScaleType( "PERCENT" );
    /**
     * Indicates value was (originally) an integer count of something, such as RNAseq reads. This does not mean the
     * value is necessarily an integer.
     */
    public static final ScaleType COUNT = new ScaleType( "COUNT" );
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 2817283097042204701L;
    private static final Map<String, ScaleType> values = new LinkedHashMap<>( 10, 1 );

    private static List<String> literals = new ArrayList<>( 10 );
    private static List<String> names = new ArrayList<>( 10 );
    private static List<ScaleType> valueList = new ArrayList<>( 10 );

    static {
        values.put( LINEAR.value, LINEAR );
        valueList.add( LINEAR );
        literals.add( LINEAR.value );
        names.add( "LINEAR" );
        values.put( LN.value, LN );
        valueList.add( LN );
        literals.add( LN.value );
        names.add( "LN" );
        values.put( LOG2.value, LOG2 );
        valueList.add( LOG2 );
        literals.add( LOG2.value );
        names.add( "LOG2" );
        values.put( LOG10.value, LOG10 );
        valueList.add( LOG10 );
        literals.add( LOG10.value );
        names.add( "LOG10" );
        values.put( LOGBASEUNKNOWN.value, LOGBASEUNKNOWN );
        valueList.add( LOGBASEUNKNOWN );
        literals.add( LOGBASEUNKNOWN.value );
        names.add( "LOGBASEUNKNOWN" );
        values.put( FOLDCHANGE.value, FOLDCHANGE );
        valueList.add( FOLDCHANGE );
        literals.add( FOLDCHANGE.value );
        names.add( "FOLDCHANGE" );
        values.put( OTHER.value, OTHER );
        valueList.add( OTHER );
        literals.add( OTHER.value );
        names.add( "OTHER" );
        values.put( UNSCALED.value, UNSCALED );
        valueList.add( UNSCALED );
        literals.add( UNSCALED.value );
        names.add( "UNSCALED" );
        values.put( PERCENT.value, PERCENT );
        valueList.add( PERCENT );
        literals.add( PERCENT.value );
        names.add( "PERCENT" );
        values.put( COUNT.value, COUNT );
        valueList.add( COUNT );
        literals.add( COUNT.value );
        names.add( "COUNT" );
        valueList = Collections.unmodifiableList( valueList );
        literals = Collections.unmodifiableList( literals );
        names = Collections.unmodifiableList( names );
    }

    private String value;

    /**
     * The default constructor allowing super classes to access it.
     */
    protected ScaleType() {
    }

    private ScaleType( String value ) {
        this.value = value;
    }

    /**
     * Creates an instance of ScaleType from <code>value</code>.
     *
     * @param value the value to create the ScaleType from.
     */
    public static ScaleType fromString( String value ) {
        final ScaleType typeValue = values.get( value );
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
        return literals;
    }

    /**
     * Returns an unmodifiable list containing the names of the literals that are known by this enumeration.
     *
     * @return A List containing the actual names of the literals defined by this enumeration, this list can not be
     * modified.
     */
    public static List<String> names() {
        return names;
    }

    /**
     * Returns an unmodifiable list containing the actual enumeration instance values.
     *
     * @return A List containing the actual enumeration instance values.
     */
    public static List<ScaleType> values() {
        return valueList;
    }

    @Override
    public int compareTo( ScaleType that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    @Override
    public boolean equals( Object object ) {
        return ( this == object ) || ( object instanceof ScaleType && ( ( ScaleType ) object ).getValue()
                .equals( this.getValue() ) );
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
        return ScaleType.fromString( this.value );
    }
}