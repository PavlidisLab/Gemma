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

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.dataStructure.matrix.FastRowAccessDoubleMatrix2DNamed;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
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
 * @spring.bean id="oneWayAnovaAnalyzer"
 * @author keshav
 * @version $Id$
 */
public class OneWayAnovaAnalyzer extends AbstractDifferentialExpressionAnalyzer {

    private Log log = LogFactory.getLog( this.getClass() );

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

        Collection<DesignElementDataVector> vectorsToUse = analysisHelperService
                .getUsefulVectors( expressionExperiment );

        ExpressionDataDoubleMatrix dmatrix = this.createMaskedMatrix( vectorsToUse );

        DoubleMatrixNamed filteredNamedMatrix = this.filterMatrix( dmatrix, experimentalFactor );

        QuantitationType quantitationType = getPreferredQuantitationType( vectorsToUse );

        String facts = rc.assignStringList( rFactors );

        String tfacts = "t(" + facts + ")";

        String factor = "factor(" + tfacts + ")";

        String matrixName = rc.assignMatrix( filteredNamedMatrix );

        /* p-values */
        StringBuffer pvalueBuf = new StringBuffer();

        pvalueBuf.append( "apply(" );
        pvalueBuf.append( matrixName );
        pvalueBuf.append( ", 1, function(x) {anova(aov(x ~ " + factor + "))$Pr}" );
        pvalueBuf.append( ")" );

        String pvalueCmd = pvalueBuf.toString() + "[1,]";
        log.info( pvalueCmd );

        double[] pvalues = rc.doubleArrayEval( pvalueCmd );

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
        log.info( fStatisticCmd.toString() );

        double[] fstatistics = rc.doubleArrayEval( fStatisticCmd );

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
            probeAnalysisResult.setPvalue( pvalues[i] );
            probeAnalysisResult.setCorrectedPvalue( qvalues[i] );
            probeAnalysisResult.setScore( fstatistics[i] );

            probeAnalysisResult.setQuantitationType( quantitationType );

            analysisResults.add( probeAnalysisResult );
        }

        Collection<ExpressionAnalysisResultSet> resultSets = new HashSet<ExpressionAnalysisResultSet>();
        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        factors.add( experimentalFactor );
        ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance( expressionAnalysis,
                analysisResults, factors );
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
     * @param experimentalFactor
     * @return
     */
    private DoubleMatrixNamed filterMatrix( ExpressionDataDoubleMatrix matrix, ExperimentalFactor experimentalFactor ) {
        // TODO make this a requirement in the abstract analyzer.
        List<BioMaterial> samplesUsed = AnalyzerHelper.getBioMaterialsForBioAssays( matrix );

        rFactors = AnalyzerHelper.getRFactorsFromFactorValuesForOneWayAnova( experimentalFactor.getFactorValues(),
                samplesUsed );

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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#generateHistograms(java.lang.String,
     *      java.util.ArrayList, int, int, int, double[])
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

}
