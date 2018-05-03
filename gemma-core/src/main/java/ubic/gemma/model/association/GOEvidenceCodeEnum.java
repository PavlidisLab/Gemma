/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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

package ubic.gemma.model.association;

import org.hibernate.HibernateException;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * This enumeration was originally based on GO, but is used for all
 * entities that have evidenciary aspects; Thus it has been expanded to
 * include:
 * <ul>
 * <li>Terms from RGD&#160;(rat genome database)</li>
 * <li>IED = Inferred from experimental data</li>
 * <li>IAGP = Inferred from association of genotype and phenotype</li>
 * <li>IPM = Inferred from phenotype manipulation</li>
 * <li>QTM = Quantitative Trait Measurement</li>
 * <li>And our own custom code &quot;IIA&quot; which means Inferred from Imported
 * Annotation to distinguish IEAs that we ourselves have computed.</li>
 * </ul>
 */
public final class GOEvidenceCodeEnum extends GOEvidenceCode implements org.hibernate.usertype.EnhancedUserType {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 1672679992320181566L;
    private static final int[] SQL_TYPES = { Types.VARCHAR };

    /**
     * Default constructor. Hibernate needs the default constructor to retrieve an instance of the enum from a JDBC
     * resultSet. The instance will be converted to the correct enum instance in
     * {@link #nullSafeGet(java.sql.ResultSet, String[], Object)}.
     */
    public GOEvidenceCodeEnum() {
        super();
    }

    @Override
    public String objectToSQLString( Object object ) {
        return String.valueOf( ( ( GOEvidenceCode ) object ).getValue() );
    }

    @Override
    public String toXMLString( Object object ) {
        return String.valueOf( ( ( GOEvidenceCode ) object ).getValue() );
    }

    @Override
    public Object fromXMLString( String string ) {
        return GOEvidenceCode.fromString( String.valueOf( string ) );
    }

    @Override
    public int[] sqlTypes() {
        return GOEvidenceCodeEnum.SQL_TYPES;
    }

    @Override
    public Class<?> returnedClass() {
        return GOEvidenceCode.class;
    }

    @Override
    public boolean equals( Object x, Object y ) throws HibernateException {
        return ( x == y ) || ( y != null && y.equals( x ) );
    }

    @Override
    public int hashCode( Object value ) {
        return value.hashCode();
    }

    @Override
    public Object nullSafeGet( ResultSet resultSet, String[] values, Object owner )
            throws HibernateException, SQLException {
        final String value = ( String ) resultSet.getObject( values[0] );
        return resultSet.wasNull() ? null : GOEvidenceCode.fromString( value );
    }

    @Override
    public void nullSafeSet( PreparedStatement statement, Object value, int index )
            throws HibernateException, SQLException {
        if ( value == null ) {
            statement.setNull( index, Types.VARCHAR );
        } else {
            statement.setObject( index, String.valueOf( String.valueOf( value ) ) );
        }
    }

    @Override
    public Object deepCopy( Object value ) throws HibernateException {
        // Enums are immutable - nothing to be done to deeply clone it
        return value;
    }

    @Override
    public boolean isMutable() {
        // Enums are immutable
        return false;
    }

    @Override
    public Serializable disassemble( Object value ) {
        return ( java.io.Serializable ) value;
    }

    @Override
    public Object assemble( java.io.Serializable cached, Object owner ) {
        return cached;
    }

    @Override
    public Object replace( Object original, Object target, Object owner ) {
        return original;
    }
}