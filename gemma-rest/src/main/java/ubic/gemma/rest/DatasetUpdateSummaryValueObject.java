/*
 * The Gemma project
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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One line of "what changed here recently" for a dataset: a short label, the event class it came
 * from, when, and by whom.
 * <p>
 * Carried as {@code lastUpdate} on {@link PipelineStatusValueObject}, so a list view gets it in the
 * same round-trip as the rest of the status strip. It answers what {@code curationDetails.lastUpdated}
 * cannot — that field is a bare timestamp — without making the caller pull the whole trail from
 * {@code GET /datasets/{id}/auditEvents}.
 * <p>
 * 🛑 The label is derived from the {@link AuditEventType} SUBCLASS, never from the event's note.
 * Notes are curator prose ("rechecked, seems fine"), vary by emitter, and are not a contract; the
 * type is. {@link #eventType} is the stable machine-readable half — a client that wants to key off
 * something should key off that, not off the label text.
 */
@Getter
@Setter
@Schema(description = "Short human-readable summary of the most recent recorded change to a dataset.")
public class DatasetUpdateSummaryValueObject {

    /**
     * Label used when the dataset's trail holds no typed event at all and we fall back to its
     * creation row.
     * <p>
     * 🛑 Deliberately NOT "Made public" or "First made public". {@code AUDIT_EVENT.action='C'}
     * means the dataset was loaded into Gemma; it says nothing about visibility, and the two are
     * routinely months apart.
     */
    static final String LABEL_CREATED = "Added to Gemma";

    /**
     * Last resort: a typed event whose class is neither mapped below nor descended from anything
     * mapped below, and whose simple name is somehow empty. Should be unreachable.
     */
    static final String LABEL_UNKNOWN = "Updated";

    @Schema(description = "Short human-readable label, e.g. \"Updated from GEO\". Display text — do not key logic off it.")
    private String label;

    /**
     * Simple class name of the audit event type ({@code ExpressionExperimentUpdateFromGEOEvent},
     * {@code DifferentialExpressionAnalysisEvent}, …). {@code null} when the summary came from the
     * creation row, which carries no event type.
     */
    @Nullable
    @Schema(description = "Simple class name of the audit event type; null when the summary is the dataset's creation row.")
    private String eventType;

    @Schema(description = "When the change was recorded.")
    private Date date;

    /**
     * Username of whoever performed the change, or {@code null} when the row has no performer —
     * which is what an unattended batch job leaves behind.
     */
    @Nullable
    @Schema(description = "Username that performed the change; null for rows with no recorded performer.")
    private String performer;

    public DatasetUpdateSummaryValueObject() {
    }

    private DatasetUpdateSummaryValueObject( String label, @Nullable String eventType, Date date, @Nullable String performer ) {
        this.label = label;
        this.eventType = eventType;
        this.date = date;
        this.performer = performer;
    }

    /**
     * Build a summary from the dataset's most recent typed audit event.
     */
    public static DatasetUpdateSummaryValueObject forEvent( AuditEvent event ) {
        AuditEventType type = event.getEventType();
        if ( type == null ) {
            // The batched last-typed-event query inner-joins eventType, so this is only reachable
            // if a caller hands us a row from somewhere else.
            return forCreation( event );
        }
        Class<?> clazz = type.getClass();
        return new DatasetUpdateSummaryValueObject( labelFor( clazz ), clazz.getSimpleName(),
                event.getDate(), performerOf( event ) );
    }

    /**
     * Build a summary from the dataset's creation row, for a dataset that has never received a
     * typed event.
     */
    public static DatasetUpdateSummaryValueObject forCreation( AuditEvent event ) {
        return new DatasetUpdateSummaryValueObject( LABEL_CREATED, null, event.getDate(), performerOf( event ) );
    }

    @Nullable
    private static String performerOf( AuditEvent event ) {
        return event.getPerformer() != null ? event.getPerformer().getUserName() : null;
    }

    /**
     * Labels for the event classes a dataset's trail can actually carry.
     * <p>
     * Only classes whose name does NOT already de-camel into something a reader would accept get an
     * entry — {@code TicketOpenedEvent} reads fine as "Ticket opened" straight out of
     * {@link #derivedLabel}, so it isn't here. Entries exist where the derived form would be jargon
     * ({@code BioMaterialMappingUpdate}), where it would bury the verb
     * ({@code ExpressionExperimentUpdateFromGEOEvent} → "Expression experiment update from GEO"),
     * or where the class name understates what happened.
     * <p>
     * 🛑 Every {@code Failed*} analysis class is listed explicitly even where the derived form
     * would do, because they do NOT extend their success counterpart — they extend
     * {@link NeedsAttentionEvent}. Left to the superclass walk, a failed PCA run would render
     * "Flagged as needing attention", which is true of the side effect and silent about the thing
     * that actually happened.
     * <p>
     * The {@code ArrayDesign*} classes are absent on purpose: they attach to platform trails, never
     * to a dataset's, so listing them here would be guessing at wording nobody will ever read.
     */
    private static final Map<Class<? extends AuditEventType>, String> LABELS = new LinkedHashMap<>();

    static {
        // Loading and provenance
        LABELS.put( ExpressionExperimentUpdateFromGEOEvent.class, "Updated from GEO" );
        LABELS.put( PreboardedCreatedEvent.class, "Preboarded" );
        LABELS.put( PreboardedPromotedEvent.class, "Promoted from preboarding" );
        LABELS.put( DatasetShortNameChangedEvent.class, "Renamed" );
        LABELS.put( ReleaseDetailsUpdateEvent.class, "Release details updated" );

        // Expression data
        LABELS.put( AggregatedSingleDataAddedEvent.class, "Aggregated single-cell data added" );
        LABELS.put( SingleCellDataAddedEvent.class, "Single-cell data added" );
        LABELS.put( RawDataAddedEvent.class, "Raw data added" );
        LABELS.put( DataAddedEvent.class, "Data added" );
        LABELS.put( SingleCellDataRemovedEvent.class, "Single-cell data removed" );
        LABELS.put( RawDataRemovedEvent.class, "Raw data removed" );
        LABELS.put( DataRemovedEvent.class, "Data removed" );
        LABELS.put( FailedDataReplacedEvent.class, "Data replacement failed" );
        LABELS.put( SingleCellDataReplacedEvent.class, "Single-cell data replaced" );
        LABELS.put( RawDataReplacedEvent.class, "Raw data replaced" );
        LABELS.put( DataReplacedEvent.class, "Data replaced" );
        LABELS.put( PreferredSingleCellDataChangedEvent.class, "Preferred single-cell data changed" );
        LABELS.put( PreferredRawDataChangedEvent.class, "Preferred raw data changed" );
        LABELS.put( PreferredDataChangedEvent.class, "Preferred data changed" );
        LABELS.put( ExpressionExperimentVectorMergeEvent.class, "Vectors merged" );
        LABELS.put( ExpressionExperimentPlatformSwitchEvent.class, "Platform switched" );

        // Preprocessing and analysis
        LABELS.put( RankComputationEvent.class, "Expression ranks computed" );
        LABELS.put( VectorsReorderedEvent.class, "Vectors reordered" );
        LABELS.put( FailedProcessedVectorComputationEvent.class, "Preprocessing failed" );
        LABELS.put( ProcessedVectorComputationEvent.class, "Preprocessed" );
        LABELS.put( FailedDifferentialExpressionAnalysisEvent.class, "Differential expression analysis failed" );
        LABELS.put( DifferentialExpressionAnalysisEvent.class, "Differential expression analysis performed" );
        LABELS.put( FailedPCAAnalysisEvent.class, "PCA failed" );
        LABELS.put( PCAAnalysisEvent.class, "PCA performed" );
        LABELS.put( FailedSampleCorrelationAnalysisEvent.class, "Sample correlation failed" );
        LABELS.put( SampleCorrelationAnalysisEvent.class, "Sample correlation computed" );
        LABELS.put( FailedMeanVarianceUpdateEvent.class, "Mean-variance computation failed" );
        LABELS.put( MeanVarianceUpdateEvent.class, "Mean-variance computed" );
        LABELS.put( FailedMissingValueAnalysisEvent.class, "Missing-value analysis failed" );
        LABELS.put( MissingValueAnalysisEvent.class, "Missing values computed" );
        LABELS.put( TooSmallDatasetLinkAnalysisEvent.class, "Too small for coexpression analysis" );
        LABELS.put( FailedLinkAnalysisEvent.class, "Coexpression analysis failed" );
        LABELS.put( LinkAnalysisEvent.class, "Coexpression analysis performed" );
        LABELS.put( OutlierFoundAnalysisEvent.class, "Outliers found" );
        LABELS.put( OutliersNotFoundAnalysisEvent.class, "No outliers found" );
        LABELS.put( OutlierAnalysisEvent.class, "Outlier analysis performed" );
        LABELS.put( FailedPipelineRunEvent.class, "Pipeline run failed" );
        LABELS.put( PipelineRunEvent.class, "Pipeline run" );
        LABELS.put( ExpressionExperimentAnalysisEvent.class, "Analysis updated" );

        // Batch information
        LABELS.put( BatchCorrectionEvent.class, "Batch corrected" );
        LABELS.put( SingletonBatchInvalidEvent.class, "Batch information invalid" );
        LABELS.put( UninformativeFASTQHeadersForBatchingEvent.class, "Batch information uninformative" );
        LABELS.put( FailedBatchInformationFetchingEvent.class, "Batch information fetch failed" );
        LABELS.put( SingleBatchDeterminationEvent.class, "Single batch determined" );
        LABELS.put( BatchInformationFetchingEvent.class, "Batch information fetched" );
        LABELS.put( FailedBatchInformationMissingEvent.class, "Batch information missing" );
        LABELS.put( BatchInformationMissingEvent.class, "Batch information missing" );
        LABELS.put( BatchInformationEvent.class, "Batch information updated" );
        LABELS.put( BatchProblemsUpdateEvent.class, "Batch problems updated" );

        // Samples and design
        LABELS.put( SampleRemovalReversionEvent.class, "Sample removal reverted" );
        LABELS.put( SampleRemovalEvent.class, "Sample removed" );
        LABELS.put( BioMaterialMappingUpdate.class, "Sample mapping updated" );
        LABELS.put( DesignChangeEvent.class, "Experimental design changed" );
        LABELS.put( ExperimentalDesignUpdatedEvent.class, "Experimental design updated" );

        // Single-cell metadata
        LABELS.put( SingleCellSubSetsCreatedEvent.class, "Single-cell subsets created" );
        LABELS.put( PreferredCellTypeAssignmentChangedEvent.class, "Preferred cell type assignment changed" );
        LABELS.put( CellTypeAssignmentAddedEvent.class, "Cell type assignment added" );
        LABELS.put( CellTypeAssignmentRemovedEvent.class, "Cell type assignment removed" );
        LABELS.put( CellTypeAssignmentEvent.class, "Cell type assignment updated" );
        LABELS.put( CellLevelCharacteristicsAddedEvent.class, "Cell-level characteristics added" );
        LABELS.put( CellLevelCharacteristicsRemovedEvent.class, "Cell-level characteristics removed" );
        LABELS.put( CellLevelCharacteristicsEvent.class, "Cell-level characteristics updated" );

        // Annotation. TagAddedEvent / TagRemovedEvent are listed even though their derived form is
        // already right, because the superclass walk would otherwise reach AnnotationEvent first
        // and flatten both to "Annotations updated".
        LABELS.put( TagAddedEvent.class, "Tag added" );
        LABELS.put( TagRemovedEvent.class, "Tag removed" );
        LABELS.put( AutomatedAnnotationEvent.class, "Annotated automatically" );
        LABELS.put( ManualAnnotationEvent.class, "Annotated by a curator" );
        LABELS.put( AnnotationEvent.class, "Annotations updated" );
        LABELS.put( AnnotationSetEvent.class, "Annotation set attached" );

        // Curation status
        LABELS.put( CurationNoteUpdateEvent.class, "Curation note updated" );
        LABELS.put( FactorValueNeedsAttentionEvent.class, "Factor value flagged for attention" );
        LABELS.put( NeedsAttentionEvent.class, "Flagged as needing attention" );
        LABELS.put( DoesNotNeedAttentionEvent.class, "Cleared as not needing attention" );
        LABELS.put( NeedsAttentionAlteringEvent.class, "Attention flag changed" );
        LABELS.put( TroubledStatusFlagEvent.class, "Flagged as troubled" );
        LABELS.put( NotTroubledStatusFlagEvent.class, "Trouble flag cleared" );
        LABELS.put( TroubledStatusFlagAlteringEvent.class, "Trouble flag changed" );
        LABELS.put( CurationDetailsEvent.class, "Curation details updated" );
        LABELS.put( CommentedEvent.class, "Comment added" );
        LABELS.put( GeeqEvent.class, "Quality score computed" );

        // Suitability for analysis
        LABELS.put( ResetSuitabilityForDifferentialExpressionAnalysisEvent.class,
                "Differential expression suitability reset" );
        LABELS.put( UnsuitableForDifferentialExpressionAnalysisEvent.class,
                "Marked unsuitable for differential expression" );
        LABELS.put( DifferentialExpressionSuitabilityEvent.class,
                "Differential expression suitability changed" );
        LABELS.put( AnalysisSuitabilityEvent.class, "Analysis suitability changed" );

        // Visibility. MakePublicEvent and DatasetPublishedEvent have live emitters
        // (DatasetsWebService.makeDatasetPublic / publishDataset) but had fired zero times across
        // 200 public datasets and ~5,000 audit events sampled on production in August 2026, so
        // expect these two to render for nothing. They are here so that the day one does fire it
        // is not labelled by class name.
        LABELS.put( MakePublicEvent.class, "Made public" );
        LABELS.put( MakePrivateEvent.class, "Made private" );
        LABELS.put( DatasetPublishedEvent.class, "Published" );
        LABELS.put( PermissionChangeEvent.class, "Permissions changed" );
    }

    /**
     * Resolve a label for an audit event class.
     * <p>
     * Exact match first, then up the superclass chain, so a subclass added later inherits its
     * parent's wording instead of falling off the map. Anything still unmatched at
     * {@link AuditEventType} gets {@link #derivedLabel}, which restates the class name — dull, but
     * it cannot claim something that did not happen.
     */
    static String labelFor( Class<?> eventTypeClass ) {
        for ( Class<?> c = eventTypeClass; c != null && AuditEventType.class.isAssignableFrom( c ); c = c.getSuperclass() ) {
            String label = LABELS.get( c );
            if ( label != null ) {
                return label;
            }
        }
        return derivedLabel( eventTypeClass.getSimpleName() );
    }

    /**
     * Turn {@code UninformativeFASTQHeadersForBatchingEvent} into
     * "Uninformative FASTQ headers for batching": split on camel-case boundaries, drop a trailing
     * "Event", keep all-capital runs (PCA, GEO, FASTQ) as they are, and lower-case the rest after
     * the first word.
     */
    static String derivedLabel( String simpleName ) {
        String[] words = simpleName.split( "(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])" );
        int end = words.length;
        if ( end > 1 && "Event".equals( words[end - 1] ) ) {
            end--;
        }
        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < end; i++ ) {
            String w = words[i];
            if ( w.isEmpty() ) {
                continue;
            }
            if ( sb.length() > 0 ) {
                sb.append( ' ' );
                // An all-capital run is an acronym (PCA, GEO, FASTQ); anything else is prose.
                sb.append( w.equals( w.toUpperCase() ) && w.length() > 1 ? w : w.toLowerCase() );
            } else {
                sb.append( Character.toUpperCase( w.charAt( 0 ) ) ).append( w.substring( 1 ) );
            }
        }
        return sb.length() > 0 ? sb.toString() : LABEL_UNKNOWN;
    }
}
