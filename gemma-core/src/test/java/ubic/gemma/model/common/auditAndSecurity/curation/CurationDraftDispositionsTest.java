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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link CurationDraftDispositions}. No Spring, no DB
 * — just JSON-string -> Disposition map derivation.
 */
public class CurationDraftDispositionsTest {

    @Test
    public void retained_whenSnapshotAndPayloadEqual() {
        CurationDraft d = draft(
                "{\"factor:1:0\":{\"name\":\"sex\"},\"factor:1:1\":{\"name\":\"age\"}}",
                "{\"factor:1:0\":{\"name\":\"sex\"},\"factor:1:1\":{\"name\":\"age\"}}",
                null );
        Map<String, CurationDraftDispositions.Disposition> out =
                CurationDraftDispositions.derive( d );
        assertThat( out ).containsEntry( "factor:1:0", CurationDraftDispositions.Disposition.RETAINED );
        assertThat( out ).containsEntry( "factor:1:1", CurationDraftDispositions.Disposition.RETAINED );
    }

    @Test
    public void edited_whenSnapshotAndPayloadDiffer() {
        CurationDraft d = draft(
                "{\"factor:1:0\":{\"name\":\"sex\"}}",
                "{\"factor:1:0\":{\"name\":\"biological_sex\"}}",
                null );
        Map<String, CurationDraftDispositions.Disposition> out =
                CurationDraftDispositions.derive( d );
        assertThat( out ).containsEntry( "factor:1:0", CurationDraftDispositions.Disposition.EDITED );
    }

    @Test
    public void rejected_whenSnapshotHadKeyAndPayloadDoesNot() {
        CurationDraft d = draft(
                "{\"factor:1:0\":{\"name\":\"sex\"},\"factor:1:1\":{\"name\":\"age\"}}",
                "{\"factor:1:0\":{\"name\":\"sex\"}}",
                null );
        Map<String, CurationDraftDispositions.Disposition> out =
                CurationDraftDispositions.derive( d );
        assertThat( out ).containsEntry( "factor:1:1", CurationDraftDispositions.Disposition.REJECTED );
    }

    @Test
    public void parked_overridesDiff() {
        CurationDraft d = draft(
                "{\"factor:1:0\":{\"name\":\"sex\"}}",
                "{\"factor:1:0\":{\"name\":\"biological_sex\"}}",
                "[\"factor:1:0\"]" );
        Map<String, CurationDraftDispositions.Disposition> out =
                CurationDraftDispositions.derive( d );
        // Despite the snapshot/payload diff (which would normally be EDITED),
        // the parked flag wins.
        assertThat( out ).containsEntry( "factor:1:0", CurationDraftDispositions.Disposition.PARKED );
    }

    @Test
    public void newInPayloadOnly_isOmittedFromDispositionMap() {
        CurationDraft d = draft(
                "{\"factor:1:0\":{\"name\":\"sex\"}}",
                "{\"factor:1:0\":{\"name\":\"sex\"},\"tag:99\":\"curator-added\"}",
                null );
        Map<String, CurationDraftDispositions.Disposition> out =
                CurationDraftDispositions.derive( d );
        // factor:1:0 retained, tag:99 (curator added) not surfaced as a
        // disposition signal.
        assertThat( out ).containsOnlyKeys( "factor:1:0" );
    }

    @Test
    public void emptySnapshotAndPayload_stillSurfacesParkedKeys() {
        CurationDraft d = draft( null, null, "[\"factor:1:0\"]" );
        Map<String, CurationDraftDispositions.Disposition> out =
                CurationDraftDispositions.derive( d );
        assertThat( out ).containsEntry( "factor:1:0", CurationDraftDispositions.Disposition.PARKED );
    }

    @Test
    public void nullDraft_returnsEmptyMap() {
        assertThat( CurationDraftDispositions.derive( null ) ).isEmpty();
    }

    private CurationDraft draft( String snapshotJson, String payloadJson, String parkedJson ) {
        CurationDraft d = new CurationDraft();
        d.setProposalSnapshotJson( snapshotJson );
        d.setPayloadJson( payloadJson );
        d.setParkedElements( parkedJson );
        return d;
    }
}
