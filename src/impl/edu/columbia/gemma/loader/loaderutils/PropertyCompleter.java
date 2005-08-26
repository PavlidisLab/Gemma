package edu.columbia.gemma.loader.loaderutils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PropertyCompleter {

    /**
     * Given a detached Object and an already persisted one of the same type, fill in any missing attributes of the
     * persisted object with ones from the detached object. If update is true, then any slots that are already filled in
     * for the persisted object will be clobbered with the non-null values from the detached object.
     * 
     * @param persistentObject
     * @param detachedObject
     * @param update
     */
    public static void complete( Object persistentObject, Object detachedObject, boolean update ) {
        if ( persistentObject == null || detachedObject == null )
            throw new IllegalArgumentException( "Args must be non-null" );

        if ( persistentObject.getClass() != detachedObject.getClass() )
            throw new IllegalArgumentException( "Args must be of the same type" );

        PropertyDescriptor[] props = PropertyUtils.getPropertyDescriptors( persistentObject );
        for ( int i = 0; i < props.length; i++ ) {
            PropertyDescriptor descriptor = props[i];
            Method setter = descriptor.getWriteMethod();
            if ( setter == null ) continue;
            Method getter = descriptor.getReadMethod();
           
            try {
                Object detachedValue = getter.invoke( detachedObject, new Object[] {} );
                if ( detachedValue == null ) continue;

                Object persistedValue = getter.invoke( persistentObject, new Object[] {} );

                if ( persistedValue == null || update ) {
                    setter.invoke( persistentObject, new Object[] { detachedValue } );
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
