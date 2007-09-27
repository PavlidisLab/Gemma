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
import java.sql.SQLException;
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
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.BusinessKey;
import ubic.gemma.util.QueryUtils;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
public class ArrayDesignDaoImpl extends ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase {

    private static final int LOGGING_UPDATE_EVENT_COUNT = 5000;
    static Log log = LogFactory.getLog( ArrayDesignDaoImpl.class.getName() );

    public ArrayDesign arrayDesignValueObjectToEntity( ArrayDesignValueObject arrayDesignValueObject ) {
        Long id = arrayDesignValueObject.getId();
        return ( ArrayDesign ) this.load( id );
    }

    /**
     * 
     */
    private void debug( List results ) {
        for ( Object ad : results ) {
            log.error( ad );
        }

    }

    @Override
    public void remove( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            throw new IllegalArgumentException( "ArrayDesign.remove - 'arrayDesign' can not be null" );
        }

        this.getHibernateTemplate().execute( new HibernateCallback() {

            public Object doInHibernate( Session session ) throws HibernateException, SQLException {
                session.update( arrayDesign );
                arrayDesign.getMergees().clear();
                arrayDesign.getSubsumedArrayDesigns().clear();
                return null;
            }
        } );

        this.getHibernateTemplate().delete( arrayDesign );
    }

    /**
     * Efficiently delete objects.
     * 
     * @param toBeRemoved
     */
    private void deleteInBatches( Collection toBeRemoved ) {
        Collection<Object> batch = new ArrayList<Object>();
        int BATCH_SIZE = 10000;
        for ( Object result : toBeRemoved ) {
            batch.add( result );
            if ( batch.size() == BATCH_SIZE ) {
                this.getHibernateTemplate().deleteAll( batch );
                batch.clear();
            }
        }
        if ( batch.size() > 0 ) this.getHibernateTemplate().deleteAll( batch );
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

    /**
     * queries the database and gets the number of expression experiments per ArrayDesign
     * 
     * @return Map
     */
    private Map getExpressionExperimentCountMap() {
        final String queryString = "select ad.id, count(distinct ee) from ArrayDesignImpl ad, "
                + "BioAssayImpl ba, ExpressionExperimentImpl ee inner join ee.bioAssays bas where "
                + "ba.arrayDesignUsed=ad and bas=ba group by ad";

        Map<Long, Long> eeCount = new HashMap<Long, Long>();
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            ScrollableResults list = queryObject.scroll( ScrollMode.FORWARD_ONLY );
            while ( list.next() ) {
                Long id = list.getLong( 0 );
                Long count = list.getLong( 1 );
                eeCount.put( id, count );
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return eeCount;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleCompositeSequenceWithoutBioSequences( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select distinct cs from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " left join cs.biologicalCharacteristic bs where ar.id = :id and " + " bs IS NULL";
        return QueryUtils.queryByIdReturnCollection( getSession(), arrayDesign.getId(), queryString );
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
                + "COMPOSITE_SEQUENCE cs left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on BIO_SEQUENCE_FK=BIOLOGICAL_CHARACTERISTIC_FK "
                + "left join SEQUENCE_SIMILARITY_SEARCH_RESULT ssResult on bs2gp.BLAT_RESULT_FK=ssResult.ID "
                + "WHERE ssResult.ID is NULL AND ARRAY_DESIGN_FK = :id ";

        return QueryUtils.nativeQueryById( getSession(), id, nativeQueryString );
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
        return QueryUtils.nativeQueryById( getSession(), id, nativeQueryString );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from ArrayDesignImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( ( Long ) queryObject.iterate().next() ).intValue();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleDeleteAlignmentData( ArrayDesign arrayDesign ) throws Exception {

        // probably can do this as one query...

        // First have to delete all blatAssociations, because they are referred to by the alignments
        final String blatAssociationQueryString = "select ba from ArrayDesignImpl ad inner join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, BlatResultImpl br,BlatAssociationImpl ba "
                + "where br.querySequence = bs and ad=:arrayDesign and ba.bioSequence = bs";
        org.hibernate.Query queryObject = super.getSession( false ).createQuery( blatAssociationQueryString );
        queryObject.setParameter( "arrayDesign", arrayDesign );
        Collection<BlatResult> toBeRemoved = queryObject.list();

        if ( toBeRemoved.size() == 0 ) {
            log.info( "No old blatAssociations for " + arrayDesign );
        } else {
            log.info( "Have " + toBeRemoved.size() + " BlatAssociations to remove for " + arrayDesign
                    + "(they have to be removed to make way for new alignment data)" );
            deleteInBatches( toBeRemoved );
        }

        final String sequenceQueryString = "select br from ArrayDesignImpl ad inner join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, BlatResultImpl br "
                + "where br.querySequence = bs and ad=:arrayDesign";
        queryObject = super.getSession( false ).createQuery( sequenceQueryString );
        queryObject.setParameter( "arrayDesign", arrayDesign );
        toBeRemoved = queryObject.list();

        if ( toBeRemoved.size() == 0 ) {
            log.info( "No old alignments to be removed for " + arrayDesign );
        } else {
            log.info( "Have " + toBeRemoved.size() + " BlatResults to remove for " + arrayDesign );
            deleteInBatches( toBeRemoved );
            log.info( "Done deleting." );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleDeleteGeneProductAssociations( ArrayDesign arrayDesign ) {
        final String sequenceQueryString = "select ba from ArrayDesignImpl ad inner join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, BlatAssociationImpl ba "
                + "where ba.bioSequence = bs and ad=:arrayDesign";
        org.hibernate.Query queryObject = super.getSession( false ).createQuery( sequenceQueryString );
        queryObject.setFetchSize( 1000 );
        queryObject.setParameter( "arrayDesign", arrayDesign );
        final Collection<BlatAssociation> toBeRemoved = queryObject.list();

        if ( toBeRemoved.size() == 0 ) {
            log.info( "No old associations to be removed for " + arrayDesign );
            return;
        }
        log.info( "Have " + toBeRemoved.size() + " BlatAssociations to remove for " + arrayDesign );
        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                int i = 0;
                for ( java.util.Iterator entityIterator = toBeRemoved.iterator(); entityIterator.hasNext(); ) {
                    session.delete( entityIterator.next() );
                    if ( ++i % 5000 == 0 ) {
                        log.info( "Delete Progress: " + i + "/" + toBeRemoved.size() + "..." );
                        try {
                            Thread.sleep( 100 );
                        } catch ( InterruptedException e ) {
                            //
                        }
                    }
                }
                return null;
            }
        }, true );
        log.info( "Done deleting." );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleFindByGoId(String)
     */
    @Override
    protected Collection handleFindByGoId( String goId ) throws Exception {

        if ( goId == null || goId.length() == 0 ) {
            throw new IllegalArgumentException();
        }

        final String queryString = "select ad from ArrayDesignImpl ad inner join ad.compositeSequences as cs inner"
                + " join cs.biologicalCharacteristic as bs inner join bs.bioSequence2GeneProduct as bs2gp, Gene2GOAssociationImpl"
                + " g2o inner join g2o.ontologyEntry oe inner join g2o.gene g inner join g.products prod "
                + " where bs2gp.geneProduct=prod and oe.accession = :accession group by ad";

        Query queryObject = super.getSession( false ).createQuery( queryString );
        queryObject.setParameter( "accession", goId );

        return queryObject.list();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadCompositeSequences(java.lang.Long)
     */
    @Override
    protected Collection handleGetAllAssociatedBioAssays( Long id ) throws Exception {
        final String queryString = "select bioAssay from BioAssayImpl as bioAssay where bioAssay.arrayDesignUsed.id = :id";
        return QueryUtils.queryByIdReturnCollection( getSession(), id, queryString );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetAuditEvents(java.util.Collection)
     */
    @Override
    protected Map handleGetAuditEvents( Collection ids ) throws Exception {
        final String queryString = "select ad.id, auditEvent from ArrayDesignImpl ad"
                + " inner join ad.auditTrail as auditTrail inner join auditTrail.events as auditEvent "
                + " where ad.id in (:ids) ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            ScrollableResults list = queryObject.scroll();
            Map<Long, Collection<AuditEvent>> eventMap = new HashMap<Long, Collection<AuditEvent>>();
            // process list of expression experiment ids that have events
            while ( list.next() ) {
                Long id = list.getLong( 0 );
                AuditEvent event = ( AuditEvent ) list.get( 1 );

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
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetExpressionExperimentsById(long)
     */
    @Override
    protected Collection handleGetExpressionExperiments( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select distinct ee from ArrayDesignImpl ad, "
                + "BioAssayImpl ba, ExpressionExperimentImpl ee inner join ee.bioAssays eeba where"
                + " ba.arrayDesignUsed=ad and eeba=ba and ad.id = :id";
        return QueryUtils.queryByIdReturnCollection( getSession(), id, queryString );
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

        try {
            org.hibernate.Query queryObject = this.getSession().createQuery( queryString );
            queryObject.setParameter( "id", id );
            queryObject.setMaxResults( 1 );
            List list = queryObject.list();
            if ( list.size() == 0 ) {
                log.warn( "Could not determine taxon for array design" + id + " (no sequences?)" );
                return null;
            }
            return ( Taxon ) list.iterator().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map handleIsMerged( Collection ids ) throws Exception {
        final String queryString = "select ad.id, count(subs) from ArrayDesignImpl as ad inner join ad.mergees subs where ad.id in (:ids) group by ad";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            ScrollableResults list = queryObject.scroll();
            Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
            // process list of ids that have events
            while ( list.next() ) {
                Long id = list.getLong( 0 );
                Long mergeeCount = list.getLong( 1 );
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
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map handleIsMergee( final Collection ids ) throws Exception {
        final String queryString = "select ad.id, ad.mergedInto from ArrayDesignImpl as ad where ad.id in (:ids) ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            ScrollableResults list = queryObject.scroll();
            Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
            // process list of ids that have events
            while ( list.next() ) {
                Long id = list.getLong( 0 );
                ArrayDesign merger = ( ArrayDesign ) list.get( 1 );
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
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map handleIsSubsumed( final Collection ids ) throws Exception {
        final String queryString = "select ad.id, ad.subsumingArrayDesign from ArrayDesignImpl as ad where ad.id in (:ids) ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            ScrollableResults list = queryObject.scroll();
            Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
            // process list of ids that have events
            while ( list.next() ) {
                Long id = list.getLong( 0 );
                ArrayDesign subsumer = ( ArrayDesign ) list.get( 1 );
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
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map handleIsSubsumer( Collection ids ) throws Exception {
        final String queryString = "select ad.id, count(subs) from ArrayDesignImpl as ad inner join ad.subsumedArrayDesigns subs where ad.id in (:ids) group by ad";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            ScrollableResults list = queryObject.scroll();
            Map<Long, Boolean> eventMap = new HashMap<Long, Boolean>();
            // process list of ids that have events
            while ( list.next() ) {
                Long id = list.getLong( 0 );
                Long subsumeeCount = list.getLong( 1 );
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
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    protected Collection handleLoadAllValueObjects() throws Exception {

        // get the expression experiment counts
        Map eeCounts = this.getExpressionExperimentCountMap();

        Collection<ArrayDesignValueObject> result = new ArrayList<ArrayDesignValueObject>();

        final String queryString = "select ad.id as id, " + " ad.name as name, " + " ad.shortName as shortName, "
                + " ad.technologyType from ArrayDesignImpl as ad " + " group by ad order by ad.name";

        // separated out composite sequence query to grab just one to make it easier to join to the taxon
        final String csString = "select ad.id, cs.id from ArrayDesignImpl as ad inner join ad.compositeSequences as cs where cs.biologicalCharacteristic IS NOT NULL group by ad";
        final String taxonString = "select cs.id, taxon.commonName from CompositeSequenceImpl as cs inner join cs.biologicalCharacteristic as bioC inner join bioC.taxon as taxon"
                + "   WHERE cs.id in (:id) group by cs.id";
        try {
            // do queries for representative compositeSequences so we can get taxon information easily

            Map<Long, Long> csToArray = new HashMap<Long, Long>();
            Map<Long, String> arrayToTaxon = new HashMap<Long, String>();

            org.hibernate.Query csQueryObject = super.getSession( false ).createQuery( csString );
            csQueryObject.setCacheable( true );

            // the name of the cache region is configured in ehcache.xml.vsl
            csQueryObject.setCacheRegion( "arrayDesignListing" );

            List csList = csQueryObject.list();
            for ( Object object : csList ) {
                Object[] res = ( Object[] ) object;
                Long arrayId = ( Long ) res[0];
                Long csId = ( Long ) res[1];
                csToArray.put( csId, arrayId );
            }

            org.hibernate.Query taxonQueryObject = super.getSession( false ).createQuery( taxonString );
            taxonQueryObject.setParameterList( "id", csToArray.keySet() );
            ScrollableResults taxonList = taxonQueryObject.scroll();
            while ( taxonList.next() ) {
                Long csId = taxonList.getLong( 0 );
                String taxon = taxonList.getString( 1 );
                Long arrayId = csToArray.get( csId );
                arrayToTaxon.put( arrayId, taxon );
            }

            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            ScrollableResults list = queryObject.scroll( ScrollMode.FORWARD_ONLY );
            if ( list != null ) {
                while ( list.next() ) {
                    ArrayDesignValueObject v = new ArrayDesignValueObject();
                    v.setId( list.getLong( 0 ) );
                    v.setName( list.getString( 1 ) );
                    v.setShortName( list.getString( 2 ) );

                    TechnologyType color = ( TechnologyType ) list.get( 3 );
                    if ( color != null ) v.setColor( color.getValue() );

                    v.setTaxon( arrayToTaxon.get( v.getId() ) );
                    v.setExpressionExperimentCount( ( Long ) eeCounts.get( v.getId() ) );
                    result.add( v );
                }
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadCompositeSequences(java.lang.Long)
     */
    @Override
    protected Collection handleLoadCompositeSequences( Long id ) throws Exception {
        final String queryString = "select cs from CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return QueryUtils.queryByIdReturnCollection( getSession(), id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadFully(java.lang.Long)
     */
    @Override
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
        ArrayDesign arrayDesign = ( ArrayDesign ) QueryUtils.queryById( session, id, queryString );
        log.info( "Thaw done (" + timer.getTime() / 1000 + " s elapsed)" );
        return arrayDesign;
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

        final String queryString = "select ad.id as id, ad.name as name, ad.shortName as shortName, "
                + " ad.technologyType" + " from ArrayDesignImpl ad "
                + " where ad.id in (:ids) group by ad order by ad.name";

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
                v.setColor( color.getValue() );
                v.setExpressionExperimentCount( ( Long ) eeCounts.get( v.getId() ) );

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
                + " where " + "cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) QueryUtils.query( getSession(), queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBioSequences(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences( Collection ids ) throws Exception {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " where ar.id in (:ids) and cs.biologicalCharacteristic.sequence is not null";
        return ( Long ) QueryUtils.queryByIds( getSession(), ids, queryString );
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
        return ( Long ) QueryUtils.query( getSession(), queryString );
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
        return ( Long ) QueryUtils.queryByIds( getSession(), ids, queryString );
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
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp";
        return ( Long ) QueryUtils.query( getSession(), queryString );
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
                + "bs2gp.geneProduct=gp and ar.id in (:id)";
        return ( Long ) QueryUtils.queryByIds( getSession(), ids, queryString );
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
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp";
        return ( Long ) QueryUtils.query( getSession(), queryString );
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
        return ( Long ) QueryUtils.queryByIds( getSession(), ids, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumBioSequencesById(long)
     */
    @Override
    protected long handleNumBioSequences( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct cs.biologicalCharacteristic) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " where ar.id = :id and cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) QueryUtils.queryById( getSession(), id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumBlatResultsById(long)
     */
    @Override
    protected long handleNumBlatResults( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct bs2gp) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl as bs2gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and ar.id = :id";
        return ( Long ) QueryUtils.queryById( getSession(), id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequences(java.lang.Long)
     */// FIXME this method should return a long.
    @Override
    protected Integer handleNumCompositeSequences( Long id ) throws Exception {
        final String queryString = "select count (*) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return ( ( Long ) QueryUtils.queryById( getSession(), id, queryString ) ).intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequenceWithBioSequence(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithBioSequences( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " where ar.id = :id and cs.biologicalCharacteristic.sequence IS NOT NULL";
        return ( Long ) QueryUtils.queryById( getSession(), id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequenceWithBlatResults()
     */
    @Override
    protected long handleNumCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BlatResultImpl as blat where blat.querySequence=cs.biologicalCharacteristic and ar.id = :id";
        return ( Long ) QueryUtils.queryById( getSession(), id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithGenes( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp and ar.id = :id";
        return ( Long ) QueryUtils.queryById( getSession(), id, queryString );
    }

    @Override
    protected long handleNumCompositeSequenceWithPredictedGene( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl bs2gp, PredictedGeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp and ar.id = :id";
        return ( Long ) QueryUtils.queryById( getSession(), id, queryString );
    }

    @Override
    protected long handleNumCompositeSequenceWithProbeAlignedRegion( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " , BioSequence2GeneProductImpl bs2gp, ProbeAlignedRegionImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp and ar.id = :id";
        return ( Long ) QueryUtils.queryById( getSession(), id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumGeneProductsById(long)
     */
    @Override
    protected long handleNumGenes( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct gene) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + ", BioSequence2GeneProductImpl bs2gp, GeneImpl gene inner join gene.products gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct=gp and ar.id = :id";
        return ( Long ) QueryUtils.queryById( getSession(), id, queryString );
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
        return ( ( Long ) QueryUtils.queryById( getSession(), id, queryString ) ).intValue();
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
     * @param arrayDesign
     * @param deep Whether to thaw the biosequence associations (genes).
     * @throws Exception
     */
    private void thaw( final ArrayDesign arrayDesign, final boolean deep ) throws Exception {
        if ( arrayDesign == null ) return;
        if ( arrayDesign.getId() == null ) return;
        HibernateTemplate templ = this.getHibernateTemplate();

        templ.setFetchSize( 400 );
        log.debug( "Fetch size for thaw is " + templ.getFetchSize() );

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

                int numToDo = arrayDesign.getCompositeSequences().size(); // this takes a little while.
                // log.info( "Must thaw " + numToDo + " composite sequence associations ..." );

                String deepQuery = "select cs from CompositeSequenceImpl cs left outer join fetch cs.biologicalCharacteristic bs "
                        + "left outer join fetch bs.taxon left outer join fetch bs.bioSequence2GeneProduct bs2gp "
                        + " left outer join fetch bs2gp.geneProduct gp left outer join fetch gp.gene g left outer join fetch g.aliases where cs = :cs";
                org.hibernate.Query queryObject = session.createQuery( deepQuery );
                queryObject.setReadOnly( true );

                StopWatch timer = new StopWatch();
                timer.start();
                int i = 0;

                Collection<BioSequence> seen = new HashSet<BioSequence>();
                for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {

                    if ( ++i % LOGGING_UPDATE_EVENT_COUNT == 0 ) {
                        log.info( "CS assoc thaw progress: " + i + "/" + numToDo + " ... (" + timer.getTime() / 1000
                                + "s elapsed)" );
                        try {
                            Thread.sleep( 10 );
                        } catch ( InterruptedException e ) {
                            //
                        }
                    }

                    cs.getComponentReporters().size();
                    BioSequence bs = cs.getBiologicalCharacteristic();
                    if ( bs == null ) {
                        continue;
                    }

                    // Sequences can show up more than once per arraydesign. Skipping this check will result in a
                    // hibernate exception.
                    if ( !seen.contains( bs ) ) {
                        try { // just in case...
                            // note: LockMode.NONE results in lazy errors in TESTS but not actual runs.
                            session.lock( bs, LockMode.READ );
                            seen.add( bs );
                        } catch ( org.hibernate.NonUniqueObjectException e ) {
                            continue; // no need to process it then, we've already thawed it.
                        }
                    }

                    bs.getTaxon();

                    if ( !deep ) {
                        continue;
                    }
                    for ( BioSequence2GeneProduct bs2gp : bs.getBioSequence2GeneProduct() ) {
                        GeneProduct geneProduct = bs2gp.getGeneProduct();
                        Gene g = geneProduct.getGene();
                        if ( g != null ) {
                            g.getAliases().size();
                        }
                    }

                    if ( bs.getSequenceDatabaseEntry() != null ) {
                        Hibernate.initialize( bs.getSequenceDatabaseEntry() );
                    }
                }

                if ( timer.getTime() > 2000 )
                    log.info( "CS assoc thaw done (" + timer.getTime() / 1000 + "s elapsed)" );

                // session.update( arrayDesign );

                session.clear();
                session.setFlushMode( oldFlushMode );
                session.setCacheMode( oldCacheMode );
                return null;
            }
        }, true );

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

}