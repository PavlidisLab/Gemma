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

import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;

/**
 * Implementation of BibliographicReferenceService.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @see edu.columbia.gemma.common.description.BibliographicReferenceService
 */
public class BibliographicReferenceServiceImpl extends
        edu.columbia.gemma.common.description.BibliographicReferenceServiceBase {

    /**
     * Check to see if the reference already exists
     * 
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#alreadyExists(edu.columbia.gemma.common.description.BibliographicReference)
     */
    protected boolean handleAlreadyExists(
            edu.columbia.gemma.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception {

        boolean exists = false;
        //Collection col = getBibliographicReferenceDao().findByTitle( bibliographicReference.getTitle() );
        //if ( col.size() > 0 ) exists = true;
        BibliographicReference br = getBibliographicReferenceDao().findByTitle( bibliographicReference.getTitle() );
        if ( br != null ) exists = true;

        return exists;
    }

    /**
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#findByExternalId(java.lang.String)
     */
    protected edu.columbia.gemma.common.description.BibliographicReference handleFindByExternalId( java.lang.String id )
            throws java.lang.Exception {
    	
    	int pubMedId = new Integer(id).intValue();
    	PubMedXMLFetcher fetch = new PubMedXMLFetcher();
    	BibliographicReference br = fetch.retrieveByHTTP( pubMedId );
        if ( !alreadyExists( br ) ){
        	saveBibliographicReference( br );
        }
        return br;    	
        
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
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#getAllBibliographicReferences()
     */
    protected java.util.Collection handleGetAllBibliographicReferences() throws java.lang.Exception {
        return getBibliographicReferenceDao().findAllBibliographicReferences();
    }

    /**
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#getAllBibliographicReferences(int
     *      maxResults)
     */
    //TODO can you create a finder method for this where the parameter maxResults
    //does not end up as a named parameter. I want to use Hibernate's maxResults(int max)
    //method.
    protected java.util.Collection handleGetAllBibliographicReferences(
            edu.columbia.gemma.common.description.BibliographicReference BibliographicReference )
            throws java.lang.Exception {
        return getBibliographicReferenceDao().findAllBibliographicReferences();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.BibliographicReferenceServiceBase#handleGetBibliographicReferenceByTitle(java.lang.String)
     */
    protected BibliographicReference handleGetBibliographicReferenceByTitle( String title ) throws Exception {

        return getBibliographicReferenceDao().findByTitle( title );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.BibliographicReferenceServiceBase#handleLoadBibliographicReference(java.lang.Long)
     */
    protected BibliographicReference handleLoadBibliographicReference( Long id ) throws Exception {
        return ( BibliographicReference ) getBibliographicReferenceDao().load( id );
    }

    /**
     * @see edu.columbia.gemma.common.description.BibliographicReferenceService#saveBibliographicReference(edu.columbia.gemma.common.description.BibliographicReference)
     */
    protected void handleSaveBibliographicReference(
            edu.columbia.gemma.common.description.BibliographicReference BibliographicReference )
            throws java.lang.Exception {
        getBibliographicReferenceDao().create( BibliographicReference );
    }

}