/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.ontology;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.eventType.AutomatedAnnotationEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.CharacteristicReadService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @see ObsoleteTermCorrectionService
 */
@Service
public class ObsoleteTermCorrectionServiceImpl implements ObsoleteTermCorrectionService {

    private static final Logger log = LoggerFactory.getLogger( ObsoleteTermCorrectionServiceImpl.class );

    /** Key under which the correction records itself in {@code Characteristic.supportingEvidence}. */
    private static final String PROVENANCE_KEY = "obsoleteTermCorrection";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OntologyService ontologyService;
    @Autowired
    private CharacteristicReadService characteristicReadService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;
    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    @Transactional
    public ObsoleteTermCorrectionResult apply( Collection<String> uris, boolean dryRun, long timeout, TimeUnit timeUnit )
            throws TimeoutException {
        ObsoleteTermCorrectionResult result = new ObsoleteTermCorrectionResult();
        result.setDryRun( dryRun );

        Set<String> requested = uris != null ? new LinkedHashSet<>( uris ) : Collections.emptySet();
        List<ObsoleteTermUsage> report = ontologyService.findObsoleteTermsInUse( timeout, timeUnit );

        Set<Long> allAffectedExperiments = new LinkedHashSet<>();
        for ( ObsoleteTermUsage usage : report ) {
            if ( !requested.isEmpty() && !requested.contains( usage.getUri() ) ) {
                continue;
            }
            if ( !usage.isAutoCorrectable() ) {
                if ( requested.contains( usage.getUri() ) ) {
                    // Only report a skip the caller asked about; the un-asked-for ones are simply not in scope.
                    result.getSkippedNotCorrectable().add( usage.getUri() + ": " + usage.getBlockedReason() );
                }
                continue;
            }
            // Deferred terms are skipped for a blanket run and honoured for an explicit one. "Not selected" is not
            // the same as "excluded": a run over everything must not quietly include them.
            if ( DEFERRED_URIS.contains( usage.getUri() ) && !requested.contains( usage.getUri() ) ) {
                result.getSkippedDeferred().add( usage.getUri() );
                continue;
            }
            result.getTerms().add( correctOne( usage, dryRun, allAffectedExperiments ) );
        }

        result.setExperimentsAffected( allAffectedExperiments.size() );
        result.setCharacteristicsRewritten( result.getTerms().stream()
                .mapToInt( ObsoleteTermCorrectionResult.TermCorrection::getCharacteristicsRewritten ).sum() );

        if ( !dryRun && !allAffectedExperiments.isEmpty() ) {
            result.setResync( resync( allAffectedExperiments ) );
        }
        log.info( "Obsolete-term correction {}: {} terms, {} characteristics, {} experiments.",
                dryRun ? "DRY RUN" : "applied", result.getTerms().size(),
                result.getCharacteristicsRewritten(), result.getExperimentsAffected() );
        return result;
    }

    private ObsoleteTermCorrectionResult.TermCorrection correctOne( ObsoleteTermUsage usage, boolean dryRun,
            Set<Long> allAffectedExperiments ) {
        ObsoleteTermCorrectionResult.TermCorrection tc = new ObsoleteTermCorrectionResult.TermCorrection();
        tc.setFromUri( usage.getUri() );
        tc.setFromLabel( usage.getStoredValue() );
        tc.setToUri( usage.getReplacedByUri() );
        tc.setToLabel( usage.getReplacedByLabel() );
        tc.setResolvedVia( usage.getResolvedVia() );

        // Read the experiments BEFORE rewriting: afterwards the old URI is gone and EE2C no longer points at it.
        Collection<Long> eeIds = characteristicReadService.findExperimentIdsByUriInAnySlot( usage.getUri() );
        tc.setExperimentsAffected( eeIds.size() );
        allAffectedExperiments.addAll( eeIds );

        String provenance = provenanceFor( usage );
        for ( Characteristic c : characteristicReadService.findByUriInAnySlot( usage.getUri() ) ) {
            boolean touched = false;
            if ( usage.getUri().equals( c.getCategoryUri() ) ) {
                if ( !dryRun ) {
                    c.setCategoryUri( usage.getReplacedByUri() );
                    c.setCategory( usage.getReplacedByLabel() );
                }
                tc.setInCategory( tc.getInCategory() + 1 );
                touched = true;
            }
            if ( usage.getUri().equals( c.getValueUri() ) ) {
                if ( !dryRun ) {
                    c.setValueUri( usage.getReplacedByUri() );
                    c.setValue( usage.getReplacedByLabel() );
                }
                tc.setInValue( tc.getInValue() + 1 );
                touched = true;
            }
            if ( c instanceof Statement ) {
                touched |= correctStatementSlots( ( Statement ) c, usage, dryRun, tc );
            }
            if ( touched ) {
                if ( !dryRun ) {
                    c.setSupportingEvidence( mergeProvenance( c.getSupportingEvidence(), provenance ) );
                }
                tc.setCharacteristicsRewritten( tc.getCharacteristicsRewritten() + 1 );
            }
        }
        return tc;
    }

    /**
     * A Statement carries the term in up to four further slots. Rewriting only the subject would leave the same
     * dead URI standing in the object columns, which is the failure this whole exercise exists to find.
     */
    private boolean correctStatementSlots( Statement s, ObsoleteTermUsage usage, boolean dryRun,
            ObsoleteTermCorrectionResult.TermCorrection tc ) {
        boolean touched = false;
        String from = usage.getUri(), to = usage.getReplacedByUri(), label = usage.getReplacedByLabel();
        if ( from.equals( s.getPredicateUri() ) ) {
            if ( !dryRun ) {
                s.setPredicateUri( to );
                s.setPredicate( label );
            }
            tc.setInPredicate( tc.getInPredicate() + 1 );
            touched = true;
        }
        if ( from.equals( s.getSecondPredicateUri() ) ) {
            if ( !dryRun ) {
                s.setSecondPredicateUri( to );
                s.setSecondPredicate( label );
            }
            tc.setInPredicate( tc.getInPredicate() + 1 );
            touched = true;
        }
        if ( from.equals( s.getObjectUri() ) ) {
            if ( !dryRun ) {
                s.setObjectUri( to );
                s.setObject( label );
            }
            tc.setInObject( tc.getInObject() + 1 );
            touched = true;
        }
        if ( from.equals( s.getSecondObjectUri() ) ) {
            if ( !dryRun ) {
                s.setSecondObjectUri( to );
                s.setSecondObject( label );
            }
            tc.setInObject( tc.getInObject() + 1 );
            touched = true;
        }
        return touched;
    }

    /**
     * The record a later reader needs to tell a DERIVED correction from a curator's decision. {@code assertedBy}
     * is the point of it: without that field this is indistinguishable from someone having retyped the annotation.
     */
    private String provenanceFor( ObsoleteTermUsage usage ) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put( "from", usage.getUri() );
        n.put( "fromLabel", usage.getStoredValue() );
        n.put( "to", usage.getReplacedByUri() );
        n.put( "toLabel", usage.getReplacedByLabel() );
        n.put( "assertedBy", usage.getResolvedVia() );
        // Which release made the claim. A correction is only as checkable as the version that asserted it: without
        // this, a reader who finds the successor has since been retired cannot tell whether we acted on stale data.
        n.put( "ontologyVersion", versionOf( usage.getUri() ) );
        n.put( "appliedAt", Instant.now().toString() );
        return n.toString();
    }

    private String versionOf( String uri ) {
        try {
            return ontologyService.getVersion( uri, 10, TimeUnit.SECONDS );
        } catch ( TimeoutException | RuntimeException e ) {
            // Not worth failing a correction over; the rest of the record still identifies what was asserted.
            log.debug( "Could not read the ontology version for {}.", uri, e );
            return null;
        }
    }

    /**
     * Add our record to whatever is already there rather than over it — the field is shared and a curator's
     * evidence must survive. Anything unparseable is preserved verbatim under {@code _previous}.
     */
    private String mergeProvenance( String existing, String provenanceJson ) {
        ObjectNode root;
        try {
            JsonNode parsed = StringUtils.isBlank( existing ) ? null : objectMapper.readTree( existing );
            if ( parsed instanceof ObjectNode ) {
                root = ( ObjectNode ) parsed;
            } else {
                root = objectMapper.createObjectNode();
                if ( parsed != null ) {
                    root.set( "_previous", parsed );
                }
            }
        } catch ( Exception e ) {
            root = objectMapper.createObjectNode();
            root.put( "_previous", existing );
        }
        try {
            root.set( PROVENANCE_KEY, objectMapper.readTree( provenanceJson ) );
        } catch ( Exception e ) {
            throw new IllegalStateException( "Could not build correction provenance.", e );
        }
        return root.toString();
    }

    /**
     * Rebuild the denormalizations for the experiments we touched. ANNOTATION_RELATION is derived from the curated
     * statements EE2C carries, so it must run after EE2C rather than beside it.
     * <p>
     * A failure here leaves the rewrite standing and is reported rather than thrown: the annotations are already
     * correct, and EE2C is rebuilt nightly, so the worst case is a stale denormalization until then.
     */
    private ObsoleteTermCorrectionResult.Resync resync( Set<Long> experimentIds ) {
        ObsoleteTermCorrectionResult.Resync r = new ObsoleteTermCorrectionResult.Resync();
        for ( Long id : experimentIds ) {
            try {
                ExpressionExperiment ee = expressionExperimentService.load( id );
                if ( ee == null ) {
                    r.getResyncFailures().add( id + ": no such experiment" );
                    continue;
                }
                r.setEe2cRowsWritten( r.getEe2cRowsWritten()
                        + tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( ee, null ) );
                r.setAnnotationRelationRowsWritten( r.getAnnotationRelationRowsWritten()
                        + tableMaintenanceUtil.updateAnnotationRelationEntries( ee ) );
                // One event per experiment, not per characteristic: any audit event bumps
                // curationDetails.lastUpdated (which 409s in-flight drafts), and AutomatedAnnotationEvent is a type
                // Gemma 1.32.7 can load, so this does not break the 1.0 side of the shared production database.
                auditTrailService.addUpdateEvent( ee, AutomatedAnnotationEvent.class,
                        "Obsolete ontology terms corrected to the successors their ontologies assert." );
                r.setExperimentsResynced( r.getExperimentsResynced() + 1 );
            } catch ( Exception e ) {
                log.warn( "Resync failed for experiment {}; the annotation rewrite stands.", id, e );
                r.getResyncFailures().add( id + ": " + e.getClass().getSimpleName() + ": " + e.getMessage() );
            }
        }
        return r;
    }
}
