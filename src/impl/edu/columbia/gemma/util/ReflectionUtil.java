package edu.columbia.gemma.util;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ReflectionUtil {
    /**
     * @param obj
     * @return Unqualified type name
     */
    public static String objectToTypeName( Object obj ) {
        return obj.getClass().getName().substring( obj.getClass().getName().lastIndexOf( '.' ) + 1 );
    }

    /**
     * @param cls
     * @return Unqualified type name.
     */
    public static String classToTypeName( Class cls ) {
        return cls.getName().substring( cls.getName().lastIndexOf( '.' ) + 1 );
    }

    /**
     * @param obj
     * @return
     */
    public static Class getImplForBase( Object obj ) {
        return getImplForBase( obj.getClass() );
    }

    /**
     * @param cls
     * @return
     */
    public static Class getImplForBase( Class cls ) {
        if ( cls.getName().endsWith( "Impl" ) ) {
            return cls.getSuperclass();
        }
        return cls;
    }

}
