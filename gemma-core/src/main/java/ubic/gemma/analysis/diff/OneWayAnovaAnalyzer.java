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
import java.util.HashSet;
import java.util.List;

import org.rosuda.JRclient.REXP;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ProbeAnalysisResult;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
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
 * R Call:
 * <p>
 * apply(matrix,1,function(x){anova(aov(x~factor))$Pr})
 * <p>
 * apply(matrix,1,function(x){anova(aov(x~factor))$F})
 * <p>
 * where factor is a vector that has first been transposed and then had factor() applied.
 * 
 * @author keshav
 * @version $Id$
 */
public class OneWayAnovaAnalyzer extends AbstractAnalyzer {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getPValues(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType,
     *      ubic.gemma.model.expression.bioAssayData.BioAssayDimension, java.util.Collection)
     */
    @Override
    public ExpressionAnalysis getExpressionAnalysis( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType, BioAssayDimension bioAssayDimension ) {

        return oneWayAnova( expressionExperiment, quantitationType, bioAssayDimension );
    }

    /**
     * See class level javadoc for R Call.
     * 
     * @param matrix
     * @param factorValues
     * @param samplesUsed
     * @return
     */
    public ExpressionAnalysis oneWayAnova( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType, BioAssayDimension bioAssayDimension ) {

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

        ExpressionDataDoubleMatrix dmatrix = new ExpressionDataDoubleMatrix( expressionExperiment
                .getDesignElementDataVectors(), bioAssayDimension, quantitationType );

        Collection<BioMaterial> samplesUsed = AnalyzerHelper.getBioMaterialsForBioAssays( dmatrix );

        DoubleMatrixNamed namedMatrix = dmatrix.getNamedMatrix();

        List<String> rFactors = AnalyzerHelper.getRFactorsFromFactorValuesForOneWayAnova( factorValues, samplesUsed );

        String facts = rc.assignStringList( rFactors );

        String tfacts = "t(" + facts + ")";

        String factor = "factor(" + tfacts + ")";

        String matrixName = rc.assignMatrix( namedMatrix );

        /* p-values */
        StringBuffer command = new StringBuffer();

        command.append( "apply(" );
        command.append( matrixName );
        command.append( ", 1, function(x) {anova(aov(x ~ " + factor + "))$Pr}" );
        command.append( ")" );

        log.info( command.toString() );

        REXP regExp = rc.eval( command.toString() );

        double[] pvalues = ( double[] ) regExp.getContent();

        double[] filteredPvalues = new double[pvalues.length / 2];// removes the NaN row

        for ( int i = 0, j = 0; j < filteredPvalues.length; i++ ) {
            if ( i % 2 == 0 ) {
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

        REXP fRegExp = rc.eval( fStatisticCommand.toString() );

        double[] fstatistics = ( double[] ) fRegExp.getContent();

        double[] filteredFStatistics = new double[fstatistics.length / 2];// removes the NaN row

        for ( int i = 0, j = 0; j < filteredFStatistics.length; i++ ) {
            if ( i % 2 == 0 ) {
                filteredFStatistics[j] = fstatistics[i];
                j++;
            }
        }

        /* Create the expression analysis and pack the results. */
        ExpressionAnalysis expressionAnalysis = ExpressionAnalysis.Factory.newInstance();

        Collection<ExpressionExperiment> experimentsAnalyzed = new HashSet<ExpressionExperiment>();
        expressionAnalysis.setExperimentsAnalyzed( experimentsAnalyzed );

        List<ExpressionAnalysisResult> analysisResults = new ArrayList<ExpressionAnalysisResult>();
        for ( int i = 0; i < dmatrix.rows(); i++ ) {
            DesignElement de = dmatrix.getDesignElementForRow( i );
            // FIXME maybe ProbeAnalysisResult should have a DesignElement to avoid typecasting
            CompositeSequence cs = ( CompositeSequence ) de;

            ProbeAnalysisResult probeAnalysisResult = ProbeAnalysisResult.Factory.newInstance();
            probeAnalysisResult.setProbe( cs );
            probeAnalysisResult.setPvalue( filteredPvalues[i] );
            probeAnalysisResult.setScore( filteredFStatistics[i] );
            probeAnalysisResult.setQuantitationType( quantitationType );

            analysisResults.add( probeAnalysisResult );
        }

        expressionAnalysis.setAnalysisResults( analysisResults );

        return expressionAnalysis;
    }
}
