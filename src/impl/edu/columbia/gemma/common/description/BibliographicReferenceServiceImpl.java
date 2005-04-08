/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package edu.columbia.gemma.common.description;

import java.util.Collection;

/**
 * Allows user to view the results of a pubMed search, with the option of submitting the results.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author $Id$
 * @see edu.columbia.gemma.common.description.BibliographicReferenceService
 */
public class BibliographicReferenceServiceImpl extends
        edu.columbia.gemma.common.description.BibliographicReferenceServiceBase {

    /**
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#getAllBibliographicReferences()
     */
    protected java.util.Collection handleGetAllBibliographicReferences() throws java.lang.Exception {
        //@todo implement protected java.util.Collection handleGetAllBibliographicReferences()
        return null;
    }

    /**
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#saveBibliographicReference(edu.columbia.gemma.common.description.BibliographicReference)
     */
    protected void handleSaveBibliographicReference(
            edu.columbia.gemma.common.description.BibliographicReference BibliographicReference )
            throws java.lang.Exception {
        getBibliographicReferenceDao().create( BibliographicReference );
    }

    /**
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#findByExternalId(java.lang.String)
     */
    protected edu.columbia.gemma.common.description.BibliographicReference handleFindByExternalId( java.lang.String id )
            throws java.lang.Exception {
        //@todo implement protected edu.columbia.gemma.common.description.BibliographicReference
        // handleFindByExternalId(java.lang.String id)
        return null;
    }

    /**
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#findByExternalId(java.lang.String,
     *      java.lang.String)
     */
    protected edu.columbia.gemma.common.description.BibliographicReference handleFindByExternalId( java.lang.String id,
            java.lang.String databaseName ) throws java.lang.Exception {
        //@todo implement protected edu.columbia.gemma.common.description.BibliographicReference
        // handleFindByExternalId(java.lang.String id, java.lang.String databaseName)
        return null;
    }

    /**
     * Check to see if the reference already exists
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#alreadyExists(edu.columbia.gemma.common.description.BibliographicReference)
     */
    protected boolean handleAlreadyExists(
            edu.columbia.gemma.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception {
        boolean exists = false;
        Collection col = getBibliographicReferenceDao().findByTitlePublicationDate( bibliographicReference.getTitle() );
        
        if ( col.size() > 0 ) exists = true;

        return exists;
    }

}