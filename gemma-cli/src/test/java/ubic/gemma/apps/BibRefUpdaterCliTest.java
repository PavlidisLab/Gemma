package ubic.gemma.apps;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.authentication.CLIAuthenticationManager;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * updatePubMeds: a refresh that fails is reported as an error.
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class BibRefUpdaterCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public BibRefUpdaterCli bibRefUpdaterCli() {
            return new BibRefUpdaterCli();
        }

        @Bean
        public BibliographicReferenceService bibliographicReferenceService() {
            return mock();
        }

        @Bean
        public CLIAuthenticationManager cliAuthenticationManager() {
            return mock();
        }
    }

    @Autowired
    private BibRefUpdaterCli cli;
    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    /**
     * 🛑 Each failed refresh was logged at INFO with no error object, so a run in which every refresh
     * failed (a rejected NCBI key, for one) exited 0.
     */
    @Test
    @WithMockUser
    public void testAFailedRefreshFailsTheRun() {
        BibliographicReference ref = new BibliographicReference();
        ref.setId( 1L );
        DatabaseEntry acc = new DatabaseEntry();
        acc.setAccession( "123" );
        ref.setPubAccession( acc );
        when( bibliographicReferenceService.findByExternalId( "123" ) ).thenReturn( ref );
        when( bibliographicReferenceService.load( 1L ) ).thenReturn( ref );
        when( bibliographicReferenceService.thaw( ref ) ).thenReturn( ref );
        when( bibliographicReferenceService.refresh( "123" ) ).thenThrow( new RuntimeException( "API key rejected" ) );

        assertThat( cli ).withArguments( "-pmids", "123" )
                .fails()
                .standardOutput()
                .asString( StandardCharsets.UTF_8 )
                .contains( "Failed to refresh" );
    }
}
