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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.hibernate.SessionFactory;
import org.hibernate.type.DoubleType;
import org.hibernate.type.LongType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Repository;

import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.CommonQueries;
import ubic.gemma.util.NativeQueryUtils;
import ubic.gemma.util.TaxonUtility;

/**
 * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression
 * @version $Id$
 * @author joseph
 * @author paul
 */
@Repository
public class Probe2ProbeCoexpressionDaoImpl extends
        ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase {

    private static final String TMP_TABLE_PREFIX = "TMP_";

    private static Log log = LogFactory.getLog( Probe2ProbeCoexpressionDaoImpl.class.getName() );

    // FIXME figure out the taxon instead of this iteration.
    private static final String[] p2pClassNames = new String[] { "HumanProbeCoExpressionImpl",
            "MouseProbeCoExpressionImpl", "RatProbeCoExpressionImpl", "OtherProbeCoExpressionImpl" };

    @Autowired
    public Probe2ProbeCoexpressionDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    public Collection<ProbeLink> getTopCoexpressedLinks( ExpressionExperiment ee, double threshold, Integer limit ) {

        for ( String p2pClassName : p2pClassNames ) {

            final String queryString = "SELECT p2p.firstVector, p2p.secondVector, p2p.score FROM "
                    + getTableName( p2pClassName, false )
                    + " as p2p where EXPRESSION_EXPERIMENT_FK = :eeid AND score <= :threshold order by p2p.score";

            int oldmax = getHibernateTemplate().getMaxResults();
            if ( limit != null ) getHibernateTemplate().setMaxResults( limit );

            SQLQuery queryObject = super.getSession().createSQLQuery( queryString );
            queryObject.setMaxResults( 1 );
            queryObject.setParameter( "eeid", ee.getId() );
            queryObject.setParameter( "threshold", threshold );
            List<?> results = queryObject.list();

            // Was the right taxon or not?
            if ( results.iterator().next() == null ) {
                if ( limit != null ) {
                    getHibernateTemplate().setMaxResults( oldmax );
                }
                continue;
            }

            Collection<ProbeLink> links = new ArrayList<ProbeLink>();

            for ( Object o : results ) {
                Object[] oa = ( Object[] ) o;
                Long firstProbeId = ( Long ) oa[0];
                Long secondProbeId = ( Long ) oa[1];
                Double score = ( Double ) oa[2];

                ProbeLink link = new ProbeLink();

                assert firstProbeId != null;
                assert secondProbeId != null;

                link.setFirstDesignElementId( firstProbeId );
                link.setSecondDesignElementId( secondProbeId );
                link.setScore( score );
                links.add( link );

            }

            if ( limit != null ) {
                getHibernateTemplate().setMaxResults( oldmax );
            }

            return links;

        }
        return null;
    }

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

        Query query = super.getSession().createQuery( "DELETE from " + className + " d where d in (:vals)" );

        int count = 0;
        for ( Probe2ProbeCoexpression o : ( Collection<Probe2ProbeCoexpression> ) entities ) {
            batch.add( o );
            if ( batch.size() == batchSize ) {
                count += batch.size();
                query.setParameterList( "vals", batch );
                query.executeUpdate();
                super.getSession().flush();
                super.getSession().clear();
                batch.clear();

                if ( count % 100000 == 0 ) log.debug( "Deleted " + count + "/" + entities.size() + " links" );
            }
        }

        if ( batch.size() > 0 ) {
            count += batch.size();
            query.setParameterList( "vals", batch );
            query.executeUpdate();
            super.getSession().flush();
            super.getSession().clear();
        }

        if ( entities.size() > 0 && count != entities.size() )
            throw new IllegalStateException( "Failed to delete entries (deleted " + count + " of " + entities.size()
                    + ")" );

        // log.debug( "Deleted " + count + " links" );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDao#getCoexpressedProbes(java.util.
     * Collection, java.util.Collection, ubic.gemma.model.expression.experiment.ExpressionExperiment, java.lang.String)
     */
    public Collection<Long> getCoexpressedProbes( Collection<Long> queryProbeIds, Collection<Long> coexpressedProbeIds,
            ExpressionExperiment ee, String taxon ) {

        String tableName = getTableName( taxon, false );

        Collection<ProbeLink> links = getLinks( queryProbeIds, coexpressedProbeIds, ee.getId(), tableName );

        Collection<Long> results = new HashSet<Long>();
        if ( links == null || links.isEmpty() ) return results;

        for ( ProbeLink probeLink : links ) {
            results.add( probeLink.getFirstDesignElementId() );
            results.add( probeLink.getSecondDesignElementId() );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleCountLinks(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Integer handleCountLinks( ExpressionExperiment expressionExperiment ) throws Exception {

        for ( String p2pClassName : p2pClassNames ) {

            final String queryString = "SELECT COUNT(*) FROM " + getTableName( p2pClassName, false )
                    + " where EXPRESSION_EXPERIMENT_FK = :eeid";

            SQLQuery queryObject = super.getSession().createSQLQuery( queryString );
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleCreate(java.util.Collection)
     */
    @Override
    protected Collection<? extends Probe2ProbeCoexpression> handleCreate(
            final Collection<? extends Probe2ProbeCoexpression> links ) {

        Session session = getSession();

        int numDone = 0;
        List<Probe2ProbeCoexpression> result = new ArrayList<Probe2ProbeCoexpression>();
        for ( Probe2ProbeCoexpression object : links ) {
            session.save( object );
            result.add( object );
            if ( ++numDone % 500 == 0 ) {
                session.flush();
                session.clear();
            }
        }

        session.flush();
        session.clear();
        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleDeleteLinks(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void handleDeleteLinks( final ExpressionExperiment ee ) throws Exception {

        if ( ee == null ) {
            throw new IllegalArgumentException( "Experiment cannot be null" );
        }

        /*
         * Note that the expression experiment is not directly associated with P2P objects. The EE column in the P2P
         * tables is a denormalization and is indexed, but not accessible via Hibernate. Thus it can be much more
         * efficient to access P2P with native queries.
         */
        int totalDone = 0;
        ProbeCoexpressionAnalysis analysis = null;
        final int DELETE_CHUNK_SIZE = 10000;

        for ( String p2pClassName : p2pClassNames ) {

            final String findLinkAnalysisObject = "select p from ProbeCoexpressionAnalysisImpl p inner join"
                    + " p.experimentAnalyzed e where e = :ee";
            List o = this.getHibernateTemplate().findByNamedParam( findLinkAnalysisObject, "ee", ee );
            if ( o.size() > 0 ) {
                analysis = ( ProbeCoexpressionAnalysis ) o.iterator().next();
            }

            final String nativeDeleteQuery = "DELETE FROM " + getTableName( p2pClassName, false )
                    + " where EXPRESSION_EXPERIMENT_FK = :eeid limit " + DELETE_CHUNK_SIZE;

            SQLQuery q = super.getSession().createSQLQuery( nativeDeleteQuery );
            q.setParameter( "eeid", ee.getId() );

            StopWatch timer = new StopWatch();
            timer.start();

            while ( true ) {
                int deleted = q.executeUpdate();
                if ( deleted < DELETE_CHUNK_SIZE ) break;
                totalDone += deleted;
            }

            if ( totalDone > 0 ) {
                log.info( totalDone + " coexpression results removed for " + ee + ": " + timer.getTime() + "ms" );
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
     * @see
     * ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetExpressionExperimentsLinkTestedIn
     * (ubic.gemma.model.genome.Gene, java.util.Collection, boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<BioAssaySet> handleGetExpressionExperimentsLinkTestedIn( Gene gene,
            Collection expressionExperiments, boolean filterNonSpecific ) throws Exception {

        if ( expressionExperiments == null || expressionExperiments.isEmpty() ) {
            log.warn( "No expression experiments entered" );
            return new HashSet<BioAssaySet>();
        }

        // FIXME implement filterNonSpecific.
        if ( filterNonSpecific ) {
            throw new UnsupportedOperationException( "Sorry, filterNonSpecific is not supported yet" );
        }

        Collection<CompositeSequence> probes = CommonQueries.getCompositeSequences( gene, this.getSession( true ) );

        if ( probes.size() == 0 ) return new HashSet<BioAssaySet>();

        // Locate analyses which use these probes, return the expression experiments
        String queryString = "select distinct e from ProbeCoexpressionAnalysisImpl pca inner join"
                + " pca.experimentAnalyzed e inner join pca.probesUsed pu inner join pu.probe p where e in (:ees) and p in (:probes)";
        List result = this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "ees", "probes" },
                new Object[] { expressionExperiments, probes } );

        assert result.size() <= expressionExperiments.size();
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetExpressionExperimentsLinkTestedIn
     * (ubic.gemma.model.genome.Gene, ubic.gemma.model.genome.Gene, java.util.Collection, boolean)
     */
    @Override
    protected Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsLinkTestedIn( Gene geneA,
            Collection<Long> genesB, Collection<BioAssaySet> expressionExperiments, boolean filterNonSpecific )
            throws Exception {

        // FIXME implement filterNonSpecific.
        if ( filterNonSpecific ) {
            throw new UnsupportedOperationException( "Sorry, filterNonSpecific is not supported yet" );
        }

        Map<Long, Collection<BioAssaySet>> result = new HashMap<Long, Collection<BioAssaySet>>();

        // this is an upper bound - if it isn't in the query gene, it's not going to be a tested link
        Collection<BioAssaySet> eesA = getExpressionExperimentsLinkTestedIn( geneA, expressionExperiments,
                filterNonSpecific );
        if ( eesA.size() == 0 ) {
            return result;
        }

        return handleGetExpressionExperimentsTestedIn( genesB, expressionExperiments, filterNonSpecific );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetExpressionExperimentsTestedIn
     * (java.util.Collection, java.util.Collection, boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, Collection<BioAssaySet>> handleGetExpressionExperimentsTestedIn( final Collection<Long> genes,
            Collection<BioAssaySet> expressionExperiments, boolean filterNonSpecific ) {

        // FIXME implement filterNonSpecific.
        if ( filterNonSpecific ) {
            throw new UnsupportedOperationException( "Sorry, filterNonSpecific is not supported yet" );
        }

        String queryString = "select distinct pu,e from ProbeCoexpressionAnalysisImpl pca inner join pca.experimentAnalyzed e"
                + " inner join pca.probesUsed pu inner join fetch pu.probe where pu.id in (:probes) and e in (:ees)";

        Map<Long, Collection<BioAssaySet>> result = new HashMap<Long, Collection<BioAssaySet>>();

        if ( genes == null || genes.isEmpty() ) return result;

        // this step is fast.
        Map<Long, Collection<Long>> cs2genes = CommonQueries.getCs2GeneIdMap( genes, this.getSession() );

        if ( log.isDebugEnabled() )
            log.debug( cs2genes.size() + " probes for " + genes.size() + " genes to examined in "
                    + expressionExperiments.size() + " ees." );

        List eesre = new ArrayList();
        StopWatch watch = new StopWatch();
        watch.start();
        for ( Collection<Long> csBatch : BatchIterator.batches( cs2genes.keySet(), 2000 ) ) {
            eesre.addAll( this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "probes", "ees" },
                    new Object[] { csBatch, expressionExperiments } ) );
        }

        if ( watch.getTime() > 1000 ) {
            log.info( "Done in " + watch.getTime() + "ms: " + eesre.size() + " records." );
        }

        for ( Object o : eesre ) {
            Object[] ol = ( Object[] ) o;
            CoexpressionProbe c = ( CoexpressionProbe ) ol[0];
            BioAssaySet e = ( BioAssaySet ) ol[1];

            /*
             * Restrict clause in the query should make this true.
             */
            assert expressionExperiments.contains( e );

            Long probeId = c.getProbe().getId();

            if ( !cs2genes.containsKey( probeId ) ) {
                /*
                 * This means that, while there is coexpression for the probe, there no gene mapping.
                 */
                if ( log.isDebugEnabled() ) log.debug( "No probe to gene map for probe id=" + probeId );
                continue;
            }

            Collection<Long> geneIds = cs2genes.get( probeId );
            for ( Long id : geneIds ) {
                if ( !result.containsKey( id ) ) {
                    result.put( id, new HashSet<BioAssaySet>() );
                }
                result.get( id ).add( e );
            }

        }

        for ( Long gene : result.keySet() ) {
            assert result.get( gene ).size() <= expressionExperiments.size();
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetGenesTestedBy(ubic.gemma.model
     * .expression.experiment.ExpressionExperiment, boolean)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Long> handleGetGenesTestedBy( BioAssaySet ee, boolean filterNonSpecific ) throws Exception {

        // FIXME implement filterNonSpecific.
        if ( filterNonSpecific ) {
            throw new UnsupportedOperationException( "Sorry, filterNonSpecific is not supported yet" );
        }

        // this is _much_ faster than going through blatassociation.
        final String nativeQueryString = "SELECT gc.GENE FROM "
                + " EXPERIMENTS2EXPRESSION_EXPERIMENT_SETS e2ees INNER JOIN INVESTIGATION e ON e.ID=e2ees.EXPERIMENTS_FK "
                + "INNER JOIN EXPRESSION_EXPERIMENT_SET eeset ON eeset.ID=e2ees.EXPRESSION_EXPERIMENT_SETS_FK "
                + "INNER JOIN ANALYSIS a ON a.EXPRESSION_EXPERIMENT_SET_ANALYZED_FK=eeset.ID "
                + "INNER JOIN COEXPRESSION_PROBE pu ON pu.PROBE_COEXPRESSION_ANALYSIS_FK=a.ID "
                + "INNER JOIN GENE2CS gc ON gc.CS=pu.PROBE_FK WHERE a.class='ProbeCoexpressionAnalysisImpl' AND e.ID= :eeid ";

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
     * @see
     * ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetProbeCoExpression(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment, java.lang.String, boolean)
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
     * @seeubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetVectorsForLinks(java.util.
     * Collection, java.util.Collection)
     */
    @Override
    protected Map<Gene, Collection<DesignElementDataVector>> handleGetVectorsForLinks( Collection<Gene> genes,
            Collection<ExpressionExperiment> ees ) throws Exception {

        Gene testG = genes.iterator().next(); // todo: check to make sure that all the given genes are of the
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

            org.hibernate.Query queryObject = super.getSession().createQuery( queryStringFirstVector );
            queryObject.setParameterList( "collectionOfEE", ees );
            queryObject.setParameterList( "collectionOfGenes", genes );
            ScrollableResults list1 = queryObject.scroll();
            buildMap( results, list1 );

            // do query joining coexpressed genes through the secondVector to the firstVector
            queryObject = super.getSession().createQuery( queryStringSecondVector );
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
     * @see
     * ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handleGetVectorsForLinks(ubic.gemma.
     * model.genome.Gene, java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
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
     * @see
     * ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoBase#handlePrepareForShuffling(java.util.
     * Collection, java.lang.String, boolean)
     */
    @Override
    protected void handlePrepareForShuffling( Collection<ExpressionExperiment> ees, String taxon,
            boolean filterNonSpecific ) throws Exception {
        String tableName = getTableName( taxon, true );
        createTable( tableName );
        int i = 1;
        for ( Object ee : ees ) {
            log.info( "Preparing simplified probe-level links for EE " + ( ( ExpressionExperiment ) ee ).getShortName()
                    + " (" + i++ + "/" + ees.size() + ")" );
            processRawLinksForExperiment( ( ExpressionExperiment ) ee, taxon, filterNonSpecific );
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
     * Only used for experimental exploration of links, not used by regular applications.
     * 
     * @param tableName
     * @throws Exception
     */
    private void createTable( final String tableName ) throws Exception {
        // guard against mistakes...
        if ( !tableName.startsWith( TMP_TABLE_PREFIX ) ) {
            throw new IllegalStateException( "Attempt to create table named " + tableName );
        }

        Session session = getSession();

        String queryString = "DROP TABLE IF EXISTS " + tableName + ";";

        session.createSQLQuery( queryString ).executeUpdate();

        queryString = "CREATE TABLE " + tableName
                + "(id BIGINT NOT NULL AUTO_INCREMENT, FIRST_DESIGN_ELEMENT_FK BIGINT NOT NULL, "
                + "SECOND_DESIGN_ELEMENT_FK BIGINT NOT NULL, SCORE DOUBLE, EXPRESSION_EXPERIMENT_FK BIGINT NOT NULL, "
                + "PRIMARY KEY(id), KEY(EXPRESSION_EXPERIMENT_FK)) " + "ENGINE=MYISAM";

        session.createSQLQuery( queryString ).executeUpdate();
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
    @SuppressWarnings("unchecked")
    private Map<Long, Collection<Long>> getCs2GenesMap( final Collection<Long> csIds ) {

        final Map<Long, Collection<Long>> cs2genes = new HashMap<Long, Collection<Long>>();
        if ( csIds == null || csIds.size() == 0 ) return cs2genes;

        Session session = this.getSession();

        int CHUNK_LIMIT = 1000;

        Collection<Long> batch = new HashSet<Long>();

        for ( Iterator<Long> it = csIds.iterator(); it.hasNext(); ) {
            batch.add( it.next() );

            if ( batch.size() > 0 && ( batch.size() == CHUNK_LIMIT || !it.hasNext() ) ) {
                String queryString = "SELECT CS, GENE FROM GENE2CS, CHROMOSOME_FEATURE as C WHERE GENE2CS.GENE = C.ID and C.CLASS = 'GeneImpl' and "
                        + " CS in (" + StringUtils.join( batch, "," ) + ")";

                org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
                List results = queryObject.list();
                for ( Object r : results ) {
                    Object[] or = ( Object[] ) r;
                    Long csid = ( ( BigInteger ) or[0] ).longValue();
                    Long geneId = ( ( BigInteger ) or[1] ).longValue();

                    Collection<Long> geneIds = cs2genes.get( csid );
                    if ( geneIds == null ) {
                        geneIds = new HashSet<Long>();
                        cs2genes.put( csid, geneIds );
                    }
                    geneIds.add( geneId );
                }
                session.clear();
                batch.clear();
            }
        }

        return cs2genes;
    }

    /**
     * Retrieve links, if any, between two sets of probes in an EE.
     * <p>
     * IMPLEMENTATION NOTE: this requires that the triggers are installed so the DESIGN_ELEMENT columns are populated in
     * the PROBE_COEXIRESSION tables; and furthermore assumes that probes are stored twice (once with A-B and again B-A)
     * so only one query is needed.
     * 
     * @param probeAIds
     * @param probeBIds
     * @param eeId
     * @param tableName
     * @return links that are between the two sets of given probes, if any, within the given experiment.
     * @throws Exception
     */
    private Collection<ProbeLink> getLinks( Collection<Long> probeAIds, Collection<Long> probeBIds, Long eeId,
            String tableName ) {

        if ( probeAIds == null || probeBIds == null || probeAIds.isEmpty() || probeBIds.isEmpty() ) {
            log.info( "Called get links with null or empty collection a: " + probeAIds + "  b: " + probeBIds );
            return null;
        }

        String baseQueryString = "SELECT FIRST_DESIGN_ELEMENT_FK, SECOND_DESIGN_ELEMENT_FK FROM " + tableName
                + " WHERE EXPRESSION_EXPERIMENT_FK = " + eeId + " AND FIRST_DESIGN_ELEMENT_FK IN ("
                + StringUtils.join( probeAIds, "," ) + ") AND SECOND_DESIGN_ELEMENT_FK IN ("
                + StringUtils.join( probeBIds, "," ) + ") limit ";

        int chunkSize = 500000;
        Collection<ProbeLink> links = new ArrayList<ProbeLink>();

        Session session = getSession();

        long offset = 0;

        while ( true ) {
            String queryString = baseQueryString + offset + "," + chunkSize + ";";

            org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
            queryObject.addScalar( "FIRST_DESIGN_ELEMENT_FK", new LongType() );
            queryObject.addScalar( "SECOND_DESIGN_ELEMENT_FK", new LongType() );

            ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
            int count = 0;
            while ( scroll.next() ) {
                Long firstDEId = scroll.getLong( 0 );
                Long secondDEId = scroll.getLong( 1 );

                ProbeLink oneLink = new ProbeLink();
                oneLink.setFirstDesignElementId( firstDEId );
                oneLink.setSecondDesignElementId( secondDEId );

                links.add( oneLink );
                if ( ++count == chunkSize ) {
                    offset = offset + chunkSize;
                    log.info( "Read " + offset );
                }
            }
            if ( count < chunkSize ) break;
        }

        session.clear();

        return links;
    }

    /**
     * @param expressionExperiment
     * @param tableName
     * @return
     * @throws Exception
     */
    private Collection<ProbeLink> getLinks( final ExpressionExperiment expressionExperiment, String tableName ) {
        final String baseQueryString = "SELECT FIRST_DESIGN_ELEMENT_FK, SECOND_DESIGN_ELEMENT_FK, SCORE FROM "
                + tableName + " WHERE EXPRESSION_EXPERIMENT_FK = " + expressionExperiment.getId() + " limit ";
        final int chunkSize = 500000;
        final Collection<ProbeLink> links = new ArrayList<ProbeLink>();

        Session session = getSession();

        long offset = 0;

        while ( true ) {
            String queryString = baseQueryString + offset + "," + chunkSize + ";";

            org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
            queryObject.addScalar( "FIRST_DESIGN_ELEMENT_FK", new LongType() );
            queryObject.addScalar( "SECOND_DESIGN_ELEMENT_FK", new LongType() );
            queryObject.addScalar( "SCORE", new DoubleType() );

            List<?> results = queryObject.list();

            int count = 0;
            for ( Object o : results ) {
                Object[] oa = ( Object[] ) o;
                Long firstProbeId = ( Long ) oa[0];
                Long secondProbeId = ( Long ) oa[1];
                Double score = ( Double ) oa[2];

                ProbeLink link = new ProbeLink();

                assert firstProbeId != null;
                assert secondProbeId != null;

                link.setFirstDesignElementId( firstProbeId );
                link.setSecondDesignElementId( secondProbeId );
                link.setScore( score );
                links.add( link );
                if ( ++count == chunkSize ) {
                    offset = offset + chunkSize;
                    log.info( "Read " + offset );
                }
            }

            if ( count < chunkSize ) break;
        }
        log.info( "Done with " + expressionExperiment.getShortName() + ": Fetched " + links.size() );
        session.clear();

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
     * Dump the probe-probe data for an experiment to the working table for shuffling. This ignores links for probes
     * that are not mapped to 'known' genes.
     * 
     * @param ee
     * @param taxon
     * @throws Exception
     */
    private void processRawLinksForExperiment( ExpressionExperiment ee, String taxon, boolean filterNonSpecific )
            throws Exception {
        String tableName = getTableName( taxon, false );
        Collection<ProbeLink> links = getLinks( ee, tableName );
        Set<Long> csIds = new HashSet<Long>();
        for ( ProbeLink link : links ) {
            assert link.getFirstDesignElementId() != null;
            assert link.getSecondDesignElementId() != null;
            csIds.add( link.getFirstDesignElementId() );
            csIds.add( link.getSecondDesignElementId() );
        }
        Map<Long, Collection<Long>> cs2genes = getCs2GenesMap( csIds );
        links = filterNonSpecificAndRedundant( links, cs2genes, filterNonSpecific );
        String workingTableName = getTableName( taxon, true );
        savedSimplifiedLinks( links, ee, workingTableName );

    }

    /**
     * @param analysis
     */
    private void removeAnalysisObject( ProbeCoexpressionAnalysis analysis ) {
        if ( analysis != null ) {
            log.info( "Deleting analysis object" );

            this.getHibernateTemplate().delete( analysis ); // cascade to CoexpressionProbe collection
            this.getHibernateTemplate().flush();
        } else {
            log.info( "No analysis object associated with link " );
        }
    }

    /**
     * Write probe-level links to a temporary table; Used for creating simplified 'shuffled' links etc. The links
     * obtained are taken from the permanent store.
     * 
     * @param links
     * @param ee
     * @param tableName
     * @throws Exception
     */
    private void savedSimplifiedLinks( final Collection<ProbeLink> links, final ExpressionExperiment ee,
            final String tableName ) throws Exception {
        if ( links == null || links.size() == 0 ) return;

        final int CHUNK_LIMIT = 10000;

        this.getHibernateTemplate().execute( new HibernateCallback<Object>() {

            public Object doInHibernate( Session session ) throws HibernateException, SQLException {
                Collection<String> chunk = new ArrayList<String>();
                log.info( ee + ": Writing " + links.size() + " links into tables" );
                int chunkNum = 0;

                for ( Iterator<ProbeLink> it = links.iterator(); it.hasNext(); ) {
                    ProbeLink link = it.next();
                    link.setEeId( ee.getId() );
                    chunk.add( link.toSqlString() );
                    if ( chunk.size() > 0 && ( chunk.size() == CHUNK_LIMIT || !it.hasNext() ) ) {
                        String queryString = "INSERT INTO "
                                + tableName
                                + "(FIRST_DESIGN_ELEMENT_FK, SECOND_DESIGN_ELEMENT_FK, SCORE, EXPRESSION_EXPERIMENT_FK) "
                                + " VALUES " + StringUtils.join( chunk, "," ) + ";";
                        SQLQuery query = session.createSQLQuery( queryString );
                        int updated = query.executeUpdate();
                        session.flush();
                        session.clear();
                        chunk.clear();
                        if ( ++chunkNum % 20 == 0 ) {
                            log.info( "Processed chunk " + chunkNum + ", " + updated + " in current chunk" );
                        }
                    }
                }
                log.info( " Finished writing " + links.size() + " links." );
                return null;
            }
        } );

    }

}