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

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject;

import javax.annotation.Nullable;
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
 * not hold the commit transaction open.
 */
public class CurationCommitRequest {

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
    private BibliographicReference primaryPublication;
    private List<BibliographicReference> otherRelevantPublications = Collections.emptyList();

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

    @Nullable
    public BibliographicReference getPrimaryPublication() {
        return primaryPublication;
    }

    public void setPrimaryPublication( @Nullable BibliographicReference primaryPublication ) {
        this.primaryPublication = primaryPublication;
    }

    public List<BibliographicReference> getOtherRelevantPublications() {
        return otherRelevantPublications;
    }

    public void setOtherRelevantPublications( List<BibliographicReference> otherRelevantPublications ) {
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
}
