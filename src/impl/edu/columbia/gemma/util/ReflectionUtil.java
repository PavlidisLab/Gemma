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
     * @param cls
     * @return Unqualified type name; e.g., for edu.foo.Bar, yields Bar.
     */
    public static String classToTypeName( Class cls ) {
        return cls.getName().substring( cls.getName().lastIndexOf( '.' ) + 1 );
    }

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
    public static Class getBaseForImpl( Object obj ) {
        return getImplForBase( obj.getClass() );
    }

    /**
     * @param cls
     * @return impl class for a base class; for example, for Foo.class it returns FooImpl.class.
     */
    public static Class getImplForBase( Class cls ) {
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
        return obj.getClass().getName().substring( obj.getClass().getName().lastIndexOf( '.' ) + 1 );
    }

}
