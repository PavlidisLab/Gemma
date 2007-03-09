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
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.Link;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.Stats;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.association.coexpression.HumanProbeCoExpression;
import ubic.gemma.model.association.coexpression.MouseProbeCoExpression;
import ubic.gemma.model.association.coexpression.OtherProbeCoExpression;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpression;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.association.coexpression.RatProbeCoExpression;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.TaxonUtility;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.ObjectArrayList;

/**
 * @author xiangwan
 * @version $Id$
 */
public class LinkAnalysis {
    private static final int LINK_BATCH_SIZE = 1000;
    protected static final Log log = LogFactory.getLog( LinkAnalysis.class );
    private MatrixRowPairAnalysis metricMatrix;
    private DoubleArrayList cdf;
    private ObjectArrayList keep;
    private ExpressionDataDoubleMatrix dataMatrix = null;
    private Collection<DesignElementDataVector> dataVectors = null;
    private Probe2ProbeCoexpressionService ppService = null;
    private DesignElementDataVectorService deService = null;
    private CompositeSequenceService csService = null;

    private Map<CompositeSequence, Collection<Gene>> probeToGeneMap = null;
    private Map<Gene, Collection<CompositeSequence>> geneToProbeMap = null;
    private Map<CompositeSequence, DesignElementDataVector> p2v = null;

    private Taxon taxon = null;
    private int uniqueItems = 0;
    private double upperTailCut;
    private double lowerTailCut;

    private NumberFormat form;
    private String metric = "pearson";
    private double tooSmallToKeep = 0.5;
    private boolean absoluteValue = false;
    private double fwe = 0.01;
    private double cdfCut = 0.01; // 1.0 means, keep everything.

    private boolean useDB = true;

    public LinkAnalysis() {
        this.form = NumberFormat.getInstance();
        if ( form instanceof DecimalFormat ) ( ( DecimalFormat ) form ).applyPattern( "00.###E0" );
    }

    /**
     * @return
     */
    public boolean analyze() throws Exception {
        assert this.dataMatrix != null;
        assert this.dataVectors != null;
        assert this.ppService != null;
        assert this.taxon != null;
        assert this.deService != null;
        log.debug( "Taxon: " + this.taxon.getCommonName() );

        this.init();
        if ( this.uniqueItems == 0 ) {
            log.info( "Couldn't find the map between probe and gene " );
            return false;
        }

        this.outputOptions();
        this.calculateDistribution();
        this.writeDistribution();
        this.getLinks();
        this.saveLinks();
        return true;
    }

    public void writeDistribution() throws IOException {

        String path = "";

        DoubleArrayList histogramArrayList = this.metricMatrix.getHistogramArrayList();

        FileWriter out = new FileWriter( new File( path ) );

        double d = -1.0;
        double step = 2.0 / histogramArrayList.size();

        out.write( "# Correlation distribution " );
        out.write( "# date=" + Calendar.getInstance() );
        out.write( "# exp=" + this.dataVectors.iterator().next().getExpressionExperiment() );
        out.write( "Bin\tCount\n" );

        for ( int i = 0; i < histogramArrayList.size(); i++ ) {
            double v = histogramArrayList.get( i );
            out.write( form.format( d ) + "\t" + ( int ) v + "\n" );
            d += step;
        }

        out.close();

    }

    /**
     * 
     *
     */
    private void chooseCutPoints() {

        if ( cdfCut <= 0.0 ) {
            upperTailCut = 1.0;
            lowerTailCut = -1.0;
            return;
        }

        if ( cdfCut >= 1.0 ) {
            upperTailCut = 0.0;
            lowerTailCut = 0.0;
            return;
        }

        double cdfTailCut = cdfCut;
        double cdfUpperCutScore = 0.0;
        double cdfLowerCutScore = 0.0;

        // find the lower tail cutpoint, if we have to.
        if ( !this.absoluteValue ) {
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
        if ( fwe != 0.0 ) {
            maxP = fwe / uniqueItems; // bonferroni.
            scoreAtP = CorrelationStats.correlationForPvalue( maxP, this.dataMatrix.columns() );
            log.info( "Minimum correlation to get " + form.format( maxP ) + " is about " + form.format( scoreAtP )
                    + " for " + uniqueItems + " unique items (if all " + this.dataMatrix.columns()
                    + " items are present)" );
        }
        this.metricMatrix.setPValueThreshold( maxP ); // this is the corrected
        // value.

        upperTailCut = Math.max( scoreAtP, cdfUpperCutScore );
        log.info( "Final upper cut is " + form.format( upperTailCut ) );

        if ( !this.absoluteValue ) {
            lowerTailCut = Math.min( -scoreAtP, cdfLowerCutScore );
            log.info( "Final lower cut is " + form.format( lowerTailCut ) );
        }

        metricMatrix.setUpperTailThreshold( upperTailCut );
        if ( absoluteValue ) {
            metricMatrix.setLowerTailThreshold( upperTailCut );
        } else {
            metricMatrix.setLowerTailThreshold( lowerTailCut );
        }

    }

    private void outputOptions() {
        log.info( "Current Settings" );
        log.info( "AbsouteValue Setting:" + this.absoluteValue );
        log.info( "cdfCut:" + this.cdfCut );
        log.info( "cacheCut:" + this.tooSmallToKeep );
        log.info( "Unique Items:" + this.uniqueItems );
        log.info( "fwe:" + this.fwe );
        log.info( "useDB:" + this.useDB );
    }

    public void setAbsoluteValue() {
        this.absoluteValue = true;
    }

    public void setCdfCut( double cdfCut ) {
        this.cdfCut = cdfCut;
    }

    public void setDataMatrix( ExpressionDataDoubleMatrix paraDataMatrix ) {
        this.dataMatrix = paraDataMatrix;
    }

    public void setDataVectors( Collection<DesignElementDataVector> vectors ) {
        this.dataVectors = vectors;
    }

    public void setDEService( DesignElementDataVectorService deService ) {
        this.deService = deService;
    }

    public void setFwe( double fwe ) {
        this.fwe = fwe;
    }

    public void setMetric( String metric ) {
        this.metric = metric;
    }

    public void setPPService( Probe2ProbeCoexpressionService ppService ) {
        this.ppService = ppService;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    public void setTooSmallToKeep( double tooSmallToKeep ) {
        this.tooSmallToKeep = tooSmallToKeep;
    }

    public void setUseDB( boolean value ) {
        this.useDB = value;
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

    /**
     * Compute the distribution of similarity metrics for the entire matrix.
     */
    private void calculateDistribution() {
        if ( metric.equals( "pearson" ) ) {
            metricMatrix = MatrixRowPairAnalysisFactory.pearson( this.dataMatrix, this.tooSmallToKeep );
        } else if ( metric.equals( "spearmann" ) ) {
            throw new UnsupportedOperationException( "Spearmann not supported" );
            // metricMatrix = MatrixRowPairAnalysisFactory.spearman(dataMatrix,
            // tooSmallToKeep);
        }

        metricMatrix.setDuplicateMap( probeToGeneMap, geneToProbeMap );

        metricMatrix.setUseAbsoluteValue( this.absoluteValue );
        metricMatrix.calculateMetrics();
        log.info( "Completed first pass over the data. Cached " + metricMatrix.numCached()
                + " values in the correlation matrix with values over " + this.tooSmallToKeep );

    }

    /**
     * 
     *
     */
    private void getLinks() {
        cdf = Stats.cdf( metricMatrix.getHistogramArrayList() );
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
        this.p2v = new HashMap<CompositeSequence, DesignElementDataVector>();

        this.geneToProbeMap = new HashMap<Gene, Collection<CompositeSequence>>();

        StopWatch watch = new StopWatch();
        watch.start();

        log.info( "Collecting probes..." );
        Collection<CompositeSequence> probesForVectors = new HashSet<CompositeSequence>();
        for ( DesignElementDataVector v : dataVectors ) {
            CompositeSequence cs = ( CompositeSequence ) v.getDesignElement();
            probesForVectors.add( cs );
            p2v.put( cs, v );
        }

        log.info( "Mapping probes to genes..." );
        this.probeToGeneMap = new HashMap<CompositeSequence, Collection<Gene>>();

        probeToGeneMap = csService.getGenes( probesForVectors );

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

        this.uniqueItems = this.geneToProbeMap.keySet().size();
        watch.stop();
        log.info( "Finished mapping in " + ( double ) watch.getTime() / 1000.0 + " seconds" );
        log.info( "Mapping Stats " + ArrayUtils.toString( stats ) );
        if ( this.uniqueItems == 0 ) return;

        // estimate the correlation needed to reach significance.
        double scoreP = CorrelationStats.correlationForPvalue( this.fwe / this.uniqueItems, this.dataMatrix.columns() ) - 0.001;
        if ( scoreP > this.tooSmallToKeep ) this.tooSmallToKeep = scoreP;
    }

    /**
     * Persist the links to the database.
     */
    private void saveLinks() {

        if ( !this.useDB ) {
            return;
        }

        /*
         * Delete old links for this expressionexperiment
         */

        ExpressionExperiment expressionExperiment = p2v.values().iterator().next().getExpressionExperiment();
        log.info( "Deleting any old links for " + expressionExperiment );
        ppService.deleteLinks( expressionExperiment );

        /* *******Find the dataVector for each probe first*** */
        int c = dataMatrix.columns();
        int[] p1vAr = new int[keep.size()];
        int[] p2vAr = new int[keep.size()];
        int[] cbAr = new int[keep.size()];
        int[] pbAr = new int[keep.size()];
        log.info( "Start submitting data to database." );
        StopWatch watch = new StopWatch();
        watch.start();
        Collection<Probe2ProbeCoexpression> p2plinks = new HashSet<Probe2ProbeCoexpression>();
        for ( int i = 0, n = keep.size(); i < n; i++ ) {
            Link m = ( Link ) keep.get( i );
            double w = m.getWeight();

            Collection<DesignElement> p1s = dataMatrix.getDesignElementsForRow( m.getx() );
            Collection<DesignElement> p2s = dataMatrix.getDesignElementsForRow( m.gety() );

            if ( p1s.size() > 1 || p2s.size() > 1 ) {
                throw new UnsupportedOperationException(
                        "Sorry, we don't know what to do when a row refers to more than one DesignElement." );
            }

            DesignElement p1 = p1s.iterator().next();
            DesignElement p2 = p2s.iterator().next();

            DesignElementDataVector v1 = p2v.get( p1 );
            DesignElementDataVector v2 = p2v.get( p2 );
            p1vAr[i] = new Long( v1.getId() ).intValue();
            p2vAr[i] = new Long( v2.getId() ).intValue();
            cbAr[i] = CorrelationStats.correlAsByte( w );
            pbAr[i] = CorrelationStats.pvalueAsByte( w, c );

            persist( c, p2plinks, i, w, v1, v2 );

            if ( i > 0 && i % 20000 == 0 ) {
                log.info( i + " links loaded into the database" );
            }

        }
        if ( p2plinks.size() > 0 ) this.ppService.create( p2plinks );
        watch.stop();
        log.info( "Seconds to insert " + this.keep.size() + " links:" + ( double ) watch.getTime() / 1000.0 );
        /**
         * System.err.println( "Ready to submit to tmm database." ); if(this.useDB) { LinkInserter li = new
         * LinkInserter( this.dbManager.getDbHandle() ); int dataSetId = 0; int rd = li.insertBulkLink( dataSetId,
         * p1vAr, p2vAr, cbAr, pbAr ); System.err.println( "Inserted " + rd + " links into the database" ); }
         */

    }

    /**
     * @param c
     * @param p2plinks
     * @param i
     * @param w
     * @param v1
     * @param v2
     * @return
     */
    private Probe2ProbeCoexpression persist( int c, Collection<Probe2ProbeCoexpression> p2plinks, int i, double w,
            DesignElementDataVector v1, DesignElementDataVector v2 ) {
        Probe2ProbeCoexpression ppCoexpression;
        if ( taxon == null ) {
            throw new IllegalStateException( "Taxon cannot be null" );
        }

        if ( TaxonUtility.isMouse( taxon ) ) {
            ppCoexpression = MouseProbeCoExpression.Factory.newInstance();
        } else if ( TaxonUtility.isRat( taxon ) ) {
            ppCoexpression = RatProbeCoExpression.Factory.newInstance();
        } else if ( TaxonUtility.isHuman( taxon ) ) {
            ppCoexpression = HumanProbeCoExpression.Factory.newInstance();
        } else {
            ppCoexpression = OtherProbeCoExpression.Factory.newInstance();
        }

        ppCoexpression.setFirstVector( v1 );
        ppCoexpression.setSecondVector( v2 );
        ppCoexpression.setScore( w );
        ppCoexpression.setPvalue( CorrelationStats.pvalue( w, c ) );
        ppCoexpression.setQuantitationType( v1.getQuantitationType() );
        p2plinks.add( ppCoexpression );
        if ( i % LINK_BATCH_SIZE == 0 ) {
            this.ppService.create( p2plinks );
            p2plinks.clear();
        }
        return ppCoexpression;
    }

    public void setCsService( CompositeSequenceService csService ) {
        this.csService = csService;
    }

}
