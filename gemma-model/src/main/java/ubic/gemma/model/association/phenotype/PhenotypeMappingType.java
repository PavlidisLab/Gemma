/*
 * The gemma-model project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.association.phenotype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;

/**
 * represents an enum of evidence mapping type, copied logic of GOEvidenceCodeEnum
 */
public class PhenotypeMappingType implements java.io.Serializable, Comparable<PhenotypeMappingType>,
        org.hibernate.usertype.EnhancedUserType {

    private static final long serialVersionUID = -3336933794060950406L;
    public static final PhenotypeMappingType XREF = new PhenotypeMappingType( "Cross Reference" );
    public static final PhenotypeMappingType CURATED = new PhenotypeMappingType( "Curated" );
    public static final PhenotypeMappingType INFERRED_XREF = new PhenotypeMappingType( "Inferred Cross Reference" );
    public static final PhenotypeMappingType INFERRED_CURATED = new PhenotypeMappingType( "Inferred Curated" );

    private String value;

    private static final int[] SQL_TYPES = { Types.VARCHAR };

    private static final java.util.Map<String, PhenotypeMappingType> values = new java.util.LinkedHashMap<>();

    static {
        values.put( XREF.value, XREF );
        values.put( CURATED.value, CURATED );
        values.put( INFERRED_XREF.value, INFERRED_XREF );
        values.put( INFERRED_CURATED.value, INFERRED_CURATED );
    }

    private PhenotypeMappingType( String value ) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf( value );
    }

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public int compareTo( PhenotypeMappingType that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    @Override
    public boolean equals( Object object ) {
        return ( this == object )
                || ( object instanceof PhenotypeMappingType && ( ( PhenotypeMappingType ) object ).getValue().equals(
                        this.getValue() ) );
    }

    public PhenotypeMappingType() {
        super();
    }

    /**
     * @see org.hibernate.usertype.UserType#assemble(java.io.Serializable cached, Object owner)
     */
    @Override
    public Object assemble( java.io.Serializable cached, Object owner ) {
        return cached;
    }

    /**
     * @see org.hibernate.usertype.UserType#deepCopy(Object)
     */
    @Override
    public Object deepCopy( Object v ) throws HibernateException {
        // Enums are immutable - nothing to be done to deeply clone it
        return v;
    }

    /**
     * @see org.hibernate.usertype.UserType#disassemble(Object value)
     */
    @Override
    public java.io.Serializable disassemble( Object v ) {
        return ( Serializable ) v;
    }

    /**
     * @see org.hibernate.usertype.UserType#equals(Object, Object)
     */
    @Override
    public boolean equals( Object x, Object y ) throws HibernateException {
        return ( x == y ) || ( x != null && y != null && y.equals( x ) );
    }

    /**
     * @see org.hibernate.usertype.EnhancedUserType#fromXMLString(String string)
     */
    @Override
    public Object fromXMLString( String string ) {
        return PhenotypeMappingType.fromString( String.valueOf( string ) );
    }

    /**
     * @see org.hibernate.usertype.UserType#hashCode(Object value)
     */
    @Override
    public int hashCode( Object v ) {
        return value.hashCode();
    }

    /**
     * @see org.hibernate.usertype.UserType#isMutable()
     */
    @Override
    public boolean isMutable() {
        // Enums are immutable
        return false;
    }

    /**
     * @see org.hibernate.usertype.UserType#nullSafeGet(java.sql.ResultSet, String[], Object)
     */
    @Override
    public Object nullSafeGet( ResultSet resultSet, String[] vs, Object owner ) throws HibernateException, SQLException {
        final String v = ( String ) resultSet.getObject( vs[0] );
        return resultSet.wasNull() ? null : fromString( v );
    }

    /**
     * @see org.hibernate.usertype.UserType#nullSafeSet(java.sql.PreparedStatement, Object, int)
     */
    @Override
    public void nullSafeSet( PreparedStatement statement, Object v, int index ) throws HibernateException, SQLException {
        if ( v == null ) {
            statement.setNull( index, Types.VARCHAR );
        } else {
            statement.setObject( index, String.valueOf( String.valueOf( v ) ) );
        }
    }

    /**
     * @see org.hibernate.usertype.EnhancedUserType#objectToSQLString(Object object)
     */
    @Override
    public String objectToSQLString( Object object ) {
        return String.valueOf( ( ( ubic.gemma.model.association.phenotype.PhenotypeMappingType ) object ).getValue() );
    }

    /**
     * @see org.hibernate.usertype.UserType#replace(Object original, Object target, Object owner)
     */
    @Override
    public Object replace( Object original, Object target, Object owner ) {
        return original;
    }

    /**
     * @see org.hibernate.usertype.UserType#returnedClass()
     */
    @Override
    public Class<?> returnedClass() {
        return PhenotypeMappingType.class;
    }

    /**
     * @see org.hibernate.usertype.UserType#sqlTypes()
     */
    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    /**
     * @see org.hibernate.usertype.EnhancedUserType#toXMLString(Object object)
     */
    @Override
    public String toXMLString( Object object ) {
        return String.valueOf( ( ( ubic.gemma.model.association.GOEvidenceCode ) object ).getValue() );
    }

    public static PhenotypeMappingType fromString( String value ) {
        final PhenotypeMappingType typeValue = values.get( value );
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
}
