package ubic.gemma.apps;

import org.junit.jupiter.api.AfterEach;
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
import ubic.gemma.cli.util.TestCLIContext;
import ubic.gemma.cli.util.test.BaseCliTest5;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * The backfill writes the GEO document and nothing else, and it skips what is already done.
 *
 * @author gembro
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class UpdateGeoSourceMetadataCliTest extends BaseCliTest5 {

    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public UpdateGeoSourceMetadataCli updateGeoSourceMetadataCli() {
            return new UpdateGeoSourceMetadataCli();
        }

        @Bean
        public GeoService geoService() {
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
    private UpdateGeoSourceMetadataCli cli;

    @Autowired
    private GeoService geoService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private EntityLocator entityLocator;

    @AfterEach
    public void resetMocks() {
        reset( geoService, eeService, entityLocator );
    }

    private ExpressionExperiment geoExperiment() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        ee.setShortName( "GSE1234" );
        ExternalDatabase geo = new ExternalDatabase();
        geo.setName( ExternalDatabases.GEO );
        DatabaseEntry acc = new DatabaseEntry();
        acc.setAccession( "GSE1234" );
        acc.setExternalDatabase( geo );
        ee.setAccession( acc );
        when( entityLocator.locateExpressionExperiment( eq( "GSE1234" ), anyBoolean() ) ).thenReturn( ee );
        when( eeService.thawLite( ee ) ).thenReturn( ee );
        return ee;
    }

    /**
     * 🛑 The safety rule. A corpus-wide refetch that also took GEO's experiment tags, sample
     * characteristics or publications would overwrite curation on 23,000 datasets, and this is the
     * only place that says it must not. It goes red if another effect is switched on here.
     */
    @Test
    @WithMockUser
    public void testItStoresTheDocumentAndNothingElse() {
        ExpressionExperiment ee = geoExperiment();
        when( eeService.hasSourceMetadata( ee ) ).thenReturn( false );

        assertThat( cli ).withArguments( "-e", "GSE1234" ).succeeds();

        ArgumentCaptor<GeoService.GeoUpdateConfig> config = ArgumentCaptor.forClass( GeoService.GeoUpdateConfig.class );
        verify( geoService ).updateFromGEO( eq( ee ), config.capture() );
        assertThat( config.getValue().isSourceMetadata() ).isTrue();
        assertThat( config.getValue().isExperimentTags() ).isFalse();
        assertThat( config.getValue().isSampleCharacteristics() ).isFalse();
        assertThat( config.getValue().isPublications() ).isFalse();
    }

    /** Resumable: what already has a document is not refetched, so an interrupted run continues. */
    @Test
    @WithMockUser
    public void testItSkipsAnExperimentThatAlreadyHasOne() {
        ExpressionExperiment ee = geoExperiment();
        when( eeService.hasSourceMetadata( ee ) ).thenReturn( true );

        assertThat( cli ).withArguments( "-e", "GSE1234" ).succeeds();

        verify( geoService, never() ).updateFromGEO( any( ExpressionExperiment.class ), any() );
    }

    /** ... unless the caller says to replace it. */
    @Test
    @WithMockUser
    public void testForceRefetchesEvenWhenOneIsStored() {
        ExpressionExperiment ee = geoExperiment();
        when( eeService.hasSourceMetadata( ee ) ).thenReturn( true );

        assertThat( cli ).withArguments( "-e", "GSE1234", "--force" ).succeeds();

        verify( geoService ).updateFromGEO( eq( ee ), any() );
    }

    /**
     * A non-GEO experiment has no record to store. It is skipped rather than failed — counting
     * thousands of them as errors would bury the fetches that really did fail.
     */
    @Test
    @WithMockUser
    public void testANonGeoExperimentIsSkippedRatherThanFailed() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 2L );
        ee.setShortName( "GPL_only" );
        when( entityLocator.locateExpressionExperiment( eq( "GPL_only" ), anyBoolean() ) ).thenReturn( ee );
        when( eeService.thawLite( ee ) ).thenReturn( ee );

        assertThat( cli ).withArguments( "-e", "GPL_only" ).succeeds();

        verify( geoService, never() ).updateFromGEO( any( ExpressionExperiment.class ), any() );
        verify( eeService, never() ).hasSourceMetadata( any() );
    }

    /**
     * 🛑 The accession must be read from a THAWED experiment. The batch task runs off the main
     * thread with no session, so the instance it is handed is detached and
     * {@code ee.getAccession()} is an uninitialized proxy —
     * {@code LazyInitializationException: Could not initialize proxy [DatabaseEntry#…]}, on every
     * experiment in the run, measured on production 2026-08-29.
     * <p>
     * The other tests here cannot catch it: they build an experiment in memory, where the
     * accession is a real object and reading it works whether or not anything was thawed. This one
     * gives the un-thawed instance NO accession, so a CLI that reads it sees "not from GEO", skips,
     * and never calls the fetch.
     */
    @Test
    @WithMockUser
    public void testItReadsTheAccessionFromTheThawedExperiment() {
        ExpressionExperiment detached = new ExpressionExperiment();
        detached.setId( 3L );
        detached.setShortName( "GSE9999" );
        // no accession: standing in for the proxy that cannot be initialized off-session

        ExpressionExperiment thawed = new ExpressionExperiment();
        thawed.setId( 3L );
        thawed.setShortName( "GSE9999" );
        ExternalDatabase geo = new ExternalDatabase();
        geo.setName( ExternalDatabases.GEO );
        DatabaseEntry acc = new DatabaseEntry();
        acc.setAccession( "GSE9999" );
        acc.setExternalDatabase( geo );
        thawed.setAccession( acc );

        when( entityLocator.locateExpressionExperiment( eq( "GSE9999" ), anyBoolean() ) ).thenReturn( detached );
        when( eeService.thawLite( detached ) ).thenReturn( thawed );
        when( eeService.hasSourceMetadata( thawed ) ).thenReturn( false );

        assertThat( cli ).withArguments( "-e", "GSE9999" ).succeeds();

        verify( eeService ).thawLite( detached );
        verify( geoService ).updateFromGEO( eq( thawed ), any() );
    }

    /**
     * 🛑 The corpus sweep is the point of this command, and it is what you get by default.
     * <p>
     * With {@code setAllIsLazy()} the base class handed {@code -all} to
     * {@code processAllExpressionExperiments()}, whose default body is empty. Nothing was
     * processed, nothing was logged beyond "Loading all expression experiments as a stub...", and
     * the run reported success in 0 seconds — on production, with no output to suggest anything was
     * wrong. Lazy-all belongs to the commands that replace a per-experiment sweep with one
     * statement; this one has a GEO fetch per experiment.
     */
    @Test
    @WithMockUser
    // A fresh CLI bean: ExpressionExperimentManipulatingCLI keeps the -e values and the
    // single-experiment options it has seen in fields that nothing clears, so a bean that already
    // ran with -e refuses this one with "single-experiment options used ... but more than one
    // experiments was found".
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void testTheDefaultRunVisitsEveryExperiment() {
        ExpressionExperiment first = geoExperimentReference( 11L, "GSE1111" );
        when( eeService.loadAllReferences() ).thenReturn( Arrays.asList( first ) );

        TestCLIContext ctx = new TestCLIContext( "updateGeoSourceMetadata", new String[0] );
        cli.executeCommand( ctx );
        assertThat( ctx.getExitStatus() ).withFailMessage( "%s", ctx.getExitCause() ).isZero();

        // Loading the corpus by reference IS the fix: setAllIsLazy() sent -all to
        // processAllExpressionExperiments() instead, and that hook is empty here.
        verify( eeService ).loadAllReferences();
        verify( eeService ).thawLite( first );
        verify( geoService ).updateFromGEO( eq( first ), any() );
    }

    private ExpressionExperiment geoExperimentReference( long id, String accession ) {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( id );
        ee.setShortName( accession );
        ExternalDatabase geo = new ExternalDatabase();
        geo.setName( ExternalDatabases.GEO );
        DatabaseEntry acc = new DatabaseEntry();
        acc.setAccession( accession );
        acc.setExternalDatabase( geo );
        ee.setAccession( acc );
        when( eeService.thawLite( ee ) ).thenReturn( ee );
        when( eeService.hasSourceMetadata( ee ) ).thenReturn( false );
        return ee;
    }
}
