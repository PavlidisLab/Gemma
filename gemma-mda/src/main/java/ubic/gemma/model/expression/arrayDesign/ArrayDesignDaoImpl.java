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
package ubic.gemma.model.expression.arrayDesign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.collection.PersistentCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.NativeQueryUtils;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
@Repository
public class ArrayDesignDaoImpl extends ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase {

    static Log log = LogFactory.getLog( ArrayDesignDaoImpl.class.getName() );
    private static final int LOGGING_UPDATE_EVENT_COUNT = 5000;

    @Autowired
    public ArrayDesignDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    public ArrayDesign arrayDesignValueObjectToEntity( ArrayDesignValueObject arrayDesignValueObject ) {
        Long id = arrayDesignValueObject.getId();
        return this.load( id );
    }

    /**
     * 
     */
    private void debug( List<? extends Object> results ) {
        for ( Object ad : results ) {
            log.error( ad );
        }

    }

    /**
     * @param arrayDesign
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private ArrayDesign doThaw( ArrayDesign arrayDesign ) throws Exception {

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
        long lastTime = 0;
        for ( CompositeSequence cs : result.getCompositeSequences() ) {
            batch.add( cs );
            if ( batch.size() == 1000 ) {
                lastTime = timer.getTime();
                if ( timer.getTime() > 10000 && timer.getTime() - lastTime > 10000 ) {
                    log.info( "Batch : " + timer.getTime() );
                }
                List<?> bb = thawBatchOfProbes( batch );
                thawed.addAll( ( Collection<? extends CompositeSequence> ) bb );

                batch.clear();
            }
        }

        if ( !batch.isEmpty() ) {

            List<?> bb = thawBatchOfProbes( batch );
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

    public ArrayDesign thawLite( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "array design cannot be null" );
        }
        List<?> res = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct a from ArrayDesignImpl a "
                                + "left join fetch a.subsumedArrayDesigns "
                                + " left join fetch a.mergees  left join fetch a.designProvider left join fetch a.primaryTaxon "
                                + " join fetch a.auditTrail trail join fetch trail.events left join fetch a.externalReferences"
                                + " left join fetch a.subsumedArrayDesigns left join fetch a.mergees left join fetch a.subsumingArrayDesign "
                                + " left join fetch a.mergedInto " + "where a.id=:adid", "adid", arrayDesign.getId() );

        if ( res.size() == 0 ) {
            throw new IllegalArgumentException( "No array design with id=" + arrayDesign.getId() + " could be loaded." );
        }
        ArrayDesign result = ( ArrayDesign ) res.iterator().next();

        return result;
    }

    @SuppressWarnings("unchecked")
    public Collection<ArrayDesign> thawLite( Collection<ArrayDesign> arrayDesigns ) {
        if ( arrayDesigns.isEmpty() ) return arrayDesigns;
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct a from ArrayDesignImpl a "
                                + "left join fetch a.subsumedArrayDesigns "
                                + " left join fetch a.mergees  left join fetch a.designProvider left join fetch a.primaryTaxon "
                                + " join fetch a.auditTrail trail join fetch trail.events left join fetch a.externalReferences"
                                + " left join fetch a.subsumedArrayDesigns left join fetch a.mergees left join fetch a.subsumingArrayDesign "
                                + " left join fetch a.mergedInto where a.id in (:adids)", "adids",
                        EntityUtils.getIds( arrayDesigns ) );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    @SuppressWarnings("unchecked")
    public ArrayDesign find( ArrayDesign arrayDesign ) {
        try {

            BusinessKey.checkValidKey( arrayDesign );
            Criteria queryObject = super.getSession().createCriteria( ArrayDesign.class );
            BusinessKey.addRestrictions( queryObject, arrayDesign );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    debug( results );
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException( results.size() + " "
                            + ArrayDesign.class.getName() + "s were found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( ArrayDesign ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findByManufacturer(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Collection<ArrayDesign> findByManufacturer( String queryString ) {
        if ( StringUtils.isBlank( queryString ) ) {
            return new HashSet<ArrayDesign>();
        }
        return this.getHibernateTemplate().find(
                "select ad from ArrayDesignImpl ad inner join ad.designProvider n where n.name like ?",
                queryString + "%" );

    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findOrCreate(ubic.gemma.model.expression.arrayDesign.
     * ArrayDesign)
     */
    @Override
    public ArrayDesign findOrCreate( ArrayDesign arrayDesign ) {
        ArrayDesign existingArrayDesign = this.find( arrayDesign );
        if ( existingArrayDesign != null ) {
            assert existingArrayDesign.getId() != null;
            return existingArrayDesign;
        }
        log.debug( "Creating new arrayDesign: " + arrayDesign.getName() );
        return create( arrayDesign );
    }

    /**
     * @return
     */
    private Map<Long, String> getArrayToPrimaryTaxonMap() {

        StopWatch timer = new StopWatch();
        timer.start();

        final String csString = "select  ad.primaryTaxon, ad from ArrayDesignImpl ad ";
        org.hibernate.Query csQueryObject = super.getSession().createQuery( csString );
        csQueryObject.setReadOnly( true );
        csQueryObject.setCacheable( true );

        List<?> csList = csQueryObject.list();

        Map<ArrayDesign, Collection<String>> raw = new HashMap<ArrayDesign, Collection<String>>();
        Taxon t = null;
        for ( Object object : csList ) {
            Object[] oa = ( Object[] ) object;
            t = ( Taxon ) oa[0];
            ArrayDesign ad = ( ArrayDesign ) oa[1];

            if ( !raw.containsKey( ad ) ) {
                raw.put( ad, new TreeSet<String>() );
            }

            if ( t.getCommonName() != null ) {
                raw.get( ad ).add( t.getCommonName() );
            }

        }

        Map<Long, String> arrayToTaxon = new HashMap<Long, String>();
        for ( ArrayDesign ad : raw.keySet() ) {
            String taxonListString = StringUtils.join( raw.get( ad ), "; " );
            arrayToTaxon.put( ad.getId(), taxonListString );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Get array design taxa: " + timer.getTime() + "ms" );
        }

        return arrayToTaxon;

    }

    /**
     * @param ids
     * @return
     */
    private Map<Long, String> getArrayToPrimaryTaxonMap( Collection<Long> ids ) {

        StopWatch timer = new StopWatch();
        timer.start();

        final String csString = "select  ad.primaryTaxon, ad from ArrayDesignImpl ad where ad.id in (:ids)";
        org.hibernate.Query csQueryObject = super.getSession().createQuery( csString );
        csQueryObject.setReadOnly( true );
        csQueryObject.setCacheable( true );

        csQueryObject.setParameterList( "ids", ids );

        List<?> csList = csQueryObject.list();

        Map<ArrayDesign, Collection<String>> raw = new HashMap<ArrayDesign, Collection<String>>();
        Taxon t = null;
        for ( Object object : csList ) {
            Object[] oa = ( Object[] ) object;
            t = ( Taxon ) oa[0];
            ArrayDesign ad = ( ArrayDesign ) oa[1];

            if ( !raw.containsKey( ad ) ) {
                raw.put( ad, new TreeSet<String>() );
            }

            if ( t.getCommonName() != null ) {
                raw.get( ad ).add( t.getCommonName() );
            }

        }

        Map<Long, String> arrayToTaxon = new HashMap<Long, String>();
        for ( ArrayDesign ad : raw.keySet() ) {
            String taxonListString = StringUtils.join( raw.get( ad ), "; " );
            arrayToTaxon.put( ad.getId(), taxonListString );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Get array design taxa: " + timer.getTime() + "ms" );
        }

        return arrayToTaxon;

    }

    /**
     * Gets the number of expression experiments per ArrayDesign
     * 
     * @return Map
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getExpressionExperimentCountMap() {
        final String queryString = "select ad.id, count(distinct ee) from   "
                + " ExpressionExperimentImpl ee inner join ee.bioAssays bas inner join bas.arrayDesignUsed ad group by ad";

        Map<Long, Integer> eeCount = new HashMap<Long, Integer>();
        List<Object[]> list = getHibernateTemplate().find( queryString );

        // Bug 1549: for unknown reasons, this method sometimes returns only a single record (or no records). Obviously
        // if we only have 1 array design this warning is spurious.
        if ( list.size() < 2 ) log.warn( list.size() + " rows from getExpressionExperimentCountMap query" );

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Integer count = ( ( Long ) o[1] ).intValue();
            eeCount.put( id, count );
        }

        return eeCount;
    }

    /**
     * Gets the number of expression experiments per ArrayDesign
     * 
     * @return Map
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Integer> getExpressionExperimentCountMap( Collection<Long> arrayDesignIds ) {

        Map<Long, Integer> result = new HashMap<Long, Integer>();

        if ( arrayDesignIds == null || arrayDesignIds.isEmpty() ) {
            return result;
        }
        final String queryString = "select ad.id, count(distinct ee) from   "
                + " ExpressionExperimentImpl ee inner join ee.bioAssays bas inner join bas.arrayDesignUsed ad  where ad.id in (:ids) group by ad ";

        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", arrayDesignIds );

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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#getPerTaxonCount()
     */
    public Map<Taxon, Integer> getPerTaxonCount() {
        Map<Taxon, Integer> result = new HashMap<Taxon, Integer>();

        final String csString = "select t, count(ad) from ArrayDesignImpl ad inner join ad.primaryTaxon t group by t ";
        org.hibernate.Query csQueryObject = super.getSession().createQuery( csString );
        csQueryObject.setReadOnly( true );
        csQueryObject.setCacheable( true );

        List<?> csList = csQueryObject.list();

        Taxon t = null;
        for ( Object object : csList ) {
            Object[] oa = ( Object[] ) object;
            t = ( Taxon ) oa[0];
            Long count = ( Long ) oa[1];

            result.put( t, count.intValue() );

        }

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutBioSequences(ubic.gemma
     * .model.expression.arrayDesign.ArrayDesign)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutBioSequences( ArrayDesign arrayDesign )
            throws Exception {
        final String queryString = "select distinct cs from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " left join cs.biologicalCharacteristic bs where ar = :ar and bs IS NULL";
        return getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutBlatResults(ubic.gemma
     * .model.expression.arrayDesign.ArrayDesign)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutBlatResults( ArrayDesign arrayDesign )
            throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String nativeQueryString = "SELECT distinct cs.id from "
                + "COMPOSITE_SEQUENCE cs left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on bs2gp.BIO_SEQUENCE_FK=cs.BIOLOGICAL_CHARACTERISTIC_FK "
                + "left join SEQUENCE_SIMILARITY_SEARCH_RESULT ssResult on bs2gp.BLAT_RESULT_FK=ssResult.ID "
                + "WHERE ssResult.ID is NULL AND ARRAY_DESIGN_FK = :id ";

        // final String queryString = "select distinct cs id from CompositeSequenceImpl cs, BlatAssociationImpl bs2gp
        // inner join bs2gp.blatResult";

        return NativeQueryUtils.findByNamedParam( this.getHibernateTemplate(), nativeQueryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutGenes(ubic.gemma.model
     * .expression.arrayDesign.ArrayDesign)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<CompositeSequence> handleCompositeSequenceWithoutGenes( ArrayDesign arrayDesign )
            throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();

        final String nativeQueryString = "SELECT distinct cs.id from "
                + "COMPOSITE_SEQUENCE cs left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on BIO_SEQUENCE_FK=BIOLOGICAL_CHARACTERISTIC_FK "
                + "left join CHROMOSOME_FEATURE geneProduct on (geneProduct.ID=bs2gp.GENE_PRODUCT_FK AND geneProduct.class='GeneProductImpl') "
                + "left join CHROMOSOME_FEATURE gene on (geneProduct.GENE_FK=gene.ID AND gene.class in ('GeneImpl', 'PredictedGeneImpl', 'ProbeAlignedRegionImpl')) "
                + "WHERE gene.ID IS NULL AND ARRAY_DESIGN_FK = :id";
        return NativeQueryUtils.findByNamedParam( this.getHibernateTemplate(), nativeQueryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCountAll()
     */
    @Override
    protected Integer handleCountAll() throws Exception {
        final String queryString = "select count(*) from ArrayDesignImpl";
        return ( ( Long ) getHibernateTemplate().find( queryString ).iterator().next() ).intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleDeleteAlignmentData(ubic.gemma.model.expression
     * .arrayDesign.ArrayDesign)
     */
    @Override
    protected void handleDeleteAlignmentData( ArrayDesign arrayDesign ) throws Exception {
        // First have to delete all blatAssociations, because they are referred to by the alignments
        deleteGeneProductAssociations( arrayDesign );

        // Note attempts to do this with bulk updates were unsuccessful due to the need for joins.
        final String queryString = "select br from ArrayDesignImpl ad inner join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, BlatResultImpl br "
                + "where br.querySequence = bs and ad=:arrayDesign";
        getHibernateTemplate().deleteAll(
                getHibernateTemplate().findByNamedParam( queryString, "arrayDesign", arrayDesign ) );

        log.info( "Done deleting  BlatResults for " + arrayDesign );

    }

    @Override
    protected void handleDeleteGeneProductAssociations( ArrayDesign arrayDesign ) {
        final String queryString = "select ba from ArrayDesignImpl ad inner join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, BioSequence2GeneProductImpl ba "
                + "where ba.bioSequence = bs and ad=:arrayDesign";
        getHibernateTemplate().deleteAll(
                getHibernateTemplate().findByNamedParam( queryString, "arrayDesign", arrayDesign ) );
        log.info( "Done deleting BlatAssociations for " + arrayDesign );

        final String annotationAssociationQueryString = "select ba from ArrayDesignImpl ad inner join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, AnnotationAssociationImpl ba "
                + "where ba.bioSequence = bs and ad=:arrayDesign";
        getHibernateTemplate()
                .deleteAll(
                        getHibernateTemplate().findByNamedParam( annotationAssociationQueryString, "arrayDesign",
                                arrayDesign ) );
        log.info( "Done deleting AnnotationAssociations for " + arrayDesign );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ArrayDesign> handleFindByAlternateName( String queryString ) throws Exception {
        return this.getHibernateTemplate().findByNamedParam(
                "select ad from ArrayDesignImpl ad inner join ad.alternateNames n where n.name = :q", "q", queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadCompositeSequences(java.lang.Long)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<BioAssay> handleGetAllAssociatedBioAssays( Long id ) throws Exception {
        final String queryString = "select b from BioAssayImpl as b inner join b.arrayDesignUsed a where a.id = :id";
        return getHibernateTemplate().findByNamedParam( queryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetAuditEvents(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetAuditEvents( Collection ids ) throws Exception {
        final String queryString = "select ad.id, auditEvent from ArrayDesignImpl ad"
                + " join ad.auditTrail as auditTrail join auditTrail.events as auditEvent join fetch auditEvent.performer "
                + " where ad.id in (:ids) ";

        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );
        Map<Long, Collection<AuditEvent>> eventMap = new HashMap<Long, Collection<AuditEvent>>();
        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            AuditEvent event = ( AuditEvent ) o[1];

            if ( eventMap.containsKey( id ) ) {
                Collection<AuditEvent> events = eventMap.get( id );
                events.add( event );
            } else {
                Collection<AuditEvent> events = new ArrayList<AuditEvent>();
                events.add( event );
                eventMap.put( id, events );
            }
        }
        // add in the array design ids that do not have events. Set their values to null.
        for ( Object object : ids ) {
            Long id = ( Long ) object;
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, null );
            }
        }
        return eventMap;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetExpressionExperimentsById(long)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ExpressionExperiment> handleGetExpressionExperiments( ArrayDesign arrayDesign )
            throws Exception {
        final String queryString = "select distinct ee from ArrayDesignImpl ad, "
                + "BioAssayImpl ba, ExpressionExperimentImpl ee inner join ee.bioAssays eeba where"
                + " ba.arrayDesignUsed=ad and eeba=ba and ad = :ad";
        return getHibernateTemplate().findByNamedParam( queryString, "ad", arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetTaxon(java.lang.Long)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Taxon> handleGetTaxa( Long id ) throws Exception {

        final String queryString = "select distinct t from ArrayDesignImpl as arrayD "
                + "inner join arrayD.compositeSequences as cs inner join " + "cs.biologicalCharacteristic as bioC"
                + " inner join bioC.taxon t where arrayD.id = :id";

        return getHibernateTemplate().findByNamedParam( queryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetTaxon(java.lang.Long)
     */
    @Override
    protected Taxon handleGetTaxon( Long id ) throws Exception {
        Collection<Taxon> taxon = handleGetTaxa( id );
        if ( taxon.size() == 0 ) {
            log.warn( "No taxon found for array " + id );
            return null; // printwarning
        }

        if ( taxon.size() > 1 ) {
            log.warn( taxon.size() + " taxon found for array " + id );
        }
        return taxon.iterator().next();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, Boolean> handleIsMerged( Collection<Long> ids ) throws Exception {

        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, count(subs) from ArrayDesignImpl as ad inner join ad.mergees subs where ad.id in (:ids) group by ad";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Long mergeeCount = ( Long ) o[1];
            if ( mergeeCount != null && mergeeCount > 0 ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }

        return eventMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, Boolean> handleIsMergee( final Collection<Long> ids ) throws Exception {

        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, ad.mergedInto from ArrayDesignImpl as ad where ad.id in (:ids) ";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            ArrayDesign merger = ( ArrayDesign ) o[1];
            if ( merger != null ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }

        return eventMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, Boolean> handleIsSubsumed( final Collection<Long> ids ) throws Exception {
        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, ad.subsumingArrayDesign from ArrayDesignImpl as ad where ad.id in (:ids) ";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            ArrayDesign subsumer = ( ArrayDesign ) o[1];
            if ( subsumer != null ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }
        return eventMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, Boolean> handleIsSubsumer( Collection<Long> ids ) throws Exception {

        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        if ( ids.size() == 0 ) {
            return eventMap;
        }

        final String queryString = "select ad.id, count(subs) from ArrayDesignImpl as ad inner join ad.subsumedArrayDesigns subs where ad.id in (:ids) group by ad";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Long subsumeeCount = ( Long ) o[1];
            if ( subsumeeCount != null && subsumeeCount > 0 ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }
        return eventMap;
    }
    
    @Override
    protected Collection<ArrayDesignValueObject> handleLoadAllValueObjects() throws Exception {
        
        // get the expression experiment counts
        Map<Long, Integer> eeCounts = this.getExpressionExperimentCountMap();
        final String queryString = "select ad.id as id, ad.name as name, ad.shortName as shortName, "
            + "ad.technologyType, ad.description, event.date as createdDate,  mergedInto  from ArrayDesignImpl ad "
            + "left join ad.auditTrail as trail inner join trail.events as event left join ad.mergedInto as mergedInto "
            + "where event.action='C' group by ad ";
        
        try {
            Map<Long, String> arrayToTaxon = getArrayToPrimaryTaxonMap();
            
            Query queryObject = super.getSession().createQuery( queryString );
            
            Collection<ArrayDesignValueObject> result = processADValueObjectQueryResults( eeCounts, queryObject,
                    arrayToTaxon );
            
            return result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadCompositeSequences(java.lang.Long)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<CompositeSequence> handleLoadCompositeSequences( Long id ) throws Exception {
        final String queryString = "select cs from CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return getHibernateTemplate().findByNamedParam( queryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadFully(java.lang.Long)
     */
    @SuppressWarnings("unchecked")
    @Override
    @Deprecated
    protected ArrayDesign handleLoadFully( Long id ) throws Exception {
        StopWatch timer = new StopWatch();
        timer.start();
        log.info( "Thawing array design ..." );
        String queryString = "select ad from ArrayDesignImpl ad inner join "
                + " fetch ad.compositeSequences cs left join fetch cs.biologicalCharacteristic bs "
                + " inner join fetch bs.taxon "
                + " inner join fetch ad.auditTrail auditTrail "
                + " left join fetch auditTrail.events "
                + " left join fetch ad.localFiles "
                + " left join fetch ad.externalReferences "
                + " left join fetch ad.subsumedArrayDesigns left join fetch ad.mergees "
                + " left join fetch bs.bioSequence2GeneProduct bs2gp "
                + " left join fetch bs2gp.geneProduct gp "
                + " left join fetch ad.designProvider dp inner join fetch dp.auditTrail dpat inner join fetch dpat.events "
                + " left join fetch gp.gene gene " + " left join fetch gene.aliases " + " where  " + " ad.id = :id";

        Session session = getSession();
        session.setCacheMode( CacheMode.IGNORE );
        List list = getHibernateTemplate().findByNamedParam( queryString, "id", id );
        if ( list.size() == 0 ) return null;
        ArrayDesign arrayDesign = ( ArrayDesign ) list.iterator().next();
        log.info( "Thaw done (" + timer.getTime() / 1000 + " s elapsed)" );
        return arrayDesign;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadMultiple(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ArrayDesign> handleLoadMultiple( Collection ids ) throws Exception {
        if ( ids == null || ids.isEmpty() ) return new HashSet<ArrayDesign>();
        final String queryString = "select ad from ArrayDesignImpl as ad where ad.id in (:ids) ";
        return getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadValueObjects(java.util.Collection)
     */
    @Override
    protected Collection<ArrayDesignValueObject> handleLoadValueObjects( Collection<Long> ids ) throws Exception {
        // sanity check
        if ( ids == null || ids.size() == 0 ) {
            return new ArrayList<ArrayDesignValueObject>();
        }

        Map<Long, String> arrayToTaxon = getArrayToPrimaryTaxonMap( ids );
        Map<Long, Integer> eeCounts = this.getExpressionExperimentCountMap( ids );

        final String queryString = "select ad.id as id, ad.name as name, "
                + "ad.shortName as shortName, "
                + "ad.technologyType, ad.description, "
                + "event.date as createdDate,  mergedInto   "
                + "from ArrayDesignImpl ad "
                + "left join ad.auditTrail as trail inner join trail.events as event  left join ad.mergedInto as mergedInto "
                + " where ad.id in (:ids) and event.action='C' group by ad  ";

        try {

            Query queryObject = super.getSession().createQuery( queryString );
            queryObject.setParameterList( "ids", ids );

            return processADValueObjectQueryResults( eeCounts, queryObject, arrayToTaxon );
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBioSequences()
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences() throws Exception {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " where " + "cs.biologicalCharacteristic.sequence is not null";
        return ( Long ) getHibernateTemplate().find( queryString ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBioSequences(java
     * .util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences( Collection ids ) throws Exception {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " where ar.id in (:ids) and cs.biologicalCharacteristic.sequence is not null";
        return ( Long ) getHibernateTemplate().find( queryString ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBlatResults()
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults() throws Exception {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BlatResultImpl as blat where blat.querySequence=cs.biologicalCharacteristic";
        return ( Long ) getHibernateTemplate().find( queryString ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBlatResults(java.
     * util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults( Collection ids ) throws Exception {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + ", BlatResultImpl as blat where blat.querySequence= and ar.id in (:ids)";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ids", ids ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithGenes()
     */
    @Override
    protected long handleNumAllCompositeSequenceWithGenes() throws Exception {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + ", BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and bs2gp.geneProduct=gp";
        return ( Long ) getHibernateTemplate().find( queryString ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithGenes(java.util.
     * Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected long handleNumAllCompositeSequenceWithGenes( Collection ids ) throws Exception {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp"
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                + "bs2gp.geneProduct=gp and ar.id in (:ids)";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ids", ids ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllGenes()
     */
    @Override
    protected long handleNumAllGenes() throws Exception {
        final String queryString = "select count (distinct gene) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + ", BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and  bs2gp.geneProduct=gp";
        return ( Long ) getHibernateTemplate().find( queryString ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllGenes(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected long handleNumAllGenes( Collection ids ) throws Exception {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select count (distinct gene) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                + "bs2gp.geneProduct=gp  and ar.id in (:ids)";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ids", ids ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumBioSequencesById(long)
     */
    @Override
    protected long handleNumBioSequences( ArrayDesign arrayDesign ) throws Exception {
        final String queryString = "select count (distinct cs.biologicalCharacteristic) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " where ar = :ar and cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumBlatResultsById(long)
     */
    @Override
    protected long handleNumBlatResults( ArrayDesign arrayDesign ) throws Exception {
        final String queryString = "select count (distinct bs2gp) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl as bs2gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and ar = :ar";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequences(java.lang.Long)
     */
    @Override
    protected Integer handleNumCompositeSequences( Long id ) throws Exception {
        final String queryString = "select count (*) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return ( ( Long ) getHibernateTemplate().findByNamedParam( queryString, "id", id ).iterator().next() )
                .intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequenceWithBioSequence(ubic.gemma
     * .model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithBioSequences( ArrayDesign arrayDesign ) throws Exception {
        final String queryString = "select count (distinct cs) from CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " where ar = :ar and cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequenceWithBlatResults()
     */
    @Override
    protected long handleNumCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) throws Exception {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BlatResultImpl as blat where blat.querySequence=cs.biologicalCharacteristic and ar = :ar";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequenceWithGenes(ubic.gemma.model
     * .expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithGenes( ArrayDesign arrayDesign ) throws Exception {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp and ar = :ar";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    @Override
    protected long handleNumCompositeSequenceWithPredictedGene( ArrayDesign arrayDesign ) throws Exception {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl bs2gp, PredictedGeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp and ar = :ar";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    @Override
    protected long handleNumCompositeSequenceWithProbeAlignedRegion( ArrayDesign arrayDesign ) throws Exception {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl bs2gp, ProbeAlignedRegionImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp and ar = :ar";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumGeneProductsById(long)
     */
    @Override
    protected long handleNumGenes( ArrayDesign arrayDesign ) throws Exception {
        final String queryString = "select count (distinct gene) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + ", BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp and ar = :ar";
        return ( Long ) getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign ).iterator().next();
    }

    @Override
    protected void handleRemoveBiologicalCharacteristics( final ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "Array design cannot be null" );
        }
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( arrayDesign, LockMode.READ );
                int count = 0;
                for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                    cs.setBiologicalCharacteristic( null );
                    session.update( cs );
                    session.evict( cs );
                    if ( ++count % LOGGING_UPDATE_EVENT_COUNT == 0 ) {
                        log.info( "Cleared sequence association for " + count + " composite sequences" );
                    }
                }

                return null;
            }
        } );
    }

    @Override
    protected ArrayDesign handleThaw( final ArrayDesign arrayDesign ) throws Exception {
        return this.doThaw( arrayDesign );
    }

    @Override
    protected Boolean handleUpdateSubsumingStatus( ArrayDesign candidateSubsumer, ArrayDesign candidateSubsumee )
            throws Exception {

        // Size does not automatically disqualify, because we only consider BioSequences that actually have
        // sequences in them.
        if ( candidateSubsumee.getCompositeSequences().size() > candidateSubsumer.getCompositeSequences().size() ) {
            log.info( "Subsumee has more sequences than subsumer so probably cannot be subsumed ... checking anyway" );
        }

        Collection<BioSequence> subsumerSeqs = new HashSet<BioSequence>();
        Collection<BioSequence> subsumeeSeqs = new HashSet<BioSequence>();

        for ( CompositeSequence cs : candidateSubsumee.getCompositeSequences() ) {
            BioSequence seq = cs.getBiologicalCharacteristic();
            if ( seq == null ) continue;
            subsumeeSeqs.add( seq );
        }

        for ( CompositeSequence cs : candidateSubsumer.getCompositeSequences() ) {
            BioSequence seq = cs.getBiologicalCharacteristic();
            if ( seq == null ) continue;
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

        // if we got this far, then we definitely have a subsuming situtation.
        if ( candidateSubsumee.getCompositeSequences().size() == candidateSubsumer.getCompositeSequences().size() ) {
            log.info( candidateSubsumee + " and " + candidateSubsumer + " are apparently exactly equivalent" );
        } else {
            log.info( candidateSubsumer + " subsumes " + candidateSubsumee );
        }
        candidateSubsumer.getSubsumedArrayDesigns().add( candidateSubsumee );
        candidateSubsumee.setSubsumingArrayDesign( candidateSubsumer );
        this.update( candidateSubsumer );
        this.getHibernateTemplate().flush();
        this.getHibernateTemplate().clear();
        this.update( candidateSubsumee );
        this.getHibernateTemplate().flush();
        this.getHibernateTemplate().clear();

        return true;
    }

    /**
     * Process query results for handleLoadAllValueObjects or handleLoadValueObjects
     * 
     * @param eeCounts
     * @param queryString
     * @param arrayToTaxon
     * @return
     */
    private Collection<ArrayDesignValueObject> processADValueObjectQueryResults( Map<Long, Integer> eeCounts,
            final Query queryObject, Map<Long, String> arrayToTaxon ) {
        Collection<ArrayDesignValueObject> result = new ArrayList<ArrayDesignValueObject>();

        queryObject.setCacheable( true );
        ScrollableResults list = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        if ( list != null ) {
            while ( list.next() ) {
                ArrayDesignValueObject v = new ArrayDesignValueObject();
                v.setId( list.getLong( 0 ) );
                v.setName( list.getString( 1 ) );
                v.setShortName( list.getString( 2 ) );

                TechnologyType color = ( TechnologyType ) list.get( 3 );
                v.setTechnologyType( color );
                if ( color != null ) v.setColor( color.getValue() );
                v.setDescription( list.getString( 4 ) );
                v.setTaxon( arrayToTaxon.get( v.getId() ) );

                if ( !eeCounts.containsKey( v.getId() ) ) {
                    v.setExpressionExperimentCount( 0L );
                } else {
                    v.setExpressionExperimentCount( eeCounts.get( v.getId() ).longValue() );
                }
                v.setDateCreated( list.getDate( 5 ) );

                v.setIsMergee( list.get( 6 ) != null );

                result.add( v );
            }
        }
        return result;
    }

    @Override
    public void remove( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "ArrayDesign.remove - 'arrayDesign' can not be null" );
        }

        this.getHibernateTemplate().executeWithNativeSession( new HibernateCallback<Object>() {
            public Object doInHibernate( Session session ) throws HibernateException {
                session.lock( arrayDesign, LockMode.NONE );
                Hibernate.initialize( arrayDesign.getMergees() );
                Hibernate.initialize( arrayDesign.getSubsumedArrayDesigns() );
                arrayDesign.getMergees().clear();
                arrayDesign.getSubsumedArrayDesigns().clear();
                return null;
            }
        } );

        this.getHibernateTemplate().delete( arrayDesign );
    }

    private List<?> thawBatchOfProbes( Collection<CompositeSequence> batch ) {
        List<?> bb = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select cs from CompositeSequenceImpl cs left join fetch cs.biologicalCharacteristic where cs in (:batch)",
                        "batch", batch );
        return bb;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<ArrayDesign> findByTaxon( Taxon taxon ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select a from ArrayDesignImpl a where a.primaryTaxon = :t", "t", taxon );
    }
}