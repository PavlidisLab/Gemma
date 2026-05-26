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

    private AdminWebService service;

    @BeforeEach
    void setUp() {
        // chebi.getName() and mondo.getName() are stubbed per-test via when() so we don't
        // hit Mockito's unused-stub strictness when a test only inspects one bean.
        service = new AdminWebService( cacheManager, sessionFactory, taskRunningService, sessionRegistry,
                List.of( chebi, mondo ), dataSource, userManager, agentProposalService, ticketService,
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

    /* ===== /admin/ontologies/{name}/rebuild-slim ===== */

    @Test
    void rebuildSlimRefusesNonChebiOntology() {
        when( chebi.getName() ).thenReturn( "MONDO" );
        // a generic OntologyService mock that says its name is MONDO — not a ChebiOntologyService
        assertThatThrownBy( () -> service.rebuildOntologySlim( "MONDO" ) )
                .isInstanceOf( NotFoundException.class )
                .hasMessageContaining( "does not support slim rebuild" );
    }

    @Test
    void rebuildSlimUnknownNameThrows404() {
        when( chebi.getName() ).thenReturn( "CHEBI" );
        when( mondo.getName() ).thenReturn( "MONDO" );

        assertThatThrownBy( () -> service.rebuildOntologySlim( "UBERON" ) )
                .isInstanceOf( NotFoundException.class )
                .hasMessageContaining( "UBERON" );
    }

    @Test
    void rebuildSlimReturns503WhenNotLoaded() {
        ubic.gemma.core.ontology.providers.ChebiOntologyService realChebi =
                mock( ubic.gemma.core.ontology.providers.ChebiOntologyService.class );
        when( realChebi.getName() ).thenReturn( "CHEBI" );
        when( realChebi.triggerSlimRebuildAsync() )
                .thenThrow( new IllegalStateException( "CHEBI is not loaded yet." ) );

        AdminWebService svc = new AdminWebService( cacheManager, sessionFactory, taskRunningService,
                sessionRegistry, java.util.List.of( realChebi ), dataSource, userManager,
                agentProposalService, ticketService, taxonArgService, blacklistedEntityService,
                externalDatabaseReadService, geoScrapeService, indexerService );

        assertThatThrownBy( () -> svc.rebuildOntologySlim( "CHEBI" ) )
                .isInstanceOf( ServiceUnavailableException.class );
    }

    @Test
    void rebuildSlimReturns409WhenAlreadyInFlight() {
        ubic.gemma.core.ontology.providers.ChebiOntologyService realChebi =
                mock( ubic.gemma.core.ontology.providers.ChebiOntologyService.class );
        when( realChebi.getName() ).thenReturn( "CHEBI" );
        when( realChebi.triggerSlimRebuildAsync() ).thenReturn( false );

        AdminWebService svc = new AdminWebService( cacheManager, sessionFactory, taskRunningService,
                sessionRegistry, java.util.List.of( realChebi ), dataSource, userManager,
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
        when( realChebi.getName() ).thenReturn( "CHEBI" );
        when( realChebi.triggerSlimRebuildAsync() ).thenReturn( true );

        AdminWebService svc = new AdminWebService( cacheManager, sessionFactory, taskRunningService,
                sessionRegistry, java.util.List.of( realChebi ), dataSource, userManager,
                agentProposalService, ticketService, taxonArgService, blacklistedEntityService,
                externalDatabaseReadService, geoScrapeService, indexerService );

        Response resp = svc.rebuildOntologySlim( "CHEBI" );

        assertThat( resp.getStatus() ).isEqualTo( 202 );
        verify( realChebi ).triggerSlimRebuildAsync();
    }

    @Test
    void refreshHandlesEmptyOntologyList() {
        AdminWebService emptyService = new AdminWebService( cacheManager, sessionFactory, taskRunningService,
                sessionRegistry, Collections.emptyList(), dataSource, userManager, agentProposalService,
                ticketService, taxonArgService, blacklistedEntityService, externalDatabaseReadService,
                geoScrapeService, indexerService );

        assertThatThrownBy( () -> emptyService.refreshOntology( "CHEBI", false ) )
                .isInstanceOf( NotFoundException.class );
    }
}
