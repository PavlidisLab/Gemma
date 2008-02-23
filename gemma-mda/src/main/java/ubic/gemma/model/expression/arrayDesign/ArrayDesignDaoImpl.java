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

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceImpl;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.NativeQueryUtils;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
public class ArrayDesignDaoImpl extends ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase {

    static Log log = LogFactory.getLog( ArrayDesignDaoImpl.class.getName() );
    private static final int LOGGING_UPDATE_EVENT_COUNT = 5000;

    public ArrayDesign arrayDesignValueObjectToEntity( ArrayDesignValueObject arrayDesignValueObject ) {
        Long id = arrayDesignValueObject.getId();
        return ( ArrayDesign ) this.load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign find( ArrayDesign arrayDesign ) {
        try {

            BusinessKey.checkValidKey( arrayDesign );
            Criteria queryObject = super.getSession( false ).createCriteria( ArrayDesign.class );
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign findOrCreate( ArrayDesign arrayDesign ) {
        ArrayDesign existingArrayDesign = this.find( arrayDesign );
        if ( existingArrayDesign != null ) {
            assert existingArrayDesign.getId() != null;
            return existingArrayDesign;
        }
        log.debug( "Creating new arrayDesign: " + arrayDesign.getName() );
        return ( ArrayDesign ) create( arrayDesign );
    }

    @Override
    public void remove( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "ArrayDesign.remove - 'arrayDesign' can not be null" );
        }

        this.getHibernateTemplate().execute( new HibernateCallback() {
            public Object doInHibernate( Session session ) throws HibernateException {
                session.update( arrayDesign );
                arrayDesign.getMergees().clear();
                arrayDesign.getSubsumedArrayDesigns().clear();
                return null;
            }
        } );

        this.getHibernateTemplate().delete( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleCompositeSequenceWithoutBioSequences( ArrayDesign arrayDesign ) throws Exception {
        final String queryString = "select distinct cs from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " left join cs.biologicalCharacteristic bs where ar = :ar and bs IS NULL";
        return getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleCompositeSequenceWithoutBlatResults( ArrayDesign arrayDesign ) throws Exception {
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleCompositeSequenceWithoutGenes( ArrayDesign arrayDesign ) throws Exception {
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

    @Override
    protected Integer handleCountAll() throws Exception {
        final String queryString = "select count(*) from ArrayDesignImpl";
        return ( ( Long ) getHibernateTemplate().find( queryString ).iterator().next() ).intValue();
    }

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    @Override
    protected void handleDeleteGeneProductAssociations( ArrayDesign arrayDesign ) {
        final String queryString = "select ba from ArrayDesignImpl ad inner join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, BlatAssociationImpl ba "
                + "where ba.bioSequence = bs and ad=:arrayDesign";
        getHibernateTemplate().deleteAll(
                getHibernateTemplate().findByNamedParam( queryString, "arrayDesign", arrayDesign ) );
        log.info( "Done deleting BlatAssociations for " + arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadCompositeSequences(java.lang.Long)
     */
    @Override
    protected Collection handleGetAllAssociatedBioAssays( Long id ) throws Exception {
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
                + " inner join ad.auditTrail as auditTrail inner join auditTrail.events as auditEvent "
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
    @Override
    protected Collection handleGetExpressionExperiments( ArrayDesign arrayDesign ) throws Exception {
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
    @Override
    protected Taxon handleGetTaxon( Long id ) throws Exception {
        final String queryString = "select t from ArrayDesignImpl as arrayD "
                + "inner join arrayD.compositeSequences as cs inner join " + "cs.biologicalCharacteristic as bioC"
                + " inner join bioC.taxon t where arrayD.id = :id";
        getHibernateTemplate().setMaxResults( 1 );
        List list = getHibernateTemplate().findByNamedParam( queryString, "id", id );
        if ( list.size() == 0 ) {
            log.warn( "Could not determine taxon for array design" + id + " (no sequences?)" );
            return null;
        }
        getHibernateTemplate().setMaxResults( 0 ); // restore to default.
        return ( Taxon ) list.iterator().next();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map handleIsMerged( Collection ids ) throws Exception {
        final String queryString = "select ad.id, count(subs) from ArrayDesignImpl as ad inner join ad.mergees subs where ad.id in (:ids) group by ad";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );
        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Long mergeeCount = ( Long ) o[1];
            if ( mergeeCount != null && mergeeCount > 0 ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ( Collection<Long> ) ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }

        return eventMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map handleIsMergee( final Collection ids ) throws Exception {
        final String queryString = "select ad.id, ad.mergedInto from ArrayDesignImpl as ad where ad.id in (:ids) ";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );
        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            ArrayDesign merger = ( ArrayDesign ) o[1];
            if ( merger != null ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ( Collection<Long> ) ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }

        return eventMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map handleIsSubsumed( final Collection ids ) throws Exception {
        final String queryString = "select ad.id, ad.subsumingArrayDesign from ArrayDesignImpl as ad where ad.id in (:ids) ";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );
        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            ArrayDesign subsumer = ( ArrayDesign ) o[1];
            if ( subsumer != null ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ( Collection<Long> ) ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }
        return eventMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map handleIsSubsumer( Collection ids ) throws Exception {
        final String queryString = "select ad.id, count(subs) from ArrayDesignImpl as ad inner join ad.subsumedArrayDesigns subs where ad.id in (:ids) group by ad";
        List<Object[]> list = getHibernateTemplate().findByNamedParam( queryString, "ids", ids );
        Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Long subsumeeCount = ( Long ) o[1];
            if ( subsumeeCount != null && subsumeeCount > 0 ) {
                eventMap.put( id, Boolean.TRUE );
            }
        }
        for ( Long id : ( Collection<Long> ) ids ) {
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, Boolean.FALSE );
            }
        }
        return eventMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    // FIXME why is this so much different than handleLoadValueObjects(collection)?
    // refarctoring is necessary
    protected Collection handleLoadAllValueObjects() throws Exception {

        // get the expression experiment counts
        Map eeCounts = this.getExpressionExperimentCountMap();

        Collection<ArrayDesignValueObject> result = new ArrayList<ArrayDesignValueObject>();

        final String queryString = "select ad.id as id, " + "ad.name as name, " + "ad.shortName as shortName, "
                + "ad.technologyType, ad.description, " + "event.date as createdDate " + "from ArrayDesignImpl ad "
                + "left join ad.auditTrail as trail " + "inner join trail.events as event "
                + "where event.action='C' group by ad order by ad.name";

        // separated out composite sequence query to grab just one to make it easier to join to the taxon

        try {
            Map<Long, String> arrayToTaxon = getArrayToTaxonMap();

            /*
             * Now load the ADs.
             */
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
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
                    v.setExpressionExperimentCount( ( Long ) eeCounts.get( v.getId() ) );
                    v.setDateCreated( list.getDate( 5 ) );
                    result.add( v );
                }
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return result;
    }

    /**
     * @return Map of ArrayDesign id to Taxon string for ValueObjects.
     */
    @SuppressWarnings("unchecked")
    private Map<Long, String> getArrayToTaxonMap() {
        Map<Long, String> arrayToTaxon = new HashMap<Long, String>();
        Collection<ArrayDesign> arrayDesigns = this.loadAll();
        for ( ArrayDesign ad : arrayDesigns ) {
            final String csString = "select taxon from ArrayDesignImpl "
                    + "as ad inner join ad.compositeSequences as cs inner join cs.biologicalCharacteristic as bioC inner join bioC.taxon as taxon"
                    + " where ad = :ad";
            org.hibernate.Query csQueryObject = super.getSession( false ).createQuery( csString );
            csQueryObject.setParameter( "ad", ad );
            csQueryObject.setCacheable( true );
            csQueryObject.setMaxResults( 1 );
            // the name of the cache region is configured in ehcache.xml.vsl
            csQueryObject.setCacheRegion( null );

            List csList = csQueryObject.list();

            if ( csList.size() == 0 ) {
                continue;
            }

            for ( Object object : csList ) {
                Taxon t = ( Taxon ) object;
                arrayToTaxon.put( ad.getId(), t.getCommonName() );
            }
        }

        return arrayToTaxon;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadCompositeSequences(java.lang.Long)
     */
    @Override
    protected Collection handleLoadCompositeSequences( Long id ) throws Exception {
        final String queryString = "select cs from CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return getHibernateTemplate().findByNamedParam( queryString, "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadFully(java.lang.Long)
     */
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

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ArrayDesign> handleLoadMultiple( Collection ids ) throws Exception {
        if ( ids == null || ids.isEmpty() ) return new HashSet<ArrayDesign>();
        final String queryString = "select ad from ArrayDesignImpl as ad where ad.id in (:ids) ";
        return getHibernateTemplate().findByNamedParam( queryString, "ids", ids );

    }

    @Override
    protected Collection handleLoadValueObjects( Collection ids ) throws Exception {
        Collection<ArrayDesignValueObject> vo = new ArrayList<ArrayDesignValueObject>();
        // sanity check
        if ( ids == null || ids.size() == 0 ) {
            return vo;
        }

        // get the expression experiment counts
        Map eeCounts = this.getExpressionExperimentCountMap();

        // FIXME is it necessary to order this query?
        final String queryString = "select ad.id as id, " + "ad.name as name, " + "ad.shortName as shortName, "
                + "ad.technologyType, ad.description, " + "event.date as createdDate " + "from ArrayDesignImpl ad "
                + "left join ad.auditTrail as trail " + "inner join trail.events as event "
                + " where ad.id in (:ids) and event.action='C' group by ad order by ad.name";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            ScrollableResults list = queryObject.scroll( ScrollMode.FORWARD_ONLY );
            while ( list.next() ) {
                ArrayDesignValueObject v = new ArrayDesignValueObject();
                v.setId( list.getLong( 0 ) );
                v.setName( list.getString( 1 ) );
                v.setShortName( list.getString( 2 ) );
                TechnologyType color = ( TechnologyType ) list.get( 3 );
                v.setTechnologyType( color );
                if ( color != null ) v.setColor( color.getValue() );
                v.setDescription( list.getString( 4 ) );
                v.setExpressionExperimentCount( ( Long ) eeCounts.get( v.getId() ) );
                v.setDateCreated( list.getDate( 5 ) );

                vo.add( v );
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return vo;
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBioSequences(java.util.Collection)
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBlatResults(java.util.Collection)
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithGenes(java.util.Collection)
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequenceWithBioSequence(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
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
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumReporters(java.lang.Long)
     */
    @Override
    protected Integer handleNumReporters( Long id ) throws Exception {
        final String queryString = "select count (*) from ArrayDesignImpl as ar inner join"
                + " ar.compositeSequences as cs inner join cs.componentReporters as rep where ar.id = :id";
        return ( ( Long ) getHibernateTemplate().findByNamedParam( queryString, "id", id ).iterator().next() )
                .intValue();
    }

    @Override
    protected void handleRemoveBiologicalCharacteristics( final ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "Array design cannot be null" );
        }
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
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
    protected void handleThaw( final ArrayDesign arrayDesign ) throws Exception {
        this.thaw( arrayDesign, true );
    }

    @Override
    protected void handleThawLite( final ArrayDesign arrayDesign ) throws Exception {
        this.thaw( arrayDesign, false );
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
     * 
     */
    private void debug( List results ) {
        for ( Object ad : results ) {
            log.error( ad );
        }

    }

    /**
     * Gets the number of expression experiments per ArrayDesign
     * 
     * @return Map
     */
    @SuppressWarnings("unchecked")
    private Map getExpressionExperimentCountMap() {
        final String queryString = "select ad.id, count(distinct ee) from ArrayDesignImpl ad, "
                + "BioAssayImpl ba, ExpressionExperimentImpl ee inner join ee.bioAssays bas where "
                + "ba.arrayDesignUsed=ad and bas=ba group by ad";

        Map<Long, Long> eeCount = new HashMap<Long, Long>();
        List<Object[]> list = getHibernateTemplate().find( queryString );
        for ( Object[] o : list ) {
            Long id = ( Long ) o[0];
            Long count = ( Long ) o[1];
            eeCount.put( id, count );
        }
        return eeCount;
    }

    /**
     * @param arrayDesign
     * @param deep Whether to thaw the biosequence associations (genes).
     * @throws Exception
     */
    private void thaw( final ArrayDesign arrayDesign, final boolean deep ) throws Exception {
        if ( arrayDesign == null ) return;
        if ( arrayDesign.getId() == null ) return;
        HibernateTemplate templ = this.getHibernateTemplate();
        final int FETCH_SIZE = 400;
        templ.setFetchSize( FETCH_SIZE );

        final String deepQuery = "select cs from CompositeSequenceImpl cs left outer join fetch cs.biologicalCharacteristic bs "
                + "left outer join fetch bs.taxon left outer join fetch bs.bioSequence2GeneProduct bs2gp "
                + " left outer join fetch bs2gp.geneProduct gp left outer join fetch gp.gene g left outer join fetch g.aliases where cs = :cs";

        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {

                // The following are VERY important for performance.
                FlushMode oldFlushMode = session.getFlushMode();
                CacheMode oldCacheMode = session.getCacheMode();
                session.setCacheMode( CacheMode.IGNORE ); // Don't hit the secondary cache
                session.setFlushMode( FlushMode.MANUAL ); // We're READ-ONLY so this is okay.
                session.lock( arrayDesign, LockMode.NONE );

                if ( log.isDebugEnabled() ) log.debug( "Thawing " + arrayDesign + " ..." );

                arrayDesign.getLocalFiles().size();
                for ( DatabaseEntry d : arrayDesign.getExternalReferences() ) {
                    session.update( d );
                    session.evict( d );
                }

                arrayDesign.getAuditTrail().getEvents().size();

                if ( arrayDesign.getDesignProvider() != null ) {
                    session.update( arrayDesign.getDesignProvider() );
                    session.update( arrayDesign.getDesignProvider().getAuditTrail() );
                    arrayDesign.getDesignProvider().getAuditTrail().getEvents().size();
                }

                if ( arrayDesign.getMergees() != null ) arrayDesign.getMergees().size();

                if ( arrayDesign.getSubsumedArrayDesigns() != null ) arrayDesign.getSubsumedArrayDesigns().size();

                if ( arrayDesign.getCompositeSequences() == null ) return null;

                log.debug( "Loading CS proxies for " + arrayDesign + " ..." );
                int numToDo = arrayDesign.getCompositeSequences().size();
                if ( numToDo > LOGGING_UPDATE_EVENT_COUNT )
                    log.info( "Must thaw " + ( deep ? " (deep) " : " (lite) " ) + numToDo
                            + " composite sequence associations ..." );

                org.hibernate.Query queryObject = session.createQuery( deepQuery );
                queryObject.setReadOnly( true );

                StopWatch timer = new StopWatch();
                timer.start();
                int i = 0;

                for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {

                    if ( ++i % LOGGING_UPDATE_EVENT_COUNT == 0 && timer.getTime() > 5000 ) {
                        log.info( arrayDesign.getShortName() + " CS assoc thaw progress: " + i + "/" + numToDo
                                + " ... (" + timer.getTime() / 1000 + "s elapsed)" );
                    }

                    if ( log.isDebugEnabled() ) log.debug( "Processing: " + cs );
                    if ( cs.getId() != null ) session.lock( cs, LockMode.NONE );

                    BioSequence bs = cs.getBiologicalCharacteristic();
                    if ( bs != null && session.get( BioSequenceImpl.class, bs.getId() ) == null ) {
                        session.lock( bs, LockMode.NONE );
                        if ( !Hibernate.isInitialized( bs ) ) {
                            Hibernate.initialize( bs );
                        }

                        if ( deep ) {
                            throw new UnsupportedOperationException(
                                    "Are you sure you need to deeply thaw that ArrayDesign" );
                            /*
                             * for ( BioSequence2GeneProduct bs2gp : bs.getBioSequence2GeneProduct() ) { GeneProduct
                             * geneProduct = bs2gp.getGeneProduct(); Gene g = geneProduct.getGene(); if ( g != null ) {
                             * g.getAliases().size(); } } if ( bs.getSequenceDatabaseEntry() != null ) {
                             * Hibernate.initialize( bs.getSequenceDatabaseEntry() ); }
                             */
                        }
                    }
                    session.evict( cs );

                }

                if ( timer.getTime() > 5000 )
                    log.info( arrayDesign.getShortName() + ": CS assoc thaw done (" + timer.getTime() / 1000
                            + "s elapsed)" );

                session.setFlushMode( oldFlushMode );
                session.setCacheMode( oldCacheMode );
                return null;
            }
        }, true );

    }

}