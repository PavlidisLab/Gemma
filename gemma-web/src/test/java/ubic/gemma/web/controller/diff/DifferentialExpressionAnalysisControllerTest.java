package ubic.gemma.web.controller.diff;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.expression.diff.AnalysisSelectionAndExecutionService;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.job.executor.webapp.TaskRunningService;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.TestComponent;
import ubic.gemma.web.util.BaseWebTest;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ubic.gemma.web.util.dwr.MockDwrRequestBuilders.dwr;
import static ubic.gemma.web.util.dwr.MockDwrRequestBuilders.dwrStaticPage;
import static ubic.gemma.web.util.dwr.MockDwrResultMatchers.callback;
import static ubic.gemma.web.util.dwr.MockDwrResultMatchers.exception;

@ContextConfiguration
public class DifferentialExpressionAnalysisControllerTest extends BaseWebTest {

    @Configuration
    @TestComponent
    @ImportResource("classpath:ubic/gemma/web/controller/diff/DifferentialExpressionAnalysisControllerTest-dwr.xml")
    static class DifferentialExpressionAnalysisControllerTestContextConfiguration extends BaseWebTestContextConfiguration {

        @Bean
        public TaskRunningService taskRunningService() {
            return mock();
        }

        @Bean
        public AnalysisSelectionAndExecutionService analysisSelectionAndExecutionService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentReportService experimentReportService() {
            return mock();
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private TaskRunningService taskRunningService;

    @After
    public void resetMocks() {
        reset( expressionExperimentService, taskRunningService );
    }

    @Test
    public void testIndex() throws Exception {
        mvc.perform( dwrStaticPage( "/index.html" ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentType( MediaType.TEXT_HTML ) );
    }

    @Test
    public void testDiffExAnalysisControllerTestPage() throws Exception {
        mvc.perform( dwrStaticPage( "/test/DifferentialExpressionAnalysisController" ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentType( MediaType.TEXT_HTML ) );
    }

    /**
     * This error is produced via {@link javax.servlet.http.HttpServletResponse#sendError(int)} and thus is not
     * intercepted by Spring's error handler. It will however be handled by the Web server via {@code error.jsp}.
     */
    @Test
    public void testUndefinedTestPage() throws Exception {
        mvc.perform( dwrStaticPage( "/test/bleh" ) )
                .andExpect( status().isNotImplemented() );
    }

    @Test
    public void testJsEngine() throws Exception {
        mvc.perform( dwrStaticPage( "/engine.js" ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentType( "text/javascript;charset=utf-8" ) );
    }

    @Test
    public void test() throws Exception {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        when( expressionExperimentService.loadAndThawLiteOrFail( eq( 1L ), any(), any() ) ).thenReturn( ee );
        when( taskRunningService.submitTaskCommand( any() ) ).thenReturn( "23" );
        mvc.perform( dwr( DifferentialExpressionAnalysisController.class, "run", 1L ) )
                .andExpect( callback().value( "23" ) );
        verify( taskRunningService ).submitTaskCommand( any() );
    }

    @Test
    public void testBatchCall() throws Exception {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        when( expressionExperimentService.loadAndThawLiteOrFail( eq( 1L ), any(), any() ) ).thenReturn( ee );
        mvc.perform( dwr( DifferentialExpressionAnalysisController.class, "run", 1L )
                        .batch( 1 ) )
                .andExpect( callback().batch( 0 ).doesNotExist() )
                .andExpect( callback().batch( 1 ).value( nullValue() ) );
        verify( taskRunningService ).submitTaskCommand( any() );
    }

    @Test
    public void testMultipleCalls() throws Exception {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        when( expressionExperimentService.loadAndThawLiteOrFail( eq( 1L ), any(), any() ) ).thenReturn( ee );
        when( expressionExperimentService.loadAndThawLiteOrFail( eq( 2L ), any(), any() ) ).thenReturn( ee );
        mvc.perform( dwr( DifferentialExpressionAnalysisController.class, "run", 1L )
                        .and( 2L ) )
                .andExpect( callback( 0 ).value( nullValue() ) )
                .andExpect( callback( 1 ).value( nullValue() ) )
                .andExpect( callback( 2 ).doesNotExist() )
                .andExpect( exception( 2 ).doesNotExist() );
        verify( taskRunningService, times( 2 ) ).submitTaskCommand( any() );
    }

    @Test
    public void testMissingEndpoint() throws Exception {
        mvc.perform( dwr( DifferentialExpressionAnalysisController.class, "run2", 1L ) )
                .andExpect( exception().javaClassName( "java.lang.Throwable" ) )
                .andExpect( exception().message( "Error" ) );
        verifyNoInteractions( taskRunningService );
    }
}