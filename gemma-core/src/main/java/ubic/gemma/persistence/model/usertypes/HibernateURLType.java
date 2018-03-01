/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence.model.usertypes;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Converts strings in the database into java.net.URL objects.
 *
 * @author pavlidis
 */
public class HibernateURLType implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[] { Types.CLOB };
    }

    @Override
    public Class returnedClass() {
        return java.net.URL.class;
    }

    @SuppressWarnings("SimplifiableIfStatement") // Better readability
    @Override
    public boolean equals( Object x, Object y ) throws HibernateException {
        boolean equal;
        if ( x == null || y == null ) {
            equal = false;
        } else if ( !( x instanceof java.net.URL ) || !( y instanceof java.net.URL ) ) {
            equal = false;
        } else {
            equal = x.equals( y );
        }
        return equal;
    }

    @Override
    public int hashCode( Object x ) throws HibernateException {
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet( ResultSet rs, String[] names, Object owner ) throws HibernateException, SQLException {
        String s = rs.getString( names[0] );
        if ( s == null || s.length() == 0 ) {
            return null;
        }
        try {
            return new java.net.URL( s );
        } catch ( MalformedURLException e ) {
            throw new HibernateException( "Malformed url", e );
        }
    }

    @Override
    public void nullSafeSet( PreparedStatement preparedStatement, Object data, int index )
            throws HibernateException, SQLException {
        if ( data == null ) {
            preparedStatement.setString( index, null );
        } else {
            java.net.URL in = ( java.net.URL ) data;
            String s = in.toString();
            byte[] buf = s.getBytes();
            int len = buf.length;
            ByteArrayInputStream bais = new ByteArrayInputStream( buf );
            preparedStatement.setAsciiStream( index, bais, len );
        }
    }

    @Override
    public Object deepCopy( Object value ) throws HibernateException {
        String ret;
        if ( value == null ) {
            return null;
        }
        java.net.URL in = ( java.net.URL ) value;
        String s = in.toString();
        int len = s.length();
        char[] buf = new char[len];

        for ( int i = 0; i < len; i++ ) {
            buf[i] = s.charAt( i );
        }
        ret = new String( buf );
        java.net.URL result;
        try {
            result = new java.net.URL( ret );
        } catch ( MalformedURLException e ) {
            throw new HibernateException( "Malformed url", e );
        }
        return result;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble( Object value ) throws HibernateException {
        return ( java.io.Serializable ) value;
    }

    @Override
    public Object assemble( Serializable cached, Object owner ) throws HibernateException {
        return this.deepCopy( cached );
    }

    @Override
    public Object replace( Object original, Object target, Object owner ) throws HibernateException {
        return this.deepCopy( original );
    }

}
