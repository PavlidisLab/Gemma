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
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentIdAndShortName;
import ubic.gemma.persistence.hibernate.HibernateUtils;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.AclQueryUtils;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.QueryUtils;

import java.util.*;

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
        this.eeBatchSize = HibernateUtils.getBatchSize( sessionFactory.getClassMetadata( ExpressionExperiment.class ), sessionFactory );
    }

    @Override
    public BibliographicReference findByExternalId( final String id, final String databaseName ) {
        return ( BibliographicReference ) this.getSessionFactory().getCurrentSession().createQuery(
                        "from BibliographicReference b "
                                + "where b.pubAccession.accession=:id AND b.pubAccession.externalDatabase.name=:databaseName" )
                .setParameter( "id", id )
                .setParameter( "databaseName", databaseName )
                .uniqueResult();
    }

    @Override
    public BibliographicReference findByExternalId( final DatabaseEntry externalId ) {
        return ( BibliographicReference ) this.getSessionFactory().getCurrentSession()
                .createQuery( "from BibliographicReference b where b.pubAccession=:externalId" )
                .setParameter( "externalId", externalId ).uniqueResult();
    }

    @Override
    public long countDistinctWithRelatedExperiments() {
        Query q = this.getSessionFactory().getCurrentSession()
                .createQuery( "select count(" + ( AclQueryUtils.requiresCountDistinct() ? "distinct " : "" ) + " b)" + " "
                        + "from ExpressionExperiment e join e.primaryPublication b "
                        + AclQueryUtils.formAclRestrictionClause( "e.id" ) );
        AclQueryUtils.addAclParameters( q, ExpressionExperiment.class );
        return ( Long ) q.uniqueResult();
    }

    @Override
    public long countWithRelatedExperiments() {
        Query q = this.getSessionFactory().getCurrentSession()
                // the slight difference here is that we count the number of distinct experiment, which is equivalent to
                // the number of ref-experiment pairs due to the one-to-many relation
                .createQuery( "select count(" + ( AclQueryUtils.requiresCountDistinct() ? "distinct " : "" ) + " e)" + " "
                        + "from ExpressionExperiment e join e.primaryPublication b"
                        + AclQueryUtils.formAclRestrictionClause( "e.id" ) );
        AclQueryUtils.addAclParameters( q, ExpressionExperiment.class );
        return ( Long ) q.uniqueResult();
    }

    @Override
    public LinkedHashMap<BibliographicReference, Set<ExpressionExperimentIdAndShortName>> getRelatedExperiments( int offset, int limit ) {
        Query q = this.getSessionFactory().getCurrentSession()
                .createQuery( "select b, e.id, e.shortName from ExpressionExperiment e join e.primaryPublication b "
                        + AclQueryUtils.formAclRestrictionClause( "e.id" ) + " "
                        + ( AclQueryUtils.requiresGroupBy() ? "group by b, e " : "" )
                        + "order by b.authorList nulls last, b.title nulls last"
                );
        AclQueryUtils.addAclParameters( q, ExpressionExperiment.class );
        //noinspection unchecked
        List<Object[]> os = q
                .setFirstResult( offset )
                .setMaxResults( limit )
                .list();
        LinkedHashMap<BibliographicReference, Set<ExpressionExperimentIdAndShortName>> result = new LinkedHashMap<>();
        for ( Object[] o : os ) {
            BibliographicReference b = ( BibliographicReference ) o[0];
            ExpressionExperimentIdAndShortName ee = new ExpressionExperimentIdAndShortName( ( Long ) o[1], ( String ) o[2] );
            result.computeIfAbsent( b, k -> new HashSet<>() ).add( ee );
        }
        return result;
    }

    @Override
    public LinkedHashMap<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records ) {
        if ( records.isEmpty() ) {
            return new LinkedHashMap<>();
        }
        Query query = getSessionFactory().getCurrentSession()
                .createQuery( "select b, e from ExpressionExperiment e join e.primaryPublication b "
                        + AclQueryUtils.formAclRestrictionClause( "e.id" ) + " "
                        + "and b in (:recs) "
                        + ( AclQueryUtils.requiresGroupBy() ? "group by b, e " : "" )
                        + "order by b.authorList nulls last, b.title nulls last" );
        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        List<Object[]> os = QueryUtils.listByIdentifiableBatch( query, "recs", records, eeBatchSize );
        LinkedHashMap<BibliographicReference, Collection<ExpressionExperiment>> result = new LinkedHashMap<>();
        for ( Object[] o : os ) {
            BibliographicReference b = ( BibliographicReference ) o[0];
            ExpressionExperiment e = ( ExpressionExperiment ) o[1];
            result.computeIfAbsent( b, k -> new HashSet<>() ).add( e );
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