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
package ubic.gemma.core.analysis.expression.coexpression.links;

import cern.colt.list.ObjectArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.jet.math.Functions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.dataStructure.Link;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.analysis.preprocess.SVDRelatedPreprocessingException;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchEffectDetails;
import ubic.gemma.core.analysis.preprocess.batcheffects.ExpressionExperimentBatchInformationService;
import ubic.gemma.core.analysis.preprocess.filter.*;
import ubic.gemma.core.analysis.preprocess.svd.ExpressionDataSVD;
import ubic.gemma.core.analysis.preprocess.svd.SVDException;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.coexpression.CoexpCorrelationDistribution;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedLinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TooSmallDatasetLinkAnalysisEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.*;

/**
 * Running link analyses through the spring context; will persist the results if the configuration says so. See
 * LinkAnalysisCli for more instructions.
 *
 * @author Paul
 */
@Component
public class LinkAnalysisServiceImpl implements LinkAnalysisService {

    private static final Log log = LogFactory.getLog( LinkAnalysisServiceImpl.class );

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

    @Autowired
    private ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Override
    public LinkAnalysis process( ExpressionExperiment ee, ExpressionExperimentFilterConfig filterConfig,
            LinkAnalysisConfig linkAnalysisConfig ) {

        try {
            LinkAnalysis la = new LinkAnalysis( linkAnalysisConfig );
            la.clear();

            LinkAnalysisServiceImpl.log.info( "Fetching expression data for " + ee );

            Collection<ProcessedExpressionDataVector> dataVectors = processedExpressionDataVectorService
                    .getProcessedDataVectorsAndThaw( ee );

            LinkAnalysisServiceImpl.log.info( "Starting analysis" );
            this.analyze( ee, filterConfig, linkAnalysisConfig, la, dataVectors );

            LinkAnalysisServiceImpl.log.info( "Done with analysis phase, starting persistence" );
            this.saveResults( ee, la, linkAnalysisConfig, filterConfig );
            LinkAnalysisServiceImpl.log.info( "Done with saving results for " + ee );
            return la;

        } catch ( Exception e ) {

            if ( linkAnalysisConfig.isUseDb() ) {
                this.logFailure( ee, e );
            }
            throw new RuntimeException( e );
        }

    }

    @Override
    public LinkAnalysis processVectors( Taxon t, Collection<ProcessedExpressionDataVector> dataVectors,
            ExpressionExperimentFilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig ) throws FilteringException, SVDRelatedPreprocessingException {
        ArrayDesign ad = arrayDesignService.findByShortName( linkAnalysisConfig.getArrayName() );
        if ( ad == null ) {
            throw new IllegalArgumentException( "No platform named '" + linkAnalysisConfig.getArrayName() + "'" );
        }
        ExpressionDataDoubleMatrix datamatrix = expressionDataMatrixService
                .getFilteredMatrix( dataVectors, ad, filterConfig, linkAnalysisConfig.isLogTransform() );

        this.checkDatamatrix( datamatrix );
        LinkAnalysis la = new LinkAnalysis( linkAnalysisConfig );

        try {
            datamatrix = this.normalize( datamatrix, linkAnalysisConfig );
        } catch ( SVDException e ) {
            throw new SVDRelatedPreprocessingException( datamatrix.getExpressionExperiment(), e );
        }

        this.setUpForAnalysis( t, la, dataVectors, datamatrix );

        la.analyze();

        try {
            this.writeLinks( la, filterConfig, new PrintWriter( System.out ) );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return la;

    }

    private void checkDatamatrix( ExpressionDataDoubleMatrix datamatrix ) throws InsufficientDesignElementsException {
        if ( datamatrix.rows() == 0 ) {
            LinkAnalysisServiceImpl.log.info( "No rows left after filtering" );
            throw new InsufficientDesignElementsException( "No rows left after filtering" );
        } else if ( datamatrix.rows() < ExpressionExperimentFilter.MINIMUM_DESIGN_ELEMENTS ) {
            throw new InsufficientDesignElementsException(
                    "To few rows (" + datamatrix.rows() + "), data sets are not analyzed unless they have at least "
                            + ExpressionExperimentFilter.MINIMUM_DESIGN_ELEMENTS + " rows" );
        }
    }

    private void addAnalysisObj( ExpressionExperiment ee, ExpressionExperimentFilterConfig filterConfig,
            LinkAnalysisConfig linkAnalysisConfig, LinkAnalysis la ) {

        /*
         * Set up basics.
         */
        CoexpressionAnalysis analysis = linkAnalysisConfig.toAnalysis();

        analysis.setExperimentAnalyzed( ee );
        analysis.setName( ee.getShortName() + " link analysis" );
        if ( analysis.getProtocol() != null ) {
            analysis.getProtocol().setDescription(
                    analysis.getProtocol().getDescription() + "# FilterConfig:\n" + filterConfig.toString() );
        } else {
            log.warn( analysis + " has no protocol object associated, cannot append the filter configuration." );
        }

        la.setAnalysisObj( analysis );
    }

    private void analyze( ExpressionExperiment ee, ExpressionExperimentFilterConfig filterConfig, LinkAnalysisConfig linkAnalysisConfig,
            LinkAnalysis la, Collection<ProcessedExpressionDataVector> dataVectors ) throws FilteringException {

        this.qcCheck( linkAnalysisConfig, ee );

        ExpressionDataDoubleMatrix datamatrix = expressionDataMatrixService
                .getFilteredMatrix( ee, dataVectors, filterConfig, linkAnalysisConfig.isLogTransform() );

        this.setUpForAnalysis( ee, la, dataVectors, datamatrix );

        Map<CompositeSequence, Set<Gene>> probeToGeneMap = la.getProbeToGeneMap();

        assert !probeToGeneMap.isEmpty();

        /*
         * remove probes that have no gene mapped to them, not just those that have no sequence
         */
        datamatrix = this.filterUnmappedProbes( datamatrix, probeToGeneMap );

        this.checkDatamatrix( datamatrix );

        LinkAnalysisServiceImpl.log.info( "Starting link analysis... " + ee );

        try {
            this.normalize( datamatrix, linkAnalysisConfig );
        } catch ( SVDException e ) {
            throw new SVDRelatedPreprocessingException( datamatrix.getExpressionExperiment(), e );
        }

        /*
         * Link analysis section.
         */
        this.addAnalysisObj( ee, filterConfig, linkAnalysisConfig, la );
        la.analyze();

        CoexpCorrelationDistribution corrDist = la.getCorrelationDistribution();

        // another qc check.
        if ( linkAnalysisConfig.isCheckCorrelationDistribution() ) {
            this.diagnoseCorrelationDistribution( ee, corrDist );
        }
    }

    private void audit( ExpressionExperiment ee, String note, Class<? extends AuditEventType> eventType ) {
        expressionExperimentReportService.generateSummary( ee.getId() );
        ee = this.eeService.thawLite( ee );
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    private double binToCorrelation( int bin, int numBins ) {
        return bin * 2.0 / numBins - 1.0;
    }

    /**
     * Check properties of the distribution
     */
    @SuppressWarnings("StatementWithEmptyBody") // Better readability
    private void diagnoseCorrelationDistribution( ExpressionExperiment ee, CoexpCorrelationDistribution corrDist )
            throws UnsuitableForAnalysisException {

        /*
         * Find the median, etc.
         */
        double[] binCounts = corrDist.getBinCounts();
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
            } else if ( bin > ( int ) Math.floor( lowerLimitofMiddle * numBins ) && bin < ( int ) Math
                    .floor( upperLimitofMiddle * numBins ) ) {
                middleDensity += histogram.get( bin );
            }

            if ( s >= 0.2 ) {
                // firstQuintile = binToCorrelation( i, numBins );
            } else if ( s >= 0.5 ) {
                median = this.binToCorrelation( bin, numBins );
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
     */
    private ExpressionDataDoubleMatrix filterUnmappedProbes( ExpressionDataDoubleMatrix dataMatrix,
            Map<CompositeSequence, Set<Gene>> probeToGeneMap ) {
        return dataMatrix.sliceRows( new ArrayList<>( probeToGeneMap.keySet() ) );
    }

    /**
     * Fills in the probe2gene map for the linkAnalysis. Note that the collection DOES NOT contain probes that have NO
     * genes mapped
     *
     * @param eeDoubleMatrix - used to make sure we don't use probes from vectors that are removed?
     */
    private void getProbe2GeneMap( LinkAnalysis la, Collection<ProcessedExpressionDataVector> dataVectors,
            ExpressionDataDoubleMatrix eeDoubleMatrix ) {

        Collection<CompositeSequence> probesForVectors = new HashSet<>();
        for ( DesignElementDataVector v : dataVectors ) {
            CompositeSequence cs = v.getDesignElement();
            if ( eeDoubleMatrix.getRowAsDoubles( cs ) != null )
                probesForVectors.add( cs );
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
            LinkAnalysisServiceImpl.log
                    .info( numRemoved + "/" + startingSize + " elements had no genes mapped and were removed." );
        }

        // assert !probeToGeneMap.isEmpty();
        if ( probeToGeneMap.isEmpty() ) {
            throw new IllegalStateException(
                    "No probes are mapped to genes; example=" + probeToGeneMap.keySet().iterator().next() );
        }

        la.setProbeToGeneMap( probeToGeneMap );
    }

    private void logFailure( ExpressionExperiment expressionExperiment, Exception e ) {

        if ( e instanceof InsufficientSamplesException ) {
            this.audit( expressionExperiment, e.getMessage(), TooSmallDatasetLinkAnalysisEvent.class );
        } else if ( e instanceof InsufficientDesignElementsException ) {
            this.audit( expressionExperiment, e.getMessage(), TooSmallDatasetLinkAnalysisEvent.class );
        } else {
            LinkAnalysisServiceImpl.log.error( "While processing " + expressionExperiment, e );
            this.audit( expressionExperiment, ExceptionUtils.getStackTrace( e ),
                    FailedLinkAnalysisEvent.class );
        }
    }

    /**
     * Normalize the data, as configured (possibly no normalization).
     */
    private ExpressionDataDoubleMatrix normalize( ExpressionDataDoubleMatrix datamatrix, LinkAnalysisConfig config ) throws SVDException {

        ExpressionDataSVD svd;
        switch ( config.getNormalizationMethod() ) {
            case none:
                return datamatrix;
            case SVD:
                LinkAnalysisServiceImpl.log.info( "SVD normalizing" );
                svd = new ExpressionDataSVD( datamatrix, true );
                return svd.removeHighestComponents( 1 );
            case SPELL:
                LinkAnalysisServiceImpl.log.info( "Computing U matrix via SVD" );
                svd = new ExpressionDataSVD( datamatrix, true );
                return svd.uMatrixAsExpressionData();
            case BALANCE:
                LinkAnalysisServiceImpl.log.info( "SVD-balanceing" );
                svd = new ExpressionDataSVD( datamatrix, true );
                return svd.equalize();
            default:
                return null;
        }
    }

    /**
     * Reject if experiment has outliers or batch effects.
     */
    private void qcCheck( LinkAnalysisConfig config, ExpressionExperiment ee ) throws UnsuitableForAnalysisException, FilteringException {

        if ( config.isCheckForOutliers() ) {
            Collection<OutlierDetails> outliers = outlierDetectionService.identifyOutliersByMedianCorrelation( ee );
            if ( !outliers.isEmpty() ) {
                throw new UnsuitableForAnalysisException( ee, "Potential outlier samples detected" );
            }
        }

        if ( config.isCheckForBatchEffect() ) {
            BatchEffectDetails batchEffect = expressionExperimentBatchInformationService.getBatchEffectDetails( ee );

            if ( batchEffect.dataWasBatchCorrected() ) {
                LinkAnalysisServiceImpl.log.info( "Data are batch-corrected" );
                return;
            }

            if ( !batchEffect.hasBatchInformation() ) {
                // we may change this behaviour...
                throw new UnsuitableForAnalysisException( ee,
                        "No batch information available, out of an abundance of caution we are skipping" );
            }

            if ( batchEffect.getBatchEffectStatistics() == null ) {
                throw new UnsuitableForAnalysisException( ee,
                        "Batch effect statistics are missing, it's not possible to detect a batch effect." );
            }

            if ( batchEffect.getBatchEffectStatistics().getPvalue() < 0.001 ) {
                double componentVarianceProportion = batchEffect.getBatchEffectStatistics().getComponentVarianceProportion();
                int component = batchEffect.getBatchEffectStatistics().getComponent();
                // don't worry if it is a "minor" component. remember that is must be one of the first few to make it
                // this far.
                if ( component > 2 && componentVarianceProportion < 0.1 ) {
                    return;
                }
                throw new UnsuitableForAnalysisException( ee,
                        String.format( "Strong batch effect detected (%s)", batchEffect ) );
            }
        }
    }

    /**
     * Save the analysis data, either to DB or a file.
     */
    private void saveResults( ExpressionExperiment ee, LinkAnalysis la, LinkAnalysisConfig linkAnalysisConfig,
            ExpressionExperimentFilterConfig filterConfig ) {
        try {

            if ( linkAnalysisConfig.isUseDb() && !linkAnalysisConfig.isTextOut() ) {
                persister.saveLinksToDb( la );
                this.audit( ee, "", LinkAnalysisEvent.class );
            } else if ( linkAnalysisConfig.isTextOut() ) {
                try {
                    PrintWriter w;

                    if ( linkAnalysisConfig.getOutputFile() != null ) {
                        w = new PrintWriter( linkAnalysisConfig.getOutputFile() );
                    } else {
                        w = new PrintWriter( System.out );
                    }
                    this.writeLinks( la, filterConfig, w );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            }

            LinkAnalysisServiceImpl.log.info( "Done with processing of " + ee );

        } catch ( Exception e ) {
            if ( linkAnalysisConfig.isUseDb() ) {
                this.logFailure( ee, e );
            }
            throw new RuntimeException( e );
        }

    }

    /**
     * Initializes the LinkAnalysis object; populates the probe2gene map.
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

        this.getProbe2GeneMap( la, dataVectors, eeDoubleMatrix );
    }

    /**
     * Initializes the LinkAnalysis object for data file input; populates the probe2gene map.
     */
    private void setUpForAnalysis( Taxon t, LinkAnalysis la, Collection<ProcessedExpressionDataVector> dataVectors,
            ExpressionDataDoubleMatrix eeDoubleMatrix ) {

        la.setDataMatrix( eeDoubleMatrix );
        la.setTaxon( t );
        this.getProbe2GeneMap( la, dataVectors, eeDoubleMatrix );
    }

    /**
     * Write links as text.
     */
    private void writeLinks( final LinkAnalysis la, ExpressionExperimentFilterConfig filterConfig, Writer wr ) throws IOException {
        Map<CompositeSequence, Set<Gene>> probeToGeneMap = la.getProbeToGeneMap();
        ObjectArrayList links = la.getKeep();
        double subsetSize = la.getConfig().getSubsetSize();
        List<String> buf = new ArrayList<>();
        if ( la.getConfig().isSubset() && links.size() > subsetSize ) {
            la.getConfig().setSubsetUsed( true );
        }
        wr.write( la.getConfig().toString() );
        wr.write( filterConfig.toString() );
        NumberFormat nf = NumberFormat.getInstance( Locale.ENGLISH );
        nf.setMaximumFractionDigits( 4 );

        Integer probeDegreeThreshold = la.getConfig().getProbeDegreeThreshold();

        int i = 0;
        int keptLinksCount = 0;
        Random generator = new Random();
        double rand;
        double fraction = subsetSize / links.size();
        int skippedDueToDegree = 0;
        for ( int n = links.size(); i < n; i++ ) {

            Object val = links.getQuick( i );
            if ( val == null )
                continue;
            Link m = ( Link ) val;
            Double w = m.getWeight();

            int x = m.getx();
            int y = m.gety();

            if ( probeDegreeThreshold > 0 && ( la.getProbeDegree( x ) > probeDegreeThreshold
                    || la.getProbeDegree( y ) > probeDegreeThreshold ) ) {
                skippedDueToDegree++;
                continue;
            }

            CompositeSequence p1 = la.getProbe( x );
            CompositeSequence p2 = la.getProbe( y );

            Set<Gene> g1 = probeToGeneMap.get( p1 );
            Set<Gene> g2 = probeToGeneMap.get( p2 );

            List<String> genes1 = new ArrayList<>();
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
                LinkAnalysisServiceImpl.log.info( keptLinksCount + " links retained" );
            }

            if ( la.getConfig().isSubsetUsed() ) {
                rand = generator.nextDouble();
                if ( rand > fraction )
                    continue;
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
            LinkAnalysisServiceImpl.log
                    .info( "Done, " + keptLinksCount + "/" + links.size() + " links kept, " + buf.size()
                            + " links printed" );
            // wr.write("# Amount of links before subsetting/after subsetting: " + links.size() + "/" + numPrinted +
            // "\n" );
        } else {
            LinkAnalysisServiceImpl.log.info( "Done, " + keptLinksCount + "/" + links.size()
                    + " links printed (some may have been filtered)" );
        }
        wr.flush();

    }

}
