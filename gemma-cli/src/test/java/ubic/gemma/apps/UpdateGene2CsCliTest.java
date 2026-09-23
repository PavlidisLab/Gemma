package ubic.gemma.apps;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.security.authentication.ManualAuthenticationService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import java.util.Date;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class UpdateGene2CsCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class UpdateGene2CsCliTestContextConfiguration {

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        public UpdateGene2CsCli updateGene2CsCli() {
            return new UpdateGene2CsCli();
        }

        @Bean
        public ManualAuthenticationService manualAuthenticationService() {
            return mock();
        }

        @Bean
        public TableMaintenanceUtil tableMaintenanceUtil() {
            return mock();
        }

        @Bean
        public GemmaRestApiClient gemmaRestApiClient() {
            return mock();
        }

        @Bean
        public EntityLocator entityLocator() {
            return mock();
        }
    }

    @Autowired
    private ObjectProvider<UpdateGene2CsCli> cli;

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    private EntityLocator entityLocator;

    @AfterEach
    void tearDown() {
        reset( tableMaintenanceUtil, entityLocator );
    }

    /**
     * {@code -a} was never read, so a single-platform request ran the table-wide update instead.
     */
    @Test
    @WithMockUser
    void platformOptionUpdatesOnlyThatPlatform() {
        ArrayDesign platform = ArrayDesign.Factory.newInstance( "GPL570", null );
        when( entityLocator.locateArrayDesign( "GPL570" ) ).thenReturn( platform );

        assertThat( cli.getObject() )
                .withArguments( "-a", "GPL570" )
                .succeeds();

        verify( tableMaintenanceUtil ).updateGene2CsEntries( platform, false );
        verify( tableMaintenanceUtil, never() ).updateGene2CsEntries( nullable( Date.class ), anyBoolean(), anyBoolean() );
    }
}
