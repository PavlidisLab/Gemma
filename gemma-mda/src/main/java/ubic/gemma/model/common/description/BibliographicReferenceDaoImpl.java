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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.EntityUtils;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.description.BibliographicReference
 */
@Repository
public class BibliographicReferenceDaoImpl extends ubic.gemma.model.common.description.BibliographicReferenceDaoBase {

    private static Log log = LogFactory.getLog( BibliographicReferenceDaoImpl.class.getName() );

    @Autowired
    public BibliographicReferenceDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.common.description.BibliographicReferenceDaoBase#find(ubic.gemma.model.common.description.
     * BibliographicReference)
     */
    @Override
    public BibliographicReference find( BibliographicReference bibliographicReference ) {
        try {
            BusinessKey.checkKey( bibliographicReference );
            Criteria queryObject = super.getSession( true ).createCriteria( BibliographicReference.class );

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
     * @see
     * ubic.gemma.model.common.description.BibliographicReferenceDaoBase#findOrCreate(ubic.gemma.model.common.description
     * .BibliographicReference)
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
        return create( bibliographicReference );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<ExpressionExperiment, BibliographicReference> handleGetAllExperimentLinkedReferences() {
        final String query = "select distinct e, b from ExpressionExperimentImpl e join e.primaryPublication b left join fetch b.pubAccession ";
        Map<ExpressionExperiment, BibliographicReference> result = new HashMap<ExpressionExperiment, BibliographicReference>();
        List<Object[]> os = this.getHibernateTemplate().find( query );
        for ( Object[] o : os ) {
            result.put( ( ExpressionExperiment ) o[0], ( BibliographicReference ) o[1] );
        }
        return result;
    }

    // Note that almost the same method is also available from the EEservice
    @Override
    protected Collection handleGetRelatedExperiments( BibliographicReference bibliographicReference ) throws Exception {
        final String queryString = "select distinct ee FROM ExpressionExperimentImpl as ee left join ee.otherRelevantPublications as eeO"
                + " where ee.primaryPublication = :bib OR (eeO = :bib) ";

        return this.getHibernateTemplate().findByNamedParam( queryString, "bib", bibliographicReference );
    }

    @Override
    protected Collection handleLoadMultiple( Collection ids ) throws Exception {
        return this.getHibernateTemplate().find( "from BibliographicReferenceImpl b where b.id in :bib", ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.common.description.BibliographicReferenceDao#thaw(ubic.gemma.model.common.description.
     * BibliographicReference)
     */
    @Override
    public BibliographicReference thaw( BibliographicReference bibliographicReference ) {
        if ( bibliographicReference == null || bibliographicReference.getId() == null ) return bibliographicReference;
        return ( BibliographicReference ) this.getHibernateTemplate().findByNamedParam(
                "select b from BibliographicReferenceImpl b left join fetch b.pubAccession left join fetch b.chemicals "
                        + "left join fetch b.meshTerms left join fetch b.keywords where b.id = :id ", "id",
                bibliographicReference.getId() ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#thaw(java.util.Collection)
     */
    @Override
    public Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences ) {
        if ( bibliographicReferences.isEmpty() ) return bibliographicReferences;
        return this.getHibernateTemplate().findByNamedParam(
                "select b from BibliographicReferenceImpl b left join fetch b.pubAccession left join fetch b.chemicals "
                        + "left join fetch b.meshTerms left join fetch b.keywords where b.id in (:ids) ", "ids",
                EntityUtils.getIds( bibliographicReferences ) );
    }
}