package edu.columbia.gemma.util;

/**
 * Various methods useful for manipulating Gemma objects using Reflection.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ReflectionUtil {

    private static final String DAO_SUFFIX = "Dao";

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
    public static Class getBaseForImpl( Object obj ) {
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

    /**
     * @param gemmaObj A Gemma domain object.
     * @return Name of Dao bean; for example, given foo.Bar, it returns "barDao".
     */
    public static String constructDaoName( Object gemmaObj ) {
        String baseDaoName = getBaseForImpl( gemmaObj ) + DAO_SUFFIX;

        if ( baseDaoName.length() == DAO_SUFFIX.length() ) return null;

        baseDaoName = baseDaoName.substring( baseDaoName.lastIndexOf( '.' ) + 1 );
        return baseDaoName.substring( 0, 1 ).toLowerCase() + baseDaoName.substring( 1 );
    }

}
