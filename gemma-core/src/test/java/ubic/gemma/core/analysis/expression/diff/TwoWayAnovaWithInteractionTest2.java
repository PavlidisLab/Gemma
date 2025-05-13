/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.expression.diff;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimplePlatformMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimpleQuantitationTypeMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimpleTaxonMetadata;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.Collection;

import static org.junit.Assert.*;
import static ubic.gemma.core.analysis.expression.diff.DiffExAnalyzerUtils.determineAnalysisType;

/**
 * Test based on GSE8441
 *
 * @author paul
 */
public class TwoWayAnovaWithInteractionTest2 extends BaseSpringContextTest {

    @Autowired
    private SimpleExpressionDataLoaderService dataLoaderService;

    @Autowired
    private ExperimentalDesignImporter designImporter;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DiffExAnalyzer analyzer;

    @Autowired
    private AnalysisSelectionAndExecutionService analysisService = null;

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    private ExpressionExperiment ee;

    @Before
    public void setUp() throws Exception {
        SimpleExpressionExperimentMetadata metaData = new SimpleExpressionExperimentMetadata();
        metaData.setShortName( RandomStringUtils.randomAlphabetic( 10 ) );
        metaData.setTaxon( SimpleTaxonMetadata.forName( "mouse" ) );
        SimpleQuantitationTypeMetadata qtMetadata = new SimpleQuantitationTypeMetadata();
        qtMetadata.setName( "whatever" );
        // metaData.setScale( ScaleType.LOG2 ); // this is actually wrong!
        qtMetadata.setScale( ScaleType.LINEAR );
        metaData.setQuantitationType( qtMetadata );

        SimplePlatformMetadata f = new SimplePlatformMetadata();
        f.setShortName( "GSE8441_test" );
        f.setTechnologyType( TechnologyType.ONECOLOR );
        metaData.getArrayDesigns().add( f );

        DoubleMatrix<String, String> matrix;
        try ( InputStream io = this.getClass()
                .getResourceAsStream( "/data/analysis/expression/GSE8441_expmat_8probes.txt" ) ) {
            matrix = new DoubleMatrixReader().read( io );
        }

        ee = dataLoaderService.create( metaData, matrix );

        designImporter.importDesign( ee,
                this.getClass().getResourceAsStream( "/data/analysis/expression/606_GSE8441_expdesign.data.txt" ) );

        ee = expressionExperimentService.thaw( ee );
    }

    /*
     * NOTE I added a constant probe to this data after I set this up.
     *
     * <pre>
     * expMatFile &lt;- "GSE8441_expmat_8probes.txt"
     * expDesignFile &lt;- "606_GSE8441_expdesign.data.txt"
     * expMat &lt;- log2(read.table(expMatFile, header = TRUE, row.names = 1, sep = "\t", quote=""))
     * expDesign &lt;- read.table(expDesignFile, header = TRUE, row.names = 1, sep = "\t", quote="")
     *
     * expData &lt;- expMat[rownames(expDesign)]
     *
     * names(expData) == row.names(expDesign)
     * attach(expDesign)
     * lf&lt;-lm(unlist(expData["217757_at",])~Treatment*Sex )
     * summary(lf)
     * anova(lf)
     *
     * summary(lm(unlist(expData["202851_at",])~Treatment*Sex ))
     * anova(lm(unlist(expData["202851_at",])~Treatment*Sex ))
     *
     * # etc.
     * </pre>
     */
    @Test
    @Ignore
    @Category(SlowTest.class)
    public void test() {

        AnalysisType aa = determineAnalysisType( ee, ee.getExperimentalDesign().getExperimentalFactors(), null, true );

        assertEquals( AnalysisType.TWO_WAY_ANOVA_WITH_INTERACTION, aa );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        assertEquals( 2, factors.size() );
        config.setAnalysisType( aa );
        config.addFactorsToInclude( factors );
        config.addInteractionToInclude( factors );

        ExpressionDataDoubleMatrix dmatrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee, true );
        Collection<DifferentialExpressionAnalysis> result = analyzer.run( ee, dmatrix, config );
        assertEquals( 1, result.size() );

        DifferentialExpressionAnalysis analysis = result.iterator().next();

        this.checkResults( analysis );

        Collection<DifferentialExpressionAnalysis> persistent = differentialExpressionAnalyzerService
                .runDifferentialExpressionAnalyses( ee, config );

        DifferentialExpressionAnalysis refetched = differentialExpressionAnalysisService
                .load( persistent.iterator().next().getId() );
        assertNotNull( refetched );

        refetched = differentialExpressionAnalysisService.thaw( refetched );
        for ( ExpressionAnalysisResultSet ears : refetched.getResultSets() ) {
            expressionAnalysisResultSetService.thaw( ears );
        }

        this.checkResults( refetched );

        differentialExpressionAnalyzerService.redoAnalysis( ee, refetched, true );

    }

    public void checkResults( DifferentialExpressionAnalysis analysis ) {
        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        assertEquals( 3, resultSets.size() );

        boolean found1 = false, found2 = false, found3 = false, found4 = false;

        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            boolean interaction = false;
            boolean sexFactor = false;
            Collection<DifferentialExpressionAnalysisResult> results = rs.getResults();

            if ( rs.getExperimentalFactors().size() == 1 ) {
                ExperimentalFactor factor = rs.getExperimentalFactors().iterator().next();
                sexFactor = factor.getName().equals( "Sex" );
            } else {
                interaction = true;
            }

            assertEquals( 8, results.size() );

            /*
             * Test values here are computed in R, using anova(lm(unlist(expData["205969_at",])~Treatment*Sex )) etc.
             */
            for ( DifferentialExpressionAnalysisResult r : results ) {
                CompositeSequence probe = r.getProbe();
                Double pvalue = r.getPvalue();
                switch ( probe.getName() ) {
                    case "205969_at":
                        if ( sexFactor ) {
                            found1 = true;
                            assertEquals( 0.3333, pvalue, 0.001 );
                        } else if ( interaction ) {
                            found2 = true;
                            assertEquals( 0.8480, pvalue, 0.001 );
                        } else {
                            found3 = true;
                            assertEquals( 0.1323, pvalue, 0.001 );
                        }
                        break;
                    case "217757_at":
                        if ( interaction ) {
                            found4 = true;
                            assertEquals( 0.7621, pvalue, 0.001 );
                        }
                        break;
                    case "constant":
                        fail( "Should not have found a result for constant probe" );
                        break;
                }
            }

        }

        assertTrue( found1 && found2 && found3 && found4 );
    }
}
