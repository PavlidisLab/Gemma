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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

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
                .lintNotChildWithParent( true )
                .lintPermissions( true )
                .applyFixes( false )
                .build();
        aclLinterService.lintAcls( config );
        aclLinterService.lintAcls( ExpressionExperiment.class, config );
        aclLinterService.lintAcls( ExpressionExperiment.class, 1L, config );

        config = AclLinterConfig.builder()
                .lintDanglingIdentities( true )
                .lintSecurablesLackingIdentities( true )
                .lintChildWithoutParent( true )
                .lintNotChildWithParent( true )
                .lintPermissions( true )
                .applyFixes( true )
                .build();
        aclLinterService.lintAcls( config );
        aclLinterService.lintAcls( ExpressionExperiment.class, config );
        aclLinterService.lintAcls( ExpressionExperiment.class, 1L, config );
    }
}