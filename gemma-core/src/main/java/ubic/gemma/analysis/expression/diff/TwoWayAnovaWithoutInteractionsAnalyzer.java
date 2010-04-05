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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.util.r.type.TwoWayAnovaResult;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A two way anova implementation without interactions as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * <p>
 * R Call:
 * <p>
 * apply(matrix,1,function(x){anova(aov(x~farea+ftreat))})
 * <p>
 * where area and treat are first transposed and then factor is called on each to give farea and ftreat.
 * <p>
 * qvalue(pvals)$qvalues
 * 
 * @author keshav
 * @version $Id$
 * @see AbstractTwoWayAnovaAnalyzer
 */
@Service
@Scope(value = "prototype")
public class TwoWayAnovaWithoutInteractionsAnalyzer extends AbstractTwoWayAnovaAnalyzer {

    private Log log = LogFactory.getLog( this.getClass() );

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer#generateHistograms(java.lang.String,
     * java.util.ArrayList, int, int, int, double[])
     */
    @Override
    protected Collection<Histogram> generateHistograms( String histFileName, List<ExperimentalFactor> effects,
            int numBins, int min, int max, Double[] pvalues ) {

        Collection<Histogram> hists = new HashSet<Histogram>();

        histFileName = StringUtils.removeEnd( histFileName, DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX );

        Iterator<ExperimentalFactor> iter = effects.iterator();
        String mainA = iter.next().getName();
        String mainB = iter.next().getName();

        String nameA = histFileName + "_" + mainA + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;
        String nameB = histFileName + "_" + mainB + DifferentialExpressionFileUtils.PVALUE_DIST_SUFFIX;

        Histogram histA = new Histogram( nameA, numBins, min, max );
        Histogram histB = new Histogram( nameB, numBins, min, max );

        for ( int i = 0; i < pvalues.length; i++ ) {
            if ( i % ( maxResults - 1 ) == mainEffectAIndex ) histA.fill( pvalues[i] );

            if ( i % ( maxResults - 1 ) == mainEffectBIndex ) histB.fill( pvalues[i] );

        }

        hists.add( histA );
        hists.add( histB );

        return hists;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.analysis.diff.AbstractTwoWayAnovaAnalyzer#twoWayAnova(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, ubic.gemma.model.expression.experiment.ExperimentalFactor,
     * ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    protected DifferentialExpressionAnalysis twoWayAnova( ExpressionExperiment expressionExperiment,
            ExperimentalFactor experimentalFactorA, ExperimentalFactor experimentalFactorB ) {

        connectToR();

        Collection<FactorValue> factorValuesA = experimentalFactorA.getFactorValues();
        Collection<FactorValue> factorValuesB = experimentalFactorB.getFactorValues();

        if ( factorValuesA.size() < 2 || factorValuesB.size() < 2 ) {
            throw new RuntimeException(
                    "Two way anova requires 2 or more levels (factor values) per experimental factor" );
        }

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService
                .getProcessedExpressionDataMatrix( expressionExperiment );

        QuantitationType quantitationType = dmatrix.getQuantitationTypes().iterator().next();

        List<BioMaterial> samplesUsed = DifferentialExpressionAnalysisHelperService
                .getBioMaterialsForBioAssays( dmatrix );

        DoubleMatrix<DesignElement, Integer> namedMatrix = dmatrix.getMatrix();

        List<String> rFactorsA = DifferentialExpressionAnalysisHelperService.getRFactorsFromFactorValuesForTwoWayAnova(
                experimentalFactorA, samplesUsed );
        List<String> rFactorsB = DifferentialExpressionAnalysisHelperService.getRFactorsFromFactorValuesForTwoWayAnova(
                experimentalFactorB, samplesUsed );

        String factorA = rc.assignFactor( rFactorsA );
        String factorB = rc.assignFactor( rFactorsB );

        Transformer rowNameExtractor = TransformerUtils.invokerTransformer( "getId" );
        String matrixName = rc.assignMatrix( namedMatrix, rowNameExtractor );

        String modelDeclaration = "x ~ " + factorA + "+" + factorB;

        StringBuffer command = new StringBuffer();
        command.append( "apply(" );
        command.append( matrixName );
        command.append( ", 1, function(x) { try(anova(aov(" + modelDeclaration + ")), silent=T )})" );

        log.info( "Starting ANOVA ...  " );

        if ( log.isDebugEnabled() ) {
            log.debug( "factorA<-factor(c(" + StringUtils.join( rFactorsA, "," ) + "))" );
            log.debug( "factorB<-factor(c(" + StringUtils.join( rFactorsB, "," ) + "))" );
            log.debug( command );
        }

        Map<String, TwoWayAnovaResult> anovaResult = rc.twoWayAnovaEvalWithLogging( command.toString(), false );

        if ( anovaResult == null ) throw new IllegalStateException( "No pvalues returned" );

        Double[] mainEffectAPvalues = new Double[namedMatrix.rows()];
        Double[] mainEffectBPvalues = new Double[namedMatrix.rows()];

        int i = 0;
        List<Double> d = new ArrayList<Double>();
        for ( DesignElement el : namedMatrix.getRowNames() ) {

            TwoWayAnovaResult twoWayAnovaResult = anovaResult.get( rowNameExtractor.transform( el ).toString() );

            assert twoWayAnovaResult != null;

            if ( log.isDebugEnabled() ) {
                log.debug( StringUtils.join( ArrayUtils.toObject( namedMatrix.getRowByName( el ) ), "," ) );
                log.debug( "\n" + el + "\n" + twoWayAnovaResult + "\n\n" );
            }

            mainEffectAPvalues[i] = twoWayAnovaResult.getMainEffectAPval();
            mainEffectBPvalues[i] = twoWayAnovaResult.getMainEffectBPval();

            d.add( mainEffectAPvalues[i] );
            d.add( mainEffectBPvalues[i] );

            i++;
        }

        /* write out histogram */
        ArrayList<ExperimentalFactor> effects = new ArrayList<ExperimentalFactor>();
        effects.add( experimentalFactorA );
        effects.add( experimentalFactorB );
        writePValuesHistogram( d.toArray( new Double[] {} ), expressionExperiment, effects );

        disconnectR();
        log.info( "ANOVA done" );
        return createExpressionAnalysis( dmatrix, mainEffectAPvalues, mainEffectBPvalues, null, anovaResult,
                experimentalFactorA, experimentalFactorB, quantitationType, expressionExperiment );
    }
}
