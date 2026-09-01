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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.persistence.service.common.description.PublicationAssertion;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Resolved, section-scoped inputs for one all-or-none curation commit
 * ({@link ExpressionExperimentService#commitCuration}). Phase 1 covers the two dataset-level sections that
 * reuse existing {@code ExpressionExperimentService} writes cleanly — basics (name / description / short
 * name) and publications; design / tags / sample characteristics land in later phases.
 * <p>
 * Publication identifiers are resolved to persistent {@link BibliographicReference}s by the web layer
 * <em>before</em> the transaction opens, so the (possibly slow, network-bound) PubMed/CrossRef fetch does
 * not hold the commit transaction open. Each arrives as a {@link PublicationAssertion} — the reference plus
 * the basis given for attaching it — because the basis is what a restore has to put back along with the link.
 */
public class CurationCommitRequest {

    /**
     * When true and this is not a dry run, committing advances the open CURATION / SCREENING ticket
     * targets for this dataset to DONE and resolves a ticket whose last open target this closes. Set by
     * the commit and sign routes, left false by restore and preflight — a restore reverts curation and
     * must not close the ticket that asked for it.
     */
    private boolean advanceLinkedTickets = false;

    public boolean isAdvanceLinkedTickets() {
        return advanceLinkedTickets;
    }

    public void setAdvanceLinkedTickets( boolean advanceLinkedTickets ) {
        this.advanceLinkedTickets = advanceLinkedTickets;
    }

    /**
     * The dataset {@code lastUpdated} the draft was built against (optimistic-concurrency token), or
     * {@code null} to skip the check. Commit is rejected when the dataset moved since.
     */
    @Nullable
    private Date expectedLastUpdated;

    // ── basics (null field = leave unchanged; whole section may be absent) ──
    private boolean basicsPresent;
    @Nullable
    private String name;
    @Nullable
    private String description;
    @Nullable
    private String shortName;
    /** Whether the caller may change the short name (admin-only). A short-name change without this is denied. */
    private boolean shortNameChangeAllowed;

    // ── publications (set-replace, identifier-resolved) ──
    private boolean publicationsPresent;
    @Nullable
    private PublicationAssertion primaryPublication;
    private List<PublicationAssertion> otherRelevantPublications = Collections.emptyList();

    // ── design (factors → factor-values → statements) ──
    // The web layer maps CAB's declared-delete DesignCommit onto a COMPLETE ExperimentalDesignValueObject
    // (carry-forward untouched entities + delta) so the existing replace-by-absence apply path yields CAB's
    // semantics; {@code designPlan} carries the clientRef ledgers + deferred new-FV assignments the service
    // needs to correlate clientRef → new id and wire assignments to freshly-created factor values.
    private boolean designPresent;
    @Nullable
    private ExperimentalDesignValueObject proposedDesign;
    @Nullable
    private DesignCommitPlan designPlan;
    /** Curator "split this experiment along factor X" advice — stored in the curation note (no structured home yet). */
    @Nullable
    private Long splitOnFactorId;
    @Nullable
    private String splitRationale;

    // ── experiment-level tags (id-based: add clientRef items, remove deletedIds, keep gemmaId items) ──
    /**
     * Why this commit was made, in the curator's or agent's own words. Appended to the audit-event note
     * of every annotation this commit adds or removes, after the server's own mechanical description.
     * <p>
     * 🛑 A DELETION is the reason this exists. An addition justifies itself — {@code supportingEvidence}
     * on the annotation says where the claim came from — but a deletion ends with no annotation to hang
     * evidence off, so before this the record said what went and never why. Per commit rather than per
     * change: Paul's ruling of 2026-09-01, and enough for a caller that already commits one dataset at
     * a time. It does NOT go near {@code curationDetails.curationNote}, which is dataset-scoped and
     * overwrites.
     */
    @Nullable
    private String reason;

    /**
     * An optional short key for {@link #reason}, recorded verbatim and never interpreted.
     * <p>
     * It exists so that reasons written by different callers can be GROUPED by a later query instead of
     * grepped. That is all it does.
     * <p>
     * 🛑 There is deliberately NO vocabulary for it — not here, and not by reference to one kept
     * elsewhere. Gemma does not define the keys, does not validate them, and does not have a list to
     * drift out of date. Any list named in a review thread is a proposal by its author, not a contract
     * with this field.
     * <p>
     * 🛑 In particular this is NOT the audit-finding dismissal vocabulary. Dismissing a proposed finding
     * ("the agent's evidence doesn't support it", "outside this pass") and deleting a tag a curator
     * previously asserted are different acts with different reasons, and the dismissal keys are written
     * about findings throughout. Reusing them here would file two unlike decisions under one name.
     * <p>
     * Leaving the key free-form is the point: what a good vocabulary for tag deletion looks like is not
     * yet known, and a fixed one chosen now would be a fixed one to live with. Let the keys that
     * callers actually write show what the categories are, and constrain later if it is ever worth it.
     */
    @Nullable
    private String reasonCode;

    private boolean tagsPresent;
    private List<TagAdd> tagsToAdd = new ArrayList<>();
    private List<Long> tagsToDelete = new ArrayList<>();
    private int tagsUnchanged;

    // ── per-sample characteristics (same id-based shape, resolved to a biomaterial by GSM short name) ──
    private boolean sampleCharsPresent;
    private List<SampleCharacteristicAdd> sampleCharsToAdd = new ArrayList<>();
    private List<Long> sampleCharsToDelete = new ArrayList<>();
    private int sampleCharsUnchanged;

    // ── curationDetails (only the free-text note commits here; troubled/needsAttention route through tickets) ──
    private boolean curationDetailsPresent;
    @Nullable
    private String curationDetailsNote;

    // ── run provenance: which agent run is applying this commit ──
    // Both null for an ordinary curator commit, which mints no AnnotationSet at all. Provenance is expected to be
    // sparse — Paul: "I don't expect this to be populated by default" — so absence here means "no run was named",
    // never "no run happened".
    @Nullable
    private String runId;
    @Nullable
    private AnnotationSetService.RunProvenance runProvenance;
    /** The PROPOSAL this commit is applying, if any; becomes the COMMIT row's parent. */
    @Nullable
    private AnnotationSet runParentProposal;

    // ── auto-snapshot: the dataset's curation as it stood just before this commit ──
    // Serialized by the web layer, because the CurationDocument shape lives there; minted here so the row shares
    // the commit's transaction and cannot outlive a rollback.
    @Nullable
    private String snapshotPayloadJson;
    @Nullable
    private String snapshotCreatedBy;

    /** A tag to create, paired with the document {@code clientRef} so the report can echo its new id. */
    public static class TagAdd {
        private final String clientRef;
        private final Characteristic characteristic;

        public TagAdd( String clientRef, Characteristic characteristic ) {
            this.clientRef = clientRef;
            this.characteristic = characteristic;
        }

        public String getClientRef() {
            return clientRef;
        }

        public Characteristic getCharacteristic() {
            return characteristic;
        }
    }

    /** A per-sample characteristic to create: {@code clientRef}, the resolved biomaterial id, and the tag. */
    public static class SampleCharacteristicAdd {
        private final String clientRef;
        private final Long bioMaterialId;
        private final Characteristic characteristic;

        public SampleCharacteristicAdd( String clientRef, Long bioMaterialId, Characteristic characteristic ) {
            this.clientRef = clientRef;
            this.bioMaterialId = bioMaterialId;
            this.characteristic = characteristic;
        }

        public String getClientRef() {
            return clientRef;
        }

        public Long getBioMaterialId() {
            return bioMaterialId;
        }

        public Characteristic getCharacteristic() {
            return characteristic;
        }
    }

    @Nullable
    public Date getExpectedLastUpdated() {
        return expectedLastUpdated;
    }

    public void setExpectedLastUpdated( @Nullable Date expectedLastUpdated ) {
        this.expectedLastUpdated = expectedLastUpdated;
    }

    public boolean isBasicsPresent() {
        return basicsPresent;
    }

    public void setBasicsPresent( boolean basicsPresent ) {
        this.basicsPresent = basicsPresent;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName( @Nullable String name ) {
        this.name = name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription( @Nullable String description ) {
        this.description = description;
    }

    @Nullable
    public String getShortName() {
        return shortName;
    }

    public void setShortName( @Nullable String shortName ) {
        this.shortName = shortName;
    }

    public boolean isShortNameChangeAllowed() {
        return shortNameChangeAllowed;
    }

    public void setShortNameChangeAllowed( boolean shortNameChangeAllowed ) {
        this.shortNameChangeAllowed = shortNameChangeAllowed;
    }

    public boolean isPublicationsPresent() {
        return publicationsPresent;
    }

    public void setPublicationsPresent( boolean publicationsPresent ) {
        this.publicationsPresent = publicationsPresent;
    }

    /**
     * The paper to attach as primary, with the basis for attaching it. The basis rides along rather than being
     * flattened away here: a commit that names evidence has it recorded, and a snapshot replayed as a restore
     * puts a re-attached paper back with the reason it was attached rather than as an unexplained claim.
     */
    @Nullable
    public PublicationAssertion getPrimaryPublication() {
        return primaryPublication;
    }

    public void setPrimaryPublication( @Nullable PublicationAssertion primaryPublication ) {
        this.primaryPublication = primaryPublication;
    }

    public List<PublicationAssertion> getOtherRelevantPublications() {
        return otherRelevantPublications;
    }

    public void setOtherRelevantPublications( List<PublicationAssertion> otherRelevantPublications ) {
        this.otherRelevantPublications = otherRelevantPublications;
    }

    public boolean isDesignPresent() {
        return designPresent;
    }

    public void setDesignPresent( boolean designPresent ) {
        this.designPresent = designPresent;
    }

    @Nullable
    public ExperimentalDesignValueObject getProposedDesign() {
        return proposedDesign;
    }

    public void setProposedDesign( @Nullable ExperimentalDesignValueObject proposedDesign ) {
        this.proposedDesign = proposedDesign;
    }

    @Nullable
    public DesignCommitPlan getDesignPlan() {
        return designPlan;
    }

    public void setDesignPlan( @Nullable DesignCommitPlan designPlan ) {
        this.designPlan = designPlan;
    }

    @Nullable
    public Long getSplitOnFactorId() {
        return splitOnFactorId;
    }

    public void setSplitOnFactorId( @Nullable Long splitOnFactorId ) {
        this.splitOnFactorId = splitOnFactorId;
    }

    @Nullable
    public String getSplitRationale() {
        return splitRationale;
    }

    public void setSplitRationale( @Nullable String splitRationale ) {
        this.splitRationale = splitRationale;
    }

    public boolean isTagsPresent() {
        return tagsPresent;
    }

    public void setTagsPresent( boolean tagsPresent ) {
        this.tagsPresent = tagsPresent;
    }

    public List<TagAdd> getTagsToAdd() {
        return tagsToAdd;
    }

    public void setTagsToAdd( List<TagAdd> tagsToAdd ) {
        this.tagsToAdd = tagsToAdd;
    }

    @Nullable
    public String getReason() {
        return reason;
    }

    @Nullable
    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode( @Nullable String reasonCode ) {
        this.reasonCode = reasonCode;
    }

    public void setReason( @Nullable String reason ) {
        this.reason = reason;
    }

    public List<Long> getTagsToDelete() {
        return tagsToDelete;
    }

    public void setTagsToDelete( List<Long> tagsToDelete ) {
        this.tagsToDelete = tagsToDelete;
    }

    public int getTagsUnchanged() {
        return tagsUnchanged;
    }

    public void setTagsUnchanged( int tagsUnchanged ) {
        this.tagsUnchanged = tagsUnchanged;
    }

    public boolean isSampleCharsPresent() {
        return sampleCharsPresent;
    }

    public void setSampleCharsPresent( boolean sampleCharsPresent ) {
        this.sampleCharsPresent = sampleCharsPresent;
    }

    public List<SampleCharacteristicAdd> getSampleCharsToAdd() {
        return sampleCharsToAdd;
    }

    public void setSampleCharsToAdd( List<SampleCharacteristicAdd> sampleCharsToAdd ) {
        this.sampleCharsToAdd = sampleCharsToAdd;
    }

    public List<Long> getSampleCharsToDelete() {
        return sampleCharsToDelete;
    }

    public void setSampleCharsToDelete( List<Long> sampleCharsToDelete ) {
        this.sampleCharsToDelete = sampleCharsToDelete;
    }

    public int getSampleCharsUnchanged() {
        return sampleCharsUnchanged;
    }

    public void setSampleCharsUnchanged( int sampleCharsUnchanged ) {
        this.sampleCharsUnchanged = sampleCharsUnchanged;
    }

    public boolean isCurationDetailsPresent() {
        return curationDetailsPresent;
    }

    public void setCurationDetailsPresent( boolean curationDetailsPresent ) {
        this.curationDetailsPresent = curationDetailsPresent;
    }

    @Nullable
    public String getCurationDetailsNote() {
        return curationDetailsNote;
    }

    public void setCurationDetailsNote( @Nullable String curationDetailsNote ) {
        this.curationDetailsNote = curationDetailsNote;
    }

    /**
     * The producing side's run identifier, if this commit is being applied by an agent run.
     * <p>
     * Null for a curator commit. When non-null the commit mints a {@code COMMIT} AnnotationSet in its own
     * transaction, so the row exists only if the commit itself succeeded.
     */
    @Nullable
    public String getRunId() {
        return runId;
    }

    public void setRunId( @Nullable String runId ) {
        this.runId = runId;
    }

    /** Which build produced the run named by {@link #getRunId()}. Null when no run was named. */
    @Nullable
    public AnnotationSetService.RunProvenance getRunProvenance() {
        return runProvenance;
    }

    public void setRunProvenance( @Nullable AnnotationSetService.RunProvenance runProvenance ) {
        this.runProvenance = runProvenance;
    }

    /** The PROPOSAL annotation set this commit applies, if it applies one. Null for an unsolicited commit. */
    @Nullable
    public AnnotationSet getRunParentProposal() {
        return runParentProposal;
    }

    public void setRunParentProposal( @Nullable AnnotationSet runParentProposal ) {
        this.runParentProposal = runParentProposal;
    }

    /**
     * The dataset's current curation, serialized as the {@code CurationDocument} the commit itself accepts, to be
     * stored as a {@code SNAPSHOT} AnnotationSet if this commit changes anything.
     * <p>
     * Read before the commit applies, so it records what the commit displaced. Null on a dry run, which writes
     * nothing and therefore displaces nothing.
     */
    @Nullable
    public String getSnapshotPayloadJson() {
        return snapshotPayloadJson;
    }

    public void setSnapshotPayloadJson( @Nullable String snapshotPayloadJson ) {
        this.snapshotPayloadJson = snapshotPayloadJson;
    }

    /** Who committed, recorded on the snapshot row so the displaced state names the commit that displaced it. */
    @Nullable
    public String getSnapshotCreatedBy() {
        return snapshotCreatedBy;
    }

    public void setSnapshotCreatedBy( @Nullable String snapshotCreatedBy ) {
        this.snapshotCreatedBy = snapshotCreatedBy;
    }

}
