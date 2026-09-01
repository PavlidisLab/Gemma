/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserReadService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Real JAX-RS path matching for {@code GET /tickets/scratchpad} — the one thing the mocked
 * {@link TicketsWebServiceTest} cannot see, since it calls the handler methods directly and a literal
 * route swallowed by {@code @Path("/{id}")} would still pass there.
 * <p>
 * JAX-RS sorts candidate methods by literal character count ahead of template count (JSR-370 §3.7.2),
 * so {@code /scratchpad} outranks {@code /{id}}. If that stopped holding, the request would reach
 * {@link TicketsWebService#getTicket} with {@code "scratchpad"} to convert to a {@code Long} and Jersey
 * would answer 404 — hence the 200 assertion, paired with a numeric id that must still reach
 * {@code /{id}} so the first assertion means the literal segment won a real contest.
 * <p>
 * Deliberately NOT a {@code BaseJerseyIntegrationTest5}: no database and no application context are
 * needed, only the in-memory container and a mocked service, so this stays in the fast suite. Spring
 * method security is absent, so {@code @PreAuthorize} does not fire here — the auth contract is
 * asserted by reflection in {@link TicketsWebServiceTest} instead.
 */
public class TicketsWebServiceScratchpadRoutingTest extends JerseyTest {

    // 🛑 Assigned in configure(), NOT by field initializers. JerseyTest's constructor calls the
    // overridden configure(), which runs BEFORE this subclass's field initializers — mocks created as
    // field initializers are still null when the resource is built, and the handler then NPEs on a
    // null collaborator in a way that reads exactly like a routing failure.
    private TicketService ticketService;
    private UserManager userManager;
    private UserReadService userReadService;

    private User curator;

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new InMemoryTestContainerFactory();
    }

    @Override
    protected Application configure() {
        ticketService = mock( TicketService.class );
        userManager = mock( UserManager.class );
        userReadService = mock( UserReadService.class );
        // jersey-spring6's SpringComponentProvider is on the classpath and falls back to loading
        // applicationContext.xml when no `contextConfig` is set — this module ships none. An empty
        // refreshed context satisfies it; the resource itself is registered as a wired instance.
        GenericApplicationContext emptyContext = new GenericApplicationContext();
        emptyContext.refresh();
        return new ResourceConfig()
                .register( new TicketsWebService( ticketService, userManager, userReadService,
                        mock( ExpressionExperimentService.class ) ) )
                .register( JacksonFeature.class )
                .property( "contextConfig", emptyContext )
                // Same reason as BaseJerseyTest5: Jersey 3.1's resource-model validator treats this
                // surface's parameterized arg types as fatal hints during application init.
                .property( ServerProperties.RESOURCE_VALIDATION_IGNORE_ERRORS, true );
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        curator = new User();
        curator.setId( 42L );
        curator.setName( "alice" );
        lenient().when( userManager.getCurrentUser() ).thenReturn( curator );

        Ticket pad = Ticket.Factory.newInstance( TicketType.SCRATCHPAD, "Scratchpad: alice", curator );
        pad.setId( 7L );
        pad.setAcceptsTargets( true );
        pad.setTargets( new HashSet<>() );
        pad.setEvents( new ArrayList<>() );
        lenient().when( ticketService.getOrCreateScratchpad( any() ) ).thenReturn( pad );
        // /tickets/{id} answers 404 on a null projection — the discriminator in the second test.
        lenient().when( ticketService.loadValueObject( anyLong(), anyBoolean() ) ).thenReturn( null );
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void scratchpadRoute_isNotSwallowedByTheIdTemplate() {
        Response response = target( "/tickets/scratchpad" ).request().get();

        assertThat( response.getStatus() )
                .as( "a 404 here means /{id} won the match and tried to read \"scratchpad\" as a Long" )
                .isEqualTo( 200 );
        assertThat( response.readEntity( String.class ) ).contains( "SCRATCHPAD" );
    }

    /** The numeric-id route is still live, so the 200 above is a contest won, not a template gone. */
    @Test
    public void numericIdStillRoutesToTheSingleTicketHandler() {
        assertThat( target( "/tickets/9999" ).request().get().getStatus() ).isEqualTo( 404 );
    }
}
