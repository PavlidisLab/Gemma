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
package ubic.gemma.analysis.expression.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.REngine.REXPMismatchException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.FastRowAccessDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A one way anova implementation as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * <p>
 * R Calls:
 * <p>
 * apply(matrix,1,function(x){anova(aov(x~factor))$Pr})
 * <p>
 * apply(matrix,1,function(x){anova(aov(x~factor))$F})
 * <p>
 * where factor is a vector that has first been transposed and then had factor() applied.
 * <p>
 * P values are obtained with:
 * <p>
 * results<-apply(matrix,1,function(x){anova(aov(x~factor))$Pr})
 * <p>
 * pvals<-results[1,]
 * <p>
 * Statistics are obtained in the same way.
 * <p>
 * qvalue(pvals)$qvalues
 *  
 * @author keshav
 * @version $Id$
 */
@Service
@Scope(value="prototype")
public class OneWayAnovaAnalyzer extends AbstractDifferentialExpressionAnalyzer {

    private Map<Integer, DesignElement> filteredMatrixDesignElementIndexMap = null;

    private Log log = LogFactory.getLog( this.getClass() );

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.analysis.diff.AbstractDifferentialExpressionAnalyzer#getExpressionAnalysis(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment ) {

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();

        return run( expressionExperiment, experimentalFactors );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#run(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, java.util.Collection)
     */
    @Override
    public DifferentialExpressionAnalysis run( ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors ) {

        if ( factors.size() != 1 ) {
            throw new RuntimeException( "One way anova supports one experimental factor.  Received " + factors.size()
                    + "." );
        }

        ExperimentalFactor factor = factors.iterator().next();

        return this.oneWayAnova( expressionExperiment, factor );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#generateHistograms(java.lang.String,
     * java.util.ArrayList, int, int, int, double[])
     */
    @Override
    protected Collection<Histogram> generateHistograms( String histFileName, ArrayList<ExperimentalFactor> effects,
            int numBins, int min, int max, double[] pvalues ) {

        histFileName = StringUtils.removeEnd( histFileName, DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX );

        String newName = histFileName + "_" + effects.iterator().next().getName()
                + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;

        Collection<Histogram> hists = new HashSet<Histogram>();

        Histogram hist = new Histogram( newName, numBins, min, max );
        for ( int i = 0; i < pvalues.length; i++ ) {
            hist.fill( pvalues[i] );
        }

        hists.add( hist );

        return hists;
    }

    /**
     * @param matrix
     * @param rFactors
     * @return
     */
    private DoubleMatrix<DesignElement, Integer> filterMatrix( ExpressionDataDoubleMatrix matrix, List<String> rFactors ) {

        ArrayList<double[]> filteredRows = new ArrayList<double[]>();

        Collection<String> factorLevels = new HashSet<String>( rFactors );

        DoubleMatrix<DesignElement, Integer> matrixNamed = matrix.getMatrix();

        filteredMatrixDesignElementIndexMap = new HashMap<Integer, DesignElement>();

        for ( int i = 0; i < matrixNamed.rows(); i++ ) {

            DesignElement de = matrix.getDesignElementForRow( i );

            double[] row = matrixNamed.getRow( i );

            Collection<String> seenFactors = new HashSet<String>();

            for ( int j = 0; j < row.length; j++ ) {

                String rFactor = rFactors.get( j );

                if ( Double.isNaN( row[j] ) && !seenFactors.contains( rFactor ) ) {

                    log.debug( "Looking for valid data points in row with factor " + rFactor + "." );

                    /* find all columns with the same factor as row[j] */
                    boolean skipRow = true;
                    for ( int k = 0; k < rFactors.size(); k++ ) {
                        // TODO optimize this loop
                        if ( k == j ) continue;

                        if ( !Double.isNaN( row[k] ) ) {

                            if ( rFactors.get( k ).equals( rFactor ) ) {
                                skipRow = false;
                                log.debug( "Valid data point found for factor " + rFactor + "." );
                                break;
                            }
                        }
                    }
                    if ( skipRow ) break;

                }
                seenFactors.add( rFactor );
                if ( seenFactors.size() == factorLevels.size() ) {// seen all factors?
                    filteredRows.add( row );
                    filteredMatrixDesignElementIndexMap.put( filteredRows.indexOf( row ), de );
                    break;
                }

            }
        }

        double[][] ddata = new double[filteredRows.size()][];
        for ( int i = 0; i < ddata.length; i++ ) {
            ddata[i] = filteredRows.get( i );
        }

        return new FastRowAccessDoubleMatrix<DesignElement, Integer>( ddata );
    }

    /**
     * @param expressionExperiment
     * @return
     * @throws REXPMismatchException
     */
    private DifferentialExpressionAnalysis oneWayAnova( ExpressionExperiment expressionExperiment,
            ExperimentalFactor experimentalFactor ) {

        connectToR();

        Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();

        if ( factorValues.size() < 2 )
            throw new RuntimeException(
                    "One way anova requires 2 or more factor values (2 factor values is a t-test).  Received "
                            + factorValues.size() + "." );

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment );

        List<BioMaterial> samplesUsed = DifferentialExpressionAnalysisHelperService
                .getBioMaterialsForBioAssays( dmatrix );

        List<String> rFactors = DifferentialExpressionAnalysisHelperService.getRFactorsFromFactorValuesForOneWayAnova(
                experimentalFactor.getFactorValues(), samplesUsed );

        DoubleMatrix<DesignElement, Integer> filteredNamedMatrix = this.filterMatrix( dmatrix, rFactors );

        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();

        String facts = rc.assignStringList( rFactors );

        String tfacts = "t(" + facts + ")";

        String factor = "factor(" + tfacts + ")";

        String matrixName = rc.assignMatrix( filteredNamedMatrix );

        /*
         * FIXME this runs the analysis twice (for the p values and f-statistics). Wasteful.
         */

        /* p-values */
        StringBuffer pvalueBuf = new StringBuffer();

        pvalueBuf.append( "apply(" );
        pvalueBuf.append( matrixName );
        pvalueBuf.append( ", 1, function(x) {anova(aov(x ~ " + factor + "))$Pr}" );
        pvalueBuf.append( ")" );

        String pvalueCmd = pvalueBuf.toString() + "[1,]";

        log.info( "Starting R analysis ... please wait!" );
        log.debug( pvalueCmd.toString() );

        log.info( "Calculating p values.  R analysis started." );
        double[] pvalues = rc.doubleArrayEvalWithLogging( pvalueCmd );
        double[] ranks = computeRanks( pvalues );

        if ( pvalues == null ) throw new IllegalStateException( "No pvalues returned" );

        /* write out histogram */
        ArrayList<ExperimentalFactor> effects = new ArrayList<ExperimentalFactor>();
        effects.add( experimentalFactor );
        writePValuesHistogram( pvalues, expressionExperiment, effects );

        /* f-statistic */
        StringBuffer fStatisticBuf = new StringBuffer();

        fStatisticBuf.append( "apply(" );
        fStatisticBuf.append( matrixName );
        fStatisticBuf.append( ", 1, function(x) {anova(aov(x ~ " + factor + "))$F}" );
        fStatisticBuf.append( ")" );

        String fStatisticCmd = fStatisticBuf.toString() + "[1,]";
        log.debug( fStatisticCmd.toString() );

        log.info( "Calculating f statistics.  R analysis started." );
        double[] fstatistics = rc.doubleArrayEvalWithLogging( fStatisticCmd );

        /* q-value */
        double[] qvalues = super.getQValues( pvalues );

        /* Create the expression analysis and pack the results. */
        // TODO pass the DifferentialExpressionAnalysisConfig in (see LinkAnalysisService)
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis expressionAnalysis = config
                .toAnalysis();

        ExpressionExperimentSet eeSet = ExpressionExperimentSet.Factory.newInstance();
        Collection<BioAssaySet> experimentsAnalyzed = new HashSet<BioAssaySet>();
        experimentsAnalyzed.add( expressionExperiment );
        eeSet.setExperiments( experimentsAnalyzed );
        expressionAnalysis.setExpressionExperimentSetAnalyzed( eeSet );

        List<DifferentialExpressionAnalysisResult> analysisResults = new ArrayList<DifferentialExpressionAnalysisResult>();
        for ( int i = 0; i < filteredNamedMatrix.rows(); i++ ) {

            DesignElement de = filteredMatrixDesignElementIndexMap.get( i );

            CompositeSequence cs = ( CompositeSequence ) de;

            ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
            probeAnalysisResult.setProbe( cs );
            probeAnalysisResult.setPvalue( Double.isNaN( pvalues[i] ) ? null : pvalues[i] );
            probeAnalysisResult.setCorrectedPvalue( Double.isNaN( qvalues[i] ) ? null : qvalues[i] );
            probeAnalysisResult.setScore( Double.isNaN( fstatistics[i] ) ? null : fstatistics[i] );
            probeAnalysisResult.setRank( Double.isNaN( pvalues[i] ) ? null : ranks[i] );
            probeAnalysisResult.setQuantitationType( quantitationType );

            analysisResults.add( probeAnalysisResult );
        }

        Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();
        Collection<ExperimentalFactor> factorsInAnalysis = new HashSet<ExperimentalFactor>();
        factorsInAnalysis.add( experimentalFactor );
        ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance( expressionAnalysis,
                analysisResults, factorsInAnalysis );
        resultSets.add( resultSet );

        expressionAnalysis.setResultSets( resultSets );

        expressionAnalysis.setName( this.getClass().getSimpleName() );
        expressionAnalysis.setDescription( "One-way ANOVA for " + experimentalFactor );
        
        disconnectR();
        
        log.info( "R analysis done" );
        return expressionAnalysis;

    }
}
