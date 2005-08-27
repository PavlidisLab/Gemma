package edu.columbia.gemma.loader.loaderutils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * Class to help complete beans based on other beans
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
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
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch ( IllegalAccessException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch ( InvocationTargetException e ) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }
}
