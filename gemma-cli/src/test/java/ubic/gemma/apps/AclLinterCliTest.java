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
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    /**
     * Naming one check must run only that one. The parent-link repair has to be able to run with a
     * read-only outer transaction: lintAcls is @Transactional, and running the identity-creating
     * checks in the same call leaves the outer transaction holding row locks that the repair's
     * nested REQUIRES_NEW batches then wait on forever — "Lock wait timeout exceeded" on a
     * production run of 631,709 BioAssay identities, 2026-08-30.
     */
    @Test
    @WithMockUser
    public void namingOneCheck_runsOnlyThatCheck() {
        when( aclLinterService.lintAcls( any( Class.class ), any( AclLinterConfig.class ) ) )
                .thenReturn( Collections.emptyList() );
        assertThat( aclLinterCli )
                .withArguments( "--type", "ASSAY", "--lint-child-without-parent", "--apply-fixes" )
                .succeeds();
        ArgumentCaptor<AclLinterConfig> captor = ArgumentCaptor.forClass( AclLinterConfig.class );
        verify( aclLinterService ).lintAcls( any( Class.class ), captor.capture() );
        AclLinterConfig config = captor.getValue();
        assertTrue( config.isLintChildWithoutParent() );
        assertTrue( config.isApplyFixes() );
        assertFalse( config.isLintDanglingIdentities(), "A dangling-identity sweep deletes ACL identities in the outer transaction." );
        assertFalse( config.isLintSecurablesLackingIdentities(), "A missing-identity sweep creates ACL identities in the outer transaction." );
        assertFalse( config.isLintChildWithIncorrectParent() );
        assertFalse( config.isLintNotChildWithParent() );
    }

    /**
     * Naming no check keeps the previous behaviour: all five run.
     */
    @Test
    @WithMockUser
    public void namingNoCheck_runsThemAll() {
        when( aclLinterService.lintAcls( any( Class.class ), any( AclLinterConfig.class ) ) )
                .thenReturn( Collections.emptyList() );
        assertThat( aclLinterCli )
                .withArguments( "--type", "ASSAY" )
                .succeeds();
        ArgumentCaptor<AclLinterConfig> captor = ArgumentCaptor.forClass( AclLinterConfig.class );
        verify( aclLinterService ).lintAcls( any( Class.class ), captor.capture() );
        AclLinterConfig config = captor.getValue();
        assertTrue( config.isLintDanglingIdentities() );
        assertTrue( config.isLintSecurablesLackingIdentities() );
        assertTrue( config.isLintChildWithoutParent() );
        assertTrue( config.isLintChildWithIncorrectParent() );
        assertTrue( config.isLintNotChildWithParent() );
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
