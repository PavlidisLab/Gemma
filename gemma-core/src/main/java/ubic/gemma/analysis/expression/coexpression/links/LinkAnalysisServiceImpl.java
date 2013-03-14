/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.analysis.expression.coexpression.links;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.dataStructure.Link;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.Rank;
import ubic.gemma.analysis.preprocess.InsufficientProbesException;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.preprocess.filter.InsufficientSamplesException;
import ubic.gemma.analysis.preprocess.svd.ExpressionDataSVD;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionProbe;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisService;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.association.coexpression.*;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedLinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TooSmallDatasetLinkAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.Persister;
import ubic.gemma.security.SecurityServiceImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.TaxonUtility;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.*;

/**
 * Running link analyses through the spring context; will persist the results if the configuration says so. See
 * LinkAnalysisCli for more instructions.
 * 
 * @author Paul
 * @version $Id$
 */
@Component
public class LinkAnalysisServiceImpl implements LinkAnalysisService {

    private static class Creator {

        Method m;
        private Object[] arg;
        private Class<?> clazz;
        private BioAssaySet ebas;

        Creator( Class<?> clazz, BioAssaySet experiment ) {
            this.ebas = experiment;
            this.clazz = clazz;
            this.arg = new Object[] {};
            try {
                m = clazz.getMethod( "newInstance", new Class[] {} );
            } catch ( SecurityException e ) {
                throw new RuntimeException( e );
            } catch ( NoSuchMethodException e ) {
                throw new RuntimeException( e );
            }
        }

        public Probe2ProbeCoexpression create() {
            Probe2ProbeCoexpression result = null;
            try {
                result = ( Probe2ProbeCoexpression ) m.invoke( clazz, arg );
                result.setExpressionBioAssaySet( ebas );
                return result;
            } catch ( IllegalArgumentException e ) {
                throw new RuntimeException( e );
            } catch ( IllegalAccessException e ) {
                throw new RuntimeException( e );
            } catch ( InvocationTargetException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    private static final int LINK_BATCH_SIZE = 5000;

    private static Log log = LogFactory.getLog( LinkAnalysisServiceImpl.class );
    private static final boolean useDB = true; // useful for debugging.

    @Autowired private AuditTrailService auditTrailService;
    @Autowired private CompositeSequenceService csService;
    @Autowired private ExpressionExperimentService eeService;
    @Autowired private ExpressionDataMatrixService expressionDataMatrixService;
    @Autowired private ExpressionExperimentReportService expressionExperimentReportService;
    @Autowired private Persister persisterHelper;
    @Autowired private Probe2ProbeCoexpressionService ppService;
    @Autowired private QuantitationTypeService quantitationTypeService;
    @Autowired private ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired private ProbeCoexpressionAnalysisService probeCoexpressionAnalysisService;

    /**
     * Perform the analysis. No hibernate session is used. This step is purely computational.
     * 
     * @param ee
     * @param linkAnalysisConfig
     * @param filterConfig
     * @return
     */
    @Override
    public LinkAnalysis doAnalysis( ExpressionExperiment ee, LinkAnalysisConfig linkAnalysisConfig,
            FilterConfig filterConfig ) {
        LinkAnalysis la = new LinkAnalysis( linkAnalysisConfig );
        la.clear();

        try {

            Collection<ProcessedExpressionDataVector> dataVectors = ee.getProcessedExpressionDataVectors();

            ExpressionDataDoubleMatrix dataMatrix = expressionDataMatrixService.getFilteredMatrix( ee, filterConfig,
                    dataVectors );

            if ( dataMatrix.rows() == 0 ) {
                log.info( "No rows left after filtering" );
                throw new InsufficientProbesException( "No rows left after filtering" );
            } else if ( dataMatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER ) {
                throw new InsufficientProbesException( "To few rows (" + dataMatrix.rows()
                        + "), data sets are not analyzed unless they have at least "
                        + FilterConfig.MINIMUM_ROWS_TO_BOTHER + " rows" );
            }

            dataMatrix = this.normalize( dataMatrix, linkAnalysisConfig );

            /*
             * Link analysis section.
             */
            log.info( "Starting link analysis... " + ee );
            setUpForAnalysis( ee, la, dataVectors, dataMatrix );
            addAnalysisObj( ee, dataMatrix, filterConfig, linkAnalysisConfig, la );
            la.analyze();

            if ( Thread.currentThread().isInterrupted() ) {
                log.info( "Cancelled." );
                return null;
            }

        } catch ( Exception e ) {

            if ( linkAnalysisConfig.isUseDb() ) {
                logFailure( ee, e );
            }
            throw new RuntimeException( e );
        }

        return la;
    }

    /**
     * Get data that will be used by analysis. We have to fetch/thaw all parts of EE that will be used later since
     * analysis part doesn't have an open hibernate session.
     * 
     * @param eeId
     * @param linkAnalysisConfig
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadDataForAnalysis( Long eeId ) {
        ExpressionExperiment ee = eeService.load( eeId );

        log.info( "Fetching expression data ... " + ee );

        Collection<ProcessedExpressionDataVector> dataVectors = ee.getProcessedExpressionDataVectors();
        processedExpressionDataVectorService.thaw( dataVectors );
        return ee;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService#process(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, ubic.gemma.analysis.preprocess.filter.FilterConfig,
     * ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisConfig)
     */
    @Override
    public LinkAnalysis process( Long eeId, FilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig ) {

        ExpressionExperiment ee = eeService.load( eeId );

        try {
            LinkAnalysis la = new LinkAnalysis( linkAnalysisConfig );
            la.clear();

            log.info( "Fetching expression data ... " + ee );

            Collection<ProcessedExpressionDataVector> dataVectors = expressionDataMatrixService
                    .getProcessedExpressionDataVectors( ee );

            processedExpressionDataVectorService.thaw( dataVectors );

            process( ee, filterConfig, linkAnalysisConfig, la, dataVectors );

            log.info( "Done with processing of " + ee );
            return la;

        } catch ( Exception e ) {

            if ( linkAnalysisConfig.isUseDb() ) {
                logFailure( ee, e );
            }
            throw new RuntimeException( e );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService#process(ubic.gemma.model.genome.Taxon,
     * java.util.Collection, ubic.gemma.analysis.preprocess.filter.FilterConfig,
     * ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisConfig)
     */
    @Override
    public LinkAnalysis process( Taxon t, Collection<ProcessedExpressionDataVector> dataVectors,
            FilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig ) {
        ExpressionDataDoubleMatrix datamatrix = expressionDataMatrixService.getFilteredMatrix(
                linkAnalysisConfig.getArrayName(), filterConfig, dataVectors );

        if ( datamatrix.rows() == 0 ) {
            log.info( "No rows left after filtering" );
            throw new InsufficientProbesException( "No rows left after filtering" );
        } else if ( datamatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER ) {
            throw new InsufficientProbesException( "To few rows (" + datamatrix.rows()
                    + "), data sets are not analyzed unless they have at least " + FilterConfig.MINIMUM_ROWS_TO_BOTHER
                    + " rows" );
        }
        LinkAnalysis la = new LinkAnalysis( linkAnalysisConfig );

        datamatrix = this.normalize( datamatrix, linkAnalysisConfig );

        setUpForAnalysis( t, la, dataVectors, datamatrix );

        la.analyze();

        try {
            writeLinks( la, filterConfig, new PrintWriter( System.out ) );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return la;

    }

    /**
     * Save the analysis data.
     * 
     * @param ee
     * @param la
     * @param linkAnalysisConfig
     * @param filterConfig
     */
    @Override
    public void saveResults( ExpressionExperiment ee, LinkAnalysis la, LinkAnalysisConfig linkAnalysisConfig,
            FilterConfig filterConfig ) {
        try {
            Collection<ProcessedExpressionDataVector> dataVectors = ee.getProcessedExpressionDataVectors();
            Map<CompositeSequence, ProcessedExpressionDataVector> p2v = getProbe2VectorMap( dataVectors );

            if ( linkAnalysisConfig.isUseDb() && !linkAnalysisConfig.isTextOut() ) {

                saveLinks( p2v, la );

                audit( ee, "", LinkAnalysisEvent.Factory.newInstance() );

            } else if ( linkAnalysisConfig.isTextOut() ) {
                try {
                    PrintWriter w = new PrintWriter( System.out );
                    if ( linkAnalysisConfig.getOutputFile() != null ) {
                        w = new PrintWriter( linkAnalysisConfig.getOutputFile() );
                    }

                    writeLinks( la, filterConfig, w );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            }

            log.info( "Done with processing of " + ee );

        } catch ( Exception e ) {

            if ( linkAnalysisConfig.isUseDb() ) {
                logFailure( ee, e );
            }
            throw new RuntimeException( e );
        }

    }

    /**
     * @param ee
     * @param eeDoubleMatrix
     * @param filterConfig
     * @param linkAnalysisConfig
     * @param la
     */
    private void addAnalysisObj( ExpressionExperiment ee, ExpressionDataDoubleMatrix eeDoubleMatrix,
            FilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig, LinkAnalysis la ) {

        /*
         * Set up basics.
         */
        ProbeCoexpressionAnalysis analysis = linkAnalysisConfig.toAnalysis();

        analysis.setExperimentAnalyzed( ee );
        analysis.setName( ee.getShortName() + " link analysis" );
        analysis.getProtocol().setDescription(
                analysis.getProtocol().getDescription() + "# FilterConfig:\n" + filterConfig.toString() );

        /*
         * Add probes used. Note that this includes probes that were not ......
         */
        List<ExpressionDataMatrixRowElement> rowElements = eeDoubleMatrix.getRowElements();
        Collection<CoexpressionProbe> probesUsed = new HashSet<CoexpressionProbe>();
        for ( ExpressionDataMatrixRowElement el : rowElements ) {
            CoexpressionProbe p = CoexpressionProbe.Factory.newInstance();
            p.setProbe( el.getDesignElement() );
            /*
             * later we set node degree.
             */
            assert p.getProbe().getId() != null;

            probesUsed.add( p );
        }
        analysis.setProbesUsed( probesUsed );

        la.setAnalysisObj( analysis );
    }

    private void audit( ExpressionExperiment ee, String note, LinkAnalysisEvent eventType ) {
        expressionExperimentReportService.generateSummary( ee.getId() );
        ee = eeService.thawLite( ee );
        auditTrailService.addUpdateEvent( ee, eventType, note, true );
    }

    /**
     * @param ee
     * @param dataVectors
     */
    private void checkVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> dataVectors ) {
        if ( dataVectors == null || dataVectors.size() == 0 )
            throw new IllegalArgumentException( "No data vectors in " + ee );
    }

    // Delete old links for this expressionexperiment
    private void deleteOldLinks( LinkAnalysis la ) {
        ExpressionExperiment expressionExperiment = la.getExpressionExperiment();
        log.info( "Deleting any old links for " + expressionExperiment + " ..." );
        ppService.deleteLinks( expressionExperiment );
    }

    /**
     * Populates the analysis object with the node degrees for the probes.
     * 
     * @param la
     */
    private void fillInNodeDegree( LinkAnalysis la ) {

        Map<Long, Integer> pId2ND = new HashMap<Long, Integer>();
        DoubleArrayList vals = new DoubleArrayList();
        int j = la.getDataMatrix().rows();

        // pull out the raw node degrees.
        for ( int i = 0; i < j; i++ ) {
            int pd = la.getProbeDegree( i );
            long id = la.getDataMatrix().getDesignElementForRow( i ).getId();
            pId2ND.put( id, pd );
            vals.add( pd );
        }

        // Note: There will be a lot of ties, especially at low node degrees.
        DoubleArrayList ranks = Rank.rankTransform( vals );

        // convert to relative ranks.
        Map<Long, Double> pId2NDRank = new HashMap<Long, Double>();
        for ( int i = 0; i < j; i++ ) {
            long id = la.getDataMatrix().getDesignElementForRow( i ).getId();
            double rank = ranks.get( i );
            rank = rank / j;
            pId2NDRank.put( id, rank );
        }

        boolean gotDegrees = false;
        for ( CoexpressionProbe p : la.getAnalysisObj().getProbesUsed() ) {
            Long pid = p.getProbe().getId();
            assert pid != null;
            if ( pId2ND.containsKey( pid ) ) {
                p.setNodeDegree( pId2ND.get( pid ) );
                p.setNodeDegreeRank( pId2NDRank.get( pid ) );
                gotDegrees = true;
            }
        }
        assert gotDegrees : "No node degree information was in the data structures"; // this would be a BUG.
    }

    /**
     * Somewhat misnamed. It fills in the probe2gene map for the linkAnalysis, but also returns a map of composite
     * sequence to vector.
     * 
     * @param la
     * @param dataVectors
     * @param eeDoubleMatrix
     * @return map of probes to vectors.
     */
    private void getProbe2GeneMap( LinkAnalysis la, Collection<ProcessedExpressionDataVector> dataVectors,
            ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        log.info( "Getting probe-to-gene map for retained probes." );

        // This excludes probes that were filtered out
        Collection<CompositeSequence> probesForVectors = new HashSet<CompositeSequence>();
        for ( DesignElementDataVector v : dataVectors ) {
            CompositeSequence cs = v.getDesignElement();
            if ( eeDoubleMatrix.getRow( cs ) != null ) probesForVectors.add( cs );
        }

        Map<CompositeSequence, Collection<BioSequence2GeneProduct>> specificityData = csService
                .getGenesWithSpecificity( probesForVectors );

        /*
         * Convert the specificity
         */
        Map<CompositeSequence, Collection<Collection<Gene>>> probeToGeneMap = new HashMap<CompositeSequence, Collection<Collection<Gene>>>();
        for ( CompositeSequence cs : specificityData.keySet() ) {
            if ( !probeToGeneMap.containsKey( cs ) ) {
                probeToGeneMap.put( cs, new HashSet<Collection<Gene>>() );
            }
            Collection<BioSequence2GeneProduct> bioSequenceToGeneProducts = specificityData.get( cs );
            Collection<Gene> cluster = new HashSet<Gene>();
            for ( BioSequence2GeneProduct bioSequence2GeneProduct : bioSequenceToGeneProducts ) {
                Gene gene = bioSequence2GeneProduct.getGeneProduct().getGene();
                cluster.add( gene );
            }

            /*
             * This is important. We leave the collection empty unless there are actually genes mapped.
             */
            if ( !cluster.isEmpty() ) probeToGeneMap.get( cs ).add( cluster );
        }

        la.setProbeToGeneMap( probeToGeneMap );
    }

    /**
     * @param dataVectors
     * @return map of probes to vectors.
     */
    private Map<CompositeSequence, ProcessedExpressionDataVector> getProbe2VectorMap(
            Collection<ProcessedExpressionDataVector> dataVectors ) {
        Map<CompositeSequence, ProcessedExpressionDataVector> p2v = new HashMap<CompositeSequence, ProcessedExpressionDataVector>();
        for ( ProcessedExpressionDataVector v : dataVectors ) {
            CompositeSequence cs = v.getDesignElement();
            p2v.put( cs, v );
        }
        return p2v;
    }

    /**
     * @param numColumns
     * @param w
     * @param c helper class
     * @param metric e.g. Pearson Correlation
     * @param analysisObj
     * @return
     */
    private Probe2ProbeCoexpression initCoexp( int numColumns, double w, Creator c, QuantitationType metric,
            ProbeCoexpressionAnalysis analysisObj ) {
        Probe2ProbeCoexpression ppCoexpression = c.create();
        ppCoexpression.setScore( w );
        ppCoexpression.setPvalue( CorrelationStats.pvalue( w, numColumns ) );
        ppCoexpression.setMetric( metric );
        ppCoexpression.setSourceAnalysis( analysisObj );
        return ppCoexpression;
    }

    /**
     * @param expressionExperiment
     * @param e
     */
    private void logFailure( ExpressionExperiment expressionExperiment, Exception e ) {

        if ( e instanceof InsufficientSamplesException ) {
            audit( expressionExperiment, e.getMessage(), TooSmallDatasetLinkAnalysisEvent.Factory.newInstance() );
        } else if ( e instanceof InsufficientProbesException ) {
            audit( expressionExperiment, e.getMessage(), TooSmallDatasetLinkAnalysisEvent.Factory.newInstance() );
        } else {
            log.error( "While processing " + expressionExperiment, e );
            audit( expressionExperiment, ExceptionUtils.getFullStackTrace( e ),
                    FailedLinkAnalysisEvent.Factory.newInstance() );
        }
    }

    /**
     * Normalize the data, as configured (possibly no normalization).
     */
    private ExpressionDataDoubleMatrix normalize( ExpressionDataDoubleMatrix datamatrix, LinkAnalysisConfig config ) {

        ExpressionDataSVD svd;
        switch ( config.getNormalizationMethod() ) {
            case none:
                return datamatrix;
            case SVD:
                log.info( "SVD normalizing" );
                svd = new ExpressionDataSVD( datamatrix, true );
                return svd.removeHighestComponents( 1 );
            case SPELL:
                log.info( "Computing U matrix via SVD" );
                svd = new ExpressionDataSVD( datamatrix, true );
                return svd.uMatrixAsExpressionData();
            case BALANCE:
                log.info( "SVD-balanceing" );
                svd = new ExpressionDataSVD( datamatrix, true );
                return svd.equalize();
            default:
                return null;
        }
    }

    /**
     * Process a new link, adding it to the p2plinks collection and persisting if the p2plinks collection is big enough
     * ({@link LINK_BATCH_SIZE})
     * 
     * @param numColumns
     * @param p2plinks
     * @param w
     * @param v1
     * @param v2
     * @param metric type of score (pearson correlation for example)
     * @param analysisObj
     * @param c class that can create instances of the correct type of probe2probecoexpression.
     */
    private void persist( int numColumns, List<Probe2ProbeCoexpression> p2plinks, double w,
            ProcessedExpressionDataVector v1, ProcessedExpressionDataVector v2, QuantitationType metric,
            ProbeCoexpressionAnalysis analysisObj, Creator c ) {

        Probe2ProbeCoexpression ppCoexpression = initCoexp( numColumns, w, c, metric, analysisObj );

        ppCoexpression.setFirstVector( v1 );
        ppCoexpression.setSecondVector( v2 );

        p2plinks.add( ppCoexpression );

        if ( p2plinks.size() >= LINK_BATCH_SIZE ) {
            this.ppService.create( p2plinks );
            p2plinks.clear();
        }
    }

    /**
     * @param ee
     * @param filterConfig
     * @param linkAnalysisConfig
     * @param la
     * @param dataVectors
     */
    private void process( ExpressionExperiment ee, FilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig,
            LinkAnalysis la, Collection<ProcessedExpressionDataVector> dataVectors ) {
        checkVectors( ee, dataVectors );

        ExpressionDataDoubleMatrix datamatrix = expressionDataMatrixService.getFilteredMatrix( ee, filterConfig,
                dataVectors );

        if ( datamatrix.rows() == 0 ) {
            log.info( "No rows left after filtering" );
            throw new InsufficientProbesException( "No rows left after filtering" );
        } else if ( datamatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER ) {
            throw new InsufficientProbesException( "To few rows (" + datamatrix.rows()
                    + "), data sets are not analyzed unless they have at least " + FilterConfig.MINIMUM_ROWS_TO_BOTHER
                    + " rows" );
        }

        datamatrix = this.normalize( datamatrix, linkAnalysisConfig );

        /*
         * Link analysis section.
         */
        log.info( "Starting link analysis... " + ee );
        setUpForAnalysis( ee, la, dataVectors, datamatrix );
        addAnalysisObj( ee, datamatrix, filterConfig, linkAnalysisConfig, la );
        Map<CompositeSequence, ProcessedExpressionDataVector> p2v = getProbe2VectorMap( dataVectors );
        la.analyze();

        if ( Thread.currentThread().isInterrupted() ) {
            log.info( "Cancelled." );
            return;
        }

        // Output
        if ( linkAnalysisConfig.isUseDb() && !linkAnalysisConfig.isTextOut() ) {

            Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
            ees.add( ee );

            saveLinks( p2v, la );

            audit( ee, "", LinkAnalysisEvent.Factory.newInstance() );

        } else if ( linkAnalysisConfig.isTextOut() ) {
            try {
                PrintWriter w = new PrintWriter( System.out );
                if ( linkAnalysisConfig.getOutputFile() != null ) {
                    w = new PrintWriter( linkAnalysisConfig.getOutputFile() );
                }

                writeLinks( la, filterConfig, w );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    /**
     * Persist the links to the database. This takes care of saving a 'flipped' version of the links.
     * <p>
     * Note that if "known genes only" is set, then links between probes that _only_ target other types of genes will
     * not be stored. However, because the links are stored at the probe level, the links saved will potentially
     * includes links between any type of gene (but always at least between two known genes).
     * 
     * @param p2v
     * @param la
     */
    private void saveLinks( Map<CompositeSequence, ProcessedExpressionDataVector> p2v, LinkAnalysis la ) {

        if ( useDB ) {
            deleteOldLinks( la );
            // Complete and persist the new analysis object.
            fillInNodeDegree( la );
            ProbeCoexpressionAnalysis analysisObj = la.getAnalysisObj();

            analysisObj = ( ProbeCoexpressionAnalysis ) persisterHelper.persist( analysisObj );
            la.setAnalysisObj( analysisObj );
        }

        log.info( "Start submitting data to database." );
        StopWatch watch = new StopWatch();
        watch.start();

        ObjectArrayList links = la.getKeep();

        /*
         * Important implementation note: For efficiency reason, it is important that they be stored in order of "x"
         * (where x is the index of the first DEDV in the correlation matrix, not the ID of the DEDV.) in the database:
         * so all all links with probe=x are clustered. (the actual order of x1 vs x2 doesn't matter, just so long they
         * are clustered). This makes retrievals much faster for the most common types of queries. Note that this relies
         * on expected behavior that links.sort() will sort by the x coordinate. It also requires that when we persist
         * the elements, we retain the order.
         */
        links.sort();

        if ( log.isDebugEnabled() ) {
            for ( Object link : links.elements() ) {
                if ( link == null ) continue;
                log.debug( ( ( Link ) link ).getx() + " " + ( ( Link ) link ).gety() );
            }
        }

        if ( useDB ) {
            saveLinks( p2v, la, links, false );
        }

        /*
         * now create 'reversed' links, first by sorting by the 'y' coordinate. Again, this sort is critical to keep the
         * links in an ordering that the RDBMS can use efficiently.
         */
        links.mergeSortFromTo( 0, links.size() - 1, new Comparator<Link>() {
            @Override
            public int compare( Link a, Link b ) {
                if ( a == null || b == null ) return 1;

                if ( a.gety() < b.gety() ) {
                    return -1;
                } else if ( a.gety() > b.gety() ) {
                    return 1;
                } else {
                    if ( a.getx() < b.getx() ) {
                        return -1;
                    } else if ( a.getx() > b.getx() ) {
                        return 1;
                    }
                    return 0;
                }
            }
        } );

        log.info( "Saving flipped links" );

        if ( log.isDebugEnabled() ) {
            for ( Object link : links.elements() ) {
                if ( link == null ) continue;
                log.debug( ( ( Link ) link ).getx() + " " + ( ( Link ) link ).gety() );
            }
        }

        if ( useDB ) {
            saveLinks( p2v, la, links, true );
        }

        watch.stop();
        log.info( "Seconds to process " + links.size() + " links plus flipped versions:" + watch.getTime() / 1000.0 );

    }

    @Autowired
    UserManager userManager;

    /**
     * @param p2v
     * @param la
     * @param links
     * @boolean flip
     */
    private void saveLinks( Map<CompositeSequence, ProcessedExpressionDataVector> p2v, LinkAnalysis la,
            ObjectArrayList links, boolean flip ) {
        int numColumns = la.getDataMatrix().columns();

        QuantitationType metric = quantitationTypeService.findOrCreate( la.getMetric() );

        Taxon taxon = la.getTaxon();
        Creator c;

        if ( SecurityServiceImpl.isUserAnonymous() ) {
            throw new IllegalStateException( "Can't run link analysis anonymously" );
        } else if ( !SecurityServiceImpl.isUserAdmin() ) {
            log.info( "Creating coexpression analysis for user's data" );
            c = new Creator( UserProbeCoExpression.Factory.class, la.getExpressionExperiment() );
        } else if ( TaxonUtility.isMouse( taxon ) ) {
            c = new Creator( MouseProbeCoExpression.Factory.class, la.getExpressionExperiment() );
        } else if ( TaxonUtility.isRat( taxon ) ) {
            c = new Creator( RatProbeCoExpression.Factory.class, la.getExpressionExperiment() );
        } else if ( TaxonUtility.isHuman( taxon ) ) {
            c = new Creator( HumanProbeCoExpression.Factory.class, la.getExpressionExperiment() );
        } else {
            c = new Creator( OtherProbeCoExpression.Factory.class, la.getExpressionExperiment() );
        }

        Integer probeDegreeThreshold = la.getConfig().getProbeDegreeThreshold();
        int skippedDueToDegree = 0;

        List<Probe2ProbeCoexpression> p2plinkBatch = new ArrayList<Probe2ProbeCoexpression>();
        for ( int i = 0, n = links.size(); i < n; i++ ) {
            Object val = links.getQuick( i );
            if ( val == null ) continue;
            Link m = ( Link ) val;
            Double w = m.getWeight();

            assert w != null;

            int x = m.getx();
            int y = m.gety();

            /*
             * Note that we use OR here - we don't require that _both_ probes have high degree.
             */
            if ( probeDegreeThreshold > 0
                    && ( la.getProbeDegree( x ) > probeDegreeThreshold || la.getProbeDegree( y ) > probeDegreeThreshold ) ) {
                skippedDueToDegree++;
                continue;
            }

            CompositeSequence p1 = la.getProbe( x );
            CompositeSequence p2 = la.getProbe( y );

            ProcessedExpressionDataVector v1 = p2v.get( p1 );
            ProcessedExpressionDataVector v2 = p2v.get( p2 );

            if ( flip ) {
                persist( numColumns, p2plinkBatch, w, v2, v1, metric, la.getAnalysisObj(), c );
            } else {
                persist( numColumns, p2plinkBatch, w, v1, v2, metric, la.getAnalysisObj(), c );
            }

            if ( i > 0 && i % 50000 == 0 ) {
                log.info( i - skippedDueToDegree + " links persisted (" + skippedDueToDegree + " skipped so far)" );
            }

        }

        log.info( skippedDueToDegree + " of " + links.size() + " links ("
                + String.format( "%.1f", 100 * skippedDueToDegree / ( double ) links.size() )
                + "%) skipped due to high probe node degree" );

        // last batch.
        if ( p2plinkBatch.size() > 0 ) {
            this.ppService.create( p2plinkBatch );
            p2plinkBatch.clear();
        }

        /*
         * Update the meta-data about the analysis (but just do it once)
         */
        if ( !flip ) {
            ProbeCoexpressionAnalysis analysisObj = la.getAnalysisObj();
            assert analysisObj.getId() != null;
            analysisObj.setNumberOfElementsAnalyzed( la.getDataMatrix().rows() );
            analysisObj.setNumberOfLinks( links.size() - skippedDueToDegree );
            probeCoexpressionAnalysisService.update( analysisObj );
        }

    }

    /**
     * Initializes the LinkAnalysis object; populates the probe2gene map.
     * 
     * @param ee
     * @param la
     * @param dataVectors
     * @param eeDoubleMatrix
     */
    private void setUpForAnalysis( ExpressionExperiment ee, LinkAnalysis la,
            Collection<ProcessedExpressionDataVector> dataVectors, ExpressionDataDoubleMatrix eeDoubleMatrix ) {

        la.setDataMatrix( eeDoubleMatrix );

        if ( ee != null ) {
            la.setTaxon( eeService.getTaxon( ee ) );
            la.setExpressionExperiment( ee );
        }

        getProbe2GeneMap( la, dataVectors, eeDoubleMatrix );
    }

    /**
     * Initializes the LinkAnalysis object for data file input; populates the probe2gene map.
     * 
     * @param ee
     * @param la
     * @param dataVectors
     * @param eeDoubleMatrix
     */
    private void setUpForAnalysis( Taxon t, LinkAnalysis la, Collection<ProcessedExpressionDataVector> dataVectors,
            ExpressionDataDoubleMatrix eeDoubleMatrix ) {

        la.setDataMatrix( eeDoubleMatrix );
        la.setTaxon( t );
        getProbe2GeneMap( la, dataVectors, eeDoubleMatrix );
    }

    /**
     * Write links as text. If "known genes only", only known genes will be displayed, even if the probe in question
     * targets other "types" of genes.
     * 
     * @param la
     * @param wr
     */
    private void writeLinks( final LinkAnalysis la, FilterConfig filterConfig, Writer wr ) throws IOException {
        Map<CompositeSequence, Collection<Collection<Gene>>> probeToGeneMap = la.getProbeToGeneMap();
        ObjectArrayList links = la.getKeep();
        double subsetSize = la.getConfig().getSubsetSize();
        List<String> buf = new ArrayList<String>();
        if ( la.getConfig().isSubset() && links.size() > subsetSize ) {
            la.getConfig().setSubsetUsed( true );
        }
        wr.write( la.getConfig().toString() );
        wr.write( filterConfig.toString() );
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits( 4 );

        Integer probeDegreeThreshold = la.getConfig().getProbeDegreeThreshold();

        Transformer officialSymbolExtractor = new Transformer() {
            @Override
            public Object transform( Object input ) {
                Gene g = ( Gene ) input;

                return g.getOfficialSymbol();
            }
        };

        int i = 0;
        int keptLinksCount = 0;
        Random generator = new Random();
        double rand = 0.0;
        double fraction = subsetSize / links.size();
        int skippedDueToDegree = 0;
        for ( int n = links.size(); i < n; i++ ) {

            Object val = links.getQuick( i );
            if ( val == null ) continue;
            Link m = ( Link ) val;
            Double w = m.getWeight();

            assert w != null;

            int x = m.getx();
            int y = m.gety();

            if ( probeDegreeThreshold > 0
                    && ( la.getProbeDegree( x ) > probeDegreeThreshold || la.getProbeDegree( y ) > probeDegreeThreshold ) ) {
                skippedDueToDegree++;
                continue;
            }

            CompositeSequence p1 = la.getProbe( x );
            CompositeSequence p2 = la.getProbe( y );

            Collection<Collection<Gene>> g1 = probeToGeneMap.get( p1 );
            Collection<Collection<Gene>> g2 = probeToGeneMap.get( p2 );

            List<String> genes1 = new ArrayList<String>();
            for ( Collection<Gene> cluster : g1 ) {

                if ( cluster.isEmpty() ) continue;

                String t = StringUtils.join( new TransformIterator( cluster.iterator(), officialSymbolExtractor ), "," );
                genes1.add( t );
            }

            List<String> genes2 = new ArrayList<String>();
            for ( Collection<Gene> cluster : g2 ) {

                if ( cluster.isEmpty() ) continue;

                String t = StringUtils.join( new TransformIterator( cluster.iterator(), officialSymbolExtractor ), "," );
                genes2.add( t );
            }

            if ( genes2.size() == 0 || genes1.size() == 0 ) {
                continue;
            }

            String gene1String = StringUtils.join( genes1.iterator(), "|" );
            String gene2String = StringUtils.join( genes2.iterator(), "|" );

            if ( gene1String.equals( gene2String ) ) {
                continue;
            }

            if ( ++keptLinksCount % 50000 == 0 ) {
                log.info( keptLinksCount + " links retained" );
            }

            if ( la.getConfig().isSubsetUsed() ) {
                rand = generator.nextDouble();
                if ( rand > fraction ) continue;
            }

            buf.add( p1.getId() + "\t" + p2.getId() + "\t" + gene1String + "\t" + gene2String + "\t" + nf.format( w )
                    + "\n" );// save links
            // wr.write( p1.getId() + "\t" + p2.getId() + "\t" + gene1String + "\t" + gene2String + "\t" + nf.format( w
            // ) + "\n" );

        }

        wr.write( "# totalLinks:" + keptLinksCount + "\n" );
        wr.write( "# printedLinks:" + buf.size() + "\n" );
        wr.write( "# skippedDueToHighNodeDegree:" + skippedDueToDegree + "\n" );

        for ( String line : buf ) {// write links to file
            wr.write( line );
        }

        if ( la.getConfig().isSubsetUsed() ) {// subset option activated
            log.info( "Done, " + keptLinksCount + "/" + links.size() + " links kept, " + buf.size() + " links printed" );
            // wr.write("# Amount of links before subsetting/after subsetting: " + links.size() + "/" + numPrinted +
            // "\n" );
        } else {
            log.info( "Done, " + keptLinksCount + "/" + links.size() + " links printed (some may have been filtered)" );
        }
        wr.flush();

    }
}
