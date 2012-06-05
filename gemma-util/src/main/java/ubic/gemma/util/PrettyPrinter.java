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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Very simple class to produce String versions of beans and other objects.
 * <p>
 * For beans, the entire hierarchy of associations for each object is printed in a tree-like format. This is primarily
 * used for testing.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PrettyPrinter {

    /**
     * The maximum number of items to display from a collection.
     */
    private static final int MAX_TO_SHOW = 10;

    private PrettyPrinter() {
    }

    protected static final Log log = LogFactory.getLog( PrettyPrinter.class );

    /**
     * Format an array of integers for printing.
     * 
     * @param ints
     * @return
     */
    public static String print( int[] ints ) {
        if ( ints == null || ints.length == 0 ) return "(empty)";
        StringBuffer buf = new StringBuffer();
        for ( int i : ints ) {
            buf.append( i + " " );
        }
        buf.append( "\n" );
        return buf.toString();
    }

    /**
     * Print out a collection of objects in a relatively pleasing format.
     * 
     * @param beans Collection of beans.
     * @return String representing the objects.
     */
    public static String print( Object[] objects ) {
        return print( Arrays.asList( objects ) );
    }

    /**
     * Print out a collection of beans in a relatively pleasing format.
     * 
     * @param beans Collection of beans.
     * @return String representing the objects.
     */
    public static String print( Collection<Object> beans ) {
        StringBuffer buf = new StringBuffer();
        try {
            for ( Iterator<Object> iter = beans.iterator(); iter.hasNext(); ) {
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
     * Pretty-print a single bean. Beans that are not part of this project are ignored.
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
     * @param buf
     * @param gemmeCollection
     * @param level
     */
    private static void print( StringBuffer buf, Collection<?> gemmaCollection, int level )
            throws IllegalArgumentException, IntrospectionException, IllegalAccessException, InvocationTargetException {
        int i = 0;
        for ( Object gemmaObj : gemmaCollection ) {
            print( buf, gemmaObj, level );
            i++;
            if ( i >= MAX_TO_SHOW ) {
                buf.append( "..." + ( gemmaCollection.size() - MAX_TO_SHOW ) + " more "
                        + gemmaObj.getClass().getSimpleName() + "'s\n" );
                break;
            }
        }
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
     * @param level Used to track indents.
     */
    private static void print( StringBuffer buf, Object bean, int level ) throws IntrospectionException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        if ( bean == null ) return;
        Class<?> gemmaClass = bean.getClass();

        if ( bean instanceof Collection ) {
            print( buf, ( Collection<?> ) bean, ++level );
            return;
        }

        if ( !gemmaClass.getName().startsWith( "ubic.gemma" ) ) return;

        BeanInfo bif = Introspector.getBeanInfo( gemmaClass );
        PropertyDescriptor[] props = bif.getPropertyDescriptors();

        StringBuffer indent = new StringBuffer();
        for ( int i = 0; i < level; i++ )
            indent.append( "   " );

        boolean first = true;
        level++;

        for ( int i = 0; i < props.length; i++ ) {
            PropertyDescriptor prop = props[i];

            Object o = prop.getReadMethod().invoke( bean, new Object[] {} );

            if ( prop.getDisplayName().equals( "class" ) ) continue; // everybody has it.
            if ( prop.getDisplayName().equals( "mutable" ) ) continue; // shows up in the enums, just clutter.

            // generate a 'heading' for this object.
            if ( first ) buf.append( indent + bean.getClass().getSimpleName() + " Properties:\n" );

            first = false;
            buf.append( indent + "   " + bean.getClass().getSimpleName() + "." + prop.getName() + ": "
                    + ( o == null ? "---" : o ) + "\n" );
            print( buf, o, level );
        }
    }
}
