package edu.columbia.gemma.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Very simple class to produce String versions of any Gemma domain objects. The entire hierarchy of associations for
 * each object is printed in a tree-like format. This is primarily used for testing.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PrettyPrinter {

    private PrettyPrinter() {
    }

    protected static final Log log = LogFactory.getLog( PrettyPrinter.class );

    /**
     * Print out a collection of Gemma data objects in a relatively pleasing format.
     * 
     * @param gemmaObjs Collection of objects.
     * @return String representing the objects.
     */
    public static String print( Collection<Object> gemmaObjs ) {
        StringBuffer buf = new StringBuffer();
        try {
            for ( Iterator<Object> iter = gemmaObjs.iterator(); iter.hasNext(); ) {
                Object gemmaObj = iter.next();

                if ( gemmaObj == null ) log.error( "Null object in collection" );
                print( buf, gemmaObj );

            }
        } catch ( IntrospectionException e ) {
            log.error( e, e );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( e, e );
        }
        return buf.toString();
    }

    /**
     * Pretty-print a single Gemma domain object.
     * 
     * @param gemmaObj
     * @return String representing the object.
     */
    public static String print( Object gemmaObj ) {
        StringBuffer buf = new StringBuffer();
        try {
            print( buf, gemmaObj );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( IntrospectionException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( e, e );
        }
        return buf.toString();
    }

    /**
     * @param buf
     * @param gemmaObj
     * @throws IntrospectionException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private static void print( StringBuffer buf, Object gemmaObj ) throws IntrospectionException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        print( buf, gemmaObj, 0 );
    }

    /**
     * The only class that does any real work. Recursively print an object and all its associated objects.
     * 
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws IntrospectionException
     * @param buf
     * @param gemmaObj
     */
    private static void print( StringBuffer buf, Object gemmaObj, int level ) throws IntrospectionException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        if ( gemmaObj == null ) return;
        Class gemmaClass = gemmaObj.getClass();
        if ( !gemmaClass.getName().startsWith( "edu.columbia.gemma" ) ) return;

        BeanInfo bif = Introspector.getBeanInfo( gemmaClass );
        PropertyDescriptor[] props = bif.getPropertyDescriptors();

        StringBuffer indent = new StringBuffer();
        for ( int i = 0; i < level; i++ )
            indent.append( "   " );

        boolean first = true;
        level++;

        for ( int i = 0; i < props.length; i++ ) {
            PropertyDescriptor prop = props[i];

            Object o = prop.getReadMethod().invoke( gemmaObj, new Object[] {} );

            if ( prop.getDisplayName().equals( "class" ) ) continue; // everybody has it.
            if ( prop.getDisplayName().equals( "mutable" ) ) continue; // shows up in the enums, just clutter.

            // generate a 'heading' for this object.
            if ( first ) buf.append( indent +   gemmaObj.getClass().getSimpleName() + " Properties:\n" );

            first = false;
            buf.append( indent + "   " +  gemmaObj.getClass().getSimpleName() + "." + prop.getName()
                    + ": " + ( o == null ? "---" : o ) + "\n" );
            print( buf, o, level );
        }
    }
}
