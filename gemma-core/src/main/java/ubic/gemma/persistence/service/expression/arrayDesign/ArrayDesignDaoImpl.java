/*

 *The Gemma project.
 *
 * Copyright (c) 2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.expression.arrayDesign;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.collection.PersistentCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AbstractCuratableDao;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author pavlidis
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
@Repository
@ParametersAreNonnullByDefault
public class ArrayDesignDaoImpl extends AbstractCuratableDao<ArrayDesign, ArrayDesignValueObject>
        implements ArrayDesignDao {

    private static final int LOGGING_UPDATE_EVENT_COUNT = 5000;

    @Autowired
    public ArrayDesignDaoImpl( SessionFactory sessionFactory ) {
        super( ArrayDesignDao.OBJECT_ALIAS, ArrayDesign.class, sessionFactory );
    }

    @Override
    public void addProbes( ArrayDesign arrayDesign, Collection<CompositeSequence> newProbes ) {
        for ( CompositeSequence compositeSequence : newProbes ) {
            compositeSequence.setArrayDesign( arrayDesign );
            this.getSessionFactory().getCurrentSession().update( compositeSequence );
        }
        this.update( arrayDesign );
    }

    @Override
    public Collection<CompositeSequence> compositeSequenceWithoutBioSequences( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString = "select distinct cs from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                + " left join cs.biologicalCharacteristic bs where ar = :ar and bs IS NULL";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ar", arrayDesign )
                .list();
    }

    @Override
    public Collection<CompositeSequence> compositeSequenceWithoutBlatResults( ArrayDesign arrayDesign ) {
        if ( arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String nativeQueryString = "SELECT DISTINCT cs.id FROM "
                + "COMPOSITE_SEQUENCE cs LEFT JOIN BIO_SEQUENCE2_GENE_PRODUCT bs2gp ON bs2gp.BIO_SEQUENCE_FK=cs.BIOLOGICAL_CHARACTERISTIC_FK "
                + "LEFT JOIN SEQUENCE_SIMILARITY_SEARCH_RESULT ssResult ON bs2gp.BLAT_RESULT_FK=ssResult.ID "
                + "WHERE ssResult.ID IS NULL AND ARRAY_DESIGN_FK = :id ";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createSQLQuery( nativeQueryString ).setParameter( "id", id )
                .list();
    }

    @Override
    public Collection<CompositeSequence> compositeSequenceWithoutGenes( ArrayDesign arrayDesign ) {
        if ( arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();

        final String nativeQueryString = "SELECT DISTINCT cs.id FROM "
                + "COMPOSITE_SEQUENCE cs LEFT JOIN BIO_SEQUENCE2_GENE_PRODUCT bs2gp ON BIO_SEQUENCE_FK=BIOLOGICAL_CHARACTERISTIC_FK "
                + "LEFT JOIN CHROMOSOME_FEATURE geneProduct ON (geneProduct.ID=bs2gp.GENE_PRODUCT_FK AND geneProduct.class='GeneProduct') "
                + "LEFT JOIN CHROMOSOME_FEATURE gene ON geneProduct.GENE_FK=gene.ID  "
                + "WHERE gene.ID IS NULL AND ARRAY_DESIGN_FK = :id";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createSQLQuery( nativeQueryString ).setParameter( "id", id )
                .list();
    }

    @Override
    public void deleteAlignmentData( ArrayDesign arrayDesign ) {
        // First have to remove all blatAssociations, because they are referred to by the alignments
        this.deleteGeneProductAssociations( arrayDesign );

        // Note attempts to do this with bulk updates were unsuccessful due to the need for joins.
        //language=HQL
        final String queryString = "select br from ArrayDesign ad join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, BlatResult br "
                + "where br.querySequence = bs and ad=:arrayDesign";
        //noinspection unchecked
        List<BlatResult> toDelete = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "arrayDesign", arrayDesign ).list();

        AbstractDao.log.info( "Deleting " + toDelete.size() + " alignments for sequences on " + arrayDesign
                + " (will affect other designs that use any of the same sequences)" );

        for ( BlatResult r : toDelete ) {
            this.getSessionFactory().getCurrentSession().delete( r );
        }

    }


    @Override
    public void deleteGeneProductAlignmentAssociations( ArrayDesign arrayDesign ) {
        this.getSessionFactory().getCurrentSession().buildLockRequest( LockOptions.UPGRADE )
                .setLockMode( LockMode.PESSIMISTIC_WRITE ).lock( arrayDesign );


        final String queryString = "select ba from CompositeSequence  cs "
                + "inner join cs.biologicalCharacteristic bs, BlatAssociation ba "
                + "where ba.bioSequence = bs and cs.arrayDesign=:arrayDesign";
        //noinspection unchecked
        List<BlatAssociation> blatAssociations = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString ).setParameter( "arrayDesign", arrayDesign ).list();
        if ( !blatAssociations.isEmpty() ) {
            for ( BlatAssociation r : blatAssociations ) {
                this.getSessionFactory().getCurrentSession().delete( r );
            }

        }
        AbstractDao.log.info(
                "Done deleting " + blatAssociations.size() + " blat associations for " + arrayDesign );

    }

    @Override
    public void deleteGeneProductAnnotationAssociations( ArrayDesign arrayDesign ) {
        this.getSessionFactory().getCurrentSession().buildLockRequest( LockOptions.UPGRADE )
                .setLockMode( LockMode.PESSIMISTIC_WRITE ).lock( arrayDesign );

        final String annotationAssociationQueryString = "select ba from CompositeSequence cs "
                + " inner join cs.biologicalCharacteristic bs, AnnotationAssociation ba "
                + " where ba.bioSequence = bs and cs.arrayDesign=:arrayDesign";

        //noinspection unchecked
        List<AnnotationAssociation> annotAssociations = this.getSessionFactory().getCurrentSession()
                .createQuery( annotationAssociationQueryString ).setParameter( "arrayDesign", arrayDesign ).list();

        if ( !annotAssociations.isEmpty() ) {

            for ( AnnotationAssociation r : annotAssociations ) {
                this.getSessionFactory().getCurrentSession().delete( r );
            }


        }
        AbstractDao.log.info(
                "Done deleting " + annotAssociations.size() + " AnnotationAssociations for " + arrayDesign );
    }

    @Override
    public void deleteGeneProductAssociations( ArrayDesign arrayDesign ) {

        this.getSessionFactory().getCurrentSession().buildLockRequest( LockOptions.UPGRADE )
                .setLockMode( LockMode.PESSIMISTIC_WRITE ).lock( arrayDesign );

        // these two queries could be combined by using BioSequence2GeneProduct.
        //language=HQL
        final String queryString = "select ba from CompositeSequence  cs "
                + "inner join cs.biologicalCharacteristic bs, BlatAssociation ba "
                + "where ba.bioSequence = bs and cs.arrayDesign=:arrayDesign";
        //noinspection unchecked
        List<BlatAssociation> blatAssociations = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString ).setParameter( "arrayDesign", arrayDesign ).list();
        if ( !blatAssociations.isEmpty() ) {
            for ( BlatAssociation r : blatAssociations ) {
                this.getSessionFactory().getCurrentSession().delete( r );
            }
            AbstractDao.log.info(
                    "Done deleting " + blatAssociations.size() + " blat associations for " + arrayDesign );
        }

        this.getSessionFactory().getCurrentSession().flush();
        this.getSessionFactory().getCurrentSession().clear();

        final String annotationAssociationQueryString = "select ba from CompositeSequence cs "
                + " inner join cs.biologicalCharacteristic bs, AnnotationAssociation ba "
                + " where ba.bioSequence = bs and cs.arrayDesign=:arrayDesign";

        //noinspection unchecked
        List<AnnotationAssociation> annotAssociations = this.getSessionFactory().getCurrentSession()
                .createQuery( annotationAssociationQueryString ).setParameter( "arrayDesign", arrayDesign ).list();

        if ( !annotAssociations.isEmpty() ) {

            for ( AnnotationAssociation r : annotAssociations ) {
                this.getSessionFactory().getCurrentSession().delete( r );
            }
            AbstractDao.log.info(
                    "Done deleting " + annotAssociations.size() + " AnnotationAssociations for " + arrayDesign );

        }
    }

    @Override
    public ArrayDesign find( ArrayDesign entity ) {
        BusinessKey.checkValidKey( entity );
        Criteria query = super.getSessionFactory().getCurrentSession().createCriteria( ArrayDesign.class );
        BusinessKey.addRestrictions( query, entity );

        return ( ArrayDesign ) query.uniqueResult();
    }

    @Override
    public Collection<ArrayDesign> findByAlternateName( String queryString ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select ad from ArrayDesign ad inner join ad.alternateNames n where n.name = :q" )
                .setParameter( "q", queryString ).list();
    }

    @Override
    public Collection<ArrayDesign> findByManufacturer( String queryString ) {
        if ( StringUtils.isBlank( queryString ) ) {
            return new HashSet<>();
        }
        Query query = this.getSessionFactory().getCurrentSession()
                .createQuery( "select ad from ArrayDesign ad inner join ad.designProvider n where n.name like ?" )
                .setParameter( 0, queryString + "%" );
        //noinspection unchecked
        return query.list();
    }

    @Override
    public Collection<ArrayDesign> findByTaxon( Taxon taxon ) {
        Query query = this.getSessionFactory().getCurrentSession()
                .createQuery( "select a from ArrayDesign a where a.primaryTaxon = :t" ).setParameter( "t", taxon );

        //noinspection unchecked
        return query.list();
    }

    @Override
    public Collection<BioAssay> getAllAssociatedBioAssays( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString = "select b from BioAssay as b inner join b.arrayDesignUsed a where a = :ad";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ad", arrayDesign ).list();
    }

    @Override
    public Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids ) {
        //language=HQL
        final String queryString = "select ad.id, auditEvent from ArrayDesign ad"
                + " join ad.auditTrail as auditTrail join auditTrail.events as auditEvent join fetch auditEvent.performer "
                + " where ad.id in (:ids) ";

        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();
        Map<Long, Collection<AuditEvent>> eventMap = new HashMap<>();
        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            AuditEvent event = ( AuditEvent ) o[1];

            this.addEventsToMap( eventMap, id, event );
        }
        // add in the array design ids that do not have events.
        // Set their values to null.
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, null );
            }
        }
        return eventMap;

    }

    @Override
    public Map<CompositeSequence, BioSequence> getBioSequences( ArrayDesign arrayDesign ) {

        if ( arrayDesign.getId() == null ) {
            throw new IllegalArgumentException( "Cannot fetch sequences for a non-persistent array design" );
        }

        StopWatch timer = new StopWatch();
        timer.start();

        String queryString = "select ad from ArrayDesign ad inner join fetch ad.compositeSequences cs "
                + "left outer join fetch cs.biologicalCharacteristic bs where ad = :ad";
        // have to include ad in the select to be able to use fetch join
        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ad", arrayDesign );
        List result = query.list();

        Map<CompositeSequence, BioSequence> bioSequences = new HashMap<>();
        if ( result.isEmpty() ) {
            return bioSequences;
        }

        for ( CompositeSequence cs : ( ( ArrayDesign ) result.get( 0 ) ).getCompositeSequences() ) {
            bioSequences.put( cs, cs.getBiologicalCharacteristic() );
        }

        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Fetch sequences: " + timer.getTime() + "ms" );
        }

        return bioSequences;
    }

    @Override
    public Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString = "select distinct ee from "
                + " ExpressionExperiment ee inner join ee.bioAssays bas inner join bas.arrayDesignUsed ad where ad = :ad";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ad", arrayDesign )
                .list();
    }

    @Override
    public Map<Taxon, Long> getPerTaxonCount() {
        Map<Taxon, Long> result = new HashMap<>();

        final String csString = "select t, count(ad) from ArrayDesign ad inner join ad.primaryTaxon t group by t ";
        org.hibernate.Query csQueryObject = this.getSessionFactory().getCurrentSession().createQuery( csString );
        csQueryObject.setReadOnly( true );
        csQueryObject.setCacheable( true );

        List csList = csQueryObject.list();

        Taxon t;
        for ( Object object : csList ) {
            Object[] oa = ( Object[] ) object;
            t = ( Taxon ) oa[0];
            Long count = ( Long ) oa[1];

            result.put( t, count );
        }

        return result;

    }

    /**
     * Get the ids of experiments that "originally" used this platform, but which don't any more due to a platform
     * switch.
     *
     * @param  arrayDesign a platform for which the statistic is computed
     * @return collection of experiment IDs.
     */
    @Override
    public Collection<Long> getSwitchedExpressionExperimentIds( ArrayDesign arrayDesign ) {

        //language=HQL
        final String queryString = "select distinct e.id from ExpressionExperiment e inner join e.bioAssays b where b.originalPlatform = :arrayDesign";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "arrayDesign", arrayDesign )
                .list();
    }

    @Override
    public Long getSwitchedExpressionExperimentsCount( ArrayDesign arrayDesign ) {

        //language=HQL
        final String queryString = "select count(distinct e) from ExpressionExperiment e inner join e.bioAssays b where b.originalPlatform = :arrayDesign";

        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "arrayDesign", arrayDesign )
                .setCacheable( true )
                .uniqueResult();
    }

    @Override
    public Collection<Taxon> getTaxa( ArrayDesign arrayDesign ) {
        final String queryString =
                "select distinct t from ArrayDesign as arrayD "
                        + "join arrayD.compositeSequences as cs "
                        + "join cs.biologicalCharacteristic as bioC "
                        + "join bioC.taxon t where arrayD = :ad";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ad", arrayDesign )
                .list();
    }

    @Override
    public Map<Long, Boolean> isMerged( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyMap();
        }
        final String queryString = "select ad.id, count(subs) as isMerged from ArrayDesign as ad left join ad.mergees subs where ad.id in (:ids) group by ad";
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();
        return list.stream().collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Long ) row[1] > 0 ) );
    }

    @Override
    public Map<Long, Boolean> isMergee( final Collection<Long> ids ) {
        Map<Long, Boolean> eventMap = new HashMap<>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }
        //language=HQL
        final String queryString = "select ad.id, ad.mergedInto.id from ArrayDesign as ad where ad.id in (:ids) ";
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();
        return list.stream().collect( Collectors.toMap( row -> ( Long ) row[0], row -> row[1] != null ) );
    }

    @Override
    public Map<Long, Boolean> isSubsumed( final Collection<Long> ids ) {
        if ( ids.size() == 0 ) {
            return Collections.emptyMap();
        }
        //language=HQL
        final String queryString = "select ad.id, ad.subsumingArrayDesign.id from ArrayDesign as ad where ad.id in (:ids) ";
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();
        return list.stream().collect( Collectors.toMap( row -> ( Long ) row[0], row -> row[1] != null ) );
    }

    @Override
    public Map<Long, Boolean> isSubsumer( Collection<Long> ids ) {
        if ( ids.size() == 0 ) {
            return Collections.emptyMap();
        }
        //language=HQL
        final String queryString = "select ad.id, count(subs) from ArrayDesign as ad left join ad.subsumedArrayDesigns subs where ad.id in (:ids) group by ad";
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();
        return list.stream().collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Long ) row[1] > 0 ) );
    }

    @Override
    public Map<CompositeSequence, Collection<BlatResult>> loadAlignments( ArrayDesign arrayDesign ) {
        //noinspection unchecked,JpaQlInspection - blatResult not visible because it is in a subclass (BlatAssociation)
        List<Object[]> m = this.getSessionFactory().getCurrentSession().createQuery(
                        "select cs, br from CompositeSequence cs "
                                + " join cs.biologicalCharacteristic bs join bs.bioSequence2GeneProduct bs2gp"
                                + " join bs2gp.blatResult br "
                                + "  where bs2gp.class='BlatAssociation' and cs.arrayDesign=:ad" )
                .setParameter( "ad", arrayDesign ).list();

        Map<CompositeSequence, Collection<BlatResult>> result = new HashMap<>();
        for ( Object[] objects : m ) {
            CompositeSequence cs = ( CompositeSequence ) objects[0];
            BlatResult br = ( BlatResult ) objects[1];
            if ( !result.containsKey( cs ) ) {
                result.put( cs, new HashSet<>() );
            }
            result.get( cs ).add( br );
        }

        return result;
    }

    @Override
    public Collection<CompositeSequence> loadCompositeSequences( ArrayDesign arrayDesign, int limit, int offset ) {
        //language=HQL
        final String queryString = "select cs from CompositeSequence as cs where cs.arrayDesign = :ad";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ad", arrayDesign )
                .setFirstResult( offset ).setMaxResults( limit > 0 ? limit : -1 ).list();
    }

    /**
     * Loads a single value objects for the given array design.
     *
     * @param ad the array design to be converted to a value object
     * @return a value object with properties of the given array design.
     */
    @Override
    protected ArrayDesignValueObject doLoadValueObject( ArrayDesign ad ) {
        return new ArrayDesignValueObject( ad );
    }

    @Override
    public ArrayDesignValueObject loadValueObject( ArrayDesign entity ) {
        ArrayDesignValueObject result = super.loadValueObject( entity );
        populateArrayDesignValueObjects( Collections.singleton( result ) );
        return result;
    }

    @Override
    public List<ArrayDesignValueObject> loadValueObjectsForEE( @Nullable Long eeId ) {
        if ( eeId == null ) {
            return new ArrayList<>();
        }
        Collection<Long> ids = CommonQueries.getArrayDesignIdsUsed( eeId,
                this.getSessionFactory().getCurrentSession() );
        return this.loadValueObjectsByIds( ids );
    }

    @Override
    public Slice<ArrayDesignValueObject> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        Slice<ArrayDesignValueObject> results = super.loadValueObjectsPreFilter( filters, sort, offset, limit );
        populateArrayDesignValueObjects( results );
        return results;
    }

    @Override
    public List<ArrayDesignValueObject> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort ) {
        List<ArrayDesignValueObject> results = super.loadValueObjectsPreFilter( filters, sort );
        populateArrayDesignValueObjects( results );
        return results;
    }

    private void populateArrayDesignValueObjects( Collection<ArrayDesignValueObject> results ) {
        StopWatch timer = StopWatch.createStarted();
        populateIsMerged( results );
        populateBlacklisted( results );
        populateExpressionExperimentCount( results );
        populateSwitchedExpressionExperimentCount( results );
        log.info( String.format( "Populating ArrayDesign VOs took %d ms.", timer.getTime( TimeUnit.MILLISECONDS ) ) );
    }

    @Override
    @Transactional
    public long numAllCompositeSequenceWithBioSequences() {
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " where cs.biologicalCharacteristic.sequence is not null";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString ).list().iterator()
                .next();
    }

    @Override
    @Transactional
    public long numAllCompositeSequenceWithBioSequences( Collection<Long> ids ) {

        if ( ids.isEmpty() )
            return 0L;

        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " where ar.id in (:ids) and cs.biologicalCharacteristic.sequence is not null";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numAllCompositeSequenceWithBlatResults() {
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BlatResult as blat where blat.querySequence=cs.biologicalCharacteristic";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString ).list().iterator()
                .next();
    }

    @Override
    @Transactional
    public long numAllCompositeSequenceWithBlatResults( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return 0;
        }
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + ", BlatResult as blat where blat.querySequence != null and ar.id in (:ids)";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numAllCompositeSequenceWithGenes() {
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + ", BioSequence2GeneProduct bs2gp, Gene gene inner join gene.products gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and bs2gp.geneProduct=gp";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString ).list().iterator()
                .next();
    }

    @Override
    @Transactional
    public long numAllCompositeSequenceWithGenes( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return 0;
        }
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + ", BioSequence2GeneProduct bs2gp, Gene gene inner join gene.products gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                        + "bs2gp.geneProduct=gp and ar.id in (:ids)";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numAllGenes() {
        //language=HQL
        final String queryString =
                "select count (distinct gene) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + ", BioSequence2GeneProduct bs2gp, Gene gene inner join gene.products gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and  bs2gp.geneProduct=gp";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString ).list().iterator()
                .next();
    }

    @Override
    @Transactional
    public long numAllGenes( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return 0;
        }
        //language=HQL
        final String queryString =
                "select count (distinct gene) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BioSequence2GeneProduct bs2gp, Gene gene inner join gene.products gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                        + "bs2gp.geneProduct=gp  and ar.id in (:ids)";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numBioSequences( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString =
                "select count (distinct cs.biologicalCharacteristic) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " where ar = :ar and cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numBlatResults( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString =
                "select count (distinct bs2gp) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BioSequence2GeneProduct as bs2gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and ar = :ar";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numCompositeSequences( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString = "select count (*) from  CompositeSequence as cs inner join cs.arrayDesign as ar where ar = :ad";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ad", arrayDesign )
                .uniqueResult();
    }

    @Override
    public long numCompositeSequenceWithBioSequences( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString =
                "select count (distinct cs) from CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " where ar = :ar and cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BlatResult as blat where blat.querySequence=cs.biologicalCharacteristic and ar = :ar";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numCompositeSequenceWithGenes( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BioSequence2GeneProduct bs2gp, Gene gene inner join gene.products gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                        + "bs2gp.geneProduct=gp and ar = :ar";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numExperiments( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString = "select distinct ee.id  from   "
                + " ExpressionExperiment ee inner join ee.bioAssays bas join bas.arrayDesignUsed ad where ad = :ad";

        List ids = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ad", arrayDesign ).list();

        if ( ids.isEmpty() ) {
            return 0;
        }

        //noinspection unchecked
        return EntityUtils.securityFilterIds( ExpressionExperiment.class, ids, false, true,
                this.getSessionFactory().getCurrentSession() ).size();
    }

    @Override
    @Transactional
    public long numGenes( ArrayDesign arrayDesign ) {
        //language=HQL
        return ( ( BigInteger ) getSessionFactory().getCurrentSession().createSQLQuery(
                        "select count(distinct g2cs.GENE) from GENE2CS g2cs "
                                + "where g2cs.AD = :arrayDesignId" )
                .setParameter( "arrayDesignId", arrayDesign.getId() )
                .setCacheable( true )
                .uniqueResult() ).longValue();
    }

    @Override
    public void remove( ArrayDesign arrayDesign ) {
        arrayDesign = this.load( arrayDesign.getId() );
        if ( arrayDesign == null )
            return; /* already removed... */
        //getSession().buildLockRequest( LockOptions.NONE ).lock( arrayDesign );
        Hibernate.initialize( arrayDesign.getMergees() );
        Hibernate.initialize( arrayDesign.getSubsumedArrayDesigns() );
        arrayDesign.getMergees().clear();
        arrayDesign.getSubsumedArrayDesigns().clear();

        Iterator<CompositeSequence> iterator = arrayDesign.getCompositeSequences().iterator();
        while ( iterator.hasNext() ) {
            CompositeSequence cs = iterator.next();
            iterator.remove();
            this.getSessionFactory().getCurrentSession().delete( cs );
        }

        super.remove( arrayDesign );
    }

    @Override
    public void removeBiologicalCharacteristics( final ArrayDesign arrayDesign ) {
        Session session = this.getSessionFactory().getCurrentSession();
        session.buildLockRequest( LockOptions.NONE ).lock( arrayDesign );

        int count = 0;
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            cs.setBiologicalCharacteristic( null );
            session.update( cs );
            session.evict( cs );
            if ( ++count % ArrayDesignDaoImpl.LOGGING_UPDATE_EVENT_COUNT == 0 ) {
                AbstractDao.log.info( "Cleared sequence association for " + count + " composite sequences" );
            }
        }

    }

    @Override
    public ArrayDesign thaw( final ArrayDesign arrayDesign ) {
        if ( arrayDesign.getId() == null ) {
            throw new IllegalArgumentException( "Cannot thaw a non-persistent array design" );
        }

        /*
         * Thaw basic stuff
         */
        StopWatch timer = new StopWatch();
        timer.start();

        ArrayDesign result = this.thawLite( arrayDesign );

        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Thaw array design stage 1: " + timer.getTime() + "ms" );
        }

        timer.stop();
        timer.reset();
        timer.start();

        /*
         * Thaw the composite sequences.
         */
        AbstractDao.log.info( "Start initialize composite sequences" );

        Hibernate.initialize( result.getCompositeSequences() );

        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Thaw array design stage 2: " + timer.getTime() + "ms" );
        }
        timer.stop();
        timer.reset();
        timer.start();

        /*
         * Thaw the biosequences in batches
         */
        Collection<CompositeSequence> thawed = new HashSet<>();
        Collection<CompositeSequence> batch = new HashSet<>();
        long lastTime = timer.getTime();
        for ( CompositeSequence cs : result.getCompositeSequences() ) {
            batch.add( cs );
            if ( batch.size() == 1000 ) {
                long t = timer.getTime();
                if ( t > 10000 && t - lastTime > 1000 ) {
                    AbstractDao.log.info( "Thaw Batch : " + t );
                }
                List bb = this.thawBatchOfProbes( batch );
                //noinspection unchecked
                thawed.addAll( ( Collection<? extends CompositeSequence> ) bb );
                lastTime = timer.getTime();
                batch.clear();
            }
            this.getSessionFactory().getCurrentSession().evict( cs );
        }

        if ( !batch.isEmpty() ) { // tail end
            List bb = this.thawBatchOfProbes( batch );
            //noinspection unchecked
            thawed.addAll( ( Collection<? extends CompositeSequence> ) bb );
        }

        result.getCompositeSequences().clear();
        result.getCompositeSequences().addAll( thawed );

        /*
         * This is a bit ugly, but necessary to avoid 'dirty collection' errors later.
         */
        if ( result.getCompositeSequences() instanceof PersistentCollection )
            ( ( PersistentCollection ) result.getCompositeSequences() ).clearDirty();

        if ( timer.getTime() > 1000 ) {
            AbstractDao.log.info( "Thaw array design stage 3: " + timer.getTime() );
        }

        return result;
    }

    @Override
    public ArrayDesign thawLite( @NonNull ArrayDesign arrayDesign ) {
        return Objects.requireNonNull( ( ArrayDesign ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct a from ArrayDesign a left join fetch a.subsumedArrayDesigns "
                                + " left join fetch a.mergees left join fetch a.designProvider left join fetch a.primaryTaxon "
                                + " join fetch a.auditTrail trail join fetch trail.events join fetch a.curationDetails left join fetch a.externalReferences"
                                + " left join fetch a.subsumingArrayDesign left join fetch a.mergedInto left join fetch a.alternativeTo where a.id=:adId" )
                .setParameter( "adId", arrayDesign.getId() )
                .uniqueResult(), "No array design with id=" + arrayDesign.getId() + " could be loaded." );
    }

    @Override
    public Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns ) {
        if ( arrayDesigns.isEmpty() )
            return arrayDesigns;
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct a from ArrayDesign a " + "left join fetch a.subsumedArrayDesigns "
                                + " left join fetch a.mergees left join fetch a.designProvider left join fetch a.primaryTaxon "
                                + " join fetch a.auditTrail trail join fetch trail.events join fetch a.curationDetails left join fetch a.externalReferences"
                                + " left join fetch a.subsumedArrayDesigns left join fetch a.subsumingArrayDesign "
                                + " left join fetch a.mergedInto left join fetch a.alternativeTo where a.id in (:adIds)" )
                .setParameterList( "adIds", EntityUtils.getIds( arrayDesigns ) ).list();
    }

    @Override
    public Boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee ) {

        if ( candidateSubsumee.equals( candidateSubsumer ) ) {
            log.warn( "Attempt to check a platform against itself for subsuming!" );
            return false;
        }

        // Size does not automatically disqualify, because we only consider BioSequences that actually have
        // sequences in them.
        if ( candidateSubsumee.getCompositeSequences().size() > candidateSubsumer.getCompositeSequences().size() ) {
            AbstractDao.log.info(
                    "Subsumee has more sequences than subsumer so probably cannot be subsumed ... checking anyway" );
        }

        Collection<BioSequence> subsumerSeqs = new HashSet<>();
        Collection<BioSequence> subsumeeSeqs = new HashSet<>();

        for ( CompositeSequence cs : candidateSubsumee.getCompositeSequences() ) {
            BioSequence seq = cs.getBiologicalCharacteristic();
            if ( seq == null )
                continue;
            subsumeeSeqs.add( seq );
        }

        for ( CompositeSequence cs : candidateSubsumer.getCompositeSequences() ) {
            BioSequence seq = cs.getBiologicalCharacteristic();
            if ( seq == null )
                continue;
            subsumerSeqs.add( seq );
        }

        if ( subsumeeSeqs.size() > subsumerSeqs.size() ) {
            AbstractDao.log.info(
                    "Subsumee has more sequences than subsumer so probably cannot be subsumed, checking overlap" );
        }

        int overlap = 0;
        List<BioSequence> missing = new ArrayList<>();
        for ( BioSequence sequence : subsumeeSeqs ) {
            if ( subsumerSeqs.contains( sequence ) ) {
                overlap++;
            } else {
                missing.add( sequence );
            }
        }

        AbstractDao.log.info( "Subsumer " + candidateSubsumer + " contains " + overlap + "/" + subsumeeSeqs.size()
                + " biosequences from the subsumee " + candidateSubsumee );

        if ( overlap != subsumeeSeqs.size() ) {
            int n = 5;
            System.err.println( "Up to " + n + " missing sequences will be listed." );
            for ( int i = 0; i < Math.min( n, missing.size() ); i++ ) {
                System.err.println( missing.get( i ) );
            }
            return false;
        }

        // if we got this far, then we definitely have a subsuming situation.
        if ( candidateSubsumee.getCompositeSequences().size() == candidateSubsumer.getCompositeSequences().size() ) {
            AbstractDao.log.info(
                    candidateSubsumee + " and " + candidateSubsumer + " are apparently exactly equivalent" );
        } else {
            AbstractDao.log.info( candidateSubsumer + " subsumes " + candidateSubsumee );
        }
        candidateSubsumer.getSubsumedArrayDesigns().add( candidateSubsumee );
        candidateSubsumee.setSubsumingArrayDesign( candidateSubsumer );
        this.update( candidateSubsumer );

        this.getSessionFactory().getCurrentSession().flush();
        this.getSessionFactory().getCurrentSession().clear();
        this.update( candidateSubsumee );
        this.getSessionFactory().getCurrentSession().flush();
        this.getSessionFactory().getCurrentSession().clear();

        return true;
    }

    /**
     * Gets the number of expression experiments per ArrayDesign for all array designs.
     * <p>
     * In case you're wondering why we're retrieving counts for all AD at once, it's just better to have a single
     * cacheable query to cover all possible cases.
     */
    private Map<Long, Long> getExpressionExperimentCountMap() {
        //language=HQL
        final String queryString = "select ad.id, count(distinct ee) from   "
                + " ExpressionExperiment ee inner join ee.bioAssays bas inner join bas.arrayDesignUsed ad group by ad";

        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setCacheable( true )
                .list();

        if ( list.size() < 2 )
            AbstractDao.log.warn( list.size() + " rows from getExpressionExperimentCountMap query" );

        return list.stream().collect( Collectors.toMap( o -> ( Long ) o[0], o -> ( Long ) o[1] ) );
    }

    @Override
    protected Query getLoadValueObjectsQuery( @Nullable Filters filters, @Nullable Sort sort, EnumSet<QueryHint> hints ) {
        // FIXME: the distinct is necessary because the ACL jointure creates duplicated platforms
        String queryString =
                "select distinct ad from ArrayDesign as ad "
                        + "left join fetch ad.curationDetails s "
                        + "left join fetch ad.primaryTaxon t "
                        + "left join fetch ad.mergedInto m "
                        + "left join fetch s.lastNeedsAttentionEvent as eAttn "
                        + "left join fetch s.lastNoteUpdateEvent as eNote "
                        + "left join fetch s.lastTroubledEvent as eTrbl "
                        + "left join fetch ad.alternativeTo alt";

        if ( filters == null ) {
            filters = new Filters();
        }

        // Restrict to non-troubled ADs for non-administrators
        addNonTroubledFilter( filters, getObjectAlias() );

        queryString += AclQueryUtils.formAclJoinClause( "ad" );
        queryString += AclQueryUtils.formAclRestrictionClause();
        queryString += ObjectFilterQueryUtils.formRestrictionClause( filters );

        if ( sort != null ) {
            queryString += ObjectFilterQueryUtils.formOrderByClause( sort );
        }

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        AclQueryUtils.addAclJoinParameters( query, ArrayDesign.class );
        AclQueryUtils.addAclRestrictionParameters( query );
        ObjectFilterQueryUtils.addRestrictionParameters( query, filters );

        // FIXME: caching does not work for relationships
        // query.setCacheable( true );

        return query;
    }

    @Override
    protected Query getCountValueObjectsQuery( @Nullable Filters filters ) {
        // contain duplicated platforms
        String queryString =
                "select count(distinct ad) from ArrayDesign as ad "
                        + "left join ad.curationDetails s "
                        + "left join ad.primaryTaxon t "
                        + "left join ad.mergedInto m "
                        + "left join s.lastNeedsAttentionEvent as eAttn "
                        + "left join s.lastNoteUpdateEvent as eNote "
                        + "left join s.lastTroubledEvent as eTrbl "
                        + "left join ad.alternativeTo alt";

        if ( filters == null ) {
            filters = new Filters();
        }

        // Restrict to non-troubled ADs for non-administrators
        addNonTroubledFilter( filters, getObjectAlias() );

        queryString += AclQueryUtils.formAclJoinClause( getObjectAlias() );
        queryString += AclQueryUtils.formAclRestrictionClause();
        queryString += ObjectFilterQueryUtils.formRestrictionClause( filters );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        AclQueryUtils.addAclJoinParameters( query, ArrayDesign.class );
        AclQueryUtils.addAclRestrictionParameters( query );
        ObjectFilterQueryUtils.addRestrictionParameters( query, filters );

        query.setCacheable( true );

        return query;
    }

    private void populateIsMerged( Collection<ArrayDesignValueObject> results ) {
        Map<Long, Boolean> isMergedByArrayDesignId = isMerged( EntityUtils.getIds( results ) );
        for ( ArrayDesignValueObject advo : results ) {
            advo.setIsMerged( isMergedByArrayDesignId.get( advo.getId() ) );
        }
    }

    private void populateBlacklisted( Collection<ArrayDesignValueObject> vos ) {

        if ( vos.isEmpty() ) return;

        Set<String> shortNames = vos.stream()
                .map( ArrayDesignValueObject::getShortName )
                .collect( Collectors.toSet() );

        //noinspection unchecked
        Set<String> blacklistedShortnames = new HashSet<>( this.getSessionFactory().getCurrentSession()
                .createQuery( "select b.shortName from BlacklistedPlatform b where b.shortName in :n" )
                .setParameterList( "n", shortNames )
                .list() );

        for ( ArrayDesignValueObject vo : vos ) {
            vo.setBlackListed( blacklistedShortnames.contains( vo.getShortName() ) );
        }
    }

    private void populateExpressionExperimentCount( Collection<ArrayDesignValueObject> entities ) {
        Map<Long, Long> map = getExpressionExperimentCountMap();
        for ( ArrayDesignValueObject vo : entities ) {
            vo.setExpressionExperimentCount( map.get( vo.getId() ) );
        }
    }

    private void populateSwitchedExpressionExperimentCount( Collection<ArrayDesignValueObject> entities ) {
        //noinspection unchecked
        List<Object[]> results = this.getSessionFactory().getCurrentSession().createQuery(
                        "select b.originalPlatform.id, count(distinct e) from ExpressionExperiment e "
                                + "inner join e.bioAssays b where b.originalPlatform.id in :arrayDesignIds "
                                + "group by b.originalPlatform" )
                .setParameterList( "arrayDesignIds", EntityUtils.getIds( entities ) )
                .setCacheable( true )
                .list();
        Map<Long, Long> countById = results.stream()
                .collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Long ) row[1] ) );
        for ( ArrayDesignValueObject vo : entities ) {
            vo.setSwitchedExpressionExperimentCount( countById.getOrDefault( vo.getId(), 0L ) );
        }
    }

    private List thawBatchOfProbes( Collection<CompositeSequence> batch ) {
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select cs from CompositeSequence cs left join fetch cs.biologicalCharacteristic where cs in (:batch)" )
                .setParameterList( "batch", batch ).list();
    }
}