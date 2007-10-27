/*
 * The Gemma project
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
package ubic.gemma.analysis.linkAnalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.Stats;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.ConfigUtils;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;

/**
 * Handles running a linkAnalysis. Results are made available at the end.
 * 
 * @author xiangwan
 * @author paul (refactoring)
 * @version $Id$
 */
public class LinkAnalysis {

    /**
     * A value above which we would be concerned that statistical significance is going to be hard to achieve.
     */
    private static final double VERY_HIGH_CORRELATION_THRESHOLD = 0.9;

    protected static final Log log = LogFactory.getLog( LinkAnalysis.class );
    private MatrixRowPairAnalysis metricMatrix;
    private DoubleArrayList cdf;
    private ObjectArrayList keep; // links that are retained.
    private ExpressionDataDoubleMatrix dataMatrix = null;
    private Collection<DesignElementDataVector> dataVectors = null;

    private Map<CompositeSequence, Collection<Gene>> probeToGeneMap = null;
    private Map<Gene, Collection<CompositeSequence>> geneToProbeMap = null;
    private Taxon taxon = null;
    private int uniqueGenesInDataset = 0;

    private int minSamplesToKeepCorrelation = 0;

    private NumberFormat form;

    private LinkAnalysisConfig config;
    private ExpressionExperiment expressionExperiment;

    /**
     * @param config
     */
    public LinkAnalysis( LinkAnalysisConfig config ) {
        this.form = NumberFormat.getInstance();
        if ( form instanceof DecimalFormat ) ( ( DecimalFormat ) form ).applyPattern( "0.###E0" );
        this.config = config;
    }

    /**
     * @return
     */
    public void analyze() throws Exception {
        assert this.dataMatrix != null;
        assert this.dataVectors != null;
        assert this.taxon != null;
        assert this.probeToGeneMap != null;

        log.debug( "Taxon: " + this.taxon.getCommonName() );

        this.init();
        if ( this.uniqueGenesInDataset == 0 ) {
            throw new RuntimeException( "No genes found for this dataset; make sure the probe -> gene map is complete." );
        }

        log.info( "Current Options: \n" + this.config );
        this.calculateDistribution();
        this.writeDistribution();
        this.getLinks();

    }

    /**
     * Clear/null data so this object can be reused.
     */
    public void clear() {
        this.dataMatrix = null;
        this.dataVectors = null;
        this.probeToGeneMap = null;
        this.geneToProbeMap = null;
        this.uniqueGenesInDataset = 0;
        this.metricMatrix = null;
    }

    public void setDataMatrix( ExpressionDataDoubleMatrix paraDataMatrix ) {
        this.dataMatrix = paraDataMatrix;
    }

    public void setDataVectors( Collection<DesignElementDataVector> vectors ) {
        this.dataVectors = vectors;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    /**
     * Write histogram into file.
     * 
     * @throws IOException
     */
    public void writeDistribution() throws IOException {

        File outputDir = new File( ConfigUtils.getAnalysisStoragePath() );
        if ( !outputDir.canWrite() ) {
            log.warn( "Cannot write to " + outputDir + ", correlation distribution will not be saved to disk" );
            return;
        }

        String path = outputDir + File.separator + expressionExperiment.getShortName() + ".correlDist.txt";

        File outputFile = new File( path );
        if ( outputFile.exists() ) {
            outputFile.delete();
        }

        DoubleArrayList histogramArrayList = this.metricMatrix.getHistogramArrayList();

        FileWriter out = new FileWriter( outputFile );

        double d = -1.0;
        double step = 2.0 / histogramArrayList.size();

        out.write( "# Correlation distribution\n" );
        out.write( "# date=" + ( new Date() ) + "\n" );
        out.write( "# exp=" + expressionExperiment + " " + expressionExperiment.getShortName() + "\n" );
        out.write( "Bin\tCount\n" );

        for ( int i = 0; i < histogramArrayList.size(); i++ ) {
            double v = histogramArrayList.get( i );
            out.write( form.format( d ) + "\t" + ( int ) v + "\n" );
            d += step;
        }

        out.close();

    }

    /**
     * Compute the distribution of similarity metrics for the entire matrix.
     */
    private void calculateDistribution() {
        if ( config.getMetric().equals( "pearson" ) ) {
            metricMatrix = MatrixRowPairAnalysisFactory
                    .pearson( this.dataMatrix, config.getCorrelationCacheThreshold() );
        } else if ( config.getMetric().equals( "spearmann" ) ) {
            metricMatrix = MatrixRowPairAnalysisFactory.spearmann( dataMatrix, config.getCorrelationCacheThreshold() );
        }

        /*
         * Determine the threshold number of samples in a gene pair before we consider keeping the result. This is
         * needed in data sets that have many missing values. In that case, some pairs will have a much smaller
         * effective sample size. Because with too few degrees of freedom, the values have no chance of being
         * significant (within reason), they shouldn't be included in the histograms.
         */
        double maxP = config.getFwe() / uniqueGenesInDataset;
        for ( int i = 3; i < this.dataMatrix.columns(); i++ ) {
            double scoreForSmallSampleSize = CorrelationStats.correlationForPvalue( maxP, i );
            if ( scoreForSmallSampleSize > VERY_HIGH_CORRELATION_THRESHOLD ) {
                minSamplesToKeepCorrelation = i + 1;
                break;
            }
        }

        if ( minSamplesToKeepCorrelation > 0 ) {
            log.info( "Pairs must have at least " + minSamplesToKeepCorrelation + " mutual values to be considered" );
        }

        metricMatrix.setDuplicateMap( probeToGeneMap, geneToProbeMap );
        metricMatrix.setUseAbsoluteValue( config.isAbsoluteValue() );
        metricMatrix.setMinNumpresent( minSamplesToKeepCorrelation );
        metricMatrix.calculateMetrics();
        log.info( "Completed first pass over the data. Cached " + metricMatrix.numCached()
                + " values in the correlation matrix with values over " + config.getCorrelationCacheThreshold() );

    }

    /**
     * Compute the thresholds needed to choose links for storage in the system.
     */
    private void chooseCutPoints() {
        cdf = Stats.cdf( metricMatrix.getHistogramArrayList() );
        if ( config.getCdfCut() <= 0.0 ) {
            config.setUpperTailCut( 1.0 );
            config.setLowerTailCut( -1.0 );
            return;
        }

        if ( config.getCdfCut() >= 1.0 ) {
            config.setUpperTailCut( 0.0 );
            config.setLowerTailCut( 0.0 );
            return;
        }

        double cdfTailCut = config.getCdfCut();
        double cdfUpperCutScore = 0.0;
        double cdfLowerCutScore = 0.0;

        // find the lower tail cutpoint, if we have to.
        if ( !config.isAbsoluteValue() ) {
            cdfTailCut /= 2.0;
            // find the lower cut point. Roundoff could be a problem...really
            // need two cdfs or do it directly from
            // histogram.
            for ( int i = 0; i < cdf.size(); i++ ) {
                if ( 1.0 - cdf.get( i ) >= cdfTailCut ) {
                    cdfLowerCutScore = metricMatrix.getScoreInBin( i == cdf.size() ? i : i + 1 );
                    break;
                }
            }
            log.info( form.format( cdfLowerCutScore ) + " is the lower cdf cutpoint at " + cdfTailCut );
        }

        // find the upper cut point.
        for ( int i = cdf.size() - 1; i >= 0; i-- ) {
            if ( cdf.get( i ) >= cdfTailCut ) {
                cdfUpperCutScore = metricMatrix.getScoreInBin( i == cdf.size() ? i : i + 1 );
                break;
            }
        }

        log.info( form.format( cdfUpperCutScore ) + " is the upper cdf cutpoint at " + cdfTailCut );

        // get the cutpoint based on statistical signficance.
        double maxP = 1.0;
        double scoreAtP = 0.0;
        if ( config.getFwe() != 0.0 ) {
            maxP = config.getFwe() / uniqueGenesInDataset; // bonferroni.
            scoreAtP = CorrelationStats.correlationForPvalue( maxP, this.dataMatrix.columns() );
            log.info( "Minimum correlation to get " + form.format( maxP ) + " is about " + form.format( scoreAtP )
                    + " for " + uniqueGenesInDataset + " unique items (if all " + this.dataMatrix.columns()
                    + " items are present)" );

            if ( scoreAtP > 0.9 ) {
                log.warn( "This data set has a very high threshold for statistical significance!" );
            }

        }
        this.metricMatrix.setPValueThreshold( maxP ); // this is the corrected
        // value.

        config.setUpperTailCut( Math.max( scoreAtP, cdfUpperCutScore ) );
        log.info( "Final upper cut is " + form.format( config.getUpperTailCut() ) );

        if ( !config.isAbsoluteValue() ) {
            config.setLowerTailCut( Math.min( -scoreAtP, cdfLowerCutScore ) );
            log.info( "Final lower cut is " + form.format( config.getLowerTailCut() ) );
        }

        metricMatrix.setUpperTailThreshold( config.getUpperTailCut() );
        if ( config.isAbsoluteValue() ) {
            metricMatrix.setLowerTailThreshold( config.getUpperTailCut() );
        } else {
            metricMatrix.setLowerTailThreshold( config.getLowerTailCut() );
        }

    }

    /**
     * Does the main computation and link selection.
     */
    private void getLinks() {
        chooseCutPoints();
        metricMatrix.calculateMetrics();
        keep = metricMatrix.getKeepers();
        log.info( "Selected " + keep.size() + " values to keep" );
    }

    /**
     * 
     *
     */
    @SuppressWarnings("unchecked")
    private void init() {
        int[] stats = new int[10];

        this.geneToProbeMap = new HashMap<Gene, Collection<CompositeSequence>>();

        StopWatch watch = new StopWatch();
        watch.start();

        // populate geneToProbeMap and gather stats.
        for ( CompositeSequence cs : probeToGeneMap.keySet() ) {
            Collection<Gene> genes = probeToGeneMap.get( cs );
            for ( Gene g : genes ) {
                if ( !geneToProbeMap.containsKey( g ) ) {
                    geneToProbeMap.put( g, new HashSet<CompositeSequence>() );
                }
                this.geneToProbeMap.get( g ).add( cs );
            }

            if ( genes.size() >= stats.length ) {
                stats[stats.length - 1]++;
            } else {
                stats[genes.size()]++;
            }
        }

        this.uniqueGenesInDataset = this.geneToProbeMap.keySet().size();
        watch.stop();
        log.info( "Finished mapping in " + watch.getTime() / 1000.0 + " seconds" );
        log.info( "Mapping Stats " + ArrayUtils.toString( stats ) );
        if ( this.uniqueGenesInDataset == 0 ) return;

        // estimate the correlation needed to reach significance.
        double scoreP = CorrelationStats.correlationForPvalue( config.getFwe() / this.uniqueGenesInDataset,
                this.dataMatrix.columns() ) - 0.001;
        if ( scoreP > config.getCorrelationCacheThreshold() ) config.setCorrelationCacheThreshold( scoreP );
    }

    /**
     * @param paraFileName
     */
    protected void writeDataIntoFile( String paraFileName ) throws IOException {
        BufferedWriter writer = null;

        writer = new BufferedWriter( new FileWriter( paraFileName ) );
        int cols = this.dataMatrix.columns();
        for ( int i = 0; i < cols; i++ ) {
            writer.write( "\t" + this.dataMatrix.getBioMaterialForColumn( i ) );
        }
        writer.write( "\n" );
        int rows = this.dataMatrix.rows();
        for ( int i = 0; i < rows; i++ ) {
            writer.write( this.dataMatrix.getRowElements().get( i ).toString() );
            Double rowData[] = this.dataMatrix.getRow( i );
            for ( int j = 0; j < rowData.length; j++ )
                writer.write( "\t" + rowData[j] );
            writer.write( "\n" );
        }
        writer.close();
    }

    public ExpressionDataDoubleMatrix getDataMatrix() {
        return dataMatrix;
    }

    public ObjectArrayList getKeep() {
        return keep;
    }

    public MatrixRowPairAnalysis getMetricMatrix() {
        return metricMatrix;
    }

    /**
     * @return
     */
    public Taxon getTaxon() {
        return this.taxon;
    }

    public void setProbeToGeneMap( Map<CompositeSequence, Collection<Gene>> probeToGeneMap ) {
        this.probeToGeneMap = probeToGeneMap;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return this.expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public QuantitationType getMetric() {
        return this.metricMatrix.getMetricType();
    }

    public LinkAnalysisConfig getConfig() {
        return config;
    }

    public Map<CompositeSequence, Collection<Gene>> getProbeToGeneMap() {
        return probeToGeneMap;
    }

}
