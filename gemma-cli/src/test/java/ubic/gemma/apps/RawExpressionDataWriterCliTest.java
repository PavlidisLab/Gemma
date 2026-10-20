package ubic.gemma.apps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * getRawDataMatrix -stdout: every result reaches the standard output, and a write that fails is an error.
 * <p>
 * Each test gets a fresh CLI bean: the options it has parsed stay in fields that nothing clears.
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class RawExpressionDataWriterCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public RawExpressionDataWriterCli rawExpressionDataWriterCli() {
            return new RawExpressionDataWriterCli();
        }

        @Bean
        public ExpressionDataFileService expressionDataFileService() {
            return mock();
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock();
        }

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
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
    private RawExpressionDataWriterCli cli;
    @Autowired
    private ExpressionDataFileService expressionDataFileService;
    @Autowired
    private QuantitationTypeService quantitationTypeService;
    @Autowired
    private EntityLocator entityLocator;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() throws IOException {
        ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        when( entityLocator.locateExpressionExperiment( eq( "GSE1" ), anyBoolean() ) ).thenReturn( ee );
        doReturn( Collections.singleton( RawExpressionDataVector.class ) )
                .when( quantitationTypeService ).getMappedDataVectorType( RawExpressionDataVector.class );
        // each QT writes one line naming itself
        when( expressionDataFileService.writeRawExpressionData( eq( ee ), any( QuantitationType.class ), isNull(),
                anyBoolean(), anyBoolean(), anyBoolean(), any( Writer.class ), anyBoolean() ) )
                .thenAnswer( a -> {
                    a.getArgument( 6, Writer.class ).write( "vectors of " + a.getArgument( 1, QuantitationType.class ).getName() + "\n" );
                    return 1;
                } );
    }

    /**
     * 🛑 The second QT reaches the standard output. The writer over it was closed by try-with-resources,
     * which closed the standard output, so with {@code -allQts} every QT after the first went into a
     * closed PrintStream and was still reported as written.
     */
    @Test
    @WithMockUser
    public void testEveryQuantitationTypeReachesTheStandardOutput() {
        doReturn( Arrays.asList( qt( 1L, "qt1" ), qt( 2L, "qt2" ) ) )
                .when( quantitationTypeService ).findByExpressionExperiment( ee, RawExpressionDataVector.class );

        assertThat( cli ).withArguments( "-e", "GSE1", "-stdout", "-allQts" )
                .succeeds()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "vectors of qt1" )
                .contains( "vectors of qt2" );
    }

    /**
     * 🛑 A write to the standard output that fails (closed pipe, full disk) is an error. PrintStream
     * records it in a flag nothing read, so the run exited 0 having written nothing.
     */
    @Test
    @WithMockUser
    public void testAFailedWriteToTheStandardOutputFailsTheRun() {
        doReturn( Collections.singletonList( qt( 1L, "qt1" ) ) )
                .when( quantitationTypeService ).findByExpressionExperiment( ee, RawExpressionDataVector.class );
        OutputStream brokenPipe = new OutputStream() {
            @Override
            public void write( int b ) throws IOException {
                throw new IOException( "Broken pipe" );
            }
        };

        assertThat( cli ).withArguments( "-e", "GSE1", "-stdout", "-allQts" )
                .withOutputStream( brokenPipe )
                .fails();
    }

    private static QuantitationType qt( Long id, String name ) {
        QuantitationType qt = new QuantitationType();
        qt.setId( id );
        qt.setName( name );
        return qt;
    }
}
