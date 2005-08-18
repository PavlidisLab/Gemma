package edu.columbia.gemma.security.afterInvocation;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A filter used to filter arrays.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @author Ben Alex
 * @author Paulo Neves
 * @version $Id$
 */
class ArrayFilterer implements Filterer {
    // ~ Static fields/initializers =============================================

    protected static final Log logger = LogFactory
            .getLog( AclAfterCollectionArrayDesignFiltering.class );

    // ~ Instance fields ========================================================

    private Set removeList;
    private Object[] list;

    // ~ Constructors ===========================================================

    ArrayFilterer( Object[] list ) {
        this.list = list;

        // Collect the removed objects to a HashSet so that
        // it is fast to lookup them when a filtered array
        // is constructed.
        removeList = new HashSet();
    }

    // ~ Methods ================================================================

    /**
     * @see net.sf.acegisecurity.afterinvocation.Filterer#getFilteredObject()
     */
    public Object getFilteredObject() {
        // Recreate an array of same type and filter the removed objects.
        int originalSize = list.length;
        int sizeOfResultingList = originalSize - removeList.size();
        Object[] filtered = ( Object[] ) Array.newInstance( list.getClass().getComponentType(), sizeOfResultingList );

        for ( int i = 0, j = 0; i < list.length; i++ ) {
            Object object = list[i];

            if ( !removeList.contains( object ) ) {
                filtered[j] = object;
                j++;
            }
        }

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Original array contained " + originalSize + " elements; now contains " + sizeOfResultingList
                    + " elements" );
        }

        return filtered;
    }

    /**
     * @see net.sf.acegisecurity.afterinvocation.Filterer#iterator()
     */
    public Iterator iterator() {
        return new ArrayIterator( list );
    }

    /**
     * @see net.sf.acegisecurity.afterinvocation.Filterer#remove(java.lang.Object)
     */
    public void remove( Object object ) {
        removeList.add( object );
    }
}
