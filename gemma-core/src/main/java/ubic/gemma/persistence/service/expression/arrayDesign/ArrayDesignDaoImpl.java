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
import org.hibernate.collection.PersistentCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AbstractCuratableDao;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.CommonQueries;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @author pavlidis
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
@Repository
public class ArrayDesignDaoImpl extends AbstractCuratableDao<ArrayDesign, ArrayDesignValueObject>
        implements ArrayDesignDao {

    private static final int LOGGING_UPDATE_EVENT_COUNT = 5000;


    @Autowired
    public ArrayDesignDaoImpl( SessionFactory sessionFactory ) {
        super( ArrayDesign.class, sessionFactory );
    }

    @Override
    public ArrayDesign find( ArrayDesign entity ) {
        BusinessKey.checkValidKey( entity );
        Criteria query = super.getSessionFactory().getCurrentSession().createCriteria( ArrayDesign.class );
        BusinessKey.addRestrictions( query, entity );

        return ( ArrayDesign ) query.uniqueResult();
    }

    @Override
    public Map<Taxon, Long> getPerTaxonCount() {
        Map<Taxon, Long> result = new HashMap<Taxon, Long>();

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

    @Override
    public void remove( ArrayDesign arrayDesign ) {
        arrayDesign = this.load( arrayDesign.getId() );
        //getSession().buildLockRequest( LockOptions.NONE ).lock( arrayDesign );
        Hibernate.initialize( arrayDesign.getMergees() );
        Hibernate.initialize( arrayDesign.getSubsumedArrayDesigns() );
        arrayDesign.getMergees().clear();
        arrayDesign.getSubsumedArrayDesigns().clear();

        Iterator<CompositeSequence> iterator = arrayDesign.getCompositeSequences().iterator();
        while ( iterator.hasNext() ) {
            CompositeSequence cs = iterator.next();
            iterator.remove();
            getSession().delete( cs );
        }

        getSession().delete( arrayDesign );
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
    public ArrayDesign find( String queryString, ArrayDesign entity ) {
        return ( ArrayDesign ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "arrayDesign", entity ).setParameter( "name", entity.getName() ).uniqueResult();
    }

    @Override
    public Collection<ArrayDesign> findByManufacturer( String queryString ) {
        if ( StringUtils.isBlank( queryString ) ) {
            return new HashSet<ArrayDesign>();
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

        Map<CompositeSequence, BioSequence> bioSequences = new HashMap<CompositeSequence, BioSequence>();
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
    public Map<CompositeSequence, Collection<BlatResult>> loadAlignments( ArrayDesign arrayDesign ) {
        //noinspection unchecked,JpaQlInspection - blatResult not visible because it is in a sub-class (BlatAssociation)
        List<Object[]> m = this.getSessionFactory().getCurrentSession().createQuery(
                "select cs, br from CompositeSequence cs "
                        + " join cs.biologicalCharacteristic bs join bs.bioSequence2GeneProduct bs2gp"
                        + " join bs2gp.blatResult br "
                        + "  where bs2gp.class='BlatAssociation' and cs.arrayDesign=:ad" )
                .setParameter( "ad", arrayDesign ).list();

        Map<CompositeSequence, Collection<BlatResult>> result = new HashMap<CompositeSequence, Collection<BlatResult>>();
        for ( Object[] objects : m ) {
            CompositeSequence cs = ( CompositeSequence ) objects[0];
            BlatResult br = ( BlatResult ) objects[1];
            if ( !result.containsKey( cs ) ) {
                result.put( cs, new HashSet<BlatResult>() );
            }
            result.get( cs ).add( br );
        }

        return result;
    }

    @Override
    @Transactional
    public int numExperiments( ArrayDesign arrayDesign ) {
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
    public ArrayDesign thawLite( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "array design cannot be null" );
        }
        List res = this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct a from ArrayDesign a left join fetch a.subsumedArrayDesigns "
                        + " left join fetch a.mergees left join fetch a.designProvider left join fetch a.primaryTaxon "
                        + " join fetch a.auditTrail trail join fetch trail.events join fetch a.curationDetails left join fetch a.externalReferences"
                        + " left join fetch a.subsumingArrayDesign left join fetch a.mergedInto left join fetch a.localFiles where a.id=:adid" )
                .setParameter( "adid", arrayDesign.getId() ).list();

        if ( res.size() == 0 ) {
            throw new IllegalArgumentException(
                    "No array design with id=" + arrayDesign.getId() + " could be loaded." );
        }

        return ( ArrayDesign ) res.get( 0 );
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
                        + " left join fetch a.mergedInto left join fetch a.localFiles where a.id in (:adids)" )
                .setParameterList( "adids", EntityUtils.getIds( arrayDesigns ) ).list();

    }

    @Override
    public Collection<CompositeSequence> compositeSequenceWithoutBioSequences( ArrayDesign arrayDesign ) {
        final String queryString = "select distinct cs from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                + " left join cs.biologicalCharacteristic bs where ar = :ar and bs IS NULL";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ar", arrayDesign )
                .list();
    }

    @Override
    public Collection<CompositeSequence> compositeSequenceWithoutBlatResults( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
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
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();

        final String nativeQueryString = "SELECT DISTINCT cs.id FROM "
                + "COMPOSITE_SEQUENCE cs LEFT JOIN BIO_SEQUENCE2_GENE_PRODUCT bs2gp ON BIO_SEQUENCE_FK=BIOLOGICAL_CHARACTERISTIC_FK "
                + "LEFT JOIN CHROMOSOME_FEATURE geneProduct ON (geneProduct.ID=bs2gp.GENE_PRODUCT_FK AND geneProduct.class='GeneProductImpl') "
                + "LEFT JOIN CHROMOSOME_FEATURE gene ON geneProduct.GENE_FK=gene.ID  "
                + "WHERE gene.ID IS NULL AND ARRAY_DESIGN_FK = :id";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createSQLQuery( nativeQueryString ).setParameter( "id", id )
                .list();
    }

    @Override
    public void deleteAlignmentData( ArrayDesign arrayDesign ) {
        // First have to remove all blatAssociations, because they are referred to by the alignments
        deleteGeneProductAssociations( arrayDesign );

        // Note attempts to do this with bulk updates were unsuccessful due to the need for joins.
        final String queryString = "select br from ArrayDesign ad join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, BlatResultImpl br "
                + "where br.querySequence = bs and ad=:arrayDesign";
        //noinspection unchecked
        List<BlatResult> toDelete = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "arrayDesign", arrayDesign ).list();

        log.info( "Deleting " + toDelete + " alignments for sequences on " + arrayDesign
                + " (will affect other designs that use any of the same sequences)" );

        for ( BlatResult r : toDelete ) {
            this.getSessionFactory().getCurrentSession().delete( r );
        }

    }

    @Override
    public void deleteGeneProductAssociations( ArrayDesign arrayDesign ) {

        this.getSessionFactory().getCurrentSession().buildLockRequest( LockOptions.UPGRADE )
                .setLockMode( LockMode.PESSIMISTIC_WRITE ).lock( arrayDesign );

        // this query is polymorphic, id gets the annotation associations?
        final String queryString = "select ba from CompositeSequence  cs "
                + "inner join cs.biologicalCharacteristic bs, BioSequence2GeneProduct ba "
                + "where ba.bioSequence = bs and cs.arrayDesign=:arrayDesign";
        List blatAssociations = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "arrayDesign", arrayDesign ).list();
        if ( !blatAssociations.isEmpty() ) {
            for ( Object r : blatAssociations ) {
                this.getSessionFactory().getCurrentSession().delete( r );
            }
            log.info( "Done deleting " + blatAssociations.size() + " blat associations for " + arrayDesign );
        }

        this.getSessionFactory().getCurrentSession().flush();

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
            log.info( "Done deleting " + annotAssociations.size() + " AnnotationAssociations for " + arrayDesign );

        }
    }

    @Override
    public Collection<ArrayDesign> findByAlternateName( String queryString ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select ad from ArrayDesign ad inner join ad.alternateNames n where n.name = :q" )
                .setParameter( "q", queryString ).list();
    }

    @Override
    public Collection<BioAssay> getAllAssociatedBioAssays( Long id ) {
        final String queryString = "select b from BioAssay as b inner join b.arrayDesignUsed a where a.id = :id";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "id", id ).list();
    }

    @Override
    public Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids ) {
        final String queryString = "select ad.id, auditEvent from ArrayDesign ad"
                + " join ad.auditTrail as auditTrail join auditTrail.events as auditEvent join fetch auditEvent.performer "
                + " where ad.id in (:ids) ";

        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();
        Map<Long, Collection<AuditEvent>> eventMap = new HashMap<Long, Collection<AuditEvent>>();
        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            AuditEvent event = ( AuditEvent ) o[1];

            addEventsToMap( eventMap, id, event );
        }
        // add in the array design ids that do not have events.
        // Set their values to null.
        for ( Object object : ids ) {
            Long id = ( Long ) object;
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, null );
            }
        }
        return eventMap;

    }

    @Override
    public Collection<ExpressionExperiment> getExpressionExperiments( ArrayDesign arrayDesign ) {
        final String queryString = "select distinct ee from "
                + " ExpressionExperiment ee inner join ee.bioAssays bas inner join bas.arrayDesignUsed ad where ad = :ad";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ad", arrayDesign )
                .list();
    }

    @Override
    public Collection<Taxon> getTaxa( Long id ) {

        final String queryString = "select distinct t from ArrayDesign as arrayD "
                + "inner join arrayD.compositeSequences as cs inner join " + "cs.biologicalCharacteristic as bioC"
                + " inner join bioC.taxon t where arrayD.id = :id";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "id", id ).list();
    }

    @Override
    public Taxon getTaxon( Long id ) {
        Collection<Taxon> taxon = getTaxa( id );
        if ( taxon.size() == 0 ) {
            log.warn( "No taxon found for array " + id );
            return null; // print warning
        }

        if ( taxon.size() > 1 ) {
            log.warn( taxon.size() + " taxon found for array " + id );
        }
        return taxon.iterator().next();
    }

    @Override
    public Map<Long, Boolean> isMerged( Collection<Long> ids ) {

        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, count(subs) from ArrayDesign as ad left join ad.mergees subs where ad.id in (:ids) group by ad";
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        putIdsInList( eventMap, list );
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }

        return eventMap;
    }

    @Override
    public Map<Long, Boolean> isMergee( final Collection<Long> ids ) {

        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, ad.mergedInto from ArrayDesign as ad where ad.id in (:ids) ";
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        putIdsInListCheckMerger( eventMap, list );
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }

        return eventMap;
    }

    @Override
    public Map<Long, Boolean> isSubsumed( final Collection<Long> ids ) {
        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, ad.subsumingArrayDesign from ArrayDesign as ad where ad.id in (:ids) ";
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        putIdsInListCheckMerger( eventMap, list );
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }
        return eventMap;
    }

    @Override
    public Map<Long, Boolean> isSubsumer( Collection<Long> ids ) {

        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, count(subs) from ArrayDesign as ad inner join ad.subsumedArrayDesigns subs where ad.id in (:ids) group by ad";
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        putIdsInList( eventMap, list );
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }
        return eventMap;
    }

    /**
     * Loads a single value objects for the given array design.
     *
     * @param e the array design to be converted to a value object
     * @return a value object with properties of the given array design.
     */
    @Override
    public ArrayDesignValueObject loadValueObject( ArrayDesign e ) {
        return this.loadValueObjectsByIds( Collections.singleton( e.getId() ) ).iterator().next();
    }

    /**
     * This method is extremely ineffective for Array Designs.
     *
     * @deprecated Use {@link #loadValueObjectsByIds(Collection)} instead if possible.
     */
    @Override
    @Deprecated
    public Collection<ArrayDesignValueObject> loadValueObjects( Collection<ArrayDesign> entities ) {
        throw new RuntimeException("DO NOT USE THIS METHOD FOR ADs - use the -ByIds methods instead");
//        Collection<ArrayDesignValueObject> vos = new LinkedHashSet<ArrayDesignValueObject>();
//        for ( ArrayDesign ad : entities ) {
//            vos.add( new ArrayDesignValueObject( ad ) );
//        }
//        return vos;
    }

    /**
     * Loads value objects for all array designs, and populates the EE counts.
     * @return
     */
    @Override
    public Collection<ArrayDesignValueObject> loadAllValueObjects() {

        Map<Long, Integer> eeCounts = this.getExpressionExperimentCountMap();
        final String queryString = this.getEEValueObjectQueryString();
        Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );

        return processADValueObjectQueryResults( eeCounts, queryObject );

    }

    @Override
    public Collection<ArrayDesignValueObject> loadValueObjectsByIds( Collection<Long> ids ) {
        // sanity check
        if ( ids == null ) {
            return new ArrayList<ArrayDesignValueObject>();
        }

        Map<Long, Integer> eeCounts = this.getExpressionExperimentCountMap( ids );
        final String queryString = this.getEEValueObjectQueryString() + "where ad.id in (:ids)  ";
        Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( queryString );
        queryObject.setParameterList( "ids", ids );

        return processADValueObjectQueryResults( eeCounts, queryObject );
    }

    @Override
    public Collection<ArrayDesignValueObject> loadValueObjectsForEE( Long eeId ) {
        // sanity check
        if ( eeId == null ) {
            return new ArrayList<ArrayDesignValueObject>();
        }
        Collection<Long> ids = CommonQueries
                .getArrayDesignIdsUsed( eeId, this.getSessionFactory().getCurrentSession() );
        final String queryString = this.getEEValueObjectQueryString() + "where ad.id in (:ids)  ";
        Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( queryString );
        queryObject.setParameterList( "ids", ids );

        return processADValueObjectQueryResults( null, queryObject );
    }

    @Override
    @Transactional
    public long numAllCompositeSequenceWithBioSequences() {
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " where cs.biologicalCharacteristic.sequence is not null";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString ).list().iterator()
                .next();
    }

    @Override
    @Transactional
    public long numAllCompositeSequenceWithBioSequences( Collection<Long> ids ) {
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " where ar.id in (:ids) and cs.biologicalCharacteristic.sequence is not null";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numAllCompositeSequenceWithBlatResults() {
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BlatResultImpl as blat where blat.querySequence=cs.biologicalCharacteristic";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString ).list().iterator()
                .next();
    }

    @Override
    @Transactional
    public long numAllCompositeSequenceWithBlatResults( Collection<Long> ids ) {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + ", BlatResultImpl as blat where blat.querySequence != null and ar.id in (:ids)";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numAllCompositeSequenceWithGenes() {
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
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
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
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
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
        final String queryString =
                "select count (distinct cs.biologicalCharacteristic) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " where ar = :ar and cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numBlatResults( ArrayDesign arrayDesign ) {
        final String queryString =
                "select count (distinct bs2gp) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BioSequence2GeneProduct as bs2gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and ar = :ar";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numCompositeSequences( Long id ) {
        final String queryString = "select count (*) from  CompositeSequence as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return ( ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "id", id ).list().iterator().next() ).intValue();
    }

    @Override
    public long numCompositeSequenceWithBioSequences( ArrayDesign arrayDesign ) {
        final String queryString =
                "select count (distinct cs) from CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " where ar = :ar and cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) {
        final String queryString =
                "select count (distinct cs) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + " , BlatResultImpl as blat where blat.querySequence=cs.biologicalCharacteristic and ar = :ar";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).list().iterator().next();
    }

    @Override
    @Transactional
    public long numCompositeSequenceWithGenes( ArrayDesign arrayDesign ) {
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
    public long numGenes( ArrayDesign arrayDesign ) {
        final String queryString =
                "select count (distinct gene) from  CompositeSequence as cs inner join cs.arrayDesign as ar "
                        + ", BioSequence2GeneProduct bs2gp, Gene gene inner join gene.products gp "
                        + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                        + "bs2gp.geneProduct=gp and ar = :ar";
        return ( Long ) this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ar", arrayDesign ).list().iterator().next();
    }

    @Override
    public void removeBiologicalCharacteristics( final ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "Array design cannot be null" );
        }
        Session session = this.getSessionFactory().getCurrentSession();
        session.buildLockRequest( LockOptions.NONE ).lock( arrayDesign );

        int count = 0;
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            cs.setBiologicalCharacteristic( null );
            session.update( cs );
            session.evict( cs );
            if ( ++count % LOGGING_UPDATE_EVENT_COUNT == 0 ) {
                log.info( "Cleared sequence association for " + count + " composite sequences" );
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

        ArrayDesign result = thawLite( arrayDesign );

        if ( timer.getTime() > 1000 ) {
            log.info( "Thaw array design stage 1: " + timer.getTime() + "ms" );
        }

        timer.stop();
        timer.reset();
        timer.start();

        /*
         * Thaw the composite sequences.
         */
        log.info( "Start initialize composite sequences" );

        Hibernate.initialize( result.getCompositeSequences() );

        if ( timer.getTime() > 1000 ) {
            log.info( "Thaw array design stage 2: " + timer.getTime() + "ms" );
        }
        timer.stop();
        timer.reset();
        timer.start();

        /*
         * Thaw the biosequences in batches
         */
        Collection<CompositeSequence> thawed = new HashSet<CompositeSequence>();
        Collection<CompositeSequence> batch = new HashSet<CompositeSequence>();
        long lastTime = timer.getTime();
        for ( CompositeSequence cs : result.getCompositeSequences() ) {
            batch.add( cs );
            if ( batch.size() == 1000 ) {
                long t = timer.getTime();
                if ( t > 10000 && t - lastTime > 1000 ) {
                    log.info( "Thaw Batch : " + t );
                }
                List bb = thawBatchOfProbes( batch );
                //noinspection unchecked
                thawed.addAll( ( Collection<? extends CompositeSequence> ) bb );
                lastTime = timer.getTime();
                batch.clear();
            }
            this.getSessionFactory().getCurrentSession().evict( cs );
        }

        if ( !batch.isEmpty() ) { // tail end
            List bb = thawBatchOfProbes( batch );
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
            log.info( "Thaw array design stage 3: " + timer.getTime() );
        }

        return result;
    }

    @Override
    public Boolean updateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee ) {

        // Size does not automatically disqualify, because we only consider BioSequences that actually have
        // sequences in them.
        if ( candidateSubsumee.getCompositeSequences().size() > candidateSubsumer.getCompositeSequences().size() ) {
            log.info( "Subsumee has more sequences than subsumer so probably cannot be subsumed ... checking anyway" );
        }

        Collection<BioSequence> subsumerSeqs = new HashSet<BioSequence>();
        Collection<BioSequence> subsumeeSeqs = new HashSet<BioSequence>();

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
            log.info( "Subsumee has more sequences than subsumer so probably cannot be subsumed, checking overlap" );
        }

        int overlap = 0;
        List<BioSequence> missing = new ArrayList<BioSequence>();
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
            int n = 50;
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

        this.getSessionFactory().getCurrentSession().flush();
        this.getSessionFactory().getCurrentSession().clear();
        this.update( candidateSubsumee );
        this.getSessionFactory().getCurrentSession().flush();
        this.getSessionFactory().getCurrentSession().clear();

        return true;
    }

    @Override
    public Collection<CompositeSequence> loadCompositeSequences( Long id ) {
        final String queryString = "select cs from CompositeSequence as cs where cs.arrayDesign.id = :id";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "id", id ).list();
    }

    private void putIdsInList( Map<Long, Boolean> eventMap, List<Object[]> list ) {
        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Long mergeeCount = ( Long ) o[1];
            if ( mergeeCount != null && mergeeCount > 0 ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
    }

    private void putIdsInListCheckMerger( Map<Long, Boolean> eventMap, List<Object[]> list ) {
        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            ArrayDesign merger = ( ArrayDesign ) o[1];
            if ( merger != null ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
    }

    /**
     * Gets the number of expression experiments per ArrayDesign for all array designs.
     */
    private Map<Long, Integer> getExpressionExperimentCountMap() {
        final String queryString = "select ad.id, count(distinct ee) from   "
                + " ExpressionExperiment ee inner join ee.bioAssays bas inner join bas.arrayDesignUsed ad group by ad";

        Map<Long, Integer> eeCount = new HashMap<>();
        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).list();

        if ( list.size() < 2 )
            log.warn( list.size() + " rows from getExpressionExperimentCountMap query" );

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Integer count = ( ( Long ) o[1] ).intValue();
            eeCount.put( id, count );
        }

        return eeCount;
    }

    /**
     * Gets the number of expression experiments per ArrayDesign for specified array designs.
     */
    private Map<Long, Integer> getExpressionExperimentCountMap( Collection<Long> arrayDesignIds ) {

        Map<Long, Integer> result = new HashMap<Long, Integer>();

        if ( arrayDesignIds == null || arrayDesignIds.isEmpty() ) {
            return result;
        }
        final String queryString = "select ad.id, count(distinct ee) from   "
                + " ExpressionExperiment ee inner join ee.bioAssays bas inner join bas.arrayDesignUsed ad  where ad.id in (:ids) group by ad ";

        //noinspection unchecked
        List<Object[]> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", arrayDesignIds ).list();

        // Bug 1549: for unknown reasons, this method sometimes returns only a single record (or no records)
        if ( arrayDesignIds.size() > 1 && list.size() != arrayDesignIds.size() ) {
            log.info( list.size() + " rows from getExpressionExperimentCountMap query for " + arrayDesignIds.size()
                    + " ids" );
        }

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Integer count = ( ( Long ) o[1] ).intValue();
            result.put( id, count );
        }

        return result;
    }

    private String getEEValueObjectQueryString() {
        return "select " + "ad.id, " //0
                + "ad.name, " //1
                + "ad.shortName, " //2
                + "ad.technologyType, " //3
                + "ad.description, "  //4
                + "m, " //5
                + "s.lastUpdated, " //6
                + "s.troubled, "  //7
                + "s.needsAttention, " //8
                + "s.curationNote, "  //9
                + "t.commonName " //10
                + "from ArrayDesign ad join ad.curationDetails s join ad.primaryTaxon t left join ad.mergedInto m ";
    }

    /**
     * Process query results for LoadAllValueObjects or LoadValueObjects
     */
    private Collection<ArrayDesignValueObject> processADValueObjectQueryResults( Map<Long, Integer> eeCounts,
            final Query queryObject ) {
        Collection<ArrayDesignValueObject> result = new ArrayList<ArrayDesignValueObject>();

        queryObject.setCacheable( true );
        ScrollableResults list = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        if ( list != null ) {
            while ( list.next() ) {
                ArrayDesignValueObject v = new ArrayDesignValueObject( list.getLong( 0 ) );
                v.setName( list.getString( 1 ) );
                v.setShortName( list.getString( 2 ) );

                TechnologyType color = ( TechnologyType ) list.get( 3 );
                if ( color != null ) {
                    v.setTechnologyType( color.toString() );
                    v.setColor( color.getValue() );
                }

                v.setDescription( list.getString( 4 ) );
                v.setIsMergee( list.get( 5 ) != null );

                v.setLastUpdated( list.getDate( 6 ) );
                v.setTroubled( list.getBoolean( 7 ) );
                v.setNeedsAttention( list.getBoolean( 8 ) );
                v.setCurationNote( list.getString( 9 ) );

                v.setTaxon( list.getString( 10 ) );

                if ( eeCounts == null || !eeCounts.containsKey( v.getId() ) ) {
                    v.setExpressionExperimentCount( 0 );
                } else {
                    v.setExpressionExperimentCount( eeCounts.get( v.getId() ) );
                }

                //This was causing null results when being retrieved through the original query
                this.addCurationEvents( v );

                result.add( v );
            }
        }
        return result;
    }

    private List thawBatchOfProbes( Collection<CompositeSequence> batch ) {
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select cs from CompositeSequence cs left join fetch cs.biologicalCharacteristic where cs in (:batch)" )
                .setParameterList( "batch", batch ).list();
    }
}