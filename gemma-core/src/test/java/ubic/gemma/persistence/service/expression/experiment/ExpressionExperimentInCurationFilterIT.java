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
package ubic.gemma.persistence.service.expression.experiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketState;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTarget;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.ContactDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.TicketService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code inCuration} filter behind the curation UI's "Experiments staged for curation" page.
 *
 * <p>Gemma had no way to answer "which datasets are being worked on", so that page listed the whole
 * corpus under a title saying otherwise. {@code curationDetails.curationPending} looked like the
 * field for it and is not: it is read off the curation LOCK, so it means "someone is editing this
 * right now" — true for a handful of datasets at any instant, and admin-only. The definition here is
 * Paul's ruling of 2026-09-01: needsAttention, OR targeted by a still-open ticket.</p>
 *
 * <p>Each test asserts the exact membership of its own four fixtures rather than a corpus-wide count,
 * so a filter that silently matched everything or nothing fails instead of passing.</p>
 *
 * @author gembro
 */
@Transactional
public class ExpressionExperimentInCurationFilterIT extends BaseSpringContextTest5 {

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;
    @Autowired
    private TicketService ticketService;
    @Autowired
    private TicketDao ticketDao;
    @Autowired
    private ContactDao contactDao;

    private ExpressionExperiment needsAttention, onOpenTicket, onCancelledTicket, quiet;
    private Contact reporter;

    @BeforeEach
    public void seedFixtures() {
        Contact c = new Contact();
        c.setName( "incuration-it-" + UUID.randomUUID() );
        reporter = contactDao.create( c );

        // Bare experiments on purpose: the filter reads curationDetails and TICKET_TARGET and nothing
        // else, and the full fixture helper drags in a platform whose primary taxon is not persisted
        // inside this test's transaction.
        needsAttention = createExperiment();
        onOpenTicket = createExperiment();
        onCancelledTicket = createExperiment();
        quiet = createExperiment();

        needsAttention.getCurationDetails().setNeedsAttention( true );
        expressionExperimentDao.update( needsAttention );

        openTicketOn( onOpenTicket, TicketState.OPEN );
        Ticket cancelled = openTicketOn( onCancelledTicket, TicketState.OPEN );
        ticketService.transition( cancelled, TicketState.CANCELLED, reporter, "not doing this one" );
    }

    private ExpressionExperiment createExperiment() {
        ExpressionExperiment ee = new ExpressionExperiment();
        String tag = "incuration-it-" + UUID.randomUUID();
        ee.setName( tag );
        ee.setShortName( tag );
        return expressionExperimentDao.create( ee );
    }

    private Ticket openTicketOn( ExpressionExperiment ee, TicketState state ) {
        Set<TicketTarget> targets = Collections.singleton(
                TicketTarget.Factory.newInstance( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() ) );
        return ticketService.openTicket( reporter, TicketType.CURATION,
                "incuration-it-" + UUID.randomUUID(), targets );
    }

    /** The four fixtures, so an assertion counts them and not the rest of the test corpus. */
    private List<Long> fixtureIds() {
        return Arrays.asList( needsAttention.getId(), onOpenTicket.getId(),
                onCancelledTicket.getId(), quiet.getId() );
    }

    private Set<Long> matching( boolean inCuration ) {
        Filters filters = Filters.by(
                expressionExperimentDao.getFilter( "inCuration", Filter.Operator.eq,
                        Boolean.toString( inCuration ) ) );
        filters.and( ExpressionExperimentDao.OBJECT_ALIAS, "id", Long.class,
                Filter.Operator.in, fixtureIds() );
        Set<Long> ids = new HashSet<>();
        for ( ExpressionExperiment ee : expressionExperimentDao.load( filters, null ) ) {
            ids.add( ee.getId() );
        }
        return ids;
    }

    @Test
    @DisplayName("inCuration = true is needsAttention OR a still-open ticket, and nothing else")
    public void inCurationIsTheUnionOfBothHalves() {
        assertThat( matching( true ) )
                .containsExactlyInAnyOrder( needsAttention.getId(), onOpenTicket.getId() );
    }

    /**
     * The half that is easy to get wrong: a ticket that was opened and then cancelled must not keep
     * its dataset on the page forever. Same open-state set as TicketDao#findOpenForTarget.
     */
    @Test
    @DisplayName("a cancelled ticket does not hold its dataset in curation")
    public void aCancelledTicketReleasesItsDataset() {
        assertThat( matching( true ) ).doesNotContain( onCancelledTicket.getId() );
        assertThat( matching( false ) ).contains( onCancelledTicket.getId() );
    }

    /** The two halves partition the fixtures: nothing is in both, nothing is in neither. */
    @Test
    @DisplayName("inCuration = false is exactly the complement")
    public void falseIsTheComplement() {
        Set<Long> yes = matching( true ), no = matching( false );
        assertThat( yes ).doesNotContainAnyElementsOf( no );
        Set<Long> union = new HashSet<>( yes );
        union.addAll( no );
        assertThat( union ).containsExactlyInAnyOrderElementsOf( fixtureIds() );
    }

    /**
     * The count query joins differently from the id query, so a predicate can work in one and blow up
     * in the other — and the dashboard panel uib wants this for reads the count, not the page.
     */
    @Test
    @DisplayName("the same filter counts, not only lists")
    public void theFilterAlsoCounts() {
        Filters filters = Filters.by(
                expressionExperimentDao.getFilter( "inCuration", Filter.Operator.eq, "true" ) );
        filters.and( ExpressionExperimentDao.OBJECT_ALIAS, "id", Long.class,
                Filter.Operator.in, fixtureIds() );
        assertThat( expressionExperimentDao.count( filters ) ).isEqualTo( 2L );
    }

    /**
     * The filter's ticket half and the per-dataset ticket route must name the same datasets, or the
     * list says a dataset is in curation and its own ticket drawer shows nothing.
     */
    @Test
    @DisplayName("the ticket half agrees with findOpenForTarget")
    public void theTicketHalfAgreesWithThePerDatasetLookup() {
        Set<Long> byFilter = matching( true );
        for ( Long id : fixtureIds() ) {
            boolean hasOpenTicket = !ticketDao
                    .findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, id ).isEmpty();
            if ( hasOpenTicket ) {
                assertThat( byFilter )
                        .as( "dataset %s has an open ticket, so the filter must claim it", id )
                        .contains( id );
            }
        }
    }
}
