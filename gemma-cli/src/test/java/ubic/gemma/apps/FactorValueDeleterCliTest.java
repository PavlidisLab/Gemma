package ubic.gemma.apps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import ubic.gemma.persistence.service.expression.experiment.FactorValueDeletion;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * {@code deleteFactorValues} only reports unless {@code -commit} is given, and selects factor values either by ID or by
 * factor and value, never both.
 */
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class FactorValueDeleterCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class FactorValueDeleterCliTestContextConfiguration {

        @Bean
        @Scope("prototype")
        public FactorValueDeleterCli factorValueDeleterCli() {
            return new FactorValueDeleterCli();
        }

        @Bean
        public FactorValueDeletion factorValueDeletion() {
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
    private FactorValueDeleterCli cli;

    @Autowired
    private FactorValueDeletion factorValueDeletion;

    @Autowired
    private EntityLocator entityLocator;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() {
        ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE1" );
        when( entityLocator.locateExpressionExperiment( eq( "GSE1" ), anyBoolean() ) ).thenReturn( ee );
        when( factorValueDeletion.describeDeletion( any(), any() ) ).thenReturn( "Factor value 10 of age: 61\n" );
    }

    @Test
    public void testWithoutCommitNothingIsDeleted() {
        assertThat( cli )
                .withArguments( "-e", "GSE1", "-fv", "10,11" )
                .succeeds();
        verify( factorValueDeletion ).describeDeletion( ee, Arrays.asList( 10L, 11L ) );
        verify( factorValueDeletion, never() ).deleteFactorValues( any(), anyCollection() );
    }

    @Test
    public void testCommitDeletes() {
        assertThat( cli )
                .withArguments( "-e", "GSE1", "-fv", "10,11", "-commit" )
                .succeeds();
        verify( factorValueDeletion ).deleteFactorValues( ee, Arrays.asList( 10L, 11L ) );
    }

    @Test
    public void testByFactorAndValue() {
        when( factorValueDeletion.resolveFactorValueIds( ee, "age", Arrays.asList( "61", "63" ) ) ).thenReturn( Arrays.asList( 10L, 12L ) );
        assertThat( cli )
                .withArguments( "-e", "GSE1", "-factor", "age", "-values", "61,63", "-commit" )
                .succeeds();
        verify( factorValueDeletion ).deleteFactorValues( ee, Arrays.asList( 10L, 12L ) );
    }

    @Test
    public void testIdsAndValuesTogetherAreRefused() {
        assertThat( cli )
                .withArguments( "-e", "GSE1", "-fv", "10", "-factor", "age", "-values", "61" )
                .fails();
        verify( factorValueDeletion, never() ).deleteFactorValues( any(), anyCollection() );
    }

    @Test
    public void testValuesWithoutFactorAreRefused() {
        assertThat( cli )
                .withArguments( "-e", "GSE1", "-values", "61" )
                .fails();
        verify( factorValueDeletion, never() ).resolveFactorValueIds( any(), any(), eq( Collections.singletonList( "61" ) ) );
    }
}
