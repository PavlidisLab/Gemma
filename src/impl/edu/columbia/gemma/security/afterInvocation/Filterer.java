package edu.columbia.gemma.security.afterInvocation;

import java.util.Iterator;

/**
 * Filter strategy interface.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @author Ben Alex
 * @author Paulo Neves
 * @version $Id$
 */
public interface Filterer {
    // ~ Methods ================================================================

    /**
     * Gets the filtered collection or array.
     * 
     * @return the filtered collection or array
     */
    public Object getFilteredObject();

    /**
     * Returns an iterator over the filtered collection or array.
     * 
     * @return an Iterator
     */
    public Iterator iterator();

    /**
     * Removes the the given object from the resulting list.
     * 
     * @param object the object to be removed
     */
    public void remove( Object object );
}
