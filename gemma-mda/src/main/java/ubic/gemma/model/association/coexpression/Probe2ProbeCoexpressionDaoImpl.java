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
package ubic.gemma.model.association.coexpression;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.type.DoubleType;
import org.hibernate.type.LongType;
import org.springframework.orm.hibernate3.HibernateCallback;

import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.expression.coexpression.Link;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.NativeQueryUtils;
import ubic.gemma.util.TaxonUtility;

/**
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression
 * @version $Id$
 * @author joseph
 * @author paul
 */
public class Probe2ProbeCoexpressionDaoImpl extends
        ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase {

    private static final String TMP_TABLE_PREFIX = "TMP_";
    private static Log log = LogFactory.getLog( Probe2ProbeCoexpressionDaoImpl.class.getName() );

    // FIXME figure out the taxon instead of this iteration.
    private static final String[] p2pClassNames = new String[] { "HumanProbeCoExpressionImpl",
            "MouseProbeCoExpressionImpl", "RatProbeCoExpressionImpl", "OtherProbeCoExpressionImpl" };

    private long eeId = 0L;

    /*
     * (non-Javadoc) This should be faster than doing it one at a time; uses the "DML-style" syntax. This implementation
     * assumes all the links in the collection are of the same class!F
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression#remove(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Probe2ProbeCoexpression.remove - 'entities' can not be null" );
        }
        if ( entities.size() < 1000 ) {
            super.remove( entities );
        }

        int batchSize = 5000;

        Class clazz = entities.iterator().next().getClass();
        String className = clazz.getSimpleName();

        Collection<Probe2ProbeCoexpression> batch = new HashSet<Probe2ProbeCoexpression>();

        Query query = super.getSession( false ).createQuery( "DELETE from " + className + " d where d in (:vals)" );

        int count = 0;
        for ( Probe2ProbeCoexpression o : ( Collection<Probe2ProbeCoexpression> ) entities ) {
            batch.add( o );
            if ( batch.size() == batchSize ) {
                count += batch.size();
                query.setParameterList( "vals", batch );
                query.executeUpdate();
                super.getSession( false ).flush();
                super.getSession( false ).clear();
                batch.clear();

                if ( count % 100000 == 0 ) log.debug( "Deleted " + count + "/" + entities.size() + " links" );
            }
        }

        if ( batch.size() > 0 ) {
            count += batch.size();
            query.setParameterList( "vals", batch );
            query.executeUpdate();
            super.getSession( false ).flush();
            super.getSession( false ).clear();
        }

        if ( entities.size() > 0 && count != entities.size() )
            throw new IllegalStateException( "Failed to delete entries (deleted " + count + " of " + entities.size()
                    + ")" );

        // log.debug( "Deleted " + count + " links" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleCountLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected Integer handleCountLinks( ExpressionExperiment expressionExperiment ) throws Exception {

        for ( String p2pClassName : p2pClassNames ) {

            final String queryString = "SELECT COUNT(*) FROM " + getTableName( p2pClassName, false )
                    + " where EXPRESSION_EXPERIMENT_FK = :eeid";

            SQLQuery queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setMaxResults( 1 );
            queryObject.setParameter( "eeid", expressionExperiment.getId() );
            List results = queryObject.list();
            /*
             * We divide by 2 because all links are stored twice
             */

            BigInteger count = ( BigInteger ) results.iterator().next();
            if ( count.intValue() > 0 ) return ( count.intValue() ) / 2;

        }

        return 0;

    }

    @Override
    protected List handleCreate( final List links ) {
        List result = ( List ) this.getHibernateTemplate().execute( new HibernateCallback() {
            public Object doInHibernate( Session session ) throws HibernateException {
                int numDone = 0;
                List<Object> result = new ArrayList<Object>();
                for ( Object object : links ) {
                    result.add( session.save( object ) );
                    if ( ++numDone % 500 == 0 ) {
                        session.flush();
                        session.clear();
                    }
                }
                return result;
            }
        } );
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleDeleteLinks(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void handleDeleteLinks( final ExpressionExperiment ee ) throws Exception {

        /*
         * Note that the expression experiment is not directly associated with P2P objects. The EE column in the P2P
         * tables is a denormalization and is indexed, but not accessible via Hibernate. Thus it is much more efficient
         * to access P2P with native queries.
         */
        int totalDone = 0;
        Analysis analysis = null;
        for ( String p2pClassName : p2pClassNames ) {

            /*
             * Get one vector to locate the analysis object to delete.
             */
            final String queryString = "SELECT SOURCE_ANALYSIS_FK FROM " + getTableName( p2pClassName, false )
                    + " where EXPRESSION_EXPERIMENT_FK = :eeid";

            SQLQuery queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setMaxResults( 1 );
            queryObject.setParameter( "eeid", ee.getId() );
            List results = queryObject.list();

            if ( results.size() > 0 ) {
                BigInteger analysisId = ( BigInteger ) results.iterator().next();
                if ( analysisId != null )
                    analysis = ( Analysis ) this.getHibernateTemplate().load( ProbeCoexpressionAnalysisImpl.class,
                            analysisId.longValue() );
            }

            final String nativeDeleteQuery = "DELETE FROM " + getTableName( p2pClassName, false )
                    + " where EXPRESSION_EXPERIMENT_FK = :eeid";

            SQLQuery q = super.getSession( false ).createSQLQuery( nativeDeleteQuery );
            q.setParameter( "eeid", ee.getId() );
            StopWatch timer = new StopWatch();
            timer.start();
            totalDone = q.executeUpdate();
            if ( timer.getTime() > 1000 ) {
                log.info( "Done in " + timer.getTime() + "ms" );
            }

            if ( totalDone > 0 ) {
                log.info( totalDone + " coexpression results removed for " + ee );
                break;
            }

        }
        if ( totalDone == 0 ) {
            log.info( "No coexpression results to remove for " + ee );
        }
        if ( analysis != null ) {
            removeAnalysisObject( analysis );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene,
     *      java.util.Collection, boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ExpressionExperiment> handleGetExpressionExperimentsLinkTestedIn( Gene gene,
            Collection expressionExperiments, boolean filterNonSpecific ) throws Exception {

        if ( expressionExperiments == null || expressionExperiments.isEmpty() )
            return new HashSet<ExpressionExperiment>();

        // FIXME implement filterNonSpecific.
        if ( filterNonSpecific ) {
            throw new UnsupportedOperationException( "Sorry, filterNonSpecific is not supported yet" );
        }

        Collection<DesignElement> probes = this.getCsForGene( gene );

        if ( probes.size() == 0 ) return new HashSet<ExpressionExperiment>();

        // Locate analyses which use these probes, return the expression experiments
        String queryString = "select distinct ees from ProbeCoexpressionAnalysisImpl pca inner join pca.experimentsAnalyzed ees inner join pca.probesUsed pu where ees in (:ees) and pu in (:probes)";
        return this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "ees", "probes" },
                new Object[] { expressionExperiments, probes } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetGenesTestedBy(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Long> handleGetGenesTestedBy( ExpressionExperiment ee, boolean filterNonSpecific )
            throws Exception {

        // FIXME implement filterNonSpecific.
        if ( filterNonSpecific ) {
            throw new UnsupportedOperationException( "Sorry, filterNonSpecific is not supported yet" );
        }

        // this is _much_ faster than going through blatassociation.
        final String nativeQueryString = "SELECT gc.GENE FROM EXPRESSION_EXPERIMENT e "
                + "INNER JOIN EXPERIMENTS_ANALYZED ea ON e.ID=ea.EXPERIMENTS_ANALYZED_FK "
                + "INNER JOIN ANALYSIS a ON a.ID=ea.EXPRESSION_ANALYSES_FK "
                + "INNER JOIN  PROBE_COEXPRESSION_ANALYSIS_PROBES_USED pu ON pu.PROBE_COEXPRESSION_ANALYSES_FK=a.ID "
                + "INNER JOIN GENE2CS gc ON gc.CS=pu.PROBES_USED_FK WHERE a.class='ProbeCoexpressionAnalysisImpl' AND e.ID= :eeid ";

        List<BigInteger> r = NativeQueryUtils.findByNamedParam( this.getHibernateTemplate(), nativeQueryString, "eeid",
                ee.getId() );
        List<Long> results = new ArrayList<Long>();
        for ( BigInteger i : r ) {
            results.add( i.longValue() );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetExpressionExperimentsLinkTestedIn(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.genome.Gene, java.util.Collection, boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, Collection<ExpressionExperiment>> handleGetExpressionExperimentsLinkTestedIn( Gene geneA,
            Collection /* Long */genesB, Collection expressionExperiments, boolean filterNonSpecific )
            throws Exception {

        // FIXME implement filterNonSpecific.
        if ( filterNonSpecific ) {
            throw new UnsupportedOperationException( "Sorry, filterNonSpecific is not supported yet" );
        }

        Map<Long, Collection<ExpressionExperiment>> result = new HashMap<Long, Collection<ExpressionExperiment>>();

        // this is an upper bound - if it isn't in the query gene, it's not going to be a tested link
        Collection<ExpressionExperiment> eesA = getExpressionExperimentsLinkTestedIn( geneA, expressionExperiments,
                filterNonSpecific );
        if ( eesA.size() == 0 ) {
            return result;
        }

        return handleGetExpressionExperimentsTestedIn( genesB, expressionExperiments, filterNonSpecific );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetExpressionExperimentsTestedIn(java.util.Collection,
     *      java.util.Collection, boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<Long, Collection<ExpressionExperiment>> handleGetExpressionExperimentsTestedIn( Collection genes,
            Collection expressionExperiments, boolean filterNonSpecific ) {
        // FIXME implement filterNonSpecific.
        if ( filterNonSpecific ) {
            throw new UnsupportedOperationException( "Sorry, filterNonSpecific is not supported yet" );
        }

        String queryString = "select distinct pu,ees from ProbeCoexpressionAnalysisImpl pca inner join pca.experimentsAnalyzed ees inner join pca.probesUsed pu where pu.id in (:probes)";

        Map<Long, Collection<ExpressionExperiment>> result = new HashMap<Long, Collection<ExpressionExperiment>>();

        // this step is fast.
        Map<Long, Collection<Long>> cs2genes = this.getCs2GenesMapFromGenes( genes );

        log.info( cs2genes.size() + " probes for " + genes.size() + " genes to examine in "
                + expressionExperiments.size() + " ees." );
        List eesre = new ArrayList();
        StopWatch watch = new StopWatch();
        watch.start();
        for ( Collection<Long> csBatch : BatchIterator.batches( cs2genes.keySet(), 2000 ) ) {
            // This is very rather slow, if doing this with big collections.
            eesre.addAll( this.getHibernateTemplate().findByNamedParam( queryString, "probes", csBatch ) );
        }
        if ( watch.getTime() > 1000 ) {
            log.info( "Done in " + watch.getTime() + "ms: " + eesre.size() + " records." );
        }

        for ( Object o : eesre ) {
            Object[] ol = ( Object[] ) o;
            CompositeSequence c = ( CompositeSequence ) ol[0];
            ExpressionExperiment e = ( ExpressionExperiment ) ol[1];
            Collection<Long> geneIds = cs2genes.get( c.getId() );
            for ( Long id : geneIds ) {
                if ( !result.containsKey( id ) ) {
                    result.put( id, new HashSet<ExpressionExperiment>() );
                }
                result.get( id ).add( e );
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetProbeCoExpression(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.lang.String, boolean)
     */
    @Override
    protected Collection<ProbeLink> handleGetProbeCoExpression( ExpressionExperiment expressionExperiment,
            String taxon, boolean cleaned ) throws Exception {
        String tableName = getTableName( taxon, cleaned );
        Collection<ProbeLink> links = getLinks( expressionExperiment, tableName );
        return links;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetVectorsForLinks(java.util.Collection,
     *      java.util.Collection)
     */
    @Override
    protected Map<Gene, Collection<DesignElementDataVector>> handleGetVectorsForLinks( Collection genes, Collection ees )
            throws Exception {

        Gene testG = ( Gene ) genes.iterator().next(); // todo: check to make sure that all the given genes are of the
        // same taxon throw exception

        String p2pClassName = getP2PClassName( testG );

        final String queryStringFirstVector = "select distinct gene,p2pc.secondVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                + p2pClassName
                + " as p2pc "
                + " where gene.products=bs2gp.geneProduct "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors=p2pc.firstVector "
                + " and p2pc.firstVector.expressionExperiment in (:collectionOfEE)"
                + " and gene in (:collectionOfGenes)";

        final String queryStringSecondVector = "select distinct gene, p2pc.firstVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                + p2pClassName
                + " as p2pc "
                + " where gene.products=bs2gp.geneProduct"
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors=p2pc.secondVector "
                + " and p2pc.secondVector.expressionExperiment in (:collectionOfEE)"
                + " and gene in (:collectionOfGenes)";

        Map<Gene, Collection<DesignElementDataVector>> results = new HashMap<Gene, Collection<DesignElementDataVector>>();

        try {

            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryStringFirstVector );
            queryObject.setParameterList( "collectionOfEE", ees );
            queryObject.setParameterList( "collectionOfGenes", genes );
            ScrollableResults list1 = queryObject.scroll();
            buildMap( results, list1 );

            // do query joining coexpressed genes through the secondVector to the firstVector
            queryObject = super.getSession( false ).createQuery( queryStringSecondVector );
            queryObject.setParameterList( "collectionOfEE", ees );
            queryObject.setParameterList( "collectionOfGenes", genes );
            ScrollableResults list2 = queryObject.scroll();
            buildMap( results, list2 );

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetVectorsForLinks(ubic.gemma.model.genome.Gene,
     *      java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    protected java.util.Collection<DesignElementDataVector> handleGetVectorsForLinks( Gene gene,
            java.util.Collection ees ) {

        String p2pClassName = getP2PClassName( gene );

        final String queryStringFirstVector =
        // source tables
        "select distinct p2pc.secondVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                + p2pClassName
                + " as p2pc "
                + " where gene.products=bs2gp.geneProduct "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors=p2pc.firstVector "
                + " and p2pc.firstVector.expressionExperiment in (:collectionOfEE) and gene  = :gene";

        final String queryStringSecondVector = "select distinct p2pc.firstVector from GeneImpl as gene, BioSequence2GeneProductImpl as bs2gp, CompositeSequenceImpl as compositeSequence,"
                + p2pClassName
                + " as p2pc "
                + " where gene.products=bs2gp.geneProduct "
                + " and compositeSequence.biologicalCharacteristic=bs2gp.bioSequence "
                + " and compositeSequence.designElementDataVectors =p2pc.secondVector  "
                + " and p2pc.secondVector.expressionExperiment  in (:collectionOfEE)  and gene = :gene";

        Collection<DesignElementDataVector> dedvs = new HashSet<DesignElementDataVector>();

        dedvs.addAll( this.getHibernateTemplate().findByNamedParam( queryStringFirstVector,
                new String[] { "collectionOfEE", "gene" }, new Object[] { ees, gene } ) );
        dedvs.addAll( this.getHibernateTemplate().findByNamedParam( queryStringSecondVector,
                new String[] { "collectionOfEE", "gene" }, new Object[] { ees, gene } ) );

        return dedvs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handlePrepareForShuffling(java.util.Collection,
     *      java.lang.String, boolean)
     */
    @Override
    protected void handlePrepareForShuffling( Collection ees, String taxon, boolean filterNonSpecific )
            throws Exception {
        String tableName = getTableName( taxon, true );
        createTable( tableName );
        int i = 1;
        for ( Object ee : ees ) {
            log.info( "Filtering EE " + ( ( ExpressionExperiment ) ee ).getShortName() + "(" + i + "/" + ees.size()
                    + ")" );
            doFiltering( ( ExpressionExperiment ) ee, taxon, filterNonSpecific );
            i++;
        }
    }

    /**
     * @param toBuild Map of genes to collections of vectors.
     * @param list
     */
    private void buildMap( Map<Gene, Collection<DesignElementDataVector>> toBuild, ScrollableResults list ) {

        while ( list.next() ) {
            Gene g = ( Gene ) list.get( 0 );
            DesignElementDataVector dedv = ( DesignElementDataVector ) list.get( 1 );

            if ( toBuild.containsKey( g ) )
                toBuild.get( g ).add( dedv );
            else {
                Collection<DesignElementDataVector> dedvs = new HashSet<DesignElementDataVector>();
                dedvs.add( dedv );
                toBuild.put( g, dedvs );
            }
        }

    }

    /**
     * @param tableName
     * @throws Exception
     */
    private void createTable( String tableName ) throws Exception {
        Session session = getSessionFactory().openSession();
        Connection conn = session.connection();
        Statement s = conn.createStatement();

        // guard against mistakes...
        if ( !tableName.startsWith( TMP_TABLE_PREFIX ) ) {
            throw new IllegalStateException( "Attempt to create table named " + tableName );
        }
        String queryString = "DROP TABLE IF EXISTS " + tableName + ";";
        s.executeUpdate( queryString );
        queryString = "CREATE TABLE " + tableName
                + "(id BIGINT NOT NULL AUTO_INCREMENT, FIRST_DESIGN_ELEMENT_FK BIGINT NOT NULL, "
                + "SECOND_DESIGN_ELEMENT_FK BIGINT NOT NULL, SCORE DOUBLE, EXPRESSION_EXPERIMENT_FK BIGINT NOT NULL, "
                + "PRIMARY KEY(id), KEY(EXPRESSION_EXPERIMENT_FK)) " + "ENGINE=MYISAM";
        s.executeUpdate( queryString );
        conn.close();
        session.close();

    }

    /**
     * @param ee
     * @param taxon
     * @throws Exception
     */
    private void doFiltering( ExpressionExperiment ee, String taxon, boolean filterNonSpecific ) throws Exception {
        String tableName = getTableName( taxon, false );
        Collection<ProbeLink> links = getLinks( ee, tableName );
        Set<Long> csIds = new HashSet<Long>();
        for ( ProbeLink link : links ) {
            csIds.add( link.getFirstDesignElementId() );
            csIds.add( link.getSecondDesignElementId() );
        }
        Map<Long, Collection<Long>> cs2genes = getCs2GenesMap( csIds );
        links = filterNonSpecificAndRedundant( links, cs2genes, filterNonSpecific );
        String workingTableName = getTableName( taxon, true );
        saveLinks( links, ee, workingTableName );

    }

    /**
     * @param links
     * @param cs2genes
     * @return
     */
    private Collection<ProbeLink> filterNonSpecificAndRedundant( Collection<ProbeLink> links,
            Map<Long, Collection<Long>> cs2genes, boolean filterNonSpecific ) {
        Collection<ProbeLink> specificLinks = new ArrayList<ProbeLink>();
        Collection<ProbeLink> nonRedudantLinks = new ArrayList<ProbeLink>();
        Collection<Long> mergedCsIds = new HashSet<Long>();
        long maximumId = 0;
        for ( ProbeLink link : links ) {
            Collection<Long> firstGenes = cs2genes.get( link.getFirstDesignElementId() );
            Collection<Long> secondGenes = cs2genes.get( link.getSecondDesignElementId() );
            if ( firstGenes == null || secondGenes == null ) {
                // log.error("inconsistent links for csId (" + link.getFirst_design_element_fk() + ", " +
                // link.getSecond_design_element_fk() + ") Problem: No genes for these two composite sequence id");
                continue;
            }

            // probes that hit more than one gene are excluded here.
            if ( filterNonSpecific && ( firstGenes.size() > 1 || secondGenes.size() > 1 ) ) continue;

            if ( link.getFirstDesignElementId() > maximumId ) maximumId = link.getFirstDesignElementId();
            if ( link.getSecondDesignElementId() > maximumId ) maximumId = link.getSecondDesignElementId();
            specificLinks.add( link );
        }
        maximumId = maximumId + 1;
        if ( maximumId * maximumId > Long.MAX_VALUE ) {
            log.warn( "The maximum key value is too big. Redundancy detection may be incorrect" );
            maximumId = ( long ) Math.sqrt( Long.MAX_VALUE );
        }
        // remove redundancy (links which are already listed)
        for ( ProbeLink link : specificLinks ) {
            Long forwardMerged = link.getFirstDesignElementId() * maximumId + link.getSecondDesignElementId();
            Long backwardMerged = link.getSecondDesignElementId() * maximumId + link.getFirstDesignElementId();
            if ( !mergedCsIds.contains( backwardMerged ) ) {
                nonRedudantLinks.add( link );
                mergedCsIds.add( forwardMerged );
            }
        }
        return nonRedudantLinks;
    }

    /**
     * @param csIds
     * @return
     */
    private Map<Long, Collection<Long>> getCs2GenesMap( Collection<Long> csIds ) {
        Map<Long, Collection<Long>> cs2genes = new HashMap<Long, Collection<Long>>();
        if ( csIds == null || csIds.size() == 0 ) return cs2genes;
        int count = 0;
        int CHUNK_LIMIT = 10000;
        int total = csIds.size();
        Collection<Long> idsInOneChunk = new HashSet<Long>();
        Session session = getSessionFactory().openSession();

        for ( Long csId : csIds ) {
            idsInOneChunk.add( csId );
            count++;
            total--;
            if ( count == CHUNK_LIMIT || total == 0 ) {
                String queryString = "SELECT CS as id, GENE as geneId FROM GENE2CS, CHROMOSOME_FEATURE as C WHERE GENE2CS.GENE = C.ID and C.CLASS = 'GeneImpl' and "
                        + " CS in (" + StringUtils.join( idsInOneChunk.iterator(), "," ) + ")";

                org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
                queryObject.addScalar( "id", new LongType() );
                queryObject.addScalar( "geneId", new LongType() );

                ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
                while ( scroll.next() ) {
                    Long id = scroll.getLong( 0 );
                    Long geneId = scroll.getLong( 1 );
                    Collection<Long> geneIds = cs2genes.get( id );
                    if ( geneIds == null ) {
                        geneIds = new HashSet<Long>();
                        cs2genes.put( id, geneIds );
                    }
                    geneIds.add( geneId );
                }
                count = 0;
                idsInOneChunk.clear();
            }
        }
        session.close();
        return cs2genes;
    }

    /**
     * @param genes
     * @return map of CS ids to Gene ids.
     */
    private Map<Long, Collection<Long>> getCs2GenesMapFromGenes( Collection<Long> genes ) {
        Map<Long, Collection<Long>> cs2genes = new HashMap<Long, Collection<Long>>();

        Session session = getSessionFactory().openSession();
        String queryString = "SELECT CS as csid, GENE as geneId FROM GENE2CS g WHERE g.GENE in (:geneIds)";
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        queryObject.addScalar( "csid", new LongType() );
        queryObject.addScalar( "geneId", new LongType() );

        queryObject.setParameterList( "geneIds", genes );
        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( scroll.next() ) {
            Long csid = scroll.getLong( 0 );
            Long geneId = scroll.getLong( 1 );
            if ( !cs2genes.containsKey( csid ) ) {
                cs2genes.put( csid, new HashSet<Long>() );
            }
            cs2genes.get( csid ).add( geneId );
        }

        session.close();
        return cs2genes;
    }

    /**
     * @param gene
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<DesignElement> getCsForGene( Gene gene ) {
        Long id = gene.getId();
        String queryString = "SELECT CS as id from GENE2CS WHERE GENE = " + id;
        Collection<Long> results = new HashSet<Long>();
        Session session = getSessionFactory().openSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        queryObject.addScalar( "id", new LongType() );
        ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
        while ( scroll.next() ) {
            Long cid = scroll.getLong( 0 );
            results.add( cid );
        }
        session.close();
        if ( results.size() == 0 ) {
            return new HashSet<DesignElement>();
        }

        return this.getHibernateTemplate().findByNamedParam( "from DesignElementImpl d where d.id in (:ids)", "ids",
                results );
    }

    /**
     * @param expressionExperiment
     * @param tableName
     * @return
     * @throws Exception
     */
    private Collection<ProbeLink> getLinks( ExpressionExperiment expressionExperiment, String tableName )
            throws Exception {
        String baseQueryString = "SELECT FIRST_DESIGN_ELEMENT_FK, SECOND_DESIGN_ELEMENT_FK, SCORE FROM " + tableName
                + " WHERE EXPRESSION_EXPERIMENT_FK = " + expressionExperiment.getId() + " limit ";
        int chunkSize = 1000000;
        Session session = getSessionFactory().openSession();
        long start = 0;
        Collection<ProbeLink> links = new ArrayList<ProbeLink>();
        while ( true ) {
            String queryString = baseQueryString + start + "," + chunkSize + ";";

            org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
            queryObject.addScalar( "FIRST_DESIGN_ELEMENT_FK", new LongType() );
            queryObject.addScalar( "SECOND_DESIGN_ELEMENT_FK", new LongType() );
            queryObject.addScalar( "SCORE", new DoubleType() );

            ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
            int count = 0;
            int iterations = 0;
            while ( scroll.next() ) {
                Long first_design_element_fk = scroll.getLong( 0 );
                Long second_design_element_fk = scroll.getLong( 1 );
                Double score = scroll.getDouble( 2 );

                ProbeLink oneLink = new ProbeLink();
                oneLink.setFirstDesignElementId( first_design_element_fk );
                oneLink.setSecondDesignElementId( second_design_element_fk );
                oneLink.setScore( score );
                links.add( oneLink );
                count++;
                if ( count == chunkSize ) {
                    start = start + chunkSize;
                    System.err.print( "." );
                    iterations++;
                    if ( iterations % 10 == 0 ) System.err.println();
                }
            }
            if ( count < chunkSize ) break;
        }
        log.info( "Load " + links.size() );
        session.close();
        return links;
    }

    /**
     * @param givenG
     * @return
     */
    private String getP2PClassName( ubic.gemma.model.genome.Gene givenG ) {
        String p2pClassName;
        if ( TaxonUtility.isHuman( givenG.getTaxon() ) )
            p2pClassName = "HumanProbeCoExpressionImpl";
        else if ( TaxonUtility.isMouse( givenG.getTaxon() ) )
            p2pClassName = "MouseProbeCoExpressionImpl";
        else if ( TaxonUtility.isRat( givenG.getTaxon() ) )
            p2pClassName = "RatProbeCoExpressionImpl";
        else
            // must be other
            p2pClassName = "OtherProbeCoExpressionImpl";
        return p2pClassName;
    }

    /**
     * Generate a name for the table to be used for coexpression analysis.
     * 
     * @param key the name of the taxon (human) or class name (HumanProbeCoExpressionImpl)
     * @param tmpTable if true, the table used is a temporary table.
     * @return
     */
    private String getTableName( String key, boolean tmpTable ) {
        String tableName = "";

        if ( key.equalsIgnoreCase( "human" ) || key.toUpperCase().startsWith( "HUMAN" ) ) {
            tableName = "HUMAN_PROBE_CO_EXPRESSION";
        } else if ( key.equalsIgnoreCase( "mouse" ) || key.toUpperCase().startsWith( "MOUSE" ) ) {
            tableName = "MOUSE_PROBE_CO_EXPRESSION";
        } else if ( key.equalsIgnoreCase( "rat" ) || key.toUpperCase().startsWith( "RAT" ) ) {
            tableName = "RAT_PROBE_CO_EXPRESSION";
        } else {
            tableName = "OTHER_PROBE_CO_EXPRESSION";
        }
        if ( tmpTable ) tableName = TMP_TABLE_PREFIX + tableName;

        return tableName;
    }

    /**
     * @param analysis
     */
    private void removeAnalysisObject( Analysis analysis ) {
        if ( analysis != null ) {
            log.info( "Deleting analysis object" );
            this.getHibernateTemplate().delete( analysis );
            this.getHibernateTemplate().flush();
        } else {
            log.info( "No analysis object associated with link " );
        }
    }

    /**
     * Used for creating simplified 'shuffled' links etc.
     * 
     * @param links
     * @param ee
     * @param tableName
     * @throws Exception
     */
    private void saveLinks( Collection<ProbeLink> links, ExpressionExperiment ee, String tableName ) throws Exception {
        if ( links == null || links.size() == 0 ) return;
        String queryString = "";
        Session session = getSessionFactory().openSession();
        Connection conn = session.connection();
        Statement s = conn.createStatement();

        this.eeId = ee.getId();
        int count = 0;
        int CHUNK_LIMIT = 10000;
        int total = links.size();
        Collection<String> linksInOneChunk = new ArrayList<String>();
        log.info( ee + ": Writing " + links.size() + " links into tables" );
        int chunkNum = 0;
        for ( ProbeLink link : links ) {
            linksInOneChunk.add( link.toSqlString() );
            count++;
            total--;
            if ( count == CHUNK_LIMIT || total == 0 ) {
                queryString = "INSERT INTO " + tableName
                        + "(FIRST_DESIGN_ELEMENT_FK, SECOND_DESIGN_ELEMENT_FK, SCORE, EXPRESSION_EXPERIMENT_FK) "
                        + " VALUES " + StringUtils.join( linksInOneChunk, "," ) + ";";
                s.executeUpdate( queryString );
                // conn.commit(); //not needed if autocomsmit is true.
                count = 0;
                linksInOneChunk.clear();
                chunkNum++;
                System.err.print( total + " " );
                if ( chunkNum % 20 == 0 ) System.err.println();
            }
        }
        log.info( " Finished writing " + links.size() + " links." );
        conn.close();
        session.close();

    }

    /**
     *
     */
    public class ProbeLink implements Link {
        private Long firstDesignElementId = 0L;
        private Long secondDesignElementId = 0L;
        private Double score = 0.0;

        public ProbeLink() {
        }

        public Long getFirstDesignElementId() {
            return firstDesignElementId;
        }

        public Double getScore() {
            return score;
        }

        public Long getSecondDesignElementId() {
            return secondDesignElementId;
        }

        public void setFirstDesignElementId( Long first_design_element_fk ) {
            this.firstDesignElementId = first_design_element_fk;
        }

        public void setScore( Double score ) {
            this.score = score;
        }

        public void setSecondDesignElementId( Long second_design_element_fk ) {
            this.secondDesignElementId = second_design_element_fk;
        }

        public String toSqlString() {
            return "(" + firstDesignElementId + ", " + secondDesignElementId + ",  " + score + ", " + eeId + ")";
        }

        public String toString() {
            return "DE1=" + firstDesignElementId + ", DE2=" + secondDesignElementId + ", SCORE=s" + score + ", EE="
                    + eeId;
        }
    }

}