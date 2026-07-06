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

import org.hibernate.query.Query;
import org.hibernate.SessionFactory;
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

    /**
     * Whitelist of column names that {@link #browse(int, int, String, boolean)} accepts as
     * the ORDER BY target. Mirrors the four properties the web controller exposes; values
     * not in this set are rejected to keep the ORDER BY clause from absorbing arbitrary
     * caller input.
     */
    private static final Set<String> BROWSE_SORTABLE_FIELDS = new HashSet<>( Arrays.asList(
            "title", "publicationDate", "publication", "authorList" ) );

    private final int eeBatchSize;

    @Autowired
    public BibliographicReferenceDaoImpl( SessionFactory sessionFactory ) {
        super( BibliographicReference.class, sessionFactory );
        this.eeBatchSize = HibernateUtils.getBatchSize( ExpressionExperiment.class, sessionFactory );
    }

    @Override
    public BibliographicReference findByExternalId( final String id, final String databaseName ) {
        //noinspection unchecked
        List<BibliographicReference> matches = this.getSessionFactory().getCurrentSession().createQuery(
                        "from BibliographicReference b "
                                + "where b.pubAccession.accession=:id AND b.pubAccession.externalDatabase.name=:databaseName "
                                + "order by b.id" )
                .setParameter( "id", id )
                .setParameter( "databaseName", databaseName )
                .setMaxResults( 2 )
                .list();
        return firstOfPossibleDuplicates( matches, databaseName + ":" + id );
    }

    @Override
    public List<BibliographicReference> findAllByExternalId( final String id, final String databaseName ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "from BibliographicReference b "
                                + "where b.pubAccession.accession=:id AND b.pubAccession.externalDatabase.name=:databaseName "
                                + "order by b.id" )
                .setParameter( "id", id )
                .setParameter( "databaseName", databaseName )
                .list();
    }

    @Override
    public BibliographicReference findByExternalId( final DatabaseEntry externalId ) {
        //noinspection unchecked
        List<BibliographicReference> matches = this.getSessionFactory().getCurrentSession()
                .createQuery( "from BibliographicReference b where b.pubAccession=:externalId order by b.id" )
                .setParameter( "externalId", externalId )
                .setMaxResults( 2 )
                .list();
        return firstOfPossibleDuplicates( matches, String.valueOf( externalId.getAccession() ) );
    }

    /**
     * Collapse a possibly-duplicated external-id lookup to a single, deterministic result. Duplicate
     * {@link BibliographicReference} rows for the same accession exist in prod (a known data issue); a naive
     * {@code uniqueResult()} throws {@link org.hibernate.NonUniqueResultException} on them, which surfaced as
     * a 500 on {@code PUT /datasets/{id}/publications}. Return the lowest-id (oldest, canonical) match and
     * warn so the duplicates get cleaned up, rather than failing the caller.
     *
     * @param matches up to two matches (query capped at {@code setMaxResults(2)} — enough to detect a dup).
     * @param key     accession key for the warning message.
     * @return the lowest-id match, or {@code null} when there is none.
     */
    private BibliographicReference firstOfPossibleDuplicates( List<BibliographicReference> matches, String key ) {
        if ( matches.isEmpty() ) {
            return null;
        }
        if ( matches.size() > 1 ) {
            log.warn( "Multiple BibliographicReferences share external id '" + key + "'; returning the lowest-id one (id="
                    + matches.get( 0 ).getId() + "). The bibref table needs de-duplication for this accession." );
        }
        return matches.get( 0 );
    }

    @Override
    public long countDistinctWithRelatedExperiments() {
        Query q = this.getSessionFactory().getCurrentSession()
                .createQuery( "select count(b) "
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
                .createQuery( "select count(e) "
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
                        + "order by b.authorList nulls last, b.title nulls last"
                );
        AclQueryUtils.addAclParameters( q, ExpressionExperiment.class );
        //noinspection unchecked
        List<Object[]> os = q
                .setFirstResult( offset )
                // HB6 rejects setMaxResults(<0); pagination contract treats <=0 as "no limit".
                .setMaxResults( limit > 0 ? limit : Integer.MAX_VALUE )
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
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select b from BibliographicReference b left join fetch b.pubAccession left join fetch b.chemicals "
                                + "left join fetch b.meshTerms left join fetch b.keywords where b in (:bs) ",
                        BibliographicReference.class )
                .setParameterList( "bs", optimizeIdentifiableParameterList( bibliographicReferences ) ).list();
    }

    @Override
    public Collection<Long> listAll() {
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select id from BibliographicReference", Long.class )
                .list();
    }

    @Override
    public List<BibliographicReference> browse( int start, int limit ) {
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from BibliographicReference", BibliographicReference.class )
                // HB6 rejects setMaxResults(<0); browse contract treats <=0 as "no limit".
                .setMaxResults( limit > 0 ? limit : Integer.MAX_VALUE ).setFirstResult( start ).list();
    }

    @Override
    public List<BibliographicReference> browse( int start, int limit, String orderField, boolean descending ) {
        // ORDER BY column names cannot be bound as HQL parameters; whitelist + inject so
        // the column reference is fixed alphabet, then append the direction explicitly so
        // descending=false produces a well-formed ASC clause (the prior code emitted
        // 'order by ?' with no direction in that arm).
        if ( !BROWSE_SORTABLE_FIELDS.contains( orderField ) ) {
            throw new IllegalArgumentException( "Unsupported BibliographicReference sort field: " + orderField
                    + " (allowed: " + BROWSE_SORTABLE_FIELDS + ")" );
        }
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from BibliographicReference order by " + orderField + ( descending ? " desc" : " asc" ),
                        BibliographicReference.class )
                // HB6 rejects setMaxResults(<0); browse contract treats <=0 as "no limit".
                .setMaxResults( limit > 0 ? limit : Integer.MAX_VALUE ).setFirstResult( start ).list();
    }

    @Override
    public BibliographicReference find( BibliographicReference bibliographicReference ) {
        BusinessKey.checkKey( bibliographicReference );
        if ( bibliographicReference.getPubAccession() == null ) {
            throw new NullPointerException( "PubAccession cannot be null" );
        }
        // Tolerate pre-existing duplicate rows (return the lowest-id match) rather than throwing. Beyond
        // not 500-ing reads, this is what keeps findOrCreate from ADDING another duplicate: an accession
        // that already has 2-3 rows still resolves to an existing one here, so create() is not reached.
        String accession = bibliographicReference.getPubAccession().getAccession();
        //noinspection unchecked
        List<BibliographicReference> matches = this.getSessionFactory().getCurrentSession()
                .createQuery( "from BibliographicReference b where b.pubAccession.accession = :acc order by b.id" )
                .setParameter( "acc", accession )
                .setMaxResults( 2 )
                .list();
        return firstOfPossibleDuplicates( matches, accession );
    }

    @Override
    protected BibliographicReferenceValueObject doLoadValueObject( BibliographicReference entity ) {
        return new BibliographicReferenceValueObject( entity );
    }

}