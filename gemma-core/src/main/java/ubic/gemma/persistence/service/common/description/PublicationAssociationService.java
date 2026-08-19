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
package ubic.gemma.persistence.service.common.description;

import org.springframework.lang.Nullable;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.PublicationAssociation;
import ubic.gemma.model.common.description.PublicationAssociationRole;
import ubic.gemma.model.common.description.PublicationAssociationSource;
import ubic.gemma.model.common.description.PublicationAssociationStatus;

import java.util.Collection;
import java.util.List;

/**
 * Reads and writes the evidenced claims behind an experiment's publication links.
 *
 * <p><b>This is where precedence is enforced.</b> Every writer that wants to attach a publication to
 * an experiment passes through {@link #reconcile} or asks {@link #findBlockingRejection} first, and a
 * claim is refused when a higher authority has already ruled that publication out. The rule is a rank
 * comparison on {@link PublicationAssociationSource}, evaluated at write time — not a list of
 * exclusions that each code path has to remember to consult. The distinction is the point: a denylist
 * protects exactly the writers that read it, which is how a correction applied on 2026-08-13 survived
 * until the next cache rebuild and no longer.</p>
 *
 * <p><b>What this service does not own.</b> The links themselves —
 * {@link Investigation#getPrimaryPublication()} and
 * {@link Investigation#getOtherRelevantPublications()} — stay owned by
 * {@code ExpressionExperimentService.updatePublications}, which is already the ACL-guarded, audited
 * write path for them and is what Gemma 1.32.x reads. That method calls {@link #reconcile} in the
 * same transaction so the assertion rows and the links cannot drift apart.</p>
 */
public interface PublicationAssociationService {

    /**
     * Bring the assertions for {@code investigation} into line with a set-replace of its publications,
     * and record any explicit rejections.
     *
     * <p>Called by {@code ExpressionExperimentService.updatePublications} inside its transaction,
     * after the caller has resolved every identifier to a persistent reference and before (or after —
     * the ordering does not matter within the transaction) the links themselves are rewritten.</p>
     *
     * <p>What happens to each publication:</p>
     * <ul>
     *   <li><b>In {@code primary} or {@code otherRelevant}</b> — an {@code ACCEPTED} row is created,
     *       or the existing one is updated. The row's {@link PublicationAssociation#getRole() role}
     *       always follows the new layout; its evidence is overwritten only when the incoming
     *       assertion both states a basis and comes from a source that
     *       {@link PublicationAssociationSource#outranks outranks} the one on record, so a re-import
     *       cannot quietly replace a curator's reasoning with "GEO said so".</li>
     *   <li><b>In {@code rejected}</b> — a {@code REJECTED} row is created or updated, and any link to
     *       that publication is expected to be absent from {@code primary} / {@code otherRelevant} (a
     *       publication given as both is rejected by an {@link IllegalArgumentException}; asking for
     *       the same paper to be simultaneously the dataset's publication and not is a caller bug, not
     *       something to resolve silently).</li>
     *   <li><b>Previously asserted, now in none of the three</b> — the row is deleted. A set-replace
     *       that drops a publication retracts the claim; it does not leave an {@code ACCEPTED} row
     *       pointing at a link that no longer exists. To record <em>why</em> it was dropped, name it
     *       in {@code rejected} instead — that is the difference between forgetting and deciding.</li>
     * </ul>
     *
     * <p><b>🛑 A null {@code rejected} leaves the standing rejections alone; an empty one clears
     * them.</b> The distinction exists because the two halves of this call are read back through
     * different doors. A caller assembling {@code primary} / {@code otherRelevant} has necessarily
     * seen the dataset's publications — they are what the plain GET returns. Its rejections are not:
     * they are behind {@code ?includeRejected=true}, off by default because a rejection is not one of
     * the dataset's publications. So a client that reads the dataset and writes back what it read
     * knows the accepted set exactly and knows nothing whatever about the rejected one, and treating
     * its silence as "clear them" deletes rulings it was never shown.</p>
     *
     * <p>GSE227854 is the case, and it is the same case this table was built for. Rachel's rejection
     * of PMID 38088204 — GEO's own {@code !Series_pubmed_id}, which names the wrong one of the
     * submitter's two NAR 2024 papers — is the only thing standing between the nightly GEO refresh
     * and re-installing that paper as the primary. Retract it on a caller's silence and the refresh
     * wins at rank 30 against nothing at all: precisely the 2026-08-13 correction reverted by the
     * 08-14 rebuild, one layer up from where that was fixed.</p>
     *
     * <p><b>Scope of the refusal.</b> Rank blocks <em>accepting</em> a publication that stands
     * rejected, and blocks <em>overwriting the stated basis</em> of a higher-ranked assertion. It does
     * not block retraction or re-decision through this method: reaching it at all means passing
     * {@code ACL_SECURABLE_EDIT} on the dataset, and a curator who has decided to change their mind
     * must not need a lower-level escape hatch to do it. What the rule stops is an unattended writer
     * undoing a human by accident, which is the failure that has actually happened.</p>
     *
     * @param investigation the experiment. Required.
     * @param primary       the publication to hold as primary, or {@code null} for none.
     * @param otherRelevant the publications to hold as other-relevant; may be empty.
     * @param rejected      publications to record as ruled out, replacing the standing set — an empty
     *                      collection clears every rejection. {@code null} means the caller is not
     *                      speaking to rejections at all and the standing ones are left untouched,
     *                      which is what every writer that does not manage them should pass.
     * @return the assertions now standing for this investigation, accepted first. Rejections left
     *         untouched by a {@code null} {@code rejected} are not included — the return describes
     *         what this call asserted, and read them back with
     *         {@link #findByInvestigation(Investigation, PublicationAssociationStatus)}.
     * @throws PublicationAssociationConflictException if an accepted publication stands rejected by an
     *                                                 authority the incoming source does not outrank.
     * @throws IllegalArgumentException                if a publication appears both as accepted and as
     *                                                 rejected.
     */
    List<PublicationAssociation> reconcile( Investigation investigation,
            @Nullable PublicationAssertion primary,
            Collection<PublicationAssertion> otherRelevant,
            @Nullable Collection<PublicationAssertion> rejected );

    /**
     * Record a single acceptance without disturbing the investigation's other assertions.
     *
     * <p>For writers that add one link at a time rather than replacing a set — the GEO refresh taking
     * {@code !Series_pubmed_id}, the publication CLIs. Same precedence rule as {@link #reconcile}: a
     * standing rejection the incoming source does not outrank refuses the write.</p>
     *
     * <p>The caller is responsible for the link itself. This method records the basis for a link the
     * caller is making; it does not make it.</p>
     *
     * @throws PublicationAssociationConflictException if the publication stands rejected by an
     *                                                 authority {@code assertion}'s source does not
     *                                                 outrank.
     */
    PublicationAssociation assertAccepted( Investigation investigation, PublicationAssertion assertion,
            PublicationAssociationRole role );

    /**
     * Record that a publication has been ruled out for an investigation, without disturbing its other
     * assertions.
     *
     * <p>This is the assertion Gemma could not previously make, and the one that removes the need for
     * an exclusion file kept outside the system: once it is here, a lower-ranked writer that
     * re-proposes the paper is refused by {@link #reconcile} and {@link #assertAccepted} rather than
     * having to be filtered afterwards.</p>
     *
     * <p>Refuses if the publication is currently linked to the investigation. Rejecting a live link
     * would leave the link standing while the record says it was ruled out, and this service does not
     * own the links — take that path through
     * {@code ExpressionExperimentService.updatePublications}, which owns both halves and can drop the
     * link and record the rejection in one transaction.</p>
     *
     * @throws IllegalStateException if the publication is currently the investigation's primary or
     *                               other-relevant publication.
     */
    PublicationAssociation assertRejected( Investigation investigation, PublicationAssertion assertion );

    /**
     * The standing rejection that would refuse an attempt by {@code source} to accept
     * {@code publication} for {@code investigation}, or {@code null} if the write is allowed.
     *
     * <p>The pre-flight form of the rule in {@link #reconcile}, for writers that need to skip rather
     * than fail: the GEO refresh asks this before setting a primary publication and logs the standing
     * rejection instead of re-installing the link, and the publication CLIs do the same. Cheap enough
     * to call per experiment in a loop.</p>
     */
    @Nullable
    PublicationAssociation findBlockingRejection( Investigation investigation,
            BibliographicReference publication, PublicationAssociationSource source );

    /**
     * The assertion held for this exact pair, or {@code null} if the pair has never been ruled on.
     */
    @Nullable
    PublicationAssociation find( Investigation investigation, BibliographicReference publication );

    /**
     * Every assertion attached to the investigation, accepted before rejected.
     *
     * @param statusFilter optional status filter; {@code null} = all. Pass
     *                     {@link PublicationAssociationStatus#REJECTED} for the "do not re-propose"
     *                     list a publication finder should read before it starts.
     */
    List<PublicationAssociation> findByInvestigation( Investigation investigation,
            @Nullable PublicationAssociationStatus statusFilter );

    /**
     * Assertions for the investigation covering {@code publications}, in one query, keyed by
     * publication id — the read path's way to decorate a dataset's publication list without a query
     * per row.
     */
    java.util.Map<Long, PublicationAssociation> findByPublications( Investigation investigation,
            Collection<BibliographicReference> publications );

    /**
     * Every rejection of this publication across all datasets: who ruled it out, where, and why.
     */
    List<PublicationAssociation> findRejections( BibliographicReference publication );

    /**
     * Move every assertion about {@code from} onto {@code to}, for the duplicate-reference merge.
     *
     * <p>Call this alongside the repointing of the experiment links, before deleting the duplicate:
     * the FK to the reference does not cascade, so a leftover assertion turns the delete into a
     * caught constraint violation and the merge silently stops merging. Where an investigation
     * already asserts something about {@code to}, that assertion wins and the duplicate's row is
     * dropped.</p>
     *
     * @return the number of assertions moved.
     */
    int rebindPublication( BibliographicReference from, BibliographicReference to );
}
