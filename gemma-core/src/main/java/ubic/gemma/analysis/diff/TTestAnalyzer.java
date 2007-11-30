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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rosuda.JRclient.REXP;

import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.analysis.ExpressionAnalysisResult;
import ubic.gemma.model.expression.analysis.ProbeAnalysisResult;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * A t-test implementation as described by P. Pavlidis, Methods 31 (2003) 282-289.
 * <p>
 * See http://www.bioinformatics.ubc.ca/pavlidis/lab/docs/reprints/anova-methods.pdf.
 * <p>
 * R calls:
 * <p>
 * apply(matrix, 1, function(x) {t.test(x ~ factor(t(facts)))$p.value})
 * <p>
 * apply(matrix, 1, function(x) {t.test(x ~ factor(t(facts)))$statistic})
 * <p>
 * NOTE: facts is first transposed and then factor is applied (as indicated in the equations above)
 * 
 * @spring.bean id="tTestAnalyzer"
 * @author keshav
 * @version $Id$
 */
public class TTestAnalyzer extends AbstractAnalyzer {

    private Log log = LogFactory.getLog( this.getClass() );

    public TTestAnalyzer() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.diff.AbstractAnalyzer#getExpressionAnalysis(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public ExpressionAnalysis getExpressionAnalysis( ExpressionExperiment expressionExperiment ) {

        Collection<ExperimentalFactor> experimentalFactors = expressionExperiment.getExperimentalDesign()
                .getExperimentalFactors();

        if ( experimentalFactors.size() != 1 )
            throw new RuntimeException( "T-test supports 1 experimental factor.  Received "
                    + experimentalFactors.size() + "." );

        ExperimentalFactor experimentalFactor = experimentalFactors.iterator().next();

        Collection<FactorValue> factorValues = experimentalFactor.getFactorValues();
        if ( factorValues.size() != 2 )
            throw new RuntimeException( "T-test supports 2 factor values.  Received " + factorValues.size() + "." );

        Iterator<FactorValue> iter = factorValues.iterator();

        FactorValue factorValueA = iter.next();

        FactorValue factorValueB = iter.next();

        return tTest( expressionExperiment, factorValueA, factorValueB );
    }

    /**
     * See class level javadoc for R Call.
     * 
     * @param expressionExperiment
     * @param factorValueA
     * @param factorValueB
     * @return
     */
    @SuppressWarnings("unchecked")
    protected ExpressionAnalysis tTest( ExpressionExperiment expressionExperiment, FactorValue factorValueA,
            FactorValue factorValueB ) {

        connectToR();

        Collection<DesignElementDataVector> vectorsToUse = analysisHelperService.getVectors( expressionExperiment );
        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectorsToUse );

        ExpressionDataDoubleMatrix dmatrix = builder.getMaskedPreferredData( null );

        Collection<BioMaterial> samplesUsed = AnalyzerHelper.getBioMaterialsForBioAssays( dmatrix );

        Collection<FactorValue> factorValues = new ArrayList<FactorValue>();
        factorValues.add( factorValueA );
        factorValues.add( factorValueB );

        List<String> rFactors = AnalyzerHelper.getRFactorsFromFactorValuesForOneWayAnova( factorValues, samplesUsed );

        DoubleMatrixNamed namedMatrix = dmatrix.getNamedMatrix();

        String facts = rc.assignStringList( rFactors );

        String tfacts = "t(" + facts + ")";

        String factor = "factor(" + tfacts + ")";

        String matrixName = rc.assignMatrix( namedMatrix );

        /* handle the p-values */
        StringBuffer pvalueCommand = new StringBuffer();
        pvalueCommand.append( "apply(" );
        pvalueCommand.append( matrixName );
        pvalueCommand.append( ", 1, function(x) {t.test(x ~ " + factor + ")$p.value}" );
        pvalueCommand.append( ")" );

        log.debug( pvalueCommand.toString() );

        REXP regExpPValues = rc.eval( pvalueCommand.toString() );

        double[] pvalues = ( double[] ) regExpPValues.getContent();

        /* handle the t-statistics */
        StringBuffer tstatisticCommand = new StringBuffer();
        tstatisticCommand.append( "apply(" );
        tstatisticCommand.append( matrixName );
        tstatisticCommand.append( ", 1, function(x) {t.test(x ~ " + factor + ")$statistic}" );
        tstatisticCommand.append( ")" );

        log.debug( tstatisticCommand.toString() );

        REXP regExpTStatistics = rc.eval( tstatisticCommand.toString() );

        double[] tstatistics = ( double[] ) regExpTStatistics.getContent();

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
            probeAnalysisResult.setPvalue( pvalues[i] );
            probeAnalysisResult.setScore( tstatistics[i] );
            // probeAnalysisResult.setQuantitationType( quantitationType );

            analysisResults.add( probeAnalysisResult );
        }

        expressionAnalysis.setAnalysisResults( analysisResults );

        return expressionAnalysis;
    }
}
