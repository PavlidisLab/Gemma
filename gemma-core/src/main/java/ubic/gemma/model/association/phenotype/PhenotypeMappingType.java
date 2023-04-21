/*
 * The gemma project
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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * represents an enum of evidence mapping type, copied logic of GOEvidenceCodeEnum
 */
@Deprecated
public class PhenotypeMappingType
        implements java.io.Serializable, Comparable<PhenotypeMappingType>, org.hibernate.usertype.EnhancedUserType {

    public static final PhenotypeMappingType XREF = new PhenotypeMappingType( "Cross Reference" );
    public static final PhenotypeMappingType CURATED = new PhenotypeMappingType( "Curated" );
    public static final PhenotypeMappingType INFERRED_XREF = new PhenotypeMappingType( "Inferred Cross Reference" );
    public static final PhenotypeMappingType INFERRED_CURATED = new PhenotypeMappingType( "Inferred Curated" );
    public static final PhenotypeMappingType DIRECT = new PhenotypeMappingType( "Direct" ); // when we are given a useable term right without mapping needed
    private static final long serialVersionUID = -3336933794060950406L;
    private static final int[] SQL_TYPES = { Types.VARCHAR };
    private static final java.util.Map<String, PhenotypeMappingType> values = new java.util.LinkedHashMap<>();

    static {
        PhenotypeMappingType.values.put( PhenotypeMappingType.XREF.value, PhenotypeMappingType.XREF );
        PhenotypeMappingType.values.put( PhenotypeMappingType.CURATED.value, PhenotypeMappingType.CURATED );
        PhenotypeMappingType.values.put( PhenotypeMappingType.INFERRED_XREF.value, PhenotypeMappingType.INFERRED_XREF );
        PhenotypeMappingType.values
                .put( PhenotypeMappingType.INFERRED_CURATED.value, PhenotypeMappingType.INFERRED_CURATED );
        PhenotypeMappingType.values
                .put( PhenotypeMappingType.DIRECT.value, PhenotypeMappingType.DIRECT );
    }

    private String value;

    private PhenotypeMappingType( String value ) {
        this.value = value;
    }

    @SuppressWarnings({ "WeakerAccess, unused" }) // Required by Spring
    public PhenotypeMappingType() {
        super();
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static PhenotypeMappingType fromString( String value ) {
        final PhenotypeMappingType typeValue = PhenotypeMappingType.values.get( value );
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

    @Override
    public int hashCode() {
        return this.getValue().hashCode();
    }

    @Override
    public boolean equals( Object object ) {
        return ( this == object ) || ( object instanceof PhenotypeMappingType && ( ( PhenotypeMappingType ) object )
                .getValue().equals( this.getValue() ) );
    }

    @Override
    public String toString() {
        return String.valueOf( value );
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public int compareTo( PhenotypeMappingType that ) {
        return ( this == that ) ? 0 : this.getValue().compareTo( ( that ).getValue() );
    }

    /**
     * @see org.hibernate.usertype.EnhancedUserType#objectToSQLString(Object object)
     */
    @Override
    public String objectToSQLString( Object object ) {
        return String.valueOf( ( ( ubic.gemma.model.association.phenotype.PhenotypeMappingType ) object ).getValue() );
    }

    /**
     * @see org.hibernate.usertype.EnhancedUserType#toXMLString(Object object)
     */
    @Override
    public String toXMLString( Object object ) {
        return String.valueOf( ( ( ubic.gemma.model.association.GOEvidenceCode ) object ).name() );
    }

    /**
     * @see org.hibernate.usertype.EnhancedUserType#fromXMLString(String string)
     */
    @Override
    public Object fromXMLString( String string ) {
        return PhenotypeMappingType.fromString( String.valueOf( string ) );
    }

    /**
     * @see org.hibernate.usertype.UserType#sqlTypes()
     */
    @Override
    public int[] sqlTypes() {
        return PhenotypeMappingType.SQL_TYPES;
    }

    /**
     * @see org.hibernate.usertype.UserType#returnedClass()
     */
    @Override
    public Class<?> returnedClass() {
        return PhenotypeMappingType.class;
    }

    /**
     * @see org.hibernate.usertype.UserType#equals(Object, Object)
     */
    @Override
    public boolean equals( Object x, Object y ) throws HibernateException {
        return ( x == y ) || ( y != null && y.equals( x ) );
    }

    /**
     * @see org.hibernate.usertype.UserType#hashCode(Object value)
     */
    @Override
    public int hashCode( Object v ) {
        return value.hashCode();
    }

    /**
     * @see org.hibernate.usertype.UserType#nullSafeGet(java.sql.ResultSet, String[], SessionImplementor, Object)
     */
    @Override
    public Object nullSafeGet( ResultSet resultSet, String[] vs, SessionImplementor sessionImplementor, Object owner )
            throws HibernateException, SQLException {
        final String v = ( String ) resultSet.getObject( vs[0] );
        return resultSet.wasNull() ? null : PhenotypeMappingType.fromString( v );
    }

    /**
     * @see org.hibernate.usertype.UserType#nullSafeSet(java.sql.PreparedStatement, Object, int, SessionImplementor)
     */
    @Override
    public void nullSafeSet( PreparedStatement statement, Object v, int index, SessionImplementor sessionImplementor )
            throws HibernateException, SQLException {
        if ( v == null ) {
            statement.setNull( index, Types.VARCHAR );
        } else {
            statement.setObject( index, String.valueOf( String.valueOf( v ) ) );
        }
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
     * @see org.hibernate.usertype.UserType#isMutable()
     */
    @Override
    public boolean isMutable() {
        // Enums are immutable
        return false;
    }

    /**
     * @see org.hibernate.usertype.UserType#disassemble(Object value)
     */
    @Override
    public java.io.Serializable disassemble( Object v ) {
        return ( Serializable ) v;
    }

    /**
     * @see org.hibernate.usertype.UserType#assemble(java.io.Serializable cached, Object owner)
     */
    @Override
    public Object assemble( java.io.Serializable cached, Object owner ) {
        return cached;
    }

    /**
     * @see org.hibernate.usertype.UserType#replace(Object original, Object target, Object owner)
     */
    @Override
    public Object replace( Object original, Object target, Object owner ) {
        return original;
    }
}
