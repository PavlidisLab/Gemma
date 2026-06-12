package ubic.gemma.rest;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.Response;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.session.SessionRegistry;
import ubic.gemma.core.geoscrape.GeoScrapeService;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.ontology.providers.OntologyService;
import ubic.gemma.core.search.indexer.IndexerService;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseReadService;
import ubic.gemma.persistence.service.expression.experiment.AgentProposalService;
import ubic.gemma.rest.util.args.TaxonArgService;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Focused tests for the hot-refresh endpoint on {@link AdminWebService}. Kept in its own
 * class so the mock setup for a non-empty ontologies list doesn't interfere with the
 * other AdminWebService tests (which use an empty list by default).
 */
@ExtendWith(MockitoExtension.class)
class AdminWebServiceOntologyRefreshTest {

    @Mock private CacheManager cacheManager;
    @Mock private SessionFactory sessionFactory;
    @Mock private TaskRunningService taskRunningService;
    @Mock private SessionRegistry sessionRegistry;
    @Mock private DataSource dataSource;
    @Mock private UserManager userManager;
    @Mock private AgentProposalService agentProposalService;
    @Mock private TicketService ticketService;
    @Mock private TaxonArgService taxonArgService;
    @Mock private BlacklistedEntityService blacklistedEntityService;
    @Mock private ExternalDatabaseReadService externalDatabaseReadService;
    @Mock private GeoScrapeService geoScrapeService;
    @Mock private IndexerService indexerService;

    @Mock private OntologyService chebi;
    @Mock private OntologyService mondo;
    @Mock private ubic.gemma.core.ontology.OntologyService ontologyFacade;

    private AdminWebService service;

    @BeforeEach
    void setUp() {
        // chebi.getName() and mondo.getName() are stubbed per-test via when() so we don't
        // hit Mockito's unused-stub strictness when a test only inspects one bean.
        service = new AdminWebService( cacheManager, sessionFactory, taskRunningService, sessionRegistry,
                List.of( chebi, mondo ), ontologyFacade, dataSource, userManager, agentProposalService, ticketService,
                taxonArgService, blacklistedEntityService, externalDatabaseReadService, geoScrapeService,
                indexerService );
    }

    @Test
    void refreshKicksInitThreadAndReturns202() {
        when( chebi.getName() ).thenReturn( "CHEBI" );
        when( chebi.isInitializationThreadAlive() ).thenReturn( false );

        Response resp = service.refreshOntology( "CHEBI", false );

        assertThat( resp.getStatus() ).isEqualTo( 202 );
        verify( chebi ).startInitializationThread( true, false );
        verifyNoInteractions( mondo );
    }

    @Test
    void refreshPropagatesForceIndexingFlag() {
        when( chebi.getName() ).thenReturn( "CHEBI" );
        when( chebi.isInitializationThreadAlive() ).thenReturn( false );

        service.refreshOntology( "CHEBI", true );

        verify( chebi ).startInitializationThread( true, true );
    }

    @Test
    void refreshUnknownNameThrows404() {
        when( chebi.getName() ).thenReturn( "CHEBI" );
        when( mondo.getName() ).thenReturn( "MONDO" );

        assertThatThrownBy( () -> service.refreshOntology( "UBERON", false ) )
                .isInstanceOf( NotFoundException.class )
                .hasMessageContaining( "UBERON" );
        verify( chebi, org.mockito.Mockito.never() ).startInitializationThread( anyBoolean(), anyBoolean() );
        verify( mondo, org.mockito.Mockito.never() ).startInitializationThread( anyBoolean(), anyBoolean() );
    }

    @Test
    void refreshWhileInitThreadAliveThrows409() {
        when( chebi.getName() ).thenReturn( "CHEBI" );
        when( chebi.isInitializationThreadAlive() ).thenReturn( true );

        assertThatThrownBy( () -> service.refreshOntology( "CHEBI", false ) )
                .isInstanceOf( ClientErrorException.class )
                .satisfies( e -> assertThat( ( ( ClientErrorException ) e ).getResponse().getStatus() )
                        .isEqualTo( 409 ) );
        verify( chebi, org.mockito.Mockito.never() ).startInitializationThread( anyBoolean(), anyBoolean() );
    }

    /* ===== /admin/ontologies/{name}/rebuild-slim =====
     *
     * Note: the endpoint no longer matches by OntologyService.getName() (which
     * returns the OWL's dc:title and is null for CHEBI). It matches by class
     * type, accepting any case-insensitive variant of "CHEBI" / "chebi" /
     * "ChebiOntologyService" / "chebiOntology" as the path argument.
     */

    @Test
    void rebuildSlimRefusesUnknownName() {
        assertThatThrownBy( () -> service.rebuildOntologySlim( "FOOBAR" ) )
                .isInstanceOf( NotFoundException.class )
                .hasMessageContaining( "FOOBAR" )
                .hasMessageContaining( "try CHEBI or MONDO" );
    }

    @Test
    void rebuildSlimUnknownNameThrows404() {
        assertThatThrownBy( () -> service.rebuildOntologySlim( "UBERON" ) )
                .isInstanceOf( NotFoundException.class )
                .hasMessageContaining( "UBERON" );
    }

    @Test
    void rebuildSlimReturns404IfNoSlimmableBeanRegistered() {
        // service was constructed with two plain OntologyService mocks, neither implements
        // SlimmableOntologyService, so the lookup misses regardless of the path name.
        assertThatThrownBy( () -> service.rebuildOntologySlim( "CHEBI" ) )
                .isInstanceOf( NotFoundException.class )
                .hasMessageContaining( "try CHEBI or MONDO" );
    }

    @Test
    void rebuildSlimReturns503WhenNotLoaded() {
        ubic.gemma.core.ontology.providers.ChebiOntologyService realChebi =
                mock( ubic.gemma.core.ontology.providers.ChebiOntologyService.class );
        when( realChebi.triggerSlimRebuildAsync() )
                .thenThrow( new IllegalStateException( "CHEBI is not loaded yet." ) );

        AdminWebService svc = new AdminWebService( cacheManager, sessionFactory, taskRunningService,
                sessionRegistry, java.util.List.of( realChebi ), ontologyFacade, dataSource, userManager,
                agentProposalService, ticketService, taxonArgService, blacklistedEntityService,
                externalDatabaseReadService, geoScrapeService, indexerService );

        assertThatThrownBy( () -> svc.rebuildOntologySlim( "CHEBI" ) )
                .isInstanceOf( ServiceUnavailableException.class );
    }

    @Test
    void rebuildSlimReturns409WhenAlreadyInFlight() {
        ubic.gemma.core.ontology.providers.ChebiOntologyService realChebi =
                mock( ubic.gemma.core.ontology.providers.ChebiOntologyService.class );
        when( realChebi.triggerSlimRebuildAsync() ).thenReturn( false );

        AdminWebService svc = new AdminWebService( cacheManager, sessionFactory, taskRunningService,
                sessionRegistry, java.util.List.of( realChebi ), ontologyFacade, dataSource, userManager,
                agentProposalService, ticketService, taxonArgService, blacklistedEntityService,
                externalDatabaseReadService, geoScrapeService, indexerService );

        assertThatThrownBy( () -> svc.rebuildOntologySlim( "CHEBI" ) )
                .isInstanceOf( ClientErrorException.class )
                .satisfies( e -> assertThat( ( ( ClientErrorException ) e ).getResponse().getStatus() )
                        .isEqualTo( 409 ) );
    }

    @Test
    void rebuildSlimReturns202OnHappyPath() {
        ubic.gemma.core.ontology.providers.ChebiOntologyService realChebi =
                mock( ubic.gemma.core.ontology.providers.ChebiOntologyService.class );
        when( realChebi.triggerSlimRebuildAsync() ).thenReturn( true );

        AdminWebService svc = new AdminWebService( cacheManager, sessionFactory, taskRunningService,
                sessionRegistry, java.util.List.of( realChebi ), ontologyFacade, dataSource, userManager,
                agentProposalService, ticketService, taxonArgService, blacklistedEntityService,
                externalDatabaseReadService, geoScrapeService, indexerService );

        Response resp = svc.rebuildOntologySlim( "CHEBI" );

        assertThat( resp.getStatus() ).isEqualTo( 202 );
        verify( realChebi ).triggerSlimRebuildAsync();
    }

    @Test
    void rebuildSlimAcceptsLowercaseAndAlternateNames() {
        ubic.gemma.core.ontology.providers.ChebiOntologyService realChebi =
                mock( ubic.gemma.core.ontology.providers.ChebiOntologyService.class );
        when( realChebi.triggerSlimRebuildAsync() ).thenReturn( true );

        AdminWebService svc = new AdminWebService( cacheManager, sessionFactory, taskRunningService,
                sessionRegistry, java.util.List.of( realChebi ), ontologyFacade, dataSource, userManager,
                agentProposalService, ticketService, taxonArgService, blacklistedEntityService,
                externalDatabaseReadService, geoScrapeService, indexerService );

        for ( String alias : new String[]{ "CHEBI", "chebi", "ChebiOntologyService" } ) {
            assertThat( svc.rebuildOntologySlim( alias ).getStatus() ).isEqualTo( 202 );
        }
    }

    @Test
    void refreshEvictsOntologyCacheOnceInitThreadCompletes() throws InterruptedException {
        // Pre-fix bug: AdminWebService kicked startInitializationThread but never asked the facade
        // to evict its findTerm / getParents / getChildren caches keyed by (ontologyService, ...).
        // A term added to TGEMO upstream was reread into the Jena model but search lookups kept
        // hitting the stale cache until a bounce. Pin that the evict now happens on a daemon
        // gated by waitForInitializationThread, so callers still get a 202 immediately.
        when( chebi.getName() ).thenReturn( "CHEBI" );
        when( chebi.isInitializationThreadAlive() ).thenReturn( false );

        Response resp = service.refreshOntology( "CHEBI", false );

        assertThat( resp.getStatus() ).isEqualTo( 202 );
        verify( chebi ).startInitializationThread( true, false );

        // Give the daemon a moment to run waitForInitializationThread + clearCachesForOntology.
        // The mock's waitForInitializationThread returns immediately (no real thread), so this
        // should resolve well under the timeout — generous bound so a slow CI host doesn't flake.
        // We don't stub the response-cache region here — the daemon's helper handles a null
        // cache (e.g. region not registered on this build) gracefully, and the response-cache
        // assertion lives in refreshFlushesAnnotationsSearchResponseCache.
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos( 5 );
        while ( System.nanoTime() < deadline ) {
            try {
                verify( ontologyFacade ).clearCachesForOntology( chebi );
                break;
            } catch ( AssertionError notYet ) {
                Thread.sleep( 20 );
            }
        }
        verify( ontologyFacade ).clearCachesForOntology( chebi );
        verify( chebi ).waitForInitializationThread();
    }

    @Test
    void refreshFlushesAnnotationsSearchResponseCache() throws InterruptedException {
        // Pre-fix bug (the matchedVia=null after TGEMO upstream change report):
        // AnnotationsWebService caches /annotations/search responses for 5 minutes keyed by
        // (query, strategy, limit, ...). Each cached hit has baked-in matchedVia / matchedText
        // computed against the prior ontology state. A hit that resolved with matchedVia=null
        // because TGEMO_00210 wasn't loaded yet would stay null in the cached payload until
        // the next bounce or manual DELETE /admin/caches/AnnotationsSearchResponseCache.
        // Pin that the refresh daemon now flushes that region too.
        org.springframework.cache.Cache responseCache = mock( org.springframework.cache.Cache.class );
        when( cacheManager.getCache( "AnnotationsSearchResponseCache" ) ).thenReturn( responseCache );
        when( chebi.getName() ).thenReturn( "CHEBI" );
        when( chebi.isInitializationThreadAlive() ).thenReturn( false );

        service.refreshOntology( "CHEBI", false );

        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos( 5 );
        while ( System.nanoTime() < deadline ) {
            try {
                verify( responseCache ).clear();
                break;
            } catch ( AssertionError notYet ) {
                Thread.sleep( 20 );
            }
        }
        verify( responseCache ).clear();
    }

    @Test
    void refreshHandlesEmptyOntologyList() {
        AdminWebService emptyService = new AdminWebService( cacheManager, sessionFactory, taskRunningService,
                sessionRegistry, Collections.emptyList(), ontologyFacade, dataSource, userManager, agentProposalService,
                ticketService, taxonArgService, blacklistedEntityService, externalDatabaseReadService,
                geoScrapeService, indexerService );

        assertThatThrownBy( () -> emptyService.refreshOntology( "CHEBI", false ) )
                .isInstanceOf( NotFoundException.class );
    }
}
