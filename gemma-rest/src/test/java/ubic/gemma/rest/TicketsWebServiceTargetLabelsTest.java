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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@code displayLabel} / {@code displayName} on ticket targets.
 *
 * <p>Both fields have been on {@link TicketTargetValueObject} since Phase B-2, documented as
 * "populated by the resource layer via a side-join", and nothing ever populated them — so every
 * target of every ticket arrived as a bare id and the curation UI's ticket navigator rendered
 * "31491 (no title)" for every row. The UI resolved them itself at five requests per ticket.</p>
 *
 * @author gembro
 */
@ExtendWith(MockitoExtension.class)
public class TicketsWebServiceTargetLabelsTest {

    @Mock
    private TicketService ticketService;
    @Mock
    private ubic.gemma.core.security.authentication.UserManager userManager;
    @Mock
    private ubic.gemma.core.security.authentication.UserReadService userReadService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;

    @InjectMocks
    private TicketsWebService ticketsWebService;

    private static Ticket ticketTargeting( Long... eeIds ) {
        Ticket t = new Ticket();
        t.setId( 1L );
        t.setName( "a ticket" );
        t.setType( TicketType.CURATION );
        Set<TicketTarget> targets = new LinkedHashSet<>();
        for ( Long id : eeIds ) {
            targets.add( TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, id ) );
        }
        t.setTargets( targets );
        return t;
    }

    private static ExpressionExperimentDao.Identifiers ident( Long id, String shortName, String name ) {
        ExpressionExperimentDao.Identifiers i = new ExpressionExperimentDao.Identifiers();
        i.setId( id );
        i.setShortName( shortName );
        i.setName( name );
        return i;
    }

    @Test
    @DisplayName("a target carries its dataset's short name and title, not just an id")
    public void targetsCarryTheirDatasetsNames() {
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, 42L ) )
                .thenReturn( Collections.singletonList( ticketTargeting( 42L ) ) );
        when( expressionExperimentService.loadIdentifiers( anyCollection() ) )
                .thenReturn( Collections.singletonList( ident( 42L, "GSE12345", "A study of things" ) ) );

        List<TicketValueObject> vos = ticketsWebService.openTicketsForExpressionExperiment( 42L );

        TicketTargetValueObject target = vos.get( 0 ).getTargets().get( 0 );
        assertThat( target.getDisplayLabel() ).isEqualTo( "GSE12345" );
        assertThat( target.getDisplayName() ).isEqualTo( "A study of things" );
    }

    /**
     * 🛑 The point of the fix. Resolving per ticket would move uib's N+1 onto the server; the
     * dashboard renders a page of tickets at once, so every target across all of them resolves in one
     * call.
     */
    @Test
    @DisplayName("a page of tickets resolves every target in ONE query")
    public void everyTargetAcrossEveryTicketResolvesInOneCall() {
        Ticket a = ticketTargeting( 1L, 2L );
        Ticket b = ticketTargeting( 3L );
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, 1L ) )
                .thenReturn( Arrays.asList( a, b ) );
        when( expressionExperimentService.loadIdentifiers( anyCollection() ) )
                .thenReturn( Arrays.asList( ident( 1L, "GSE1", "one" ), ident( 2L, "GSE2", "two" ),
                        ident( 3L, "GSE3", "three" ) ) );

        ticketsWebService.openTicketsForExpressionExperiment( 1L );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> asked = ArgumentCaptor.forClass( Collection.class );
        verify( expressionExperimentService, times( 1 ) ).loadIdentifiers( asked.capture() );
        assertThat( asked.getValue() ).containsExactlyInAnyOrder( 1L, 2L, 3L );
    }

    /**
     * loadIdentifiers is ACL-filtered, so an id the caller cannot read simply does not come back. The
     * target keeps its null — the documented fallback — rather than the resolution failing or, worse,
     * the name of an unreadable dataset appearing on a ticket.
     */
    @Test
    @DisplayName("an unresolvable target keeps its null rather than borrowing another row's name")
    public void anUnresolvableTargetStaysNull() {
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, 1L ) )
                .thenReturn( Collections.singletonList( ticketTargeting( 1L, 999L ) ) );
        when( expressionExperimentService.loadIdentifiers( anyCollection() ) )
                .thenReturn( Collections.singletonList( ident( 1L, "GSE1", "one" ) ) );

        List<TicketTargetValueObject> targets =
                new ArrayList<>( ticketsWebService.openTicketsForExpressionExperiment( 1L ).get( 0 ).getTargets() );

        TicketTargetValueObject resolved = targets.stream()
                .filter( t -> t.getTargetId().equals( 1L ) ).findFirst().orElseThrow( AssertionError::new );
        TicketTargetValueObject unresolved = targets.stream()
                .filter( t -> t.getTargetId().equals( 999L ) ).findFirst().orElseThrow( AssertionError::new );
        assertThat( resolved.getDisplayLabel() ).isEqualTo( "GSE1" );
        assertThat( unresolved.getDisplayLabel() ).isNull();
        assertThat( unresolved.getDisplayName() ).isNull();
    }

    /** No EE targets at all must not cost a query. */
    @Test
    @DisplayName("a ticket with no dataset targets asks nothing")
    public void noDatasetTargetsAsksNothing() {
        when( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, 1L ) )
                .thenReturn( Collections.singletonList( ticketTargeting() ) );

        ticketsWebService.openTicketsForExpressionExperiment( 1L );

        verify( expressionExperimentService, times( 0 ) ).loadIdentifiers( anyCollection() );
    }
}
