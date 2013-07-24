/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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
package ubic.gemma.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Various methods useful for manipulating Gemma objects using Reflection.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ReflectionUtil {

    private static final String DAO_SUFFIX = "Dao";

    /**
     * @param obj A data object that is expected to have an associated data access object.
     * @return Name of Dao bean; for example, given foo.Bar, it returns "barDao". This does not guarantee that the DAO
     *         exists.
     */
    public static String constructDaoName( Object obj ) {
        String baseDaoName = getBaseForImpl( obj ) + DAO_SUFFIX;

        if ( baseDaoName.length() == DAO_SUFFIX.length() ) return null;

        baseDaoName = baseDaoName.substring( baseDaoName.lastIndexOf( '.' ) + 1 );
        return baseDaoName.substring( 0, 1 ).toLowerCase() + baseDaoName.substring( 1 );
    }

    /**
     * @param obj
     * @return base object for Impl; for example, for a FooImpl instance it returns Foo.class.
     */
    public static Class<? extends Object> getBaseForImpl( Object obj ) {
        return getBaseForImpl( obj.getClass() );
    }

    /**
     * @param cls
     * @return base object for Impl; for example, for a FooImpl.class it returns Foo.class.
     */
    public static Class<? extends Object> getBaseForImpl( Class<? extends Object> cls ) {
        if ( cls.getName().endsWith( "Impl" ) ) {
            return cls.getSuperclass();
        }
        return cls;
    }

    /**
     * @param obj
     * @return Unqualified type name; for example, given an instance of an edu.bar.Foo, returns "Foo".
     */
    public static String objectToTypeName( Object obj ) {
        return obj.getClass().getSimpleName();
    }

    /**
     * @param object
     * @param descriptor
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Object getProperty( Object object, PropertyDescriptor descriptor ) throws IllegalAccessException,
            InvocationTargetException {
        Method getter = descriptor.getReadMethod();
        Object associatedObject = getter.invoke( object, new Object[] {} );
        return associatedObject;
    }

}
