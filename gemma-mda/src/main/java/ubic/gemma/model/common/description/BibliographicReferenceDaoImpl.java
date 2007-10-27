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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.QueryUtils;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.description.BibliographicReference
 */
public class BibliographicReferenceDaoImpl extends ubic.gemma.model.common.description.BibliographicReferenceDaoBase {

    private static Log log = LogFactory.getLog( BibliographicReferenceDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceDaoBase#find(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public BibliographicReference find( BibliographicReference bibliographicReference ) {
        try {
            BusinessKey.checkKey( bibliographicReference );
            Criteria queryObject = super.getSession( false ).createCriteria( BibliographicReference.class );

            /*
             * This syntax allows you to look at an association.
             */
            if ( bibliographicReference.getPubAccession() != null ) {
                queryObject.createCriteria( "pubAccession" ).add(
                        Restrictions.eq( "accession", bibliographicReference.getPubAccession().getAccession() ) );
            } else {
                throw new NullPointerException( "PubAccession cannot be null" );
            }

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + BibliographicReference.class.getName()
                                    + "' with accession " + bibliographicReference.getPubAccession().getAccession()
                                    + " was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( BibliographicReference ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceDaoBase#findOrCreate(ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    public BibliographicReference findOrCreate( BibliographicReference bibliographicReference ) {

        BibliographicReference existingBibliographicReference = find( bibliographicReference );
        if ( existingBibliographicReference != null ) {
            if ( log.isDebugEnabled() )
                log.debug( "Found existing bibliographicReference: " + existingBibliographicReference );
            return existingBibliographicReference;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new bibliographicReference: " + bibliographicReference );
        return ( BibliographicReference ) create( bibliographicReference );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<BibliographicReference> handleGetAllExperimentLinkedReferences() {
        final String query = "select distinct e.primaryPublication from ExpressionExperimentImpl e ";
        return QueryUtils.queryForCollection( this.getSession( true ), query );
    }

    // FIXME Note that almost the same method is also available from the EEservice
    @Override
    protected Collection handleGetRelatedExperiments( BibliographicReference bibliographicReference ) throws Exception {
        final String queryString = "select distinct ee FROM ExpressionExperimentImpl as ee left join ee.otherRelevantPublications as eeO"
                + " WHERE ee.primaryPublication = :bib OR (eeO = :bib) ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "bib", bibliographicReference );

            Collection results = queryObject.list();
            return results;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

}