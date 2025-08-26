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
package ubic.gemma.core.analysis.expression.coexpression.links;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.dataStructure.Link;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.Stats;
import ubic.gemma.core.analysis.expression.coexpression.links.LinkAnalysisConfig.SingularThreshold;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.coexpression.CoexpCorrelationDistribution;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Handles the actual coexpression analysis, once handed data that has been prepared. Results are made available at the
 * end. See LinkAnalysisCli for more instructions. This should be created for each analysis - it's not reusable.
 *
 * @author xiangwan, paul (refactoring)
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class LinkAnalysis {

    protected static final Log log = LogFactory.getLog( LinkAnalysis.class );
    private static final int MAX_WARNINGS = 5;
    private CoexpressionAnalysis analysis;
    private LinkAnalysisConfig config;
    private ExpressionDataDoubleMatrix dataMatrix = null;

    private ExpressionExperiment expressionExperiment;
    private NumberFormat form;

    private ObjectArrayList keep; // links that are retained.

    private MatrixRowPairAnalysis metricMatrix;
    private Map<Integer, Integer> probeDegreeMap = new HashMap<>();

    private Map<CompositeSequence, Set<Gene>> probeToGeneMap = null;

    private int numWarnings = 0;

    private Taxon taxon = null;

    public LinkAnalysis( LinkAnalysisConfig config ) {
        this.form = NumberFormat.getInstance();
        if ( form instanceof DecimalFormat )
            ( ( DecimalFormat ) form ).applyPattern( "0.###E0" );
        this.config = config;
    }

    /**
     * Main entry point.
     */
    public void analyze() {
        assert this.dataMatrix != null;
        assert this.taxon != null;
        assert this.probeToGeneMap != null;

        LinkAnalysis.log.debug( "Taxon: " + this.taxon.getCommonName() );

        if ( this.probeToGeneMap.size() == 0 ) {
            LinkAnalysis.log.warn( "No genes found for this dataset. Do the associated platforms need processing?" );
        }

        LinkAnalysis.log.info( "Current Options: \n" + this.config );
        this.calculateDistribution();

        if ( Thread.currentThread().isInterrupted() ) {
            LinkAnalysis.log.info( "Cancelled." );
            return;
        }

        if ( metricMatrix.getNumUniqueGenes() == 0 ) {
            throw new UnsupportedOperationException(
                    "Link analysis not supported when there are no genes mapped to the data set." );
        }

        this.getLinks();

        LinkAnalysis.log.info( metricMatrix.getNumUniqueGenes() + " unique genes." );
        LinkAnalysis.log.info( metricMatrix.getCrossHybridizationRejections()
                + " pairs rejected due to cross-hybridization potential" );

    }

    /**
     * Clear/null data so this object can be reused.
     */
    public void clear() {
        this.dataMatrix = null;
        this.probeToGeneMap = null;
        this.metricMatrix = null;
    }

    /**
     * @return object containing the parameters used.
     */
    public CoexpressionAnalysis getAnalysisObj() {
        return analysis;
    }

    public void setAnalysisObj( CoexpressionAnalysis analysis ) {
        this.analysis = analysis;
    }

    public LinkAnalysisConfig getConfig() {
        return config;
    }

    public CoexpCorrelationDistribution getCorrelationDistribution() {

        CoexpCorrelationDistribution result = CoexpCorrelationDistribution.Factory.newInstance();

        DoubleArrayList histogramArrayList = this.metricMatrix.getHistogramArrayList();
        result.setNumBins( histogramArrayList.size() );
        result.setBinCounts( Arrays.copyOf( histogramArrayList.elements(), histogramArrayList.size() ) );

        return result;
    }

    public ExpressionDataDoubleMatrix getDataMatrix() {
        return dataMatrix;
    }

    public void setDataMatrix( ExpressionDataDoubleMatrix paraDataMatrix ) {
        this.dataMatrix = paraDataMatrix;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return this.expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    /**
     * Gene the genes that were tested, according to the rows that are currently in the dataMatrix (so call this after
     * filtering!)
     *
     * @return set
     */
    public Set<Gene> getGenesTested() {
        Set<Gene> genes = new HashSet<>();
        for ( CompositeSequence cs : dataMatrix.getRowNames() ) {
            Set<Gene> geneClusters = this.probeToGeneMap.get( cs );
            if ( geneClusters == null ) {
                if ( numWarnings <= LinkAnalysis.MAX_WARNINGS ) {
                    LinkAnalysis.log.warn( "No genes for: " + cs );

                    numWarnings++;
                    if ( numWarnings > LinkAnalysis.MAX_WARNINGS ) {
                        LinkAnalysis.log.warn( "Further warnings will be suppressed" );
                    }
                }
                continue;
            }
            genes.addAll( geneClusters );

        }
        return genes;
    }

    public ObjectArrayList getKeep() {
        return keep;
    }

    public QuantitationType getMetric() {
        return this.metricMatrix.getMetricType();
    }

    public MatrixRowPairAnalysis getMetricMatrix() {
        return metricMatrix;
    }

    public CompositeSequence getProbe( int index ) {
        return this.getMetricMatrix().getProbeForRow( this.getDataMatrix().getRowElement( index ) );
    }

    /**
     * @param index row number in the metrixMatirx
     * @return how many Links that probe appears in, or null if the probeDegree has not been populated for that index
     * (that is, either to early to check, or it was zero).
     */
    public Integer getProbeDegree( int index ) {
        return this.probeDegreeMap.get( index );
    }

    public Map<CompositeSequence, Set<Gene>> getProbeToGeneMap() {
        return probeToGeneMap;
    }

    /**
     * Once set, is unmodifiable.
     *
     * @param probeToGeneMap map
     */
    public void setProbeToGeneMap( Map<CompositeSequence, Set<Gene>> probeToGeneMap ) {
        this.probeToGeneMap = Collections.unmodifiableMap( probeToGeneMap );
    }

    public Taxon getTaxon() {
        return this.taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    /**
     * Compute the distribution of similarity metrics for the entire matrix.
     */
    private void calculateDistribution() {
        if ( config.getMetric().equals( "pearson" ) ) {
            LinkAnalysis.log.info( "Using Pearson linear correlation" );
            metricMatrix = new PearsonMetrics( this.dataMatrix, config.getCorrelationCacheThreshold() );
        } else if ( config.getMetric().equals( "spearman" ) ) {
            LinkAnalysis.log.info( "Using Spearman rank correlation" );
            metricMatrix = new SpearmanMetrics( dataMatrix, config.getCorrelationCacheThreshold() );
        }

        metricMatrix.setMinNumpresent( config.getMinNumPresent() );
        metricMatrix.setOmitNegativeCorrelationLinks( config.isOmitNegLinks() );
        metricMatrix.setDuplicateMap( probeToGeneMap ); // populates numUniqueGenes
        metricMatrix.setUseAbsoluteValue( config.isAbsoluteValue() );
        this.init();

        metricMatrix.calculateMetrics();
        LinkAnalysis.log.info( "Completed first pass over the data. Cached " + metricMatrix.numCached()
                + " values in the correlation matrix with values over " + config.getCorrelationCacheThreshold() );

    }

    /**
     * Compute the thresholds needed to choose links for storage in the system.
     */
    private void chooseCutPoints() {
        DoubleArrayList cdf = Stats.cdf( metricMatrix.getHistogramArrayList() );
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
            LinkAnalysis.log.debug( form.format( cdfLowerCutScore ) + " is the lower cdf cutpoint at " + cdfTailCut );
        }

        // find the upper cut point.
        for ( int i = cdf.size() - 1; i >= 0; i-- ) {
            if ( cdf.get( i ) >= cdfTailCut ) {
                cdfUpperCutScore = metricMatrix.getScoreInBin( i == cdf.size() ? i : i + 1 );
                break;
            }
        }

        LinkAnalysis.log.debug( form.format( cdfUpperCutScore ) + " is the upper cdf cutpoint at " + cdfTailCut );

        // get the cutpoint based on statistical signficance.
        double maxP = 1.0;
        double scoreAtP = 0.0;
        if ( config.getFwe() != 0.0 ) {
            double numUniqueGenes = metricMatrix.getNumUniqueGenes();

            maxP = config.getFwe() / numUniqueGenes; // bonferroni.

            scoreAtP = CorrelationStats.correlationForPvalue( maxP, this.dataMatrix.columns() );
            LinkAnalysis.log
                    .debug( "Minimum correlation to get " + form.format( maxP ) + " is about " + form.format( scoreAtP )
                            + " for " + numUniqueGenes + " unique items (if all " + this.dataMatrix.columns()
                            + " items are present)" );
            if ( scoreAtP > 0.9 ) {
                LinkAnalysis.log.warn( "This data set has a very high threshold for statistical significance!" );
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

            if ( !config.isAbsoluteValue() ) {
                config.setLowerTailCut( Math.min( -scoreAtP, cdfLowerCutScore ) );
            }

            if ( config.getLowerTailCut() == scoreAtP ) {
                config.setLowerCdfCutUsed( false );
            } else if ( config.getLowerTailCut() == cdfLowerCutScore ) {
                config.setLowerCdfCutUsed( true );
            }
        } else if ( config.getSingularThreshold().equals( SingularThreshold.fwe ) ) {
            config.setUpperTailCut( scoreAtP );
            if ( !config.isAbsoluteValue() ) {
                config.setLowerTailCut( -scoreAtP );
            }
            config.setUpperCdfCutUsed( false );
            config.setLowerCdfCutUsed( false );
        } else if ( config.getSingularThreshold().equals( SingularThreshold.cdfcut ) ) {
            config.setUpperTailCut( cdfUpperCutScore );
            if ( !config.isAbsoluteValue() ) {
                config.setLowerTailCut( cdfLowerCutScore );
            }
            metricMatrix.setUsePvalueThreshold( false );// use only cdfCut exclusively to keep links
            config.setUpperCdfCutUsed( true );
            config.setLowerCdfCutUsed( true );
        }

        LinkAnalysis.log.info( "Final upper cut is " + form.format( config.getUpperTailCut() ) );
        LinkAnalysis.log.info( "Final lower cut is " + form.format( config.getLowerTailCut() ) );

        metricMatrix.setUpperTailThreshold( config.getUpperTailCut() );
        if ( config.isAbsoluteValue() ) {
            metricMatrix.setLowerTailThreshold( config.getUpperTailCut() );
        } else {
            metricMatrix.setLowerTailThreshold( config.getLowerTailCut() );
        }

    }

    /**
     * Populates a map of probe index (in matrix) -> how many links. FIXME do at gene level
     */
    private void computeProbeDegrees() {

        this.probeDegreeMap = new HashMap<>();

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

    /**
     * Does the main computation and link selection.
     */
    private void getLinks() {
        this.chooseCutPoints();
        metricMatrix.calculateMetrics();

        keep = metricMatrix.getKeepers();

        this.computeProbeDegrees();
    }

    private void init() {
        // estimate the correlation needed to reach significance.
        double scoreP = CorrelationStats.correlationForPvalue( config.getFwe() / this.metricMatrix.getNumUniqueGenes(),
                this.dataMatrix.columns() ) - 0.001; // magic number.
        if ( scoreP > config.getCorrelationCacheThreshold() )
            config.setCorrelationCacheThreshold( scoreP );
    }

}
