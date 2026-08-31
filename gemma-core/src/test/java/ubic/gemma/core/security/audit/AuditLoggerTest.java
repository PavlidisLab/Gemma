/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.security.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the identifying facts in the {@link AuditLogger} line.
 * <p>
 * {@code gemma-audit.log} is the only record of an experiment deletion that outlives the
 * experiment: the entity's audit trail and every event on it are cascade-removed with it
 * ({@code AbstractAuditable.auditTrail} and {@code AuditTrail.events} are both mapped
 * {@code CascadeType.ALL}), and no other row in the schema keeps a description of what went.
 * A line naming only the numeric id is therefore unreadable after the fact — the id resolves
 * to nothing. These tests pin the short name and name into the line.
 *
 * @author phase3-agent
 */
public class AuditLoggerTest {

    private AuditLogger auditLogger;

    @BeforeEach
    public void setUp() {
        auditLogger = new AuditLogger();
    }

    @Test
    public void deleteLineNamesTheExperimentNotJustItsId() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 22984L );
        ee.setShortName( "GSE277430" );
        ee.setName( "A study that is about to be deleted" );

        AuditEvent event = AuditEvent.Factory.newInstance( new Date(), AuditAction.DELETE, null, null, null, null );

        String line = auditLogger.format( ee, event );

        // the short name is what stays meaningful once the row is gone
        assertThat( line ).contains( "GSE277430" );
        assertThat( line ).contains( "A study that is about to be deleted" );
    }

    @Test
    public void lineStillCarriesClassIdActionAndPerformer() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 22984L );
        ee.setShortName( "GSE277430" );

        AuditEvent event = AuditEvent.Factory.newInstance( new Date(), AuditAction.DELETE, null, null, null, null );

        String line = auditLogger.format( ee, event );

        assertThat( line ).contains( ExpressionExperiment.class.getName() + ":22984" );
        assertThat( line ).contains( "D event" );
        // no performer was set on the event
        assertThat( line ).contains( "[anonymous]" );
    }

    @Test
    public void anAuditableWithNoShortNameStillRenders() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 7L );

        AuditEvent event = AuditEvent.Factory.newInstance( new Date(), AuditAction.CREATE, null, null, null, null );

        String line = auditLogger.format( ee, event );

        assertThat( line ).contains( ExpressionExperiment.class.getName() + ":7" );
        assertThat( line ).contains( "C event" );
    }
}
