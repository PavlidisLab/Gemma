/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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

package ubic.gemma.model.expression.designElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class CompositeSequenceDaoImpl extends ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase {

    private static final int PROBE_TO_GENE_MAP_BATCH_SIZE = 2000;

    /*
     * Absolute maximum number of records to return when fetching raw summaries. This is necessary to avoid retrieving
     * millions of records (some sequences are repeats and can have >200,000 records.
     */
    private static final int MAX_CS_RECORDS = 10000;

    private static Log log = LogFactory.getLog( CompositeSequenceDaoImpl.class.getName() );

    /*
     * Add your 'where' clause to this.
     */
    private static final String nativeBaseSummaryQueryString = "SELECT cs.ID as deID, cs.NAME as deName, bs.NAME as bsName, bsDb.ACCESSION as bsdbacc, ssr.ID as ssrid,"
            + "geneProductRNA.ID as gpId,geneProductRNA.NAME as gpName,geneProductRNA.NCBI_GI as gpNcbi, geneProductRNA.GENE_FK as geneid, "
            + "geneProductRNA.TYPE as type, gene.ID as gId,gene.OFFICIAL_SYMBOL as gSymbol,gene.NCBI_GENE_ID as gNcbi, ad.SHORT_NAME as adShortName, ad.ID as adId, cs.DESCRIPTION as deDesc "
            + " from "
            + "COMPOSITE_SEQUENCE cs "
            + "left join BIO_SEQUENCE bs on BIOLOGICAL_CHARACTERISTIC_FK=bs.ID "
            + "left join SEQUENCE_SIMILARITY_SEARCH_RESULT ssr on ssr.QUERY_SEQUENCE_FK=BIOLOGICAL_CHARACTERISTIC_FK "
            + "left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on BIO_SEQUENCE_FK=bs.ID "
            + "left join DATABASE_ENTRY bsDb on SEQUENCE_DATABASE_ENTRY_FK=bsDb.ID "
            + "left join CHROMOSOME_FEATURE geneProductRNA on (geneProductRNA.ID=bs2gp.GENE_PRODUCT_FK) "
            + "left join CHROMOSOME_FEATURE gene on (geneProductRNA.GENE_FK=gene.ID)"
            + " left join ARRAY_DESIGN ad on (cs.ARRAY_DESIGN_FK=ad.ID) ";

    /*
     * Add your 'where' clause to this. returns much less stuff.
     */
    private static final String nativeBaseSummaryShorterQueryString = "SELECT cs.ID as deID, cs.NAME as deName, bs.NAME as bsName, bsDb.ACCESSION as bsdbacc, ssr.ID as ssrid,"
            + " gene.ID as gId,gene.OFFICIAL_SYMBOL as gSymbol "
            + " from "
            + "COMPOSITE_SEQUENCE cs "
            + "left join BIO_SEQUENCE bs on BIOLOGICAL_CHARACTERISTIC_FK=bs.ID "
            + "left join SEQUENCE_SIMILARITY_SEARCH_RESULT ssr on ssr.QUERY_SEQUENCE_FK=BIOLOGICAL_CHARACTERISTIC_FK "
            + "left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on BIO_SEQUENCE_FK=bs.ID "
            + "left join DATABASE_ENTRY bsDb on SEQUENCE_DATABASE_ENTRY_FK=bsDb.ID "
            + "left join CHROMOSOME_FEATURE geneProductRNA on (geneProductRNA.ID=bs2gp.GENE_PRODUCT_FK) "
            + "left join CHROMOSOME_FEATURE gene on (geneProductRNA.GENE_FK=gene.ID)"
            + " left join ARRAY_DESIGN ad on (cs.ARRAY_DESIGN_FK=ad.ID) ";

    @Autowired
    public CompositeSequenceDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#find(ubic.gemma.model.expression.designElement
     * .CompositeSequence)
     */
    @Override
    public CompositeSequence find( CompositeSequence compositeSequence ) {

        if ( compositeSequence.getName() == null ) return null;

        Criteria queryObject = super.getSession().createCriteria( CompositeSequence.class );

        queryObject.add( Restrictions.eq( "name", compositeSequence.getName() ) );

        // TODO make this use the full arraydesign
        // business key.
        queryObject.createCriteria( "arrayDesign" ).add(
                Restrictions.eq( "name", compositeSequence.getArrayDesign().getName() ) );

        java.util.List<?> results = queryObject.list();
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + CompositeSequence.class.getName()
                                + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( CompositeSequence ) result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<CompositeSequence> findByGene( Gene gene ) {
        final String queryString = "select distinct cs from CompositeSequenceImpl cs, BioSequenceImpl bs, BioSequence2GeneProductImpl ba, GeneProductImpl gp, GeneImpl gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.geneProduct=gp  and ba.bioSequence=bs and gene = :gene";
        return this.getHibernateTemplate().findByNamedParam( queryString, "gene", gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#findByGene(ubic.gemma.model.genome.Gene,
     * ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign ) {
        final String queryString = "select distinct cs from CompositeSequenceImpl cs, BioSequenceImpl bs, BioSequence2GeneProductImpl ba, GeneProductImpl gp, GeneImpl gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.bioSequence=bs and ba.geneProduct=gp  and gene = :gene and cs.arrayDesign=:arrayDesign ";
        return this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "gene", "arrayDesign" },
                new Object[] { gene, arrayDesign } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#findByName(java.lang.String)
     */
    @Override
    public Collection<CompositeSequence> findByName( String name ) {
        final String queryString = "select distinct cs from CompositeSequenceImpl" + " cs where cs.name = :id";
        return this.getHibernateTemplate().findByNamedParam( queryString, "id", name );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#findOrCreate(ubic.gemma.model.expression.
     * designElement.CompositeSequence)
     */
    @Override
    public CompositeSequence findOrCreate( CompositeSequence compositeSequence ) {
        if ( compositeSequence.getName() == null || compositeSequence.getArrayDesign() == null ) {
            throw new IllegalArgumentException( "compositeSequence must have name and arrayDesign." );
        }

        CompositeSequence existingCompositeSequence = this.find( compositeSequence );
        if ( existingCompositeSequence != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing compositeSequence: " + existingCompositeSequence );
            return existingCompositeSequence;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new compositeSequence: " + compositeSequence );
        return create( compositeSequence );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from CompositeSequenceImpl";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleFindByBioSequence(ubic.gemma.model.genome
     * .biosequence.BioSequence)
     */
    @Override
    protected Collection<CompositeSequence> handleFindByBioSequence( BioSequence bioSequence ) throws Exception {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct cs from CompositeSequenceImpl"
                + " cs where cs.biologicalCharacteristic = :id";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
            queryObject.setParameter( "id", bioSequence );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleFindByBioSequenceName(java.lang.String)
     */
    @Override
    protected Collection<CompositeSequence> handleFindByBioSequenceName( String name ) throws Exception {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct cs from CompositeSequenceImpl"
                + " cs inner join cs.biologicalCharacteristic b where b.name = :name";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
            queryObject.setParameter( "name", name );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    @Override
    protected Map<CompositeSequence, Collection<Gene>> handleGetGenes( Collection<CompositeSequence> compositeSequences )
            throws Exception {
        Map<CompositeSequence, Collection<Gene>> returnVal = new HashMap<CompositeSequence, Collection<Gene>>();

        int BATCH_SIZE = 2000;

        if ( compositeSequences.size() == 0 ) return returnVal;

        /*
         * Get the cs->gene mapping
         */
        final String nativeQuery = "select CS, GENE from GENE2CS WHERE CS IN (:csids) ";

        for ( CompositeSequence cs : compositeSequences ) {
            returnVal.put( cs, new HashSet<Gene>() );
        }
        List<Object> csGene = new ArrayList<Object>();
        org.hibernate.classic.Session session = this.getHibernateTemplate().getSessionFactory().getCurrentSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( nativeQuery );
        queryObject.addScalar( "cs", new LongType() );
        queryObject.addScalar( "gene", new LongType() );

        Collection<Long> csIdBatch = new HashSet<Long>();
        for ( CompositeSequence cs : compositeSequences ) {
            csIdBatch.add( cs.getId() );

            if ( csIdBatch.size() == BATCH_SIZE ) {
                queryObject.setParameterList( "csids", csIdBatch );
                csGene.addAll( queryObject.list() );
                session.clear();
                csIdBatch.clear();
            }
        }

        if ( csIdBatch.size() > 0 ) {
            queryObject.setParameterList( "csids", csIdBatch );
            csGene.addAll( queryObject.list() );
            session.clear();
        }

        StopWatch watch = new StopWatch();
        watch.start();

        int count = 0;
        Collection<Long> genesToFetch = new HashSet<Long>();
        Map<Long, Collection<Long>> cs2geneIds = new HashMap<Long, Collection<Long>>();

        for ( Object object : csGene ) {
            Object[] ar = ( Object[] ) object;
            Long cs = ( Long ) ar[0];
            Long gene = ( Long ) ar[1];
            if ( !cs2geneIds.containsKey( cs ) ) {
                cs2geneIds.put( cs, new HashSet<Long>() );
            }
            cs2geneIds.get( cs ).add( gene );
            genesToFetch.add( gene );
        }

        // nothing found?
        if ( genesToFetch.size() == 0 ) {
            returnVal.clear();
            return returnVal;
        }

        if ( log.isDebugEnabled() )
            log.debug( "Built cs -> gene map in " + watch.getTime() + " ms; fetching " + genesToFetch.size()
                    + " genes." );

        // fetch the genes
        Collection<Long> batch = new HashSet<Long>();
        Collection<Gene> genes = new HashSet<Gene>();
        String geneQuery = "from GeneImpl g where g.id in ( :gs )";

        org.hibernate.Query geneQueryObject = super.getSession().createQuery( geneQuery ).setFetchSize( 1000 );

        for ( Long gene : genesToFetch ) {
            batch.add( gene );
            if ( batch.size() == BATCH_SIZE ) {
                log.debug( "Processing batch ... " );
                geneQueryObject.setParameterList( "gs", batch );
                genes.addAll( geneQueryObject.list() );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {
            geneQueryObject.setParameterList( "gs", batch );
            genes.addAll( geneQueryObject.list() );
        }

        if ( log.isDebugEnabled() )
            log.debug( "Got information on " + genes.size() + " genes in " + watch.getTime() + " ms" );

        Map<Long, Gene> geneIdMap = new HashMap<Long, Gene>();
        for ( Gene g : genes ) {
            Hibernate.initialize( g );
            Long id = g.getId();
            geneIdMap.put( id, g );
        }

        // fill in the return value.
        for ( CompositeSequence cs : compositeSequences ) {
            Long csId = cs.getId();
            assert csId != null;
            Collection<Long> genesToAttach = cs2geneIds.get( csId );
            if ( genesToAttach == null ) {
                // this means there was no gene for that cs; we should delete it from the result
                returnVal.remove( cs );
                continue;
            }
            for ( Long geneId : genesToAttach ) {
                returnVal.get( cs ).add( geneIdMap.get( geneId ) );
            }
            ++count;
        }

        if ( log.isDebugEnabled() )
            log.debug( "Done, " + count + " result rows processed, " + returnVal.size() + "/"
                    + compositeSequences.size() + " probes are associated with genes" );
        return returnVal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleGetGenes(ubic.gemma.model.expression
     * .designElement.CompositeSequence)
     */
    @Override
    protected Collection<Gene> handleGetGenes( CompositeSequence compositeSequence ) throws Exception {
        final String queryString = "select distinct gene from CompositeSequenceImpl cs, BioSequenceImpl bs, BlatAssociationImpl ba, GeneProductImpl gp, GeneImpl gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.bioSequence=bs and ba.geneProduct=gp and cs = :cs";

        return this.getHibernateTemplate().findByNamedParam( queryString, "cs", compositeSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleGetGenesWithSpecificity(java.util.Collection
     * )
     */
    @Override
    protected Map<CompositeSequence, Collection<BioSequence2GeneProduct>> handleGetGenesWithSpecificity(
            Collection<CompositeSequence> compositeSequences ) throws Exception {

        log.info( "Getting cs -> alignment specificity map for " + compositeSequences.size() + " composite sequences" );
        Collection<CompositeSequence> batch = new HashSet<CompositeSequence>();
        Map<CompositeSequence, Collection<BioSequence2GeneProduct>> results = new HashMap<CompositeSequence, Collection<BioSequence2GeneProduct>>();

        StopWatch timer = new StopWatch();
        timer.start();
        int total = 0;
        for ( CompositeSequence cs : compositeSequences ) {
            batch.add( cs );
            if ( batch.size() == PROBE_TO_GENE_MAP_BATCH_SIZE ) {
                batchGetGenesWithSpecificity( batch, results );
                total += batch.size();
                batch.clear();
                timer.split();
                if ( timer.getSplitTime() > 10000 ) {
                    log.info( "Probe to gene map: " + total + " retrieved in " + timer.getSplitTime() + "ms" );
                }
                timer.unsplit();
            }
        }
        // finish up any leftovers
        if ( batch.size() > 0 ) {
            batchGetGenesWithSpecificity( batch, results );
        }
        total += batch.size();
        timer.stop();
        if ( timer.getTime() > 10000 ) {
            log.info( "Probe to gene map finished: " + total + " retrieved in " + timer.getTime() + "ms" );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleGetRawSummary(ubic.gemma.model.expression
     * .arrayDesign.ArrayDesign)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Object[]> handleGetRawSummary( ArrayDesign arrayDesign, Integer numResults ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }

        if ( numResults <= 0 ) {
            // get all probes. Uses a light-weight version of this query that omits as much as possible.
            final String queryString = nativeBaseSummaryShorterQueryString + " where ad.id = " + arrayDesign.getId();
            try {
                org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( queryString );
                queryObject.addScalar( "deID" ).addScalar( "deName" ).addScalar( "bsName" ).addScalar( "bsdbacc" )
                        .addScalar( "ssrid" ).addScalar( "gId" ).addScalar( "gSymbol" );
                queryObject.setMaxResults( MAX_CS_RECORDS );
                return queryObject.list();
            } catch ( org.hibernate.HibernateException ex ) {
                throw SessionFactoryUtils.convertHibernateAccessException( ex );
            }

        }
        // just a chunk.
        final String queryString = "select cs from CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar = :ar";
        this.getHibernateTemplate().setMaxResults( numResults );
        List<?> cs = this.getHibernateTemplate().findByNamedParam( queryString, "ar", arrayDesign );
        this.getHibernateTemplate().setMaxResults( 0 );
        return getRawSummary( ( Collection<CompositeSequence> ) cs, 0 );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleGetRawSummary(java.util.Collection)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase
     */
    @Override
    protected Collection<Object[]> handleGetRawSummary( Collection<CompositeSequence> compositeSequences, Integer limit )
            throws Exception {
        if ( compositeSequences == null || compositeSequences.size() == 0 ) return null;

        Collection<CompositeSequence> compositeSequencesForQuery = new HashSet<CompositeSequence>();

        /*
         * Note that running this without a limit is dangerous. If the sequence is an unmasked repeat, then we can get
         * upwards of a million records back.
         */
        if ( limit != null && limit != 0 ) {
            int j = 0;
            for ( CompositeSequence object : compositeSequences ) {
                if ( j > limit ) break;
                compositeSequencesForQuery.add( object );
                ++j;
            }
            // nativeQueryString = nativeQueryString + " LIMIT " + limit;
        } else {
            compositeSequencesForQuery = compositeSequences;
        }

        StringBuilder buf = new StringBuilder();

        for ( Iterator<CompositeSequence> it = compositeSequences.iterator(); it.hasNext(); ) {
            CompositeSequence compositeSequence = it.next();
            if ( compositeSequence == null || compositeSequence.getId() == null ) {
                throw new IllegalArgumentException();
            }
            long id = compositeSequence.getId();
            buf.append( id );
            if ( it.hasNext() ) buf.append( "," );
        }

        // This uses the 'full' query, assuming that this list isn't too big.
        String nativeQueryString = nativeBaseSummaryQueryString + " WHERE cs.ID IN (" + buf.toString() + ")";
        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( nativeQueryString );
        queryObject.addScalar( "deID" ).addScalar( "deName" ).addScalar( "bsName" ).addScalar( "bsdbacc" )
                .addScalar( "ssrid" ).addScalar( "gpId" ).addScalar( "gpName" ).addScalar( "gpNcbi" )
                .addScalar( "geneid" ).addScalar( "type" ).addScalar( "gId" ).addScalar( "gSymbol" )
                .addScalar( "gNcbi" ).addScalar( "adShortName" ).addScalar( "adId" );
        queryObject.addScalar( "deDesc", StandardBasicTypes.TEXT ); // must do this for CLOB or Hibernate is unhappy
        queryObject.setMaxResults( MAX_CS_RECORDS );
        return queryObject.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleGetRawSummary(ubic.gemma.model.expression
     * .designElement.CompositeSequence)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase
     */
    @Override
    protected Collection<Object[]> handleGetRawSummary( CompositeSequence compositeSequence, Integer numResults )
            throws Exception {
        if ( compositeSequence == null || compositeSequence.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = compositeSequence.getId();

        String nativeQueryString = nativeBaseSummaryQueryString + " WHERE cs.ID = :id";

        int limit = MAX_CS_RECORDS;
        if ( numResults != null && numResults != 0 ) {
            limit = Math.min( numResults, MAX_CS_RECORDS );
        }

        org.hibernate.SQLQuery queryObject = this.getSession().createSQLQuery( nativeQueryString );
        queryObject.setParameter( "id", id );
        queryObject.addScalar( "deID" ).addScalar( "deName" ).addScalar( "bsName" ).addScalar( "bsdbacc" )
                .addScalar( "ssrid" ).addScalar( "gpId" ).addScalar( "gpName" ).addScalar( "gpNcbi" )
                .addScalar( "geneid" ).addScalar( "type" ).addScalar( "gId" ).addScalar( "gSymbol" )
                .addScalar( "gNcbi" ).addScalar( "adShortName" ).addScalar( "adId" );
        queryObject.addScalar( "deDesc", StandardBasicTypes.TEXT ); // must do this for CLOB or Hibernate is unhappy
        queryObject.setMaxResults( limit );
        return queryObject.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleLoad(java.util.Collection)
     */
    @Override
    protected Collection<CompositeSequence> handleLoad( Collection<Long> ids ) throws Exception {

        if ( ids == null || ids.size() == 0 ) {
            return new HashSet<CompositeSequence>();
        }

        final String queryString = "select cs from CompositeSequenceImpl cs where cs.id in (:ids)";
        org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
        int batchSize = 2000;
        Collection<Long> batch = new HashSet<Long>();
        Collection<CompositeSequence> results = new HashSet<CompositeSequence>();
        for ( Long id : ids ) {
            batch.add( id );

            if ( batch.size() == batchSize ) {
                queryObject.setParameterList( "ids", batch );
                results.addAll( queryObject.list() );
                batch.clear();
            }
        }

        // tail end.
        if ( batch.size() > 0 ) {
            queryObject.setParameterList( "ids", batch );
            results.addAll( queryObject.list() );
        }
        return results;
    }

    @Override
    protected void handleThaw( final Collection<CompositeSequence> compositeSequences ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                int i = 0;
                int numToDo = compositeSequences.size();
                for ( CompositeSequence cs : compositeSequences ) {

                    session.buildLockRequest( LockOptions.NONE ).lock( cs );
                    Hibernate.initialize( cs.getArrayDesign() );
                    cs.getArrayDesign().getName();

                    BioSequence bs = cs.getBiologicalCharacteristic();
                    if ( bs == null ) {
                        continue;
                    }

                    session.buildLockRequest( LockOptions.NONE ).lock( bs );
                    Hibernate.initialize( bs );
                    bs.getTaxon();

                    DatabaseEntry dbEntry = bs.getSequenceDatabaseEntry();
                    if ( dbEntry != null ) {
                        Hibernate.initialize( dbEntry );
                        Hibernate.initialize( dbEntry.getExternalDatabase() );
                        session.evict( dbEntry );
                        session.evict( dbEntry.getExternalDatabase() );
                    }

                    if ( bs.getBioSequence2GeneProduct() == null ) {
                        continue;
                    }

                    for ( BioSequence2GeneProduct bs2gp : bs.getBioSequence2GeneProduct() ) {
                        if ( bs2gp == null ) {
                            continue;
                        }
                        GeneProduct geneProduct = bs2gp.getGeneProduct();
                        if ( geneProduct != null && geneProduct.getGene() != null ) {
                            Gene g = geneProduct.getGene();
                            g.getAliases().size();
                            session.evict( g );
                            session.evict( geneProduct );
                        }

                    }

                    if ( ++i % 2000 == 0 ) {
                        log.info( "Progress: " + i + "/" + numToDo + "..." );
                        try {
                            Thread.sleep( 10 );
                        } catch ( InterruptedException e ) {
                            //
                        }
                    }

                    session.evict( bs );
                }
                session.clear();
                return null;
            }
        } );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.expression.designElement.CompositeSequenceDao#thaw(ubic.gemma.model.expression.designElement
     * .CompositeSequence)
     */
    @Override
    public CompositeSequence thaw( CompositeSequence compositeSequence ) {

        /*
         * TODO: clean this up and perhaps adapt it to the batch method. This thaw might be too deep.
         */
        if ( compositeSequence == null ) return null;
        List<?> list = this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select c from CompositeSequenceImpl c left join fetch c.biologicalCharacteristic b "
                                + " left join fetch b.taxon tax left join fetch tax.externalDatabase left join fetch tax.parentTaxon pt "
                                + " left join fetch pt.externalDatabase " + " left join fetch c.arrayDesign "
                                + " left join fetch b.sequenceDatabaseEntry s left join fetch s.externalDatabase"
                                + " left join fetch b.bioSequence2GeneProduct bs2gp "
                                + " left join fetch bs2gp.geneProduct gp left join fetch gp.gene g"
                                + " left join fetch g.aliases left join fetch g.accessions  where c.id=:cid", "cid",
                        compositeSequence.getId() );
        if ( list.isEmpty() ) {
            return null;
        }
        return ( CompositeSequence ) list.iterator().next();
    }

    /**
     * @param batch of composite sequences to process
     * @param results - adding to this
     */
    private void batchGetGenesWithSpecificity( Collection<CompositeSequence> batch,
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> results ) {

        if ( batch.size() == 0 ) {
            return;
        }

        final String queryString = "select cs,bas from CompositeSequenceImpl cs, BioSequence2GeneProductImpl bas inner join cs.biologicalCharacteristic bs "
                + "inner join fetch bas.geneProduct gp inner join fetch gp.gene gene "
                + "where bas.bioSequence=bs and cs in (:cs)";
        List<?> qr = this.getHibernateTemplate().findByNamedParam( queryString, "cs", batch );

        for ( Object o : qr ) {
            Object[] oa = ( Object[] ) o;
            CompositeSequence csa = ( CompositeSequence ) oa[0];
            BioSequence2GeneProduct ba = ( BioSequence2GeneProduct ) oa[1];

            if ( ba instanceof BlatAssociation ) {
                BlatResult blatResult = ( ( BlatAssociation ) ba ).getBlatResult();
                PhysicalLocation pl = blatResult.getTargetAlignedRegion();

                /*
                 * We didn't always used to fill in the targetAlignedRegion ... this is just in case.
                 */
                if ( pl == null ) {
                    pl = PhysicalLocation.Factory.newInstance();
                    pl.setChromosome( blatResult.getTargetChromosome() );
                    pl.setNucleotide( blatResult.getTargetStart() );
                    pl.setNucleotideLength( blatResult.getTargetEnd().intValue()
                            - blatResult.getTargetStart().intValue() );
                    pl.setStrand( blatResult.getStrand() );
                    // Note: not bothering to fill in the bin.
                }

            }

            if ( !results.containsKey( csa ) ) {
                results.put( csa, new HashSet<BioSequence2GeneProduct>() );
            }

            results.get( csa ).add( ba );
        }

        /*
         * This is kind of important. We ensure we return an empty map for probes that do not have a mapping.
         */
        for ( CompositeSequence cs : batch ) {
            if ( !results.containsKey( cs ) ) {
                results.put( cs, new HashSet<BioSequence2GeneProduct>() );
            }

        }

    }

}