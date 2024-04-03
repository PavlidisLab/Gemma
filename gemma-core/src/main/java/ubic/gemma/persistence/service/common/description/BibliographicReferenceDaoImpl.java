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
package ubic.gemma.persistence.service.common.description;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.HibernateUtils;

import java.util.*;

import static ubic.gemma.persistence.util.QueryUtils.batchIdentifiableParameterList;
import static ubic.gemma.persistence.util.QueryUtils.optimizeIdentifiableParameterList;

/**
 * @author pavlidis
 * @see BibliographicReference
 */
@Repository
public class BibliographicReferenceDaoImpl
        extends AbstractVoEnabledDao<BibliographicReference, BibliographicReferenceValueObject>
        implements BibliographicReferenceDao {

    private final int eeBatchSize;

    @Autowired
    public BibliographicReferenceDaoImpl( SessionFactory sessionFactory ) {
        super( BibliographicReference.class, sessionFactory );
        this.eeBatchSize = HibernateUtils.getBatchSize( sessionFactory, sessionFactory.getClassMetadata( ExpressionExperiment.class ) );
    }

    @Override
    public BibliographicReference findByExternalId( final String id, final String databaseName ) {
        //noinspection unchecked
        return ( BibliographicReference ) this.getSessionFactory().getCurrentSession().createQuery(
                        "from BibliographicReference b where b.pubAccession.accession=:id AND b.pubAccession.externalDatabase.name=:databaseName" )
                .setParameter( "id", id ).setParameter( "databaseName", databaseName ).uniqueResult();
    }

    @Override
    public BibliographicReference findByExternalId( final DatabaseEntry externalId ) {
        //noinspection unchecked
        return ( BibliographicReference ) this.getSessionFactory().getCurrentSession()
                .createQuery( "from BibliographicReference b where b.pubAccession=:externalId" )
                .setParameter( "externalId", externalId ).uniqueResult();
    }

    @Override
    public Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences() {
        final String query = "select distinct e, b from ExpressionExperiment e join e.primaryPublication b left join fetch b.pubAccession ";
        Map<ExpressionExperiment, BibliographicReference> result = new HashMap<>();
        //noinspection unchecked
        List<Object[]> os = this.getSessionFactory().getCurrentSession().createQuery( query ).list();
        for ( Object[] o : os ) {
            result.put( ( ExpressionExperiment ) o[0], ( BibliographicReference ) o[1] );
        }
        return result;
    }

    @Override
    public BibliographicReference thaw( BibliographicReference bibliographicReference ) {
        if ( bibliographicReference == null || bibliographicReference.getId() == null )
            return bibliographicReference;
        return ( BibliographicReference ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select b from BibliographicReference b left join fetch b.pubAccession left join fetch b.chemicals "
                                + "left join fetch b.meshTerms left join fetch b.keywords where b.id = :id " )
                .setParameter( "id", bibliographicReference.getId() ).uniqueResult();
    }

    @Override
    public Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences ) {
        if ( bibliographicReferences.isEmpty() )
            return bibliographicReferences;
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select b from BibliographicReference b left join fetch b.pubAccession left join fetch b.chemicals "
                                + "left join fetch b.meshTerms left join fetch b.keywords where b in (:bs) " )
                .setParameterList( "bs", optimizeIdentifiableParameterList( bibliographicReferences ) ).list();
    }

    @Override
    public Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records ) {
        final String query = "select distinct e, b from ExpressionExperiment "
                + "e join e.primaryPublication b left join fetch b.pubAccession where b in (:recs)";

        Map<BibliographicReference, Collection<ExpressionExperiment>> result = new HashMap<>();

        for ( Collection<BibliographicReference> batch : batchIdentifiableParameterList( records, eeBatchSize ) ) {
            //noinspection unchecked
            List<Object[]> os = this.getSessionFactory().getCurrentSession().createQuery( query )
                    .setParameterList( "recs", batch ).list();
            for ( Object[] o : os ) {
                ExpressionExperiment e = ( ExpressionExperiment ) o[0];
                BibliographicReference b = ( BibliographicReference ) o[1];
                result.computeIfAbsent( b, k -> new HashSet<>() ).add( e );
            }
        }
        return result;
    }

    @Override
    public Collection<Long> listAll() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "select id from BibliographicReference" )
                .list();
    }

    @Override
    public List<BibliographicReference> browse( int start, int limit ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "from BibliographicReference" )
                .setMaxResults( limit ).setFirstResult( start ).list();
    }

    @Override
    public List<BibliographicReference> browse( int start, int limit, String orderField, boolean descending ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from BibliographicReference order by :orderField " + ( descending ? "desc" : "" ) )
                .setMaxResults( limit ).setFirstResult( start ).setParameter( "orderField", orderField ).list();
    }

    @Override
    public BibliographicReference find( BibliographicReference bibliographicReference ) {

        BusinessKey.checkKey( bibliographicReference );
        Criteria queryObject = this.getSessionFactory().getCurrentSession()
                .createCriteria( BibliographicReference.class );

        /*
         * This syntax allows you to look at an association.
         */
        if ( bibliographicReference.getPubAccession() != null ) {
            queryObject.createCriteria( "pubAccession" )
                    .add( Restrictions.eq( "accession", bibliographicReference.getPubAccession().getAccession() ) );
        } else {
            throw new NullPointerException( "PubAccession cannot be null" );
        }

        return ( BibliographicReference ) queryObject.uniqueResult();
    }

    @Override
    protected BibliographicReferenceValueObject doLoadValueObject( BibliographicReference entity ) {
        return new BibliographicReferenceValueObject( entity );
    }

}