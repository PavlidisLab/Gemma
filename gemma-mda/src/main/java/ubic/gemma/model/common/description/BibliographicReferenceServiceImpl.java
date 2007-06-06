/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.common.description;

import java.util.Collection;

/**
 * Implementation of BibliographicReferenceService.
 * 
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.common.description.BibliographicReferenceService
 */
public class BibliographicReferenceServiceImpl extends
        ubic.gemma.model.common.description.BibliographicReferenceServiceBase {

    /**
     * Check to see if the reference already exists
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#alreadyExists(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected BibliographicReference handleFind(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception {

        return getBibliographicReferenceDao().find( bibliographicReference );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#findByExternalId(java.lang.String)
     */
    @Override
    protected ubic.gemma.model.common.description.BibliographicReference handleFindByExternalId( java.lang.String id )
            throws java.lang.Exception {

        return this.getBibliographicReferenceDao().findByExternalId( id, "PubMed" );

    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#findByExternalId(java.lang.String,
     *      java.lang.String)
     */
    @Override
    protected ubic.gemma.model.common.description.BibliographicReference handleFindByExternalId( java.lang.String id,
            java.lang.String databaseName ) throws java.lang.Exception {

        return this.getBibliographicReferenceDao().findByExternalId( id, databaseName );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceServiceBase#handleGetBibliographicReferenceByTitle(java.lang.String)
     */
    @Override
    protected BibliographicReference handleFindByTitle( String title ) throws Exception {

        return getBibliographicReferenceDao().findByTitle( title );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceServiceBase#handleLoadBibliographicReference(java.lang.Long)
     */
    protected BibliographicReference handleLoadBibliographicReference( Long id ) throws Exception {
        return ( BibliographicReference ) getBibliographicReferenceDao().load( id );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#saveBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected BibliographicReference handleCreate(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception {
        return ( BibliographicReference ) getBibliographicReferenceDao().create( bibliographicReference );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#saveBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.common.description.BibliographicReference BibliographicReference )
            throws java.lang.Exception {
        getBibliographicReferenceDao().update( BibliographicReference );
    }

    @Override
    protected void handleRemove( BibliographicReference bibliographicReference ) throws Exception {
        this.getBibliographicReferenceDao().remove( bibliographicReference );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceServiceBase#handleAddDocument(byte[],
     *      ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected void handleAddPDF( LocalFile pdfFile, BibliographicReference bibliographicReference ) throws Exception {
        bibliographicReference.setFullTextPDF( pdfFile );
        this.getBibliographicReferenceDao().update( bibliographicReference );

    }

    @Override
    protected BibliographicReference handleFindOrCreate( BibliographicReference bibliographicReference )
            throws Exception {
        return this.getBibliographicReferenceDao().findOrCreate( bibliographicReference );
    }

    @Override
    protected BibliographicReference handleLoad( Long id ) throws Exception {
        return ( BibliographicReference ) this.getBibliographicReferenceDao().load( id );
    }

    @Override
    protected Collection handleGetAllExperimentLinkedReferences() throws Exception {
        return this.getBibliographicReferenceDao().getAllExperimentLinkedReferences();
    }

    @Override
    protected Collection handleGetRelatedExperiments( BibliographicReference bibliographicReference ) throws Exception {
        return this.getBibliographicReferenceDao().getRelatedExperiments( bibliographicReference );
    }

}