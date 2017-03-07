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
package ubic.gemma.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.PropertyUtils;

import ubic.gemma.model.common.AbstractAuditable;

/**
 * Class to help complete beans based on other beans
 * 
 * @author pavlidis
 * @version $Id$
 */
public class BeanPropertyCompleter {

    /**
     * Given a source Object and a source one of the same type, fill in any missing attributes of the target object with
     * ones from the source object. Any non-null values in the target are NOT overwritten by corresponding values in the
     * source.
     * 
     * @param targetObject
     * @param sourceObject
     */
    public static void complete( Object targetObject, Object sourceObject ) {
        complete( targetObject, sourceObject, false );
    }

    /**
     * Given a source Object and a source one of the same type, fill in any missing attributes of the target object with
     * ones from the source object. If update is true, then any non-null values in the target object will be clobbered
     * with the non-null values from the source object.
     * <p>
     * Associated objects which are in collections or maps are not updated individually. All associations are either
     * assigned to or left alone. Thus collections will be replaced if "update" is selected, but if not, the collection
     * will not be changed, even if the sourceObject contains new members in the collection.
     * <p>
     * Note that Id, and AuditTrails on Auditables are NEVER clobbered by this method.
     * 
     * @param targetObject
     * @param sourceObject
     * @param update
     */
    public static void complete( Object targetObject, Object sourceObject, boolean update ) {

        if ( targetObject == null || sourceObject == null )
            throw new IllegalArgumentException( "Args must be non-null" );
        if ( targetObject.getClass() != sourceObject.getClass() )
            throw new IllegalArgumentException( "Args must be of the same type" );
        PropertyDescriptor[] props = PropertyUtils.getPropertyDescriptors( targetObject );
        for ( int i = 0; i < props.length; i++ ) {
            PropertyDescriptor descriptor = props[i];

            if ( targetObject instanceof AbstractAuditable && descriptor.getName().equals( "auditTrail" ) ) {
                continue;
            }

            if ( descriptor.getName().equals( "id" ) ) {
                continue;
            }

            Method setter = descriptor.getWriteMethod();
            if ( setter == null ) continue;
            Method getter = descriptor.getReadMethod();
            try {
                Object sourceValue = getter.invoke( sourceObject, new Object[] {} );
                if ( sourceValue == null ) continue;
                Object persistedValue = getter.invoke( targetObject, new Object[] {} );
                if ( persistedValue == null || update ) {
                    setter.invoke( targetObject, new Object[] { sourceValue } );
                }
            } catch ( IllegalArgumentException e ) {
                throw new RuntimeException( e );
            } catch ( IllegalAccessException e ) {
                throw new RuntimeException( e );
            } catch ( InvocationTargetException e ) {
                throw new RuntimeException( e );
            }
        }

    }
}
