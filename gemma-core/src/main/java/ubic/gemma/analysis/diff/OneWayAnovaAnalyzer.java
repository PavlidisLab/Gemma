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
package ubic.gemma.analysis.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.REngine.REXPMismatchException;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.dataStructure.matrix.FastRowAccessDoubleMatrix2DNamed;
import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.analysis.ProbeAnalysisResult;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
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
 * qvalue(pvals)$qvalues
 * 
 * @spring.bean id="oneWayAnovaAnalyzer"
 * @author keshav
 * @version $Id$
 */
public class OneWayAnovaAnalyzer extends AbstractDifferentialExpressionAnalyzer {

    private Log log = LogFactory.getLog( this.getClass() );

    private static final int NUM_RESULTS_FROM_R = 2;

    private List<String> rFactors = null;

    private Map<Integer, DesignElement> filteredMatrixDesignElementIndexMap = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractDifferentialExpressionAnalyzer#getExpressionAnalysis(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public DifferentialExpressionAnalysis getDifferentialExpressionAnalysis( ExpressionExperiment expressionExperiment ) {
        return oneWayAnova( expressionExperiment );
    }

    /**
     * @param expressionExperiment
     * @return
     * @throws REXPMismatchException
     */
    public DifferentialExpressionAnalysis oneWayAnova( ExpressionExperiment expressionExperiment ) {

        connectToR();

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();

        if ( experimentalFactors.size() != 1 )
            throw new RuntimeException( "One way anova supports one experimental factor.  Received "
                    + experimentalFactors.size() + "." );

        ExperimentalFactor experimentalFactor = experimentalFactors.iterator().next();

        Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();

        if ( factorValues.size() < 2 )
            throw new RuntimeException(
                    "One way anova requires 2 or more factor values (2 factor values is a t-test).  Received "
                            + factorValues.size() + "." );

        Collection<DesignElementDataVector> vectorsToUse = analysisHelperService.getVectors( expressionExperiment );

        QuantitationType quantitationType = getPreferredQuantitationType( vectorsToUse );
        if ( quantitationType == null ) {
            throw new RuntimeException( // FIXME could be excessive ... log as an error?
                    "Could not determine the preferred quantitation type.  Not sure what type to associate with the analysis." );
        }

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectorsToUse );

        ExpressionDataDoubleMatrix dmatrix = builder.getMaskedPreferredData( null );

        DoubleMatrixNamed filteredNamedMatrix = this.filterMatrix( dmatrix, factorValues );

        String facts = rc.assignStringList( rFactors );

        String tfacts = "t(" + facts + ")";

        String factor = "factor(" + tfacts + ")";

        String matrixName = rc.assignMatrix( filteredNamedMatrix );

        /* p-values */
        StringBuffer pvalueCommand = new StringBuffer();

        pvalueCommand.append( "apply(" );
        pvalueCommand.append( matrixName );
        pvalueCommand.append( ", 1, function(x) {anova(aov(x ~ " + factor + "))$Pr}" );
        pvalueCommand.append( ")" );

        log.info( pvalueCommand.toString() );

        double[] pvalues = rc.doubleArrayEval( pvalueCommand.toString() );

        // removes NA row
        double[] filteredPvalues = new double[pvalues.length / NUM_RESULTS_FROM_R];

        for ( int i = 0, j = 0; j < filteredPvalues.length; i++ ) {
            if ( i % NUM_RESULTS_FROM_R == 0 ) {
                filteredPvalues[j] = pvalues[i];
                j++;
            }
        }

        /* f-statistic */
        StringBuffer fStatisticCommand = new StringBuffer();

        fStatisticCommand.append( "apply(" );
        fStatisticCommand.append( matrixName );
        fStatisticCommand.append( ", 1, function(x) {anova(aov(x ~ " + factor + "))$F}" );
        fStatisticCommand.append( ")" );

        log.info( fStatisticCommand.toString() );

        double[] fstatistics = rc.doubleArrayEval( fStatisticCommand.toString() );

        // removes NA row
        double[] filteredFStatistics = new double[fstatistics.length / NUM_RESULTS_FROM_R];

        for ( int i = 0, j = 0; j < filteredFStatistics.length; i++ ) {
            if ( i % NUM_RESULTS_FROM_R == 0 ) {
                filteredFStatistics[j] = fstatistics[i];
                j++;
            }
        }

        /* q-value */
        double[] qvalues = super.getQValues( filteredPvalues );

        /* Create the expression analysis and pack the results. */
        // TODO pass the DifferentialExpressionAnalysisConfig in (see LinkAnalysisService)
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        DifferentialExpressionAnalysis expressionAnalysis = config.toAnalysis();

        Collection<ExpressionExperiment> experimentsAnalyzed = new HashSet<ExpressionExperiment>();
        expressionAnalysis.setExperimentsAnalyzed( experimentsAnalyzed );

        List<DifferentialExpressionAnalysisResult> analysisResults = new ArrayList<DifferentialExpressionAnalysisResult>();
        for ( int i = 0; i < filteredNamedMatrix.rows(); i++ ) {

            DesignElement de = filteredMatrixDesignElementIndexMap.get( i );

            CompositeSequence cs = ( CompositeSequence ) de;

            ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
            probeAnalysisResult.setProbe( cs );
            probeAnalysisResult.setPvalue( filteredPvalues[i] );
            probeAnalysisResult.setCorrectedPvalue( qvalues[i] );
            probeAnalysisResult.setScore( filteredFStatistics[i] );

            probeAnalysisResult.setQuantitationType( quantitationType );

            analysisResults.add( probeAnalysisResult );
        }

        Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();
        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        factors.add( experimentalFactor );
        ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance( analysisResults,
                expressionAnalysis, factors );
        resultSets.add( resultSet );

        expressionAnalysis.setResultSets( resultSets );

        expressionAnalysis.setName( this.getClass().getSimpleName() );
        expressionAnalysis.setDescription( expressionExperiment.getShortName() );

        return expressionAnalysis;
    }

    /**
     * Filters the {@link ExpressionDataDoubleMatrix} and removes rows with too many missing values. This filtering is
     * based on the R interpretation of too many missing values.
     * 
     * @param matrix
     * @param factorValues
     * @return
     */
    private DoubleMatrixNamed filterMatrix( ExpressionDataDoubleMatrix matrix, Collection<FactorValue> factorValues ) {
        // TODO make this a requirement in the abstract analyzer.
        Collection<BioMaterial> samplesUsed = AnalyzerHelper.getBioMaterialsForBioAssays( matrix );

        rFactors = AnalyzerHelper.getRFactorsFromFactorValuesForOneWayAnova( factorValues, samplesUsed );

        return filterDoubleMatrixNamedForValidRows( matrix, rFactors );
    }

    /**
     * @param matrix
     * @param rFactors
     * @return
     */
    private DoubleMatrixNamed filterDoubleMatrixNamedForValidRows( ExpressionDataDoubleMatrix matrix,
            List<String> rFactors ) {

        ArrayList<double[]> filteredRows = new ArrayList<double[]>();

        Collection<String> factorLevels = new HashSet<String>( rFactors );

        DoubleMatrixNamed matrixNamed = matrix.getNamedMatrix();

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

        return new FastRowAccessDoubleMatrix2DNamed( ddata );
    }
}
