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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AbstractCuratableDao;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil.EE2AD_QUERY_SPACE;
import static ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil.GENE2CS_QUERY_SPACE;
import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * @author pavlidis
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
@Repository
public class ArrayDesignDaoImpl extends AbstractCuratableDao<ArrayDesign, ArrayDesignValueObject>
        implements ArrayDesignDao {

    private static final int LOGGING_UPDATE_EVENT_COUNT = 5000;

    /**
     * Alias used for {@link ArrayDesign#getPrimaryTaxon()}.
     */
    private static final String PRIMARY_TAXON_ALIAS = "t";

    /**
     * Alias used for {@link ArrayDesign#getExternalReferences()}.
     */
    private static final String EXTERNAL_REFERENCE_ALIAS = "er";

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

        log.info( "Deleting " + toDelete.size() + " alignments for sequences on " + arrayDesign
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
        log.info(
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
        log.info(
                "Done deleting " + annotAssociations.size() + " AnnotationAssociations for " + arrayDesign );
    }

    @Override
    public Slice<ArrayDesignValueObject> loadBlacklistedValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        if ( filters == null ) {
            filters = Filters.empty();
        } else {
            // use a copy to avoid leaking the blacklisted arrayDesign short names
            filters = Filters.by( filters );
        }
        // either by shortname
        Set<String> blacklistedShortNames = getBlacklistedShortNames();
        // or by accession
        Set<String> blacklistedAccessions = getBlacklistedAccessions();
        if ( blacklistedShortNames.isEmpty() && blacklistedAccessions.isEmpty() ) {
            return new Slice<>( Collections.emptyList(), sort, offset, limit, 0L );
        }
        Filters.FiltersClauseBuilder clauseBuilder = filters.and();
        if ( !blacklistedShortNames.isEmpty() ) {
            clauseBuilder = clauseBuilder.or( OBJECT_ALIAS, "shortName", String.class, Filter.Operator.in, blacklistedShortNames );
        }
        if ( !blacklistedAccessions.isEmpty() ) {
            clauseBuilder = clauseBuilder.or( EXTERNAL_REFERENCE_ALIAS, "accession", String.class, Filter.Operator.in, blacklistedAccessions );
        }
        filters = clauseBuilder.build();
        return loadValueObjects( filters, sort, offset, limit );
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
            log.info(
                    "Done deleting " + blatAssociations.size() + " blat associations for " + arrayDesign );
        }

        getSessionFactory().getCurrentSession().flush();

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
            log.info(
                    "Done deleting " + annotAssociations.size() + " AnnotationAssociations for " + arrayDesign );

        }
    }

    @Override
    public ArrayDesign findByShortName( String shortName ) {
        return findOneByProperty( "shortName", shortName );
    }

    @Override
    public Collection<ArrayDesign> findByName( String name ) {
        return findByProperty( "name", name );
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
                .createQuery( "select ad from ArrayDesign ad inner join ad.designProvider n where n.name like :query" )
                .setParameter( "query", queryString + "%" );
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
                .setParameterList( "ids", optimizeParameterList( ids ) ).list();
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
                + "left outer join fetch cs.biologicalCharacteristic bs left join fetch bs.sequenceDatabaseEntry where ad = :ad";
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
            log.info( "Fetch sequences: " + timer.getTime() + "ms" );
        }

        return bioSequences;
    }

    @Override
    public Collection<Gene> getGenes( ArrayDesign arrayDesign ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                // using distinct for multi-mapping probes
                .createSQLQuery( "select distinct {G.*} from GENE2CS "
                        + "join CHROMOSOME_FEATURE G on GENE2CS.GENE = G.ID "
                        + "where GENE2CS.AD = :ad" )
                .addEntity( "G", Gene.class )
                .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .addSynchronizedEntityClass( CompositeSequence.class )
                .addSynchronizedEntityClass( Gene.class )
                .setParameter( "ad", arrayDesign.getId() )
                .setCacheable( true )
                .list();
    }

    @Override
    public Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString = "select ee from "
                + " ExpressionExperiment ee inner join ee.bioAssays bas inner join bas.arrayDesignUsed ad where ad = :ad "
                + "group by ee";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ad", arrayDesign )
                .list();
    }

    @Override
    public long getExpressionExperimentsCount( ArrayDesign arrayDesign ) {
        Query query = this.getSessionFactory().getCurrentSession()
                .createQuery( "select count(distinct ee) from ExpressionExperiment ee "
                        + "join ee.bioAssays bas "
                        + "join bas.arrayDesignUsed ad "
                        + AclQueryUtils.formAclRestrictionClause( "ee.id" ) + " "
                        + "where ad = :ad" )
                .setParameter( "ad", arrayDesign );
        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        return ( Long ) query.uniqueResult();
    }

    @Override
    public Map<Taxon, Long> getPerTaxonCount() {
        Query query = this.getSessionFactory().getCurrentSession()
                .createQuery( "select t, count(ad) from ArrayDesign ad "
                        + "join ad.primaryTaxon t "
                        + AclQueryUtils.formAclRestrictionClause( "ad.id" ) + " "
                        + "group by t" );
        AclQueryUtils.addAclParameters( query, ArrayDesign.class );
        //noinspection unchecked
        List<Object[]> csList = query.setCacheable( true ).list();
        return csList.stream().collect( Collectors.toMap( row -> ( Taxon ) row[0], row -> ( Long ) row[1] ) );
    }

    /**
     * Get the ids of experiments that "originally" used this platform, but which don't any more due to a platform
     * switch.
     *
     * @param  arrayDesign a platform for which the statistic is computed
     * @return collection of experiment IDs.
     */
    @Override
    public Collection<ExpressionExperiment> getSwitchedExpressionExperiments( ArrayDesign arrayDesign ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select e from ExpressionExperiment e "
                        + "join e.bioAssays b "
                        + "where b.originalPlatform = :arrayDesign "
                        + "group by e" )
                .setParameter( "arrayDesign", arrayDesign )
                .list();
    }

    @Override
    public Long getSwitchedExpressionExperimentsCount( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString = "select count(distinct e) from ExpressionExperiment e "
                + "inner join e.bioAssays b "
                + AclQueryUtils.formAclRestrictionClause( "e.id" ) + " "
                + "and b.originalPlatform = :arrayDesign";
        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "arrayDesign", arrayDesign );
        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        return ( Long ) query
                .setCacheable( true )
                .uniqueResult();
    }

    @Override
    public Collection<Taxon> getTaxa( ArrayDesign arrayDesign ) {
        final String queryString =
                "select t from ArrayDesign as arrayD "
                        + "join arrayD.compositeSequences as cs "
                        + "join cs.biologicalCharacteristic as bioC "
                        + "join bioC.taxon t where arrayD = :ad "
                        + "group by t";

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
        //noinspection unchecked,rawtypes
        Set<Long> mergedIds = new HashSet<>( this.getSessionFactory().getCurrentSession()
                .createQuery( "select ad.id from ArrayDesign as ad join ad.mergees subs where ad.id in (:ids) group by ad" )
                .setParameterList( "ids", optimizeParameterList( ids ) ).list() );
        return ids.stream().distinct().collect( Collectors.toMap( id -> id, mergedIds::contains ) );
    }

    @Override
    public Map<Long, Boolean> isMergee( final Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked,rawtypes
        Set<Long> mergeeIds = new HashSet<>( this.getSessionFactory().getCurrentSession()
                .createQuery( "select ad.id from ArrayDesign as ad where ad.mergedInto.id is not null and ad.id in (:ids)" )
                .setParameterList( "ids", optimizeParameterList( ids ) ).list() );
        return ids.stream().distinct().collect( Collectors.toMap( id -> id, mergeeIds::contains ) );
    }

    @Override
    public Map<Long, Boolean> isSubsumed( final Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked,rawtypes
        Set<Long> subsumedIds = new HashSet<>( this.getSessionFactory().getCurrentSession()
                .createQuery( "select ad.id from ArrayDesign as ad where ad.subsumingArrayDesign.id is not null and ad.id in (:ids)" )
                .setParameterList( "ids", optimizeParameterList( ids ) ).list() );
        return ids.stream().distinct().collect( Collectors.toMap( id -> id, subsumedIds::contains ) );
    }

    @Override
    public Map<Long, Boolean> isSubsumer( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked,rawtypes
        Set<Long> subsumerIds = new HashSet<>( this.getSessionFactory().getCurrentSession()
                .createQuery( "select ad.id from ArrayDesign as ad join ad.subsumedArrayDesigns subs where ad.id in (:ids) group by ad" )
                .setParameterList( "ids", optimizeParameterList( ids ) ).list() );
        return ids.stream().distinct().collect( Collectors.toMap( id -> id, subsumerIds::contains ) );
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
                .setFirstResult( offset ).setMaxResults( limit ).list();
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
    protected void postProcessValueObjects( List<ArrayDesignValueObject> results ) {
        StopWatch timer = StopWatch.createStarted();
        populateIsMerged( results );
        populateBlacklisted( results );
        populateExpressionExperimentCount( results );
        populateSwitchedExpressionExperimentCount( results );
        populateExternalReferences( results );
        if ( timer.getTime( TimeUnit.MILLISECONDS ) > 100 ) {
            log.warn( String.format( "Populating %d ArrayDesign VOs took %d ms.", results.size(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }
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
    public long numAllCompositeSequenceWithBioSequences() {
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " where cs.biologicalCharacteristic.sequence is not null";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString ).uniqueResult();
    }

    @Override
    public long numAllCompositeSequenceWithBioSequences( Collection<Long> ids ) {

        if ( ids.isEmpty() )
            return 0L;

        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " where ar.id in (:ids) and cs.biologicalCharacteristic.sequence is not null";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", optimizeParameterList( ids ) ).uniqueResult();
    }

    @Override
    public long numAllCompositeSequenceWithBlatResults() {
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BlatResult as blat where blat.querySequence=cs.biologicalCharacteristic";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString ).uniqueResult();
    }

    @Override
    public long numAllCompositeSequenceWithBlatResults( Collection<Long> ids ) {
        if ( ids.isEmpty() ) {
            return 0;
        }
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + ", BlatResult as blat where blat.querySequence != null and ar.id in (:ids)";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", optimizeParameterList( ids ) ).uniqueResult();
    }

    @Override
    public long numAllCompositeSequenceWithGenes() {
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + ", BioSequence2GeneProduct bs2gp, Gene gene inner join gene.products gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and bs2gp.geneProduct=gp";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString ).uniqueResult();
    }

    @Override
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
                .setParameterList( "ids", optimizeParameterList( ids ) ).uniqueResult();
    }

    @Override
    public long numAllGenes() {
        //language=HQL
        final String queryString =
                "select count (distinct gene) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + ", BioSequence2GeneProduct bs2gp, Gene gene inner join gene.products gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and  bs2gp.geneProduct=gp";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString ).uniqueResult();
    }

    @Override
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
                .setParameterList( "ids", optimizeParameterList( ids ) ).uniqueResult();
    }

    @Override
    public long numBioSequences( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString =
                "select count (distinct cs.biologicalCharacteristic) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " where ar = :ar and cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).uniqueResult();
    }

    @Override
    public long numBlatResults( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString =
                "select count (distinct bs2gp) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BioSequence2GeneProduct as bs2gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and ar = :ar";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).uniqueResult();
    }

    @Override
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
                .setParameter( "ar", arrayDesign ).uniqueResult();
    }

    @Override
    public long numCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BlatResult as blat where blat.querySequence=cs.biologicalCharacteristic and ar = :ar";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).uniqueResult();
    }

    @Override
    public long numCompositeSequenceWithGenes( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BioSequence2GeneProduct bs2gp, Gene gene inner join gene.products gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                        + "bs2gp.geneProduct=gp and ar = :ar";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).uniqueResult();
    }

    @Override
    public long numExperiments( ArrayDesign arrayDesign ) {
        //language=HQL
        final String queryString = "select count(distinct ee.id) from ExpressionExperiment ee "
                + "inner join ee.bioAssays bas "
                + "join bas.arrayDesignUsed ad "
                + AclQueryUtils.formAclRestrictionClause( "ee.id" ) + " "
                + "and ad = :ad";
        Query query = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "ad", arrayDesign );
        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        return ( Long ) query.uniqueResult();
    }

    @Override
    public long numGenes( ArrayDesign arrayDesign ) {
        return ( ( BigInteger ) getSessionFactory().getCurrentSession().createSQLQuery(
                        "select count(distinct g2cs.GENE) from GENE2CS g2cs "
                                + "where g2cs.AD = :arrayDesignId" )
                .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .addSynchronizedEntityClass( CompositeSequence.class )
                .addSynchronizedEntityClass( Gene.class )
                .setParameter( "arrayDesignId", arrayDesign.getId() )
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
    public void removeBiologicalCharacteristics( ArrayDesign arrayDesign ) {
        Session session = this.getSessionFactory().getCurrentSession();
        arrayDesign = ( ArrayDesign ) session.get( ArrayDesign.class, arrayDesign.getId() );
        int count = 0;
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            cs.setBiologicalCharacteristic( null );
            if ( ++count % ArrayDesignDaoImpl.LOGGING_UPDATE_EVENT_COUNT == 0 ) {
                log.info( "Cleared sequence association for " + count + " composite sequences" );
            }
        }
    }

    @Override
    public void thawLite( ArrayDesign arrayDesign ) {
        Hibernate.initialize( arrayDesign.getSubsumedArrayDesigns() );
        Hibernate.initialize( arrayDesign.getMergees() );
        Hibernate.initialize( arrayDesign.getDesignProvider() );
        Hibernate.initialize( arrayDesign.getAuditTrail() );
        Hibernate.initialize( arrayDesign.getAuditTrail().getEvents() );
        Hibernate.initialize( arrayDesign.getExternalReferences() );
        Hibernate.initialize( arrayDesign.getSubsumingArrayDesign() );
        Hibernate.initialize( arrayDesign.getMergedInto() );
        Hibernate.initialize( arrayDesign.getAlternativeTo() );
    }

    @Override
    public void thaw( final ArrayDesign arrayDesign ) {
        if ( arrayDesign.getId() == null ) {
            throw new IllegalArgumentException( "Cannot thaw a non-persistent array design" );
        }

        /*
         * Thaw basic stuff
         */
        StopWatch timer = StopWatch.createStarted();
        this.thawLite( arrayDesign );

        // Thaw the composite sequences
        StopWatch probeTimer = StopWatch.createStarted();
        Hibernate.initialize( arrayDesign.getCompositeSequences() );
        probeTimer.stop();

        String message = String.format( "Thaw array design took %d ms (metadata: %d ms, probes: %d ms)",
                timer.getTime(), timer.getTime() - probeTimer.getTime(), probeTimer.getTime() );
        if ( timer.getTime() > 1000 ) {
            log.warn( message );
        } else {
            log.debug( message );
        }
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
            log.info(
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
            log.info(
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

        log.info( "Subsumer " + candidateSubsumer + " contains " + overlap + "/" + subsumeeSeqs.size()
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
            log.info( candidateSubsumee + " and " + candidateSubsumer + " are apparently exactly equivalent" );
        } else {
            log.info( candidateSubsumer + " subsumes " + candidateSubsumee );
        }
        candidateSubsumer.getSubsumedArrayDesigns().add( candidateSubsumee );
        candidateSubsumee.setSubsumingArrayDesign( candidateSubsumer );

        this.update( candidateSubsumer );
        getSessionFactory().getCurrentSession().flush();

        this.update( candidateSubsumee );
        getSessionFactory().getCurrentSession().flush();

        return true;
    }

    @Override
    protected Query getFilteringQuery( @Nullable Filters filters, @Nullable Sort sort ) {
        //language=HQL
        return finishFilteringQuery( "select ad "
                + "from ArrayDesign as ad "
                + "left join fetch ad.curationDetails " + CURATION_DETAILS_ALIAS + " "
                + "left join fetch ad.primaryTaxon " + PRIMARY_TAXON_ALIAS + " "
                + "left join fetch ad.mergedInto m "
                + "left join fetch s.lastNeedsAttentionEvent as eAttn "
                + "left join fetch s.lastNoteUpdateEvent as eNote "
                + "left join fetch s.lastTroubledEvent as eTrbl "
                + "left join fetch ad.alternativeTo alt", filters, sort, groupByIfNecessary( sort, EXTERNAL_REFERENCE_ALIAS ) );
    }

    @Override
    protected void initializeCachedFilteringResult( ArrayDesign ad ) {
        Hibernate.initialize( ad.getCurationDetails() );
        Hibernate.initialize( ad.getPrimaryTaxon() );
        Hibernate.initialize( ad.getMergedInto() );
        Hibernate.initialize( ad.getAlternativeTo() );
    }

    @Override
    protected Query getFilteringIdQuery( @Nullable Filters filters, @Nullable Sort sort ) {
        //language=HQL
        return finishFilteringQuery(
                "select ad.id "
                        + "from ArrayDesign as ad "
                        + "left join ad.curationDetails " + CURATION_DETAILS_ALIAS + " "
                        + "left join ad.primaryTaxon " + PRIMARY_TAXON_ALIAS + " "
                        + "left join ad.mergedInto m "
                        + "left join s.lastNeedsAttentionEvent as eAttn "
                        + "left join s.lastNoteUpdateEvent as eNote "
                        + "left join s.lastTroubledEvent as eTrbl "
                        + "left join ad.alternativeTo alt", filters, sort, groupByIfNecessary( sort, EXTERNAL_REFERENCE_ALIAS ) );
    }

    @Override
    protected Query getFilteringCountQuery( @Nullable Filters filters ) {
        //language=HQL
        return finishFilteringQuery( "select count(" + distinctIfNecessary() + "ad) "
                + "from ArrayDesign as ad "
                + "left join ad.curationDetails " + CURATION_DETAILS_ALIAS + " "
                + "left join ad.primaryTaxon " + PRIMARY_TAXON_ALIAS + " "
                + "left join ad.mergedInto m "
                + "left join s.lastNeedsAttentionEvent as eAttn "
                + "left join s.lastNoteUpdateEvent as eNote "
                + "left join s.lastTroubledEvent as eTrbl "
                + "left join ad.alternativeTo alt", filters, null, null );
    }

    private Query finishFilteringQuery( String queryString, @Nullable Filters filters, @Nullable Sort sort, @Nullable String groupBy ) {
        if ( filters == null ) {
            filters = Filters.empty();
        } else {
            filters = Filters.by( filters );
        }

        if ( FiltersUtils.containsAnyAlias( null, sort, EXTERNAL_REFERENCE_ALIAS ) ) {
            queryString += " left join ad.externalReferences as " + EXTERNAL_REFERENCE_ALIAS;
        }

        // Restrict to non-troubled ADs for non-administrators
        addNonTroubledFilter( filters, OBJECT_ALIAS );

        queryString += AclQueryUtils.formAclRestrictionClause( OBJECT_ALIAS + ".id" );
        queryString += FilterQueryUtils.formRestrictionClause( filters );
        if ( groupBy != null ) {
            queryString += " group by " + groupBy;
        }
        queryString += FilterQueryUtils.formOrderByClause( sort );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        AclQueryUtils.addAclParameters( query, ArrayDesign.class );
        FilterQueryUtils.addRestrictionParameters( query, filters );

        return query;
    }

    @Override
    protected void configureFilterableProperties( FilterablePropertiesConfigurer configurer ) {
        super.configureFilterableProperties( configurer );
        configurer.registerProperty( "taxon" );
        // this is not useful, unless we add an alias to the alternate names
        configurer.unregisterProperties( p -> p.endsWith( "alternateNames.size" ) );
        // reserved for curators
        configurer.unregisterProperties( p -> p.endsWith( "curationDetails.curationNote" ) );
        configurer.unregisterProperties( p -> p.endsWith( "externalDatabases.size" ) );
        // because the ArrayDesign is the root property, and we allow at most 3 level, some of the recursive properties
        // (i.e. referring to another AD) will properties in a bunch of useless prefix such as mergedInto.mergedInto. To
        // disallow this, we remove those properties.
        // see https://github.com/PavlidisLab/Gemma/issues/546
        String recursiveProperty = String.join( "|", new String[] { "subsumingArrayDesign", "mergedInto", "alternativeTo" } );
        configurer.unregisterProperties( Pattern.compile( "^(" + recursiveProperty + ")\\.(" + recursiveProperty + ")\\..+$" ).asPredicate() );
        configurer.registerAlias( "externalReferences.", EXTERNAL_REFERENCE_ALIAS, DatabaseEntry.class, null, 2, true );
        configurer.registerAlias( "taxon.", PRIMARY_TAXON_ALIAS, Taxon.class, "primaryTaxon", 2 );
    }

    @Override
    protected FilterablePropertyMeta getFilterablePropertyMeta( String propertyName ) {
        // handle cases such as taxon = 1
        if ( propertyName.equals( "taxon" ) ) {
            return getFilterablePropertyMeta( PRIMARY_TAXON_ALIAS, "id", Taxon.class )
                    .withDescription( "alias for taxon.id" );
        }
        return super.getFilterablePropertyMeta( propertyName );
    }

    private void populateExternalReferences( Collection<ArrayDesignValueObject> results ) {
        if ( results.isEmpty() ) {
            return;
        }
        //noinspection unchecked
        List<Object[]> r = getSessionFactory().getCurrentSession()
                .createQuery( "select ad.id, e from ArrayDesign ad join ad.externalReferences e where ad.id in :ids" )
                .setParameterList( "ids", optimizeParameterList( EntityUtils.getIds( results ) ) )
                .setCacheable( true )
                .list();
        Map<Long, Set<DatabaseEntry>> dbi = r.stream()
                .collect( Collectors.groupingBy(
                        row -> ( Long ) row[0],
                        Collectors.mapping( row -> ( DatabaseEntry ) row[1], Collectors.toSet() ) ) );
        for ( ArrayDesignValueObject r2 : results ) {
            r2.setExternalReferences( dbi.getOrDefault( r2.getId(), Collections.emptySet() ).stream().map( DatabaseEntryValueObject::new ).collect( Collectors.toSet() ) );
        }
    }

    private void populateIsMerged( Collection<ArrayDesignValueObject> results ) {
        Map<Long, Boolean> isMergedByArrayDesignId = isMerged( EntityUtils.getIds( results ) );
        for ( ArrayDesignValueObject advo : results ) {
            advo.setIsMerged( isMergedByArrayDesignId.get( advo.getId() ) );
        }
    }

    private void populateBlacklisted( Collection<ArrayDesignValueObject> vos ) {
        Set<String> blacklistedShortnames = getBlacklistedShortNames();
        Set<String> blacklistedAccessions = getBlacklistedAccessions();
        for ( ArrayDesignValueObject vo : vos ) {
            boolean usesBlacklistedShortName = blacklistedShortnames.contains( vo.getShortName() );
            boolean usesBlacklistedAccession = vo.getExternalReferences() != null && vo.getExternalReferences().stream()
                    .map( DatabaseEntryValueObject::getAccession )
                    .anyMatch( blacklistedAccessions::contains );
            vo.setBlackListed( usesBlacklistedShortName || usesBlacklistedAccession );
        }
    }

    private void populateExpressionExperimentCount( Collection<ArrayDesignValueObject> entities ) {
        Query query = this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "select ee2ad.ARRAY_DESIGN_FK as ID, count(distinct ee2ad.EXPRESSION_EXPERIMENT_FK) as EE_COUNT from EXPRESSION_EXPERIMENT2ARRAY_DESIGN ee2ad "
                        + EE2CAclQueryUtils.formNativeAclJoinClause( "ee2ad.EXPRESSION_EXPERIMENT_FK" ) + " "
                        + "where ee2ad.ARRAY_DESIGN_FK in :ids "
                        + "and not ee2ad.IS_ORIGINAL_PLATFORM"
                        + EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(), "ee2ad.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" )
                        + formNativeNonTroubledClause( "ee2ad.EXPRESSION_EXPERIMENT_FK", ExpressionExperiment.class )
                        + " group by ee2ad.ARRAY_DESIGN_FK" )
                .addScalar( "ID", StandardBasicTypes.LONG )
                .addScalar( "EE_COUNT", StandardBasicTypes.LONG )
                // ensures that the cache is invalidated when the ee2ad table is regenerated
                .addSynchronizedQuerySpace( EE2AD_QUERY_SPACE )
                // ensures that the cache is invalidated when EEs or ADs are added/removed
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .setCacheable( true );
        EE2CAclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        Map<Long, Long> countById = QueryUtils.streamByBatch( query, "ids", EntityUtils.getIds( entities ), 2048, Object[].class )
                .collect( Collectors.toMap( o -> ( Long ) o[0], o -> ( Long ) o[1] ) );
        for ( ArrayDesignValueObject vo : entities ) {
            // missing implies no EEs, so zero is a valid default
            vo.setExpressionExperimentCount( countById.getOrDefault( vo.getId(), 0L ) );
        }
    }

    private void populateSwitchedExpressionExperimentCount( Collection<ArrayDesignValueObject> entities ) {
        Query query = this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "select ee2ad.ARRAY_DESIGN_FK as ID, count(distinct ee2ad.EXPRESSION_EXPERIMENT_FK) as EE_COUNT from EXPRESSION_EXPERIMENT2ARRAY_DESIGN ee2ad "
                        + EE2CAclQueryUtils.formNativeAclJoinClause( "ee2ad.EXPRESSION_EXPERIMENT_FK" ) + " "
                        + "where ee2ad.ARRAY_DESIGN_FK in :ids "
                        + "and ee2ad.IS_ORIGINAL_PLATFORM "
                        // ignore noop switches
                        + "and ee2ad.ARRAY_DESIGN_FK not in (select ARRAY_DESIGN_FK from EXPRESSION_EXPERIMENT2ARRAY_DESIGN where EXPRESSION_EXPERIMENT_FK = ee2ad.EXPRESSION_EXPERIMENT_FK and ARRAY_DESIGN_FK = ee2ad.ARRAY_DESIGN_FK and not IS_ORIGINAL_PLATFORM)"
                        + EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(), "ee2ad.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" )
                        + formNativeNonTroubledClause( "ee2ad.EXPRESSION_EXPERIMENT_FK", ExpressionExperiment.class )
                        + " group by ee2ad.ARRAY_DESIGN_FK" )
                .addScalar( "ID", StandardBasicTypes.LONG )
                .addScalar( "EE_COUNT", StandardBasicTypes.LONG )
                // ensures that the cache is invalidated when the ee2ad table is regenerated
                .addSynchronizedQuerySpace( EE2AD_QUERY_SPACE )
                // ensures that the cache is invalidated when EEs or ADs are added/removed
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .setCacheable( true );
        EE2CAclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        Map<Long, Long> switchedCountById = QueryUtils.streamByBatch( query, "ids", EntityUtils.getIds( entities ), 2048, Object[].class )
                .collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Long ) row[1] ) );
        for ( ArrayDesignValueObject vo : entities ) {
            // missing implies no switched EEs, so zero is a valid default
            vo.setSwitchedExpressionExperimentCount( switchedCountById.getOrDefault( vo.getId(), 0L ) );
        }
    }

    private Set<String> getBlacklistedShortNames() {
        return getBlacklistedShortNamesAndAccessions().stream().map( row -> ( String ) row[0] )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
    }

    private Set<String> getBlacklistedAccessions() {
        return getBlacklistedShortNamesAndAccessions().stream()
                .map( row -> ( String ) row[1] )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
    }

    private List<Object[]> getBlacklistedShortNamesAndAccessions() {
        //noinspection unchecked
        return ( List<Object[]> ) getSessionFactory().getCurrentSession()
                .createQuery( "select bp.shortName, ea.accession from BlacklistedPlatform bp left join bp.externalAccession ea" )
                .setCacheable( true )
                .list();
    }
}