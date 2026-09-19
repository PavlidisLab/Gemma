package ubic.gemma.apps;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.cli.authentication.CLIAuthenticationManager;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.geo.service.GeoBrowser;
import ubic.gemma.core.loader.expression.geo.service.GeoRecordType;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.blacklist.BlacklistedPlatform;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseReadService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * blackList -pp: which platforms are screened, and what a batch GEO would not answer for reports.
 * <p>
 * Each test gets a fresh CLI bean: the options it has parsed stay in fields that nothing clears.
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BlacklistCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public BlacklistCli blacklistCli() {
            return new BlacklistCli();
        }

        @Bean
        public BlacklistedEntityService blacklistedEntityService() {
            return mock();
        }

        @Bean
        public ExternalDatabaseReadService externalDatabaseReadService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public CLIAuthenticationManager cliAuthenticationManager() {
            return mock();
        }
    }

    @Autowired
    private BlacklistCli cli;
    @Autowired
    private BlacklistedEntityService blacklistedEntityService;
    @Autowired
    private ExternalDatabaseReadService externalDatabaseReadService;

    private final GeoBrowser geoBrowser = mock();

    @BeforeEach
    public void setUp() {
        ExternalDatabase geo = new ExternalDatabase();
        geo.setName( ExternalDatabases.GEO );
        when( externalDatabaseReadService.findByName( ExternalDatabases.GEO ) ).thenReturn( geo );
        cli.setGeoBrowser( geoBrowser );
    }

    /**
     * 🛑 A batch GEO would not return after the retries is an error. It was logged at INFO and the run
     * exited 0 with that batch's experiments never checked.
     */
    @Test
    @WithMockUser
    public void testABatchThatCannotBeFetchedFailsTheRun() throws Exception {
        when( blacklistedEntityService.loadAll() ).thenReturn( Arrays.asList( platform( "GPL1" ) ) );
        when( geoBrowser.searchAndRetrieveGeoRecords( any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean() ) )
                .thenThrow( new IOException( "500 from NCBI" ) );

        assertThat( cli ).withArguments( "-pp" ).fails();
    }

    /**
     * 🛑 {@code -a} limits the screen to the platforms it names. The filter read
     * {@code platformsToScreen == null || !platformsToScreen.isEmpty() || contains}, so any {@code -a}
     * screened every blacklisted platform; and the value was split on whitespace, so the documented
     * comma-delimited form was one platform named "GPL1,GPL3".
     */
    @Test
    @WithMockUser
    @SuppressWarnings("unchecked")
    public void testPlatformOptionLimitsTheScreen() throws Exception {
        when( blacklistedEntityService.loadAll() ).thenReturn( Arrays.asList(
                platform( "GPL1" ), platform( "GPL2" ), platform( "GPL3" ) ) );

        assertThat( cli ).withArguments( "-pp", "-a", "GPL1,GPL3" ).succeeds();

        ArgumentCaptor<Collection<String>> platforms = ArgumentCaptor.forClass( Collection.class );
        verify( geoBrowser ).searchAndRetrieveGeoRecords( eq( GeoRecordType.SERIES ), isNull(), isNull(), isNull(),
                platforms.capture(), isNull(), eq( 0 ), eq( 100 ), eq( false ) );
        assertThat( platforms.getValue() ).containsExactlyInAnyOrder( "GPL1", "GPL3" );
    }

    /**
     * With a multiple of five platforms the final batch is empty, and a search with an empty platform
     * list is a search with no platform filter.
     */
    @Test
    @WithMockUser
    public void testAnEmptyFinalBatchIsNotSearched() throws Exception {
        when( blacklistedEntityService.loadAll() ).thenReturn( Arrays.asList(
                platform( "GPL1" ), platform( "GPL2" ), platform( "GPL3" ), platform( "GPL4" ), platform( "GPL5" ) ) );

        assertThat( cli ).withArguments( "-pp" ).succeeds();

        verify( geoBrowser, times( 1 ) ).searchAndRetrieveGeoRecords( any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean() );
    }

    private static BlacklistedEntity platform( String accession ) {
        BlacklistedPlatform p = new BlacklistedPlatform();
        p.setShortName( accession );
        p.setExternalAccession( DatabaseEntry.Factory.newInstance( accession, null ) );
        return p;
    }
}
