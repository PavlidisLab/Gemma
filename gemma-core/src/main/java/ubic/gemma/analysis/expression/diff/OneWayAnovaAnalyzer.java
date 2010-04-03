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

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.REngine.REXPMismatchException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.FastRowAccessDoubleMatrix;
import ubic.basecode.util.r.type.OneWayAnovaResult;
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
@Scope(value = "prototype")
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
            throw new RuntimeException( "One way anova supports one experimental factor.  Received " + factors.size() );
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
            int numBins, int min, int max, Double[] pvalues ) {

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
     * Remove rows that have incomplete data? Why don't other analyzers need this?
     * 
     * @param rFactors
     * @return
     */
    private DoubleMatrix<DesignElement, Integer> filterMatrix( ExpressionDataDoubleMatrix matrix, List<String> rFactors ) {

        List<double[]> filteredRows = new ArrayList<double[]>();

        Collection<String> factorLevels = new HashSet<String>( rFactors );

        DoubleMatrix<DesignElement, Integer> matrixNamed = matrix.getMatrix();

        filteredMatrixDesignElementIndexMap = new HashMap<Integer, DesignElement>();

        List<DesignElement> rowNames = new ArrayList<DesignElement>();
        for ( int i = 0; i < matrixNamed.rows(); i++ ) {

            DesignElement de = matrix.getDesignElementForRow( i );

            double[] row = matrixNamed.getRow( i );

            Collection<String> seenFactors = new HashSet<String>();

            for ( int j = 0; j < row.length; j++ ) {

                String rFactor = rFactors.get( j );

                if ( Double.isNaN( row[j] ) && !seenFactors.contains( rFactor ) ) {

                    if ( log.isDebugEnabled() )
                        log.debug( "Looking for valid data points in row with factor " + rFactor );

                    /* find all columns with the same factor as row[j] */
                    boolean skipRow = true;
                    for ( int k = 0; k < rFactors.size(); k++ ) {

                        if ( k == j ) continue;

                        if ( !Double.isNaN( row[k] ) ) {

                            if ( rFactors.get( k ).equals( rFactor ) ) {
                                skipRow = false;
                                if ( log.isDebugEnabled() )
                                    log.debug( "Valid data point found for factor " + rFactor );
                                break;
                            }
                        }
                    }
                    if ( skipRow ) break;

                }
                seenFactors.add( rFactor );
                if ( seenFactors.size() == factorLevels.size() ) {// seen all factors?
                    filteredRows.add( row );
                    rowNames.add( de );
                    filteredMatrixDesignElementIndexMap.put( filteredRows.indexOf( row ), de );
                    break;
                }

            }
        }

        double[][] ddata = new double[filteredRows.size()][];
        for ( int i = 0; i < ddata.length; i++ ) {
            ddata[i] = filteredRows.get( i );
        }

        DoubleMatrix<DesignElement, Integer> filteredMatrix = new FastRowAccessDoubleMatrix<DesignElement, Integer>(
                ddata );
        filteredMatrix.setColumnNames( matrixNamed.getColNames() );
        filteredMatrix.setRowNames( rowNames );

        return filteredMatrix;
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
                            + factorValues.size() );

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment );

        List<BioMaterial> samplesUsed = DifferentialExpressionAnalysisHelperService
                .getBioMaterialsForBioAssays( dmatrix );

        List<String> rFactors = DifferentialExpressionAnalysisHelperService.getRFactorsFromFactorValuesForOneWayAnova(
                factorValues, samplesUsed );

        /*
         * if possible sue this to compute effect sizes?
         */
        FactorValue controlGroup = determineControlGroup( factorValues );

        DoubleMatrix<DesignElement, Integer> filteredNamedMatrix = this.filterMatrix( dmatrix, rFactors );

        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();

        String facts = rc.assignStringList( rFactors );
        String tfacts = "t(" + facts + ")";
        String factor = "factor(" + tfacts + ")";

        Transformer rowNameExtractor = TransformerUtils.invokerTransformer( "getId" );
        String matrixName = rc.assignMatrix( filteredNamedMatrix, rowNameExtractor );

        StringBuilder buf = new StringBuilder();
        buf.append( "apply(" );
        buf.append( matrixName );
        buf.append( ", 1, function(x) {try(anova(aov(x ~ " + factor + ")),silent=T)}" );
        buf.append( ")" );
        String pvalueCmd = buf.toString();

        log.info( "Starting ANOVA analysis ..." );
        log.debug( pvalueCmd );

        Map<String, OneWayAnovaResult> anovaResult = rc.oneWayAnovaEval( pvalueCmd );

        Double[] pvalues = new Double[filteredNamedMatrix.rows()];
        Double[] fstatistics = new Double[filteredNamedMatrix.rows()];
        int i = 0;
        for ( DesignElement el : filteredNamedMatrix.getRowNames() ) {
            OneWayAnovaResult result = anovaResult.get( rowNameExtractor.transform( el ).toString() );
            assert result != null;
            pvalues[i] = result.getPval();
            fstatistics[i] = result.getFVal();
            i++;
        }

        double[] ranks = computeRanks( ArrayUtils.toPrimitive( pvalues ) );

        /* write out histogram */
        ArrayList<ExperimentalFactor> effects = new ArrayList<ExperimentalFactor>();
        effects.add( experimentalFactor );
        writePValuesHistogram( pvalues, expressionExperiment, effects );

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
        for ( i = 0; i < filteredNamedMatrix.rows(); i++ ) {

            DesignElement de = filteredMatrixDesignElementIndexMap.get( i );

            CompositeSequence cs = ( CompositeSequence ) de;

            ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
            probeAnalysisResult.setProbe( cs );
            probeAnalysisResult.setPvalue( nan2Null( pvalues[i] ) );
            probeAnalysisResult.setCorrectedPvalue( nan2Null( qvalues[i] ) );
            probeAnalysisResult.setScore( nan2Null( fstatistics[i] ) );
            probeAnalysisResult.setRank( nan2Null( ranks[i] ) );
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

        log.info( "ANOVA complete" );

        disconnectR();

        return expressionAnalysis;

    }
}
