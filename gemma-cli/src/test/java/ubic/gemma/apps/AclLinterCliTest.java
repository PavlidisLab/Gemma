package ubic.gemma.apps;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.security.authentication.ManualAuthenticationService;
import ubic.gemma.core.security.authorization.acl.AclLinterConfig;
import ubic.gemma.core.security.authorization.acl.AclLinterService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class AclLinterCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    public static class AclLinterCliTestContextConfiguration {
        @Bean
        public AclLinterCli aclLinterCli() {
            return new AclLinterCli();
        }

        @Bean
        public AclLinterService aclLinterService() {
            return mock( AclLinterService.class );
        }

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
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
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }
    }

    @Autowired
    private AclLinterCli aclLinterCli;

    @Autowired
    private AclLinterService aclLinterService;

    @AfterEach
    public void tearDown() {
        reset( aclLinterService );
    }

    @Test
    @WithMockUser
    public void multipleIdentifiers_lintEachOne() {
        when( aclLinterService.lintAcls( any(), anyLong(), any( AclLinterConfig.class ) ) )
                .thenReturn( Collections.emptyList() );
        assertThat( aclLinterCli )
                .withArguments( "--type", "DATASET", "--identifier", "93287,93288,93289" )
                .succeeds();
        verify( aclLinterService ).lintAcls( eq( ExpressionExperiment.class ), eq( 93287L ), any( AclLinterConfig.class ) );
        verify( aclLinterService ).lintAcls( eq( ExpressionExperiment.class ), eq( 93288L ), any( AclLinterConfig.class ) );
        verify( aclLinterService ).lintAcls( eq( ExpressionExperiment.class ), eq( 93289L ), any( AclLinterConfig.class ) );
        // never the whole-class or whole-DB sweeps when specific ids are named
        verify( aclLinterService, never() ).lintAcls( any( Class.class ), any( AclLinterConfig.class ) );
        verify( aclLinterService, never() ).lintAcls( any( AclLinterConfig.class ) );
    }

    @Test
    @WithMockUser
    public void singleIdentifier_stillWorks() {
        when( aclLinterService.lintAcls( any(), anyLong(), any( AclLinterConfig.class ) ) )
                .thenReturn( Collections.emptyList() );
        assertThat( aclLinterCli )
                .withArguments( "--type", "DATASET", "--identifier", "93288" )
                .succeeds();
        verify( aclLinterService ).lintAcls( eq( ExpressionExperiment.class ), eq( 93288L ), any( AclLinterConfig.class ) );
    }
}
