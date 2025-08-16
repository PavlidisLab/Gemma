package ubic.gemma.apps;

import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest;
import ubic.gemma.core.analysis.expression.diff.AnalysisType;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@WithMockUser("bob")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class DifferentialExpressionAnalysisCliTest extends BaseTest {

    @Autowired
    private GemmaRestApiClient gemmaRestApiClient;

    @Configuration
    @TestComponent
    static class DifferentialExpressionAnalysisCliTestConfiguration extends BaseCliTest {

        @Bean
        @Scope("prototype")
        public DifferentialExpressionAnalysisCli differentialExpressionAnalysisCli() {
            return new DifferentialExpressionAnalysisCli();
        }

        @Bean
        public DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService() {
            return mock();
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock();
        }

        @Bean
        public ExpressionDataFileService expressionDataFileService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService eeService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentSetService expressionExperimentSetService() {
            return mock();
        }

        @Bean
        public SearchService searchService() {
            return mock();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }

        @Bean
        public EntityUrlBuilder entityUrlBuilder() {
            return new EntityUrlBuilder( "http://localhost:8080" );
        }

        @Bean
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }
    }

    @Autowired
    private DifferentialExpressionAnalysisCli differentialExpressionAnalysisCli;

    @Autowired
    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private EntityLocator entityLocator;

    private ExpressionExperiment ee;

    private ExperimentalFactor a, b, c, d, e;

    @Before
    public void setUp() throws IOException {
        ee = new ExpressionExperiment();
        ee.setId( 1L );
        a = ExperimentalFactor.Factory.newInstance( "genotype", FactorType.CATEGORICAL );
        a.setId( 1L );
        b = ExperimentalFactor.Factory.newInstance( "treatment", FactorType.CATEGORICAL );
        b.setId( 2L );
        c = ExperimentalFactor.Factory.newInstance( "batch", FactorType.CATEGORICAL );
        c.setId( 3L );
        d = ExperimentalFactor.Factory.newInstance( "cell type", FactorType.CATEGORICAL );
        d.setId( 4L );
        e = ExperimentalFactor.Factory.newInstance( "age", FactorType.CONTINUOUS );
        e.setId( 5L );
        when( entityLocator.locateExpressionExperiment( eq( "1" ), anyBoolean() ) ).thenReturn( ee );
        when( eeService.thawLite( ee ) ).thenReturn( ee );
        when( gemmaRestApiClient.perform( eq( "/datasets/1/refresh" ), eq( "refreshVectors" ), anyBoolean(),
                eq( "refreshReports" ), anyBoolean() ) )
                .thenReturn( ( GemmaRestApiClient.DataResponse ) () -> null );
    }

    @Test
    public void testAnalysisWithAutomaticallySelectedFactors() throws IOException {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ) )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).runDifferentialExpressionAnalyses( eq( ee ), assertArg( config -> {
            assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( a, b );
            assertThat( config.getInteractionsToInclude() ).containsExactlyInAnyOrder( Sets.set( a, b ) );
            assertThat( config.getSubsetFactor() ).isNull();
        } ) );
        verify( gemmaRestApiClient ).perform( "/datasets/1/refresh",
                "refreshVectors", false,
                "refreshReports", true );
    }

    @Test
    public void testAnalysisWithAutomaticallySelectedFactorsWhenBatchFactorIsPresent() throws IOException {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( c );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ) )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).runDifferentialExpressionAnalyses( eq( ee ), assertArg( config -> {
            assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( a, b );
            assertThat( config.getInteractionsToInclude() ).containsExactlyInAnyOrder( Sets.set( a, b ) );
            assertThat( config.getSubsetFactor() ).isNull();
        } ) );
        verify( gemmaRestApiClient ).perform( "/datasets/1/refresh",
                "refreshVectors", false,
                "refreshReports", true );
    }

    @Test
    public void testAnalysisWithAutomaticallySelectedFactorsWithBatchFactorIsPresentAndIncluded() throws IOException {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( c );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-usebatch" )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).runDifferentialExpressionAnalyses( eq( ee ), assertArg( config -> {
            assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( a, c );
            assertThat( config.getInteractionsToInclude() ).containsExactlyInAnyOrder( Sets.set( a, c ) );
            assertThat( config.getSubsetFactor() ).isNull();
        } ) );
        verify( gemmaRestApiClient ).perform( "/datasets/1/refresh",
                "refreshVectors", false,
                "refreshReports", true );
    }

    @Test
    public void testSubSetAnalysisWithAutomaticallySelectedFactorsWhenContinuousFactorIsPresent() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( e );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ) )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).runDifferentialExpressionAnalyses( eq( ee ), assertArg( config -> {
            assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( a, e );
            assertThat( config.getInteractionsToInclude() ).isEmpty();
            assertThat( config.getSubsetFactor() ).isNull();
        } ) );
    }

    @Test
    public void testAnalysisWithManuallySelectedFactors() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( c );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-factors", "genotype,treatment" )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).runDifferentialExpressionAnalyses( eq( ee ), assertArg( config -> {
            assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( a, b );
            assertThat( config.getInteractionsToInclude() ).containsExactlyInAnyOrder( Sets.set( a, b ) );
            assertThat( config.getSubsetFactor() ).isNull();
        } ) );
    }

    @Test
    public void testAnalysisWithManuallySelectedFactorsWhenFactorsAreRepeated() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( c );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-factors", "genotype,treatment,treatment" )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).runDifferentialExpressionAnalyses( eq( ee ), assertArg( config -> {
            assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( a, b );
            assertThat( config.getInteractionsToInclude() ).containsExactlyInAnyOrder( Sets.set( a, b ) );
            assertThat( config.getSubsetFactor() ).isNull();
        } ) );
    }

    @Test
    public void testAnalysisWithManuallySelectedFactorsWithNoInteraction() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( c );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-type", AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION.name(), "-factors", "genotype,treatment" )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).runDifferentialExpressionAnalyses( eq( ee ), assertArg( config -> {
            assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( a, b );
            assertThat( config.getInteractionsToInclude() ).isEmpty();
            assertThat( config.getSubsetFactor() ).isNull();
        } ) );
    }

    @Test
    public void testAnalysisWithManuallySelectedFactorsWhenFactorDoesNotExist() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-factors", "genotype,batch" )
                .fails()
                .exitCause()
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "No factor for name batch. Possible values are:\n" +
                        "\tgenotype:\tExperimentalFactor Id=1 Name=genotype Type=CATEGORICAL\n" +
                        "\ttreatment:\tExperimentalFactor Id=2 Name=treatment Type=CATEGORICAL" );
    }


    @Test
    public void testAnalysisWithManuallySelectedInteraction() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( c );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-factors", "genotype,treatment,genotype:treatment" )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).runDifferentialExpressionAnalyses( eq( ee ), assertArg( config -> {
            assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( a, b );
            assertThat( config.getInteractionsToInclude() ).containsExactlyInAnyOrder( Sets.set( a, b ) );
            assertThat( config.getSubsetFactor() ).isNull();
        } ) );
    }

    @Test
    public void testAnalysisWithManuallySelectedInteractionWhenInteractionIsRepeated() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( c );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-factors", "genotype,treatment,genotype:treatment,treatment:genotype" )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).runDifferentialExpressionAnalyses( eq( ee ), assertArg( config -> {
            assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( a, b );
            assertThat( config.getInteractionsToInclude() ).containsExactlyInAnyOrder( Sets.set( a, b ) );
            assertThat( config.getSubsetFactor() ).isNull();
        } ) );
    }
    @Test
    public void testAnalysisWithManuallySelectedInteractionOfContinuousFactor() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( e );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-factors", "genotype:age" )
                .fails()
                .exitCause()
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "Interactions can only be specified for categorical factors. Factor age in interaction genotype:age is continuous." );
    }

    @Test
    public void testSubSetAnalysisWithManuallySelectedFactors() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( d );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-subset", "cell_type", "-factors", "genotype,treatment" )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).runDifferentialExpressionAnalyses( eq( ee ), assertArg( config -> {
            assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( a, b );
            assertThat( config.getInteractionsToInclude() ).containsExactlyInAnyOrder( Sets.set( a, b ) );
            assertThat( config.getSubsetFactor() ).isEqualTo( d );
        } ) );
    }

    @Test
    public void testSubSetAnalysisWithManuallySelectedFactorsWhenSubSetFactorIsSelected() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( d );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-subset", "cell_type", "-factors", "genotype,treatment,cell_type" )
                .fails()
                .exitCause()
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessage( "A subset factor cannot be included as a factor to analyze." );
    }

    @Test
    public void testSubSetAnalysisWithManuallySelectedFactorsAndNoInteraction() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( d );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-type", AnalysisType.TWO_WAY_ANOVA_NO_INTERACTION.name(), "-subset", "cell_type", "-factors", "genotype,treatment" )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).runDifferentialExpressionAnalyses( eq( ee ), assertArg( config -> {
            assertThat( config.getFactorsToInclude() ).containsExactlyInAnyOrder( a, b );
            assertThat( config.getInteractionsToInclude() ).isEmpty();
            assertThat( config.getSubsetFactor() ).isEqualTo( d );
        } ) );
    }

    @Test
    public void testRedoAnalysis() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( d );
        ee.setExperimentalDesign( ed );

        Collection<DifferentialExpressionAnalysis> deas = new HashSet<>();
        deas.add( new DifferentialExpressionAnalysis() );
        when( differentialExpressionAnalysisService.findByExperiment( ee, true ) ).thenReturn( deas );

        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-redo" )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).redoAnalyses( eq( ee ), eq( deas ), assertArg( config -> {
            assertThat( config.getAnalysisType() ).isNull();
            assertThat( config.getFactorsToInclude() ).isEmpty();
            assertThat( config.getInteractionsToInclude() ).isEmpty();
        } ), eq( false ) );
    }

    @Test
    public void testDeleteAnalysis() {
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "-delete" )
                .succeeds();
        verify( differentialExpressionAnalyzerService ).deleteAnalyses( ee );
    }

    @Test
    public void testCompleteSubSetFactors() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( c );
        ed.getExperimentalFactors().add( d );
        ed.getExperimentalFactors().add( e );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "--complete-subset-factors" )
                .succeeds().standardOutput().asString( StandardCharsets.UTF_8 )
                .hasLineCount( 8 )
                .contains( a.getId() + "\t" + a.toString() + "\n",
                        b.getId() + "\t" + b.toString() + "\n",
                        c.getId() + "\t" + c.toString() + "\n",
                        d.getId() + "\t" + d.toString() + "\n" )
                .doesNotContain( e.getId() + "\t" + e.toString() + "\n" ); // continuous factor (not suitable for subsetting)
        verifyNoInteractions( differentialExpressionAnalysisService );
    }

    @Test
    public void testCompleteFactors() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.getExperimentalFactors().add( a );
        ed.getExperimentalFactors().add( b );
        ed.getExperimentalFactors().add( c );
        ed.getExperimentalFactors().add( d );
        ed.getExperimentalFactors().add( e );
        ee.setExperimentalDesign( ed );
        assertThat( differentialExpressionAnalysisCli )
                .withArguments( "-e", String.valueOf( ee.getId() ), "--complete-factors" )
                .succeeds().standardOutput().asString( StandardCharsets.UTF_8 )
                .hasLineCount( 58 )
                .contains( a.getId() + "\t" + a.toString() + "\n",
                        b.getId() + "\t" + b.toString() + "\n",
                        c.getId() + "\t" + c.toString() + "\n",
                        d.getId() + "\t" + d.toString() + "\n",
                        e.getId() + "\t" + e.toString() + "\n",
                        a.getName() + ":" + b.getName() + "\t" + a.toString() + " x " + b.toString() + "\n" )
                .doesNotContain(
                        a.getName() + ":" + a.getName(),  // interaction with itself
                        a.getName() + ":" + e.getName() ); // interaction with a continuous factor
        verifyNoInteractions( differentialExpressionAnalysisService );
    }
}