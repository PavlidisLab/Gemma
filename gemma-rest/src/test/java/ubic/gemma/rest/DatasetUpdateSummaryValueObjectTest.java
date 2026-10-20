/*
 * The gemma-rest project
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

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-data tests for the {@code lastUpdate} label vocabulary — no Spring, no database.
 */
public class DatasetUpdateSummaryValueObjectTest {

    @Test
    public void labelsTheEventTypeThatDominatesRecentUpdates() {
        // 688 of 694 events in a sampled production week were this type, so it is what a listing
        // will mostly render.
        assertThat( DatasetUpdateSummaryValueObject.labelFor( ExpressionExperimentUpdateFromGEOEvent.class ) )
                .isEqualTo( "Updated from GEO" );
    }

    @Test
    public void labelsADifferentialExpressionAnalysis() {
        assertThat( DatasetUpdateSummaryValueObject.labelFor( DifferentialExpressionAnalysisEvent.class ) )
                .isEqualTo( "Differential expression analysis performed" );
    }

    /**
     * 🛑 The regression this file exists for. Every {@code Failed*} analysis event extends
     * {@link NeedsAttentionEvent}, NOT its success counterpart, so a superclass walk with no
     * explicit entry labels a failed PCA run "Flagged as needing attention" — a true statement
     * about the side effect that says nothing about what actually failed, and reads identically
     * for all seven of them.
     */
    @Test
    public void failedAnalysisEventsNameTheAnalysisRatherThanTheAttentionFlagTheySet() {
        assertThat( DatasetUpdateSummaryValueObject.labelFor( FailedPCAAnalysisEvent.class ) )
                .isEqualTo( "PCA failed" );
        assertThat( DatasetUpdateSummaryValueObject.labelFor( FailedDifferentialExpressionAnalysisEvent.class ) )
                .isEqualTo( "Differential expression analysis failed" );
        assertThat( DatasetUpdateSummaryValueObject.labelFor( FailedProcessedVectorComputationEvent.class ) )
                .isEqualTo( "Preprocessing failed" );
        assertThat( DatasetUpdateSummaryValueObject.labelFor( FailedSampleCorrelationAnalysisEvent.class ) )
                .isEqualTo( "Sample correlation failed" );
        assertThat( DatasetUpdateSummaryValueObject.labelFor( FailedMeanVarianceUpdateEvent.class ) )
                .isEqualTo( "Mean-variance computation failed" );
        assertThat( DatasetUpdateSummaryValueObject.labelFor( FailedMissingValueAnalysisEvent.class ) )
                .isEqualTo( "Missing-value analysis failed" );
        assertThat( DatasetUpdateSummaryValueObject.labelFor( FailedLinkAnalysisEvent.class ) )
                .isEqualTo( "Coexpression analysis failed" );

        // …and the flag events themselves still say what they are.
        assertThat( DatasetUpdateSummaryValueObject.labelFor( NeedsAttentionEvent.class ) )
                .isEqualTo( "Flagged as needing attention" );
    }

    /**
     * The map covers every event class a dataset's trail can carry today, so the inheritance path
     * is exercised here with a subclass that does not exist yet — which is the case it is for. A
     * type added later must inherit its parent's wording rather than falling off the map.
     */
    @Test
    public void aSubclassAddedLaterInheritsItsNearestMappedAncestor() {
        assertThat( DatasetUpdateSummaryValueObject.labelFor( HypotheticalNewPcaEvent.class ) )
                .isEqualTo( "PCA performed" );
    }

    private static class HypotheticalNewPcaEvent extends PCAAnalysisEvent {
    }

    @Test
    public void tagEventsAreNotFlattenedIntoTheirAnnotationParent() {
        assertThat( DatasetUpdateSummaryValueObject.labelFor( TagAddedEvent.class ) )
                .isEqualTo( "Tag added" );
        assertThat( DatasetUpdateSummaryValueObject.labelFor( TagRemovedEvent.class ) )
                .isEqualTo( "Tag removed" );
    }

    /**
     * Nothing in the hierarchy is mapped for these, so the label restates the class name. Dull, but
     * it cannot assert something that did not happen.
     */
    @Test
    public void anEntirelyUnmappedTypeFallsBackToItsClassName() {
        assertThat( DatasetUpdateSummaryValueObject.labelFor( TicketOpenedEvent.class ) )
                .isEqualTo( "Ticket opened" );
        assertThat( DatasetUpdateSummaryValueObject.labelFor( PipelineBatchSubmittedEvent.class ) )
                .isEqualTo( "Pipeline batch submitted" );
    }

    @Test
    public void derivedLabelsKeepAcronymsAndDropTheEventSuffix() {
        assertThat( DatasetUpdateSummaryValueObject.derivedLabel( "UninformativeFASTQHeadersForBatchingEvent" ) )
                .isEqualTo( "Uninformative FASTQ headers for batching" );
        assertThat( DatasetUpdateSummaryValueObject.derivedLabel( "ExpressionExperimentUpdateFromGEOEvent" ) )
                .isEqualTo( "Expression experiment update from GEO" );
        assertThat( DatasetUpdateSummaryValueObject.derivedLabel( "FailedPCAAnalysisEvent" ) )
                .isEqualTo( "Failed PCA analysis" );
    }

    /**
     * 🛑 {@code action='C'} means the dataset was loaded into Gemma. It is NOT a visibility change,
     * and the fallback label must not imply one.
     */
    @Test
    public void theCreationFallbackSaysLoadedNotPublished() {
        AuditEvent created = AuditEvent.Factory.newInstance( new Date(), AuditAction.CREATE, null, null,
                User.Factory.newInstance( "paul" ), null );

        DatasetUpdateSummaryValueObject vo = DatasetUpdateSummaryValueObject.forCreation( created );

        assertThat( vo.getLabel() ).isEqualTo( "Added to Gemma" );
        assertThat( vo.getLabel() ).doesNotContainIgnoringCase( "public" );
        assertThat( vo.getEventType() ).isNull();
        assertThat( vo.getPerformer() ).isEqualTo( "paul" );
    }

    @Test
    public void aSummaryCarriesTheEventsDatePerformerAndTypeName() {
        Date when = new Date( 1_750_000_000_000L );
        AuditEvent event = AuditEvent.Factory.newInstance( when, AuditAction.UPDATE,
                "reprocessed, looks fine now", null, User.Factory.newInstance( "curator" ),
                new DifferentialExpressionAnalysisEvent() );

        DatasetUpdateSummaryValueObject vo = DatasetUpdateSummaryValueObject.forEvent( event );

        assertThat( vo.getLabel() ).isEqualTo( "Differential expression analysis performed" );
        assertThat( vo.getEventType() ).isEqualTo( "DifferentialExpressionAnalysisEvent" );
        assertThat( vo.getDate() ).isEqualTo( when );
        assertThat( vo.getPerformer() ).isEqualTo( "curator" );
        // The note ("reprocessed, looks fine now") is curator prose; nothing on the VO carries it,
        // and the label came from the type instead.
        assertThat( vo.getLabel() ).doesNotContain( "reprocessed" );
    }

    @Test
    public void anEventWithNoPerformerIsSummarizedAnyway() {
        AuditEvent event = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, null, null, null,
                new ExpressionExperimentUpdateFromGEOEvent() );

        DatasetUpdateSummaryValueObject vo = DatasetUpdateSummaryValueObject.forEvent( event );

        assertThat( vo.getLabel() ).isEqualTo( "Updated from GEO" );
        assertThat( vo.getPerformer() ).isNull();
    }
}
