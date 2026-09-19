package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ExpressionExperimentManipulatingCLITest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public TestSingleExperimentCli testSingleExperimentCli() {
            return new TestSingleExperimentCli();
        }

        @Bean
        public TestSingleExperimentCliWithFileOption testSingleExperimentCliWithFileOption() {
            return new TestSingleExperimentCliWithFileOption();
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

    static class TestSingleExperimentCli extends ExpressionExperimentManipulatingCLI {

        public TestSingleExperimentCli() {
            setSingleExperimentMode();
        }

        @Override
        protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
            getCliContext().getOutputStream().print( expressionExperiment );
        }
    }

    /**
     * A single-experiment CLI with its own {@code -f}, like importDesign's design file.
     */
    static class TestSingleExperimentCliWithFileOption extends ExpressionExperimentManipulatingCLI {

        private Path file;

        public TestSingleExperimentCliWithFileOption() {
            setSingleExperimentMode();
        }

        @Override
        protected void buildExperimentOptions( Options options ) {
            options.addOption( Option.builder( "f" ).longOpt( "designFile" ).hasArg().type( Path.class ).required().get() );
        }

        @Override
        protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
            file = commandLine.getParsedOptionValue( 'f' );
        }

        @Override
        protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
            getCliContext().getOutputStream().print( expressionExperiment + " " + file.getFileName() );
        }
    }

    @Autowired
    private TestSingleExperimentCli testSingleExperimentCli;

    @Autowired
    private TestSingleExperimentCliWithFileOption testSingleExperimentCliWithFileOption;

    @Autowired
    private EntityLocator entityLocator;

    @Test
    @WithMockUser
    public void testSingleExperimentMode() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        when( entityLocator.locateExpressionExperiment( eq( "test" ), anyBoolean() ) ).thenReturn( ee );
        assertThat( testSingleExperimentCli )
                .withArguments( "-e", "test" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .isEqualTo( ee.toString() );
        // -all does not work in single experiment mode
        assertThat( testSingleExperimentCli )
                .withArguments( "-all" )
                .fails()
                .standardError()
                .asString( StandardCharsets.UTF_8 )
                .startsWith( "Unrecognized option: -all" );
    }

    /**
     * In single-experiment mode the base class defines no {@code -f}, so a subclass's {@code -f} is the subclass's.
     * It used to be read as a list of datasets as well: importDesign looked up its design file's header line as a
     * dataset and failed.
     */
    @Test
    @WithMockUser
    public void testSingleExperimentModeLeavesSubclassFileOptionAlone( @TempDir Path tempDir ) throws IOException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        when( entityLocator.locateExpressionExperiment( eq( "test" ), anyBoolean() ) ).thenReturn( ee );
        Path designFile = tempDir.resolve( "design.txt" );
        Files.write( designFile, Collections.singletonList( "Bioassay\tgenotype" ), StandardCharsets.UTF_8 );
        assertThat( testSingleExperimentCliWithFileOption )
                .withArguments( "-e", "test", "-f", designFile.toString() )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .isEqualTo( ee + " design.txt" );
    }

    @Test
    public void testGetExperimentDataFileResult() {
        ExpressionExperimentManipulatingCLI cli = new ExpressionExperimentManipulatingCLI() {
            @Override
            protected void buildExperimentOptions( Options options ) {
                addDataFileOptions( options, "test data", true );
            }
        };
        assertThat( cli )
                .withArguments( "--help" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "-standardLocation,--standard-location" )
                .contains( "-stdout,--stdout" )
                .contains( "-o,--output-file" )
                .contains( "-d,--output-dir" );
        assertThat( cli ).withArguments( "--" );
    }
}