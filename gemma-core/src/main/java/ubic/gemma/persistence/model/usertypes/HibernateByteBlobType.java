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
package ubic.gemma.persistence.model.usertypes;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;

/**
 * A hibernate user type which converts a Blob into a byte[] and back again.
 */
public class HibernateByteBlobType implements UserType {
    /**
     * @see org.hibernate.usertype.UserType#sqlTypes()
     */
    @Override
    public int[] sqlTypes() {
        return new int[] { Types.BLOB };
    }

    /**
     * @see org.hibernate.usertype.UserType#returnedClass()
     */
    @Override
    public Class<?> returnedClass() {
        return byte[].class;
    }

    /**
     * @see org.hibernate.usertype.UserType#equals(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean equals( Object x, Object y ) {
        return ( x == y ) || ( x != null && y != null && java.util.Arrays.equals( ( byte[] ) x, ( byte[] ) y ) );
    }

    /**
     * @see org.hibernate.usertype.UserType#hashCode(Object)
     */
    @Override
    public int hashCode( Object x ) {
        return x.hashCode();
    }

    /**
     * @see org.hibernate.usertype.UserType#nullSafeGet(ResultSet, String[], SessionImplementor, Object)
     */
    @Override
    public Object nullSafeGet( ResultSet resultSet, String[] names, SessionImplementor sessionImplementor, Object owner )
            throws HibernateException, SQLException {
        final Object object;

        try ( final InputStream inputStream = resultSet.getBinaryStream( names[0] );
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream() ) {
            if ( inputStream == null ) {
                object = null;
            } else {

                final byte[] buffer = new byte[65536];
                int read;

                while ( ( read = inputStream.read( buffer ) ) > -1 ) {
                    outputStream.write( buffer, 0, read );
                }
                outputStream.close();

                object = outputStream.toByteArray();
            }

            return object;
        } catch ( IOException e ) {
            throw new HibernateException( "Unable to read blob " + names[0], e );
        }
    }

    /**
     * @see org.hibernate.usertype.UserType#nullSafeSet(PreparedStatement, Object, int, SessionImplementor)
     */
    @Override
    public void nullSafeSet( PreparedStatement statement, Object value, int index, SessionImplementor sessionImplementor ) throws SQLException {
        final byte[] bytes = ( byte[] ) value;
        if ( bytes == null ) {
            try {
                statement.setBinaryStream( index, null, 0 );
            } catch ( SQLException exception ) {
                Blob nullBlob = null;
                //noinspection ConstantConditions // Has to be of Blob type
                statement.setBlob( index, nullBlob );
            }
        } else {
            statement.setBinaryStream( index, new ByteArrayInputStream( bytes ), bytes.length );
        }
    }

    /**
     * @see org.hibernate.usertype.UserType#deepCopy(java.lang.Object)
     */
    @Override
    public Object deepCopy( Object value ) {
        if ( value == null )
            return null;

        byte[] bytes = ( byte[] ) value;
        byte[] result = new byte[bytes.length];
        System.arraycopy( bytes, 0, result, 0, bytes.length );

        return result;
    }

    /**
     * @see org.hibernate.usertype.UserType#isMutable()
     */
    @Override
    public boolean isMutable() {
        return true;
    }

    /**
     * @see org.hibernate.usertype.UserType#disassemble(Object)
     */
    @Override
    public java.io.Serializable disassemble( Object value ) {
        return ( java.io.Serializable ) value;
    }

    /**
     * @see org.hibernate.usertype.UserType#assemble(java.io.Serializable, Object)
     */
    @Override
    public Object assemble( java.io.Serializable cached, Object owner ) {
        return cached;
    }

    /**
     * @see org.hibernate.usertype.UserType#replace(Object, Object, Object)
     */
    @Override
    public Object replace( Object original, Object target, Object owner ) {
        return original;
    }
}