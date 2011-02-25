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
package ubic.gemma.analysis.expression.coexpression.links;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.Link;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.Stats;
import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisConfig.SingularThreshold;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.ConfigUtils;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;

/**
 * Handles running a linkAnalysis. Results are made available at the end. See LinkAnalysisCli for more instructions.
 * 
 * @author xiangwan
 * @author paul (refactoring)
 * @version $Id$
 */
public class LinkAnalysis {

    protected static final Log log = LogFactory.getLog( LinkAnalysis.class );
    private MatrixRowPairAnalysis metricMatrix;
    private DoubleArrayList cdf;
    private ObjectArrayList keep; // links that are retained.
    private ExpressionDataDoubleMatrix dataMatrix = null;

    private Map<CompositeSequence, Collection<Collection<Gene>>> probeToGeneMap = null;
    private Taxon taxon = null;

    private NumberFormat form;

    private boolean useKnownGenesOnly = false;

    private LinkAnalysisConfig config;
    private ExpressionExperiment expressionExperiment;

    private ProbeCoexpressionAnalysis analysis;

    private Map<Integer, Integer> probeDegreeMap = new HashMap<Integer, Integer>();

    /**
     * @param config
     */
    public LinkAnalysis( LinkAnalysisConfig config ) {
        this.form = NumberFormat.getInstance();
        if ( form instanceof DecimalFormat ) ( ( DecimalFormat ) form ).applyPattern( "0.###E0" );
        this.config = config;
    }

    /**
     * Main entry point.
     */
    public void analyze() {
        assert this.dataMatrix != null;
        assert this.taxon != null;
        assert this.probeToGeneMap != null;

        log.debug( "Taxon: " + this.taxon.getCommonName() );

        if ( this.probeToGeneMap.size() == 0 ) {
            log.warn( "No genes found for this dataset. Do the associated array designs need processing?" );
        }

        log.info( "Current Options: \n" + this.config );
        this.calculateDistribution();

        if ( Thread.currentThread().isInterrupted() ) {
            log.info( "Cancelled." );
            return;
        }

        if ( metricMatrix.getNumUniqueGenes() == 0 ) {
            throw new UnsupportedOperationException(
                    "Link analysis not supported when there are no genes mapped to the data set." );
        }

        this.getLinks();

        if ( expressionExperiment != null ) {// input is not from expression data file
            this.writeCorrelationDistribution();
            this.writeProbeDegreeDistribution();
        }
    }

    /**
     * Writes two flies: one the actual probe degrees, and the other a histogram (dist)
     */
    private void writeProbeDegreeDistribution() {

        File outputDir = getOutputDir();

        if ( outputDir == null ) return;

        String distPath = outputDir + File.separator + expressionExperiment.getShortName() + ".degreeDist.txt";
        String path = outputDir + File.separator + expressionExperiment.getShortName() + ".degrees.txt";

        File outputFile = new File( path );
        if ( outputFile.exists() ) {
            outputFile.delete();
        }

        File outputDistFile = new File( distPath );
        if ( outputDistFile.exists() ) {
            outputDistFile.delete();
        }

        try {
            FileWriter out = new FileWriter( outputFile );

            out.write( "# Probe degree statistics (before filtering by probeDegreeThreshold)\n" );
            out.write( "# date=" + ( new Date() ) + "\n" );
            out.write( "# exp=" + expressionExperiment + " " + expressionExperiment.getShortName() + "\n" );
            out.write( "ProbeID\tProbeName\tNumLinks\n" );

            for ( Integer i : probeDegreeMap.keySet() ) {
                CompositeSequence probe = this.getProbe( i );
                out.write( probe.getId() + "\t" + probe.getName() + "\t" + probeDegreeMap.get( i ) + "\n" );
            }

            out.close();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        Histogram hist = new Histogram( "foo", 100, 0, 1000 );
        for ( Integer i : probeDegreeMap.values() ) {
            hist.fill( i.doubleValue() );
        }

        double[] counts = hist.getArray();
        try {
            FileWriter out = new FileWriter( outputDistFile );

            int d = ( int ) hist.min();
            int step = ( int ) hist.stepSize();

            out.write( "# Probe degree histogram (before filtering by probeDegreeThreshold)\n" );
            out.write( "# date=" + ( new Date() ) + "\n" );
            out.write( "# exp=" + expressionExperiment + " " + expressionExperiment.getShortName() + "\n" );
            out.write( "Bin\tCount\n" );

            for ( int i = 0; i < counts.length; i++ ) {
                Double v = counts[i];
                out.write( d + "\t" + v.intValue() + "\n" );
                d += step;
            }

            out.close();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @return
     */
    private File getOutputDir() {
        File outputDir = null;

        if ( this.config.isUseDb() ) {
            outputDir = new File( ConfigUtils.getAnalysisStoragePath() );
        } else {
            outputDir = new File( System.getProperty( "user.home" ) + File.separator + "gemma.output" );
            if ( !outputDir.exists() ) {
                boolean ok = outputDir.mkdirs();
                if ( !ok ) {
                    log.warn( "Cannot create " + outputDir );
                    return null;
                }
            }
        }

        if ( !outputDir.canWrite() ) {
            log.warn( "Cannot write to " + outputDir );
            return null;
        }
        return outputDir;
    }

    /**
     * Clear/null data so this object can be reused.
     */
    public void clear() {
        this.dataMatrix = null;
        this.probeToGeneMap = null;
        this.metricMatrix = null;
    }

    public void setDataMatrix( ExpressionDataDoubleMatrix paraDataMatrix ) {
        this.dataMatrix = paraDataMatrix;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    /**
     * Write histogram into file.
     * 
     * @throws IOException
     */
    public void writeCorrelationDistribution() {

        File outputDir = getOutputDir();

        if ( outputDir == null ) return;

        String path = outputDir + File.separator + expressionExperiment.getShortName() + ".correlDist.txt";

        File outputFile = new File( path );
        if ( outputFile.exists() ) {
            outputFile.delete();
        }

        DoubleArrayList histogramArrayList = this.metricMatrix.getHistogramArrayList();
        try {
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
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Compute the distribution of similarity metrics for the entire matrix.
     */
    private void calculateDistribution() {
        if ( config.getMetric().equals( "pearson" ) ) {
            log.info( "Using Pearson linear correlation" );
            metricMatrix = MatrixRowPairAnalysisFactory
                    .pearson( this.dataMatrix, config.getCorrelationCacheThreshold() );
        } else if ( config.getMetric().equals( "spearman" ) ) {
            log.info( "Using Spearman rank correlation" );
            metricMatrix = MatrixRowPairAnalysisFactory.spearmann( dataMatrix, config.getCorrelationCacheThreshold() );
        }

        metricMatrix.setOmitNegativeCorrelationLinks( config.isOmitNegLinks() );
        metricMatrix.setDuplicateMap( probeToGeneMap ); // populates numUniqueGenes
        metricMatrix.setUseAbsoluteValue( config.isAbsoluteValue() );
        this.init();

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
            double numUniqueGenes = metricMatrix.getNumUniqueGenes();

            maxP = config.getFwe() / numUniqueGenes; // bonferroni.

            scoreAtP = CorrelationStats.correlationForPvalue( maxP, this.dataMatrix.columns() );
            log.info( "Minimum correlation to get " + form.format( maxP ) + " is about " + form.format( scoreAtP )
                    + " for " + numUniqueGenes + " unique items (if all " + this.dataMatrix.columns()
                    + " items are present)" );
            if ( scoreAtP > 0.9 ) {
                log.warn( "This data set has a very high threshold for statistical significance!" );
            }
        }
        this.metricMatrix.setPValueThreshold( maxP ); // this is the corrected
        // value.

        // choose cut points, with one independent criterion or the most stringent criteria
        if ( config.getSingularThreshold().equals( SingularThreshold.none ) ) {
            config.setUpperTailCut( Math.max( scoreAtP, cdfUpperCutScore ) );
            if ( config.getUpperTailCut() == scoreAtP ) {
                config.setUpperCdfCutUsed( false );
            } else if ( config.getUpperTailCut() == cdfUpperCutScore ) {
                config.setUpperCdfCutUsed( true );
            }
            log.info( "Final upper cut is " + form.format( config.getUpperTailCut() ) );
            if ( !config.isAbsoluteValue() ) {
                config.setLowerTailCut( Math.min( -scoreAtP, cdfLowerCutScore ) );
                log.info( "Final lower cut is " + form.format( config.getLowerTailCut() ) );
            }
            if ( config.getLowerTailCut() == scoreAtP ) {
                config.setLowerCdfCutUsed( false );
            } else if ( config.getLowerTailCut() == cdfLowerCutScore ) {
                config.setLowerCdfCutUsed( true );
            }
        } else if ( config.getSingularThreshold().equals( SingularThreshold.fwe ) ) {
            config.setUpperTailCut( scoreAtP );
            log.info( "Final upper cut is " + form.format( config.getUpperTailCut() ) );
            if ( !config.isAbsoluteValue() ) {
                config.setLowerTailCut( -scoreAtP );
                log.info( "Final lower cut is " + form.format( config.getLowerTailCut() ) );
            }
            config.setUpperCdfCutUsed( false );
            config.setLowerCdfCutUsed( false );
        } else if ( config.getSingularThreshold().equals( SingularThreshold.cdfcut ) ) {
            config.setUpperTailCut( cdfUpperCutScore );
            log.info( "Final upper cut is " + form.format( config.getUpperTailCut() ) );
            if ( !config.isAbsoluteValue() ) {
                config.setLowerTailCut( cdfLowerCutScore );
                log.info( "Final lower cut is " + form.format( config.getLowerTailCut() ) );
            }
            metricMatrix.setUsePvalueThreshold( false );// use only cdfCut exclusively to keep links
            config.setUpperCdfCutUsed( true );
            config.setLowerCdfCutUsed( true );
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

        computeProbeDegrees();
    }

    /**
     * Populates a map of probe index (in matrix) -> how many links.
     */
    private void computeProbeDegrees() {

        this.probeDegreeMap = new HashMap<Integer, Integer>();

        for ( Integer i = 0; i < metricMatrix.size(); i++ ) {
            probeDegreeMap.put( i, 0 );
        }

        for ( int i = 0; i < keep.size(); i++ ) {
            Link l = ( Link ) keep.get( i );
            Integer x = l.getx();
            Integer y = l.gety();

            probeDegreeMap.put( x, probeDegreeMap.get( x ) + 1 );
            probeDegreeMap.put( y, probeDegreeMap.get( y ) + 1 );

        }
    }

    public CompositeSequence getProbe( int index ) {
        return getMetricMatrix().getProbeForRow( getDataMatrix().getRowElement( index ) );
    }

    /**
     * @param index row number in the metrixMatirx
     * @return how many Links that probe appears in, or null if the probeDegree has not been populated for that index
     *         (that is, either to early to check, or it was zero).
     */
    public Integer getProbeDegree( int index ) {
        return this.probeDegreeMap.get( index );
    }

    /**
     * 
     *
     */
    private void init() {
        // estimate the correlation needed to reach significance.
        double scoreP = CorrelationStats.correlationForPvalue( config.getFwe() / this.metricMatrix.getNumUniqueGenes(),
                this.dataMatrix.columns() ) - 0.001;
        if ( scoreP > config.getCorrelationCacheThreshold() ) config.setCorrelationCacheThreshold( scoreP );
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

    public void setProbeToGeneMap( Map<CompositeSequence, Collection<Collection<Gene>>> probeToGeneMap ) {
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

    public Map<CompositeSequence, Collection<Collection<Gene>>> getProbeToGeneMap() {
        return probeToGeneMap;
    }

    public void setAnalysisObj( ProbeCoexpressionAnalysis analysis ) {
        this.analysis = analysis;
    }

    /**
     * @return object containing the parameters used.
     */
    public ProbeCoexpressionAnalysis getAnalysisObj() {
        return analysis;
    }

    /**
     * @return true if we should retain links only for known genes with other known genes.
     */
    public boolean useKnownGenesOnly() {
        return useKnownGenesOnly;
    }

    /**
     * @param useKnownGenesOnly
     */
    public void setUseKnownGenesOnly( boolean useKnownGenesOnly ) {
        this.useKnownGenesOnly = useKnownGenesOnly;
    }

}
