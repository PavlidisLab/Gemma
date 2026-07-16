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
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.Ticket;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketTargetType;
import ubic.gemma.model.common.auditAndSecurity.curation.TicketType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 11 backfill: the one-time forward-migration of legacy {@code CurationDetails} flags into
 * tickets. Sets a flag DIRECTLY on the column (no ticket, no audit event — the frozen-legacy shape,
 * exercising the null-safe date fallback), then migrates and checks a ticket is opened, the column
 * stays set (never cleared), and re-running is idempotent.
 */
class LegacyCurationFlagMigratorIT extends BaseSpringContextTest5 {

    @Autowired
    private LegacyCurationFlagMigrator migrator;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** Set a legacy flag straight on the column, bypassing tickets/events (the frozen shape). */
    private void setLegacyFlags( Long eeId, boolean troubled, boolean needsAttention ) {
        new TransactionTemplate( transactionManager ).executeWithoutResult( status -> {
            ExpressionExperiment ee = expressionExperimentDao.load( eeId );
            ee.getCurationDetails().setTroubled( troubled );
            ee.getCurationDetails().setNeedsAttention( needsAttention );
            expressionExperimentDao.update( ee );
        } );
    }

    private boolean troubledColumn( Long eeId ) {
        return expressionExperimentService.loadTroubledIds().contains( eeId );
    }

    @Test
    void legacyTroubledFlag_forwardMigratesToQualityReviewTicket_idempotently() {
        Contact operator = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        setLegacyFlags( ee.getId(), true, true );          // troubled + needsAttention, no ticket/event
        assertThat( troubledColumn( ee.getId() ) ).isTrue();

        int opened = migrator.migrate( operator );
        assertThat( opened ).isGreaterThanOrEqualTo( 1 );

        List<Ticket> open = ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() );
        assertThat( open ).hasSize( 1 );
        assertThat( open.get( 0 ).getType() ).isEqualTo( TicketType.QUALITY_REVIEW );  // troubled ⇒ QR
        assertThat( open.get( 0 ).getCreatedAt() ).isNotNull();                        // fallback date, no NPE
        // signal preserved — the column is still set, now ticket-backed
        assertThat( troubledColumn( ee.getId() ) ).isTrue();

        // idempotent: a second run opens no further ticket for this EE
        migrator.migrate( operator );
        assertThat( ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() ) ).hasSize( 1 );
    }

    @Test
    void legacyNeedsAttentionOnly_forwardMigratesToGenericTicket() {
        Contact operator = getTestPersistentContact();
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        setLegacyFlags( ee.getId(), false, true );         // needsAttention only

        migrator.migrate( operator );

        List<Ticket> open = ticketService.findOpenForTarget( TicketTargetType.EXPRESSION_EXPERIMENT, ee.getId() );
        assertThat( open ).hasSize( 1 );
        assertThat( open.get( 0 ).getType() ).isEqualTo( TicketType.GENERIC );  // needsAttention-only ⇒ GENERIC
        assertThat( troubledColumn( ee.getId() ) ).isFalse();                   // not troubled
    }
}
