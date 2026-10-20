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
package ubic.gemma.model.common.auditAndSecurity.curation;

import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link CurationDraftDispositions}. No Spring, no DB —
 * just JSON-string to Disposition-map derivation.
 * <p>
 * Restored alongside the class in this change; the pre-{@code fa4363e24f}
 * version built a {@code CurationDraft} with an embedded
 * {@code proposalSnapshotJson}. The baseline is now the draft's
 * {@link AnnotationSet#getParent() parent} PROPOSAL, so both the entity
 * overload and the string overload are exercised.
 */
public class CurationDraftDispositionsTest {

    private static final CurationDraftDispositions.Disposition RETAINED =
            CurationDraftDispositions.Disposition.RETAINED;
    private static final CurationDraftDispositions.Disposition EDITED =
            CurationDraftDispositions.Disposition.EDITED;
    private static final CurationDraftDispositions.Disposition REJECTED =
            CurationDraftDispositions.Disposition.REJECTED;
    private static final CurationDraftDispositions.Disposition PARKED =
            CurationDraftDispositions.Disposition.PARKED;

    @Test
    public void retained_whenProposalAndDraftEqual() {
        Map<String, CurationDraftDispositions.Disposition> out = CurationDraftDispositions.derive( draft(
                "{\"factor:1:0\":{\"name\":\"sex\"},\"factor:1:1\":{\"name\":\"age\"}}",
                "{\"factor:1:0\":{\"name\":\"sex\"},\"factor:1:1\":{\"name\":\"age\"}}",
                null ) );
        assertThat( out ).containsEntry( "factor:1:0", RETAINED );
        assertThat( out ).containsEntry( "factor:1:1", RETAINED );
    }

    @Test
    public void edited_whenProposalAndDraftDiffer() {
        Map<String, CurationDraftDispositions.Disposition> out = CurationDraftDispositions.derive( draft(
                "{\"factor:1:0\":{\"name\":\"sex\"}}",
                "{\"factor:1:0\":{\"name\":\"biological_sex\"}}",
                null ) );
        assertThat( out ).containsEntry( "factor:1:0", EDITED );
    }

    @Test
    public void rejected_whenProposalHadKeyAndDraftDoesNot() {
        Map<String, CurationDraftDispositions.Disposition> out = CurationDraftDispositions.derive( draft(
                "{\"factor:1:0\":{\"name\":\"sex\"},\"factor:1:1\":{\"name\":\"age\"}}",
                "{\"factor:1:0\":{\"name\":\"sex\"}}",
                null ) );
        assertThat( out ).containsEntry( "factor:1:1", REJECTED );
    }

    @Test
    public void parked_overridesDiff() {
        Map<String, CurationDraftDispositions.Disposition> out = CurationDraftDispositions.derive( draft(
                "{\"factor:1:0\":{\"name\":\"sex\"}}",
                "{\"factor:1:0\":{\"name\":\"biological_sex\"}}",
                "[\"factor:1:0\"]" ) );
        // The diff alone would say EDITED; parking wins, because "I have not
        // decided yet" and "I changed it" are different answers.
        assertThat( out ).containsEntry( "factor:1:0", PARKED );
    }

    @Test
    public void newInDraftOnly_isOmittedFromDispositionMap() {
        Map<String, CurationDraftDispositions.Disposition> out = CurationDraftDispositions.derive( draft(
                "{\"factor:1:0\":{\"name\":\"sex\"}}",
                "{\"factor:1:0\":{\"name\":\"sex\"},\"tag:99\":\"curator-added\"}",
                null ) );
        assertThat( out ).containsOnlyKeys( "factor:1:0" );
    }

    @Test
    public void emptyBothSides_stillSurfacesParkedKeys() {
        Map<String, CurationDraftDispositions.Disposition> out =
                CurationDraftDispositions.derive( draft( null, null, "[\"factor:1:0\"]" ) );
        assertThat( out ).containsEntry( "factor:1:0", PARKED );
    }

    @Test
    public void parkedElementAbsentFromBothSides_isStillListed() {
        Map<String, CurationDraftDispositions.Disposition> out = CurationDraftDispositions.derive( draft(
                "{\"factor:1:0\":{\"name\":\"sex\"}}",
                "{\"factor:1:0\":{\"name\":\"sex\"}}",
                "[\"tag:gone\"]" ) );
        // Parked, then deleted from both views: shown as parked rather than
        // silently vanishing from the curator's list.
        assertThat( out ).containsEntry( "tag:gone", PARKED );
    }

    @Test
    public void nullDraft_returnsEmptyMap() {
        assertThat( CurationDraftDispositions.derive( ( AnnotationSet ) null ) ).isEmpty();
    }

    /**
     * A draft with no parent has no diff baseline: every element reads as new,
     * so nothing but parked keys can be reported. Distinguishes "seeded from a
     * proposal and the curator kept everything" from "written from scratch".
     */
    @Test
    public void draftWithNoParent_reportsOnlyParked() {
        AnnotationSet d = new AnnotationSet();
        d.setRole( AnnotationSetRole.DRAFT );
        d.setPayloadJson( "{\"factor:1:0\":{\"name\":\"sex\"}}" );
        d.setParkedElements( "[\"tag:7\"]" );
        Map<String, CurationDraftDispositions.Disposition> out = CurationDraftDispositions.derive( d );
        assertThat( out ).containsOnlyKeys( "tag:7" );
        assertThat( out ).containsEntry( "tag:7", PARKED );
    }

    /**
     * Key order must not decide the verdict — the payloads are objects, and
     * two objects with the same members are the same content.
     */
    @Test
    public void keyOrderDoesNotMakeAnEdit() {
        Map<String, CurationDraftDispositions.Disposition> out = CurationDraftDispositions.derive(
                "{\"factor:1:0\":{\"name\":\"sex\",\"category\":\"biological sex\"}}",
                "{\"factor:1:0\":{\"category\":\"biological sex\",\"name\":\"sex\"}}",
                null );
        assertThat( out ).containsEntry( "factor:1:0", RETAINED );
    }

    /**
     * Unparseable JSON degrades to "no baseline" rather than throwing: a
     * corrupt payload must not take down the read of every other element.
     */
    @Test
    public void malformedJsonIsTreatedAsAbsent() {
        Map<String, CurationDraftDispositions.Disposition> out = CurationDraftDispositions.derive(
                "{not json", "{\"factor:1:0\":{\"name\":\"sex\"}}", null );
        assertThat( out ).isEmpty();
    }

    private AnnotationSet draft( @Nullable String proposalPayloadJson,
            @Nullable String draftPayloadJson, @Nullable String parkedJson ) {
        AnnotationSet proposal = new AnnotationSet();
        proposal.setRole( AnnotationSetRole.PROPOSAL );
        proposal.setPayloadJson( proposalPayloadJson );

        AnnotationSet d = new AnnotationSet();
        d.setRole( AnnotationSetRole.DRAFT );
        d.setPayloadJson( draftPayloadJson );
        d.setParkedElements( parkedJson );
        d.setParent( proposal );
        return d;
    }
}
