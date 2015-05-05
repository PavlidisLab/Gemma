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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.dataStructure.Link;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.analysis.preprocess.InsufficientProbesException;
import ubic.gemma.analysis.preprocess.OutlierDetails;
import ubic.gemma.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.preprocess.filter.InsufficientSamplesException;
import ubic.gemma.analysis.preprocess.svd.ExpressionDataSVD;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.coexpression.CoexpCorrelationDistribution;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedLinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TooSmallDatasetLinkAnalysisEvent;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import cern.colt.list.ObjectArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.jet.math.Functions;

/**
 * Running link analyses through the spring context; will persist the results if the configuration says so. See
 * LinkAnalysisCli for more instructions.
 * 
 * @author Paul
 * @version $Id$
 */
@Component
public class LinkAnalysisServiceImpl implements LinkAnalysisService {

    private static Log log = LogFactory.getLog( LinkAnalysisServiceImpl.class );

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private CompositeSequenceService csService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    @Autowired
    private OutlierDetectionService outlierDetectionService;

    @Autowired
    private LinkAnalysisPersister persister;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService#process(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, ubic.gemma.analysis.preprocess.filter.FilterConfig,
     * ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisConfig)
     */
    @Override
    public LinkAnalysis process( ExpressionExperiment ee, FilterConfig filterConfig,
            LinkAnalysisConfig linkAnalysisConfig ) {

        try {
            LinkAnalysis la = new LinkAnalysis( linkAnalysisConfig );
            la.clear();

            /*
             * TODO: make this possible for BioAssaySets rather than just ExpressionExperiments
             */
            log.info( "Fetching expression data for " + ee );

            Collection<ProcessedExpressionDataVector> dataVectors = expressionDataMatrixService
                    .getProcessedExpressionDataVectors( ee );

            processedExpressionDataVectorService.thaw( dataVectors );

            log.info( "Starting analysis" );
            analyze( ee, filterConfig, linkAnalysisConfig, la, dataVectors );

            log.info( "Done with analysis phase, starting persistence" );
            saveResults( ee, la, linkAnalysisConfig, filterConfig );
            log.info( "Done with saving results for " + ee );
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
    public LinkAnalysis processVectors( Taxon t, Collection<ProcessedExpressionDataVector> dataVectors,
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
        CoexpressionAnalysis analysis = linkAnalysisConfig.toAnalysis();

        analysis.setExperimentAnalyzed( ee );
        analysis.setName( ee.getShortName() + " link analysis" );
        analysis.getProtocol().setDescription(
                analysis.getProtocol().getDescription() + "# FilterConfig:\n" + filterConfig.toString() );

        // /*
        // * Add probes used. Note that this includes probes that were not ......
        // */
        // List<ExpressionDataMatrixRowElement> rowElements = eeDoubleMatrix.getRowElements();
        // Collection<CoexpressionProbe> probesUsed = new HashSet<CoexpressionProbe>();
        // for ( ExpressionDataMatrixRowElement el : rowElements ) {
        // CoexpressionProbe p = CoexpressionProbe.Factory.newInstance();
        // p.setProbe( el.getDesignElement() );
        // /*
        // * later we set node degree.
        // */
        // assert p.getProbe().getId() != null;
        //
        // probesUsed.add( p );
        // }
        // analysis.setProbesUsed( probesUsed );

        la.setAnalysisObj( analysis );
    }

    /**
     * @param ee
     * @param filterConfig
     * @param linkAnalysisConfig
     * @param la
     * @param dataVectors
     */
    private void analyze( ExpressionExperiment ee, FilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig,
            LinkAnalysis la, Collection<ProcessedExpressionDataVector> dataVectors ) {

        if ( !filterConfig.isDistinctValueThresholdSet() ) {
            filterConfig.setLowDistinctValueCut( FilterConfig.DEFAULT_DISTINCTVALUE_FRACTION );
        }

        qcCheck( linkAnalysisConfig, ee );

        ExpressionDataDoubleMatrix datamatrix = expressionDataMatrixService.getFilteredMatrix( ee, filterConfig,
                dataVectors );

        setUpForAnalysis( ee, la, dataVectors, datamatrix );

        Map<CompositeSequence, Set<Gene>> probeToGeneMap = la.getProbeToGeneMap();

        assert !probeToGeneMap.isEmpty();

        /*
         * remove probes that have no gene mapped to them, not just those that have no sequence
         */
        datamatrix = filterUnmappedProbes( datamatrix, probeToGeneMap );

        if ( datamatrix.rows() == 0 ) {
            log.info( "No rows left after filtering" );
            throw new InsufficientProbesException( "No rows left after filtering" );
        } else if ( datamatrix.rows() < FilterConfig.MINIMUM_ROWS_TO_BOTHER ) {
            throw new InsufficientProbesException( "To few rows (" + datamatrix.rows()
                    + "), data sets are not analyzed unless they have at least " + FilterConfig.MINIMUM_ROWS_TO_BOTHER
                    + " rows" );
        }

        log.info( "Starting link analysis... " + ee );

        datamatrix = this.normalize( datamatrix, linkAnalysisConfig );

        /*
         * Link analysis section.
         */
        addAnalysisObj( ee, datamatrix, filterConfig, linkAnalysisConfig, la );
        la.analyze();

        CoexpCorrelationDistribution corrDist = la.getCorrelationDistribution();

        // another qc check.
        if ( linkAnalysisConfig.isCheckCorrelationDistribution() ) {
            diagnoseCorrelationDistribution( ee, corrDist );
        }
    }

    private void audit( ExpressionExperiment ee, String note, LinkAnalysisEvent eventType ) {
        expressionExperimentReportService.generateSummary( ee.getId() );
        ee = eeService.thawLite( ee );
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    /**
     * @param bin
     * @param numBins
     * @return
     */
    private double binToCorrelation( int bin, int numBins ) {
        return bin * 2.0 / numBins - 1.0;
    }

    /**
     * Check properties of the distribution TODO refactor this out.
     * 
     * @throws UnsuitableForAnalysisException
     */
    private void diagnoseCorrelationDistribution( ExpressionExperiment ee, CoexpCorrelationDistribution corrDist )
            throws UnsuitableForAnalysisException {

        /*
         * Find the median, etc.
         */
        ByteArrayConverter bac = new ByteArrayConverter();
        double[] binCounts = bac.byteArrayToDoubles( corrDist.getBinCounts() );
        int numBins = binCounts.length;
        DoubleMatrix1D histogram = new DenseDoubleMatrix1D( binCounts );

        // QC parameters; quantile, not correlation
        double lowerLimitofMiddle = 0.45;
        double upperLimitofMiddle = 0.55;
        double tailFraction = 0.1;

        // normalize
        histogram.assign( Functions.div( histogram.zSum() ) );

        double lowerTailDensity = 0.0;
        double upperTailDensity = 0.0;
        double median = 0.0;
        double s = 0.0; // cumulative
        double middleDensity = 0.0;
        for ( int bin = 0; bin < histogram.size(); bin++ ) {

            // cumulate
            s += histogram.get( bin );

            /*
             * Perhaps these should be adjusted based on the sample size; for smaller data sets, more of the data is
             * going to be above 0.9 etc. But in practice this can't have a very big effect.
             */
            if ( bin == ( int ) Math.floor( numBins * tailFraction ) ) {
                lowerTailDensity = s;
            } else if ( bin == ( int ) Math.floor( numBins * ( 1.0 - tailFraction ) ) ) {
                upperTailDensity = 1.0 - s;
            } else if ( bin > ( int ) Math.floor( lowerLimitofMiddle * numBins )
                    && bin < ( int ) Math.floor( upperLimitofMiddle * numBins ) ) {
                middleDensity += histogram.get( bin );
            }

            if ( s >= 0.2 ) {
                // firstQuintile = binToCorrelation( i, numBins );
            } else if ( s >= 0.5 ) {
                median = binToCorrelation( bin, numBins );
            } else if ( s >= 0.8 ) {
                // lastQuintile = binToCorrelation( i, numBins );
            }
        }

        String message = "";
        boolean bad = false;

        if ( median > 0.2 || median < -0.2 ) {
            bad = true;
            message = "Correlation distribution fails QC: median far from center (" + median + ")";
        } else if ( lowerTailDensity + upperTailDensity > middleDensity ) {

            bad = true;
            message = "Correlation distribution fails QC: tails too heavy";
        }

        if ( bad ) {
            throw new UnsuitableForAnalysisException( ee, message );
        }

    }

    /**
     * Remove rows corresponding to probes that don't map to genes. Row order may be changed.
     * 
     * @param dataMatrix
     * @param probeToGeneMap
     * @return
     */
    private ExpressionDataDoubleMatrix filterUnmappedProbes( ExpressionDataDoubleMatrix dataMatrix,
            Map<CompositeSequence, Set<Gene>> probeToGeneMap ) {
        return new ExpressionDataDoubleMatrix( dataMatrix, new ArrayList<CompositeSequence>( probeToGeneMap.keySet() ) );
    }

    /**
     * Fills in the probe2gene map for the linkAnalysis. Note that the collection DOES NOT contain probes that have NO
     * genes mapped
     * 
     * @param la
     * @param dataVectors
     * @param eeDoubleMatrix - used to make sure we don't use probes from vectors that are removed?
     */
    private void getProbe2GeneMap( LinkAnalysis la, Collection<ProcessedExpressionDataVector> dataVectors,
            ExpressionDataDoubleMatrix eeDoubleMatrix ) {

        Collection<CompositeSequence> probesForVectors = new HashSet<>();
        for ( DesignElementDataVector v : dataVectors ) {
            CompositeSequence cs = v.getDesignElement();
            if ( eeDoubleMatrix.getRow( cs ) != null ) probesForVectors.add( cs );
        }

        Map<CompositeSequence, Collection<BioSequence2GeneProduct>> specificityData = csService
                .getGenesWithSpecificity( probesForVectors );

        assert !specificityData.isEmpty();

        /*
         * Convert the specificity
         */
        Map<CompositeSequence, Set<Gene>> probeToGeneMap = new HashMap<>();
        for ( CompositeSequence cs : specificityData.keySet() ) {

            Collection<BioSequence2GeneProduct> bioSequenceToGeneProducts = specificityData.get( cs );

            if ( !probeToGeneMap.containsKey( cs ) ) {
                probeToGeneMap.put( cs, new HashSet<Gene>() );
            }

            for ( BioSequence2GeneProduct bioSequence2GeneProduct : bioSequenceToGeneProducts ) {
                Gene gene = bioSequence2GeneProduct.getGeneProduct().getGene();
                probeToGeneMap.get( cs ).add( gene );
            }

        }

        /*
         * Remove the probes that have no mapping
         */
        int startingSize = probeToGeneMap.size();
        int numRemoved = 0;
        for ( Iterator<CompositeSequence> it = probeToGeneMap.keySet().iterator(); it.hasNext(); ) {
            CompositeSequence cs = it.next();
            if ( probeToGeneMap.get( cs ).isEmpty() ) {
                it.remove();
                numRemoved++;
            }
        }

        if ( numRemoved > 0 ) {
            log.info( numRemoved + "/" + startingSize + " elements had no genes mapped and were removed." );
        }

        // assert !probeToGeneMap.isEmpty();
        if ( probeToGeneMap.isEmpty() ) {
            throw new IllegalStateException( "No probes are mapped to genes; example="
                    + probeToGeneMap.keySet().iterator().next() );
        }

        la.setProbeToGeneMap( probeToGeneMap );
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
            audit( expressionExperiment, ExceptionUtils.getStackTrace( e ),
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
     * Reject if experiment has outliers or batch effects. TODO use BioAssaySet instead.
     * 
     * @param ee
     * @throws UnsuitableForAnalysisException
     */
    private void qcCheck( LinkAnalysisConfig config, ExpressionExperiment ee ) throws UnsuitableForAnalysisException {

        if ( config.isCheckForOutliers() ) {
            Collection<OutlierDetails> outliers = outlierDetectionService.identifyOutliers( ee );
            if ( !outliers.isEmpty() ) {
                throw new UnsuitableForAnalysisException( ee, "Potential outlier samples detected" );
            }
        }

        if ( config.isCheckForBatchEffect() ) {
            BatchEffectDetails batchEffect = eeService.getBatchEffect( ee );

            if ( !batchEffect.isHasBatchInformation() ) {
                // we may change this behaviour...
                throw new UnsuitableForAnalysisException( ee,
                        "No batch information available, out of an abundance of caution we are skipping" );
            }

            if ( batchEffect.getDataWasBatchCorrected() ) {
                log.info( "Data are batch-corrected" );
                return;
            }

            // FIXME might want to adjust this stringency.
            if ( batchEffect.getPvalue() < 0.001 ) {

                double componentVarianceProportion = batchEffect.getComponentVarianceProportion();
                Integer component = batchEffect.getComponent();
                // don't worry if it is a "minor" component. remember that is must be one of the first few to make it
                // this
                // far.
                if ( component > 2 && componentVarianceProportion < 0.1 ) {
                    // FIXME might want to adjust this stringency
                    return;
                }

                throw new UnsuitableForAnalysisException( ee, String.format( "Strong batch effect detected (%s)",
                        batchEffect ) );
            }
        }
    }

    /**
     * Save the analysis data, either to DB or a file.
     * 
     * @param ee
     * @param la
     * @param linkAnalysisConfig
     * @param filterConfig
     */
    private void saveResults( ExpressionExperiment ee, LinkAnalysis la, LinkAnalysisConfig linkAnalysisConfig,
            FilterConfig filterConfig ) {
        try {

            if ( linkAnalysisConfig.isUseDb() && !linkAnalysisConfig.isTextOut() ) {
                persister.saveLinksToDb( la );
                audit( ee, "", LinkAnalysisEvent.Factory.newInstance() );
            } else if ( linkAnalysisConfig.isTextOut() ) {
                try {
                    PrintWriter w;

                    if ( linkAnalysisConfig.getOutputFile() != null ) {
                        w = new PrintWriter( linkAnalysisConfig.getOutputFile() );
                    } else {
                        w = new PrintWriter( System.out );
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
     * Initializes the LinkAnalysis object; populates the probe2gene map.
     * 
     * @param ee
     * @param la
     * @param dataVectors
     * @param eeDoubleMatrix
     */
    private void setUpForAnalysis( ExpressionExperiment ee, LinkAnalysis la,
            Collection<ProcessedExpressionDataVector> dataVectors, ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        if ( dataVectors == null || dataVectors.size() == 0 )
            throw new IllegalArgumentException( "No data vectors in " + ee );
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
     * Write links as text.
     * 
     * @param la
     * @param wr
     */
    private void writeLinks( final LinkAnalysis la, FilterConfig filterConfig, Writer wr ) throws IOException {
        Map<CompositeSequence, Set<Gene>> probeToGeneMap = la.getProbeToGeneMap();
        ObjectArrayList links = la.getKeep();
        double subsetSize = la.getConfig().getSubsetSize();
        List<String> buf = new ArrayList<>();
        if ( la.getConfig().isSubset() && links.size() > subsetSize ) {
            la.getConfig().setSubsetUsed( true );
        }
        wr.write( la.getConfig().toString() );
        wr.write( filterConfig.toString() );
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits( 4 );

        Integer probeDegreeThreshold = la.getConfig().getProbeDegreeThreshold();

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

            Set<Gene> g1 = probeToGeneMap.get( p1 );
            Set<Gene> g2 = probeToGeneMap.get( p2 );

            List<String> genes1 = new ArrayList<String>();
            for ( Gene cluster : g1 ) {
                String t = cluster.getOfficialSymbol();
                genes1.add( t );
            }

            List<String> genes2 = new ArrayList<>();
            for ( Gene cluster : g2 ) {
                String t = cluster.getOfficialSymbol();
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
