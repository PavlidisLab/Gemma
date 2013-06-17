/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.common.description;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.basecode.util.BatchIterator;
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
     * @see ubic.gemma.persistence.BrowsingDao#browse(java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<BibliographicReference> browse( Integer start, Integer limit ) {
        Query query = this.getSessionFactory().getCurrentSession().createQuery( "from BibliographicReferenceImpl" );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BrowsingDao#browse(java.lang.Integer, java.lang.Integer, java.lang.String, boolean)
     */
    @Override
    public List<BibliographicReference> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        Query query = this
                .getSessionFactory()
                .getCurrentSession()
                .createQuery(
                        "from BibliographicReferenceImpl order by " + orderField + " " + ( descending ? "desc" : "" ) );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.common.description.BibliographicReferenceDaoBase#find(ubic.gemma.model.common.description.
     * BibliographicReference)
     */
    @Override
    public BibliographicReference find( BibliographicReference bibliographicReference ) {

        BusinessKey.checkKey( bibliographicReference );
        Criteria queryObject = super.getSessionFactory().getCurrentSession()
                .createCriteria( BibliographicReference.class );

        /*
         * This syntax allows you to look at an association.
         */
        if ( bibliographicReference.getPubAccession() != null ) {
            queryObject.createCriteria( "pubAccession" ).add(
                    Restrictions.eq( "accession", bibliographicReference.getPubAccession().getAccession() ) );
        } else {
            throw new NullPointerException( "PubAccession cannot be null" );
        }

        java.util.List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + BibliographicReference.class.getName() + "' with accession "
                                + bibliographicReference.getPubAccession().getAccession()
                                + " was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( BibliographicReference ) result;

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
    public Map<ExpressionExperiment, BibliographicReference> handleGetAllExperimentLinkedReferences() {
        final String query = "select distinct e, b from ExpressionExperimentImpl e join e.primaryPublication b left join fetch b.pubAccession ";
        Map<ExpressionExperiment, BibliographicReference> result = new HashMap<ExpressionExperiment, BibliographicReference>();
        List<Object[]> os = this.getHibernateTemplate().find( query );
        for ( Object[] o : os ) {
            result.put( ( ExpressionExperiment ) o[0], ( BibliographicReference ) o[1] );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#getRelatedExperiments(java.util.Collection)
     */
    @Override
    public Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records ) {
        final String query = "select distinct e, b from ExpressionExperimentImpl "
                + "e join e.primaryPublication b left join fetch b.pubAccession where b in (:recs)";

        Map<BibliographicReference, Collection<ExpressionExperiment>> result = new HashMap<BibliographicReference, Collection<ExpressionExperiment>>();

        for ( Collection<BibliographicReference> batch : BatchIterator.batches( records, 200 ) ) {
            List<Object[]> os = this.getHibernateTemplate().findByNamedParam( query, "recs", batch );
            for ( Object[] o : os ) {
                ExpressionExperiment e = ( ExpressionExperiment ) o[0];
                BibliographicReference b = ( BibliographicReference ) o[1];
                if ( !result.containsKey( b ) ) {
                    result.put( b, new HashSet<ExpressionExperiment>() );
                }
                result.get( b ).add( e );
            }
        }
        return result;
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
        return ( BibliographicReference ) this
                .getHibernateTemplate()
                .findByNamedParam(
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

    // Note that almost the same method is also available from the EEservice
    @Override
    protected Collection<ExpressionExperiment> handleGetRelatedExperiments(
            BibliographicReference bibliographicReference ) {
        final String queryString = "select distinct ee FROM ExpressionExperimentImpl as ee left join ee.otherRelevantPublications as eeO"
                + " where ee.primaryPublication = :bib OR (eeO = :bib) ";

        return this.getHibernateTemplate().findByNamedParam( queryString, "bib", bibliographicReference );
    }

    @Override
    protected Collection<BibliographicReference> handleLoadMultiple( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from BibliographicReferenceImpl b where b.id in :bib",
                "bib", ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BrowsingDao#count()
     */
    @Override
    public Integer count() {
        return ( ( Long ) getHibernateTemplate().find( "select count(*) from BibliographicReferenceImpl" ).iterator()
                .next() ).intValue();
    }

}