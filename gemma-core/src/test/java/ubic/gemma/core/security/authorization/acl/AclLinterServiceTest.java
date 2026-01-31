package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.ObjectIdentityRetrievalStrategyImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import static org.mockito.Mockito.mock;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class AclLinterServiceTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class AclLinterCliTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public AclLinterService aclLinterService() {
            return new AclLinterServiceImpl();
        }

        @Bean
        public ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy() {
            return new ObjectIdentityRetrievalStrategyImpl();
        }

        @Bean
        public ParentIdentityRetrievalStrategy parentObjectRetrievalStrategy() {
            return new ParentIdentityRetrievalStrategyImpl();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }
    }

    @Autowired
    private AclLinterService aclLinterService;

    @Test
    @WithMockUser(authorities = { "GROUP_ADMIN" })
    public void test() {
        AclLinterConfig config = AclLinterConfig.builder()
                .lintDanglingIdentities( true )
                .lintSecurablesLackingIdentities( true )
                .lintChildWithoutParent( true )
                .lintChildWithIncorrectParent( true )
                .lintNotChildWithParent( true )
                .lintPermissions( true )
                .applyFixes( false )
                .build();
        aclLinterService.lintAcls( config );
        aclLinterService.lintAcls( ExpressionExperiment.class, config );
        aclLinterService.lintAcls( ExpressionExperiment.class, 1L, config );

        aclLinterService.lintAcls( ExpressionAnalysisResultSet.class, config );
        aclLinterService.lintAcls( ExpressionAnalysisResultSet.class, 1L, config );

        aclLinterService.lintAcls( DifferentialExpressionAnalysis.class, config );
        aclLinterService.lintAcls( DifferentialExpressionAnalysis.class, 1L, config );

        aclLinterService.lintAcls( BioAssay.class, config );
        aclLinterService.lintAcls( BioAssay.class, 1L, config );

        aclLinterService.lintAcls( BioMaterial.class, config );
        aclLinterService.lintAcls( BioMaterial.class, 1L, config );

        config = AclLinterConfig.builder()
                .lintDanglingIdentities( true )
                .lintSecurablesLackingIdentities( true )
                .lintChildWithoutParent( true )
                .lintChildWithIncorrectParent( true )
                .lintNotChildWithParent( true )
                .lintPermissions( true )
                .applyFixes( true )
                .build();
        aclLinterService.lintAcls( config );
        aclLinterService.lintAcls( ExpressionExperiment.class, config );
        aclLinterService.lintAcls( ExpressionExperiment.class, 1L, config );
    }
}