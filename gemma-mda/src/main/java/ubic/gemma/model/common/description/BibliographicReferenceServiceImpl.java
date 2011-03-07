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
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Implementation of BibliographicReferenceService.
 * 
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.common.description.BibliographicReferenceService
 */
@Service
public class BibliographicReferenceServiceImpl extends
        ubic.gemma.model.common.description.BibliographicReferenceServiceBase {

    private static final String PUB_MED_DATABASE_NAME = "PubMed";

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceServiceBase#handleAddDocument(byte[],
     * ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected void handleAddPDF( LocalFile pdfFile, BibliographicReference bibliographicReference ) throws Exception {
        bibliographicReference.setFullTextPdf( pdfFile );
        this.getBibliographicReferenceDao().update( bibliographicReference );

    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#saveBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected BibliographicReference handleCreate(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception {
        return getBibliographicReferenceDao().create( bibliographicReference );
    }

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

        return this.getBibliographicReferenceDao().findByExternalId( id, PUB_MED_DATABASE_NAME );

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
     * @see
     * ubic.gemma.model.common.description.BibliographicReferenceServiceBase#handleGetBibliographicReferenceByTitle(
     * java.lang.String)
     */
    @Override
    protected BibliographicReference handleFindByTitle( String title ) throws Exception {

        return getBibliographicReferenceDao().findByTitle( title );
    }

    @Override
    protected BibliographicReference handleFindOrCreate( BibliographicReference bibliographicReference )
            throws Exception {
        return this.getBibliographicReferenceDao().findOrCreate( bibliographicReference );
    }

    @Override
    protected Map<ExpressionExperiment, BibliographicReference> handleGetAllExperimentLinkedReferences()
            throws Exception {
        return this.getBibliographicReferenceDao().getAllExperimentLinkedReferences();
    }

    @Override
    protected Collection handleGetRelatedExperiments( BibliographicReference bibliographicReference ) throws Exception {
        return this.getBibliographicReferenceDao().getRelatedExperiments( bibliographicReference );
    }

    @Override
    protected BibliographicReference handleLoad( Long id ) throws Exception {
        return this.getBibliographicReferenceDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.description.BibliographicReferenceServiceBase#handleLoadBibliographicReference(java.lang
     * .Long)
     */
    protected BibliographicReference handleLoadBibliographicReference( Long id ) throws Exception {
        return getBibliographicReferenceDao().load( id );
    }

    @Override
    protected Collection handleLoadMultiple( Collection ids ) throws Exception {
        return this.getBibliographicReferenceDao().load( ids );
    }

    @Override
    protected void handleRemove( BibliographicReference bibliographicReference ) throws Exception {
        this.getBibliographicReferenceDao().remove( bibliographicReference );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#saveBibliographicReference(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.common.description.BibliographicReference BibliographicReference )
            throws java.lang.Exception {
        getBibliographicReferenceDao().update( BibliographicReference );
    }

    /**
     * 
     */
    public BibliographicReference findByExternalId( DatabaseEntry accession ) {
        return this.getBibliographicReferenceDao().findByExternalId( accession );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.common.description.BibliographicReferenceService#thaw(ubic.gemma.model.common.description.
     * BibliographicReference)
     */
    @Override
    public BibliographicReference thaw( BibliographicReference bibliographicReference ) {
        return this.getBibliographicReferenceDao().thaw( bibliographicReference );
    }

    /*
     * (non-Javadoc)
     * 
     * 
     * /* (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#thaw(java.util.Collection)
     */
    @Override
    public Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences ) {
        return this.getBibliographicReferenceDao().thaw( bibliographicReferences );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#browse(java.lang.Integer,
     * java.lang.Integer)
     */
    @Override
    public List<BibliographicReference> browse( Integer start, Integer limit ) {
        return this.getBibliographicReferenceDao().browse( start, limit );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#browse(java.lang.Integer,
     * java.lang.Integer, java.lang.String, boolean)
     */
    @Override
    public List<BibliographicReference> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        return this.getBibliographicReferenceDao().browse( start, limit, orderField, descending );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceService#count()
     */
    @Override
    public Integer count() {
        return this.getBibliographicReferenceDao().count();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.description.BibliographicReferenceService#getRelatedExperiments(java.util.Collection)
     */
    @Override
    public Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records ) {
        return this.getBibliographicReferenceDao().getRelatedExperiments( records );

    }

}