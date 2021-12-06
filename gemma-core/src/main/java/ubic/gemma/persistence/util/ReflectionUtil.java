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
package ubic.gemma.persistence.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Various methods useful for manipulating Gemma objects using Reflection.
 *
 * @author pavlidis
 */
public class ReflectionUtil {

    /**
     * @param cls class
     * @return base object for Impl; for example, for a FooImpl.class it returns Foo.class.
     */
    public static <T> Class<? super T> getBaseForImpl( Class<T> cls ) {
        if ( cls.getName().endsWith( "Impl" ) ) {
            return cls.getSuperclass();
        }
        return cls;
    }

    public static Object getProperty( Object object, PropertyDescriptor descriptor )
            throws IllegalAccessException, InvocationTargetException {
        Method getter = descriptor.getReadMethod();
        return getter.invoke( object );
    }

}
