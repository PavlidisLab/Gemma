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
import ubic.gemma.model.common.description.PublicationAssociationStatus;
import ubic.gemma.persistence.service.BaseDao;

import java.util.List;

/**
 * DAO for {@link PublicationAssociation} rows.
 *
 * <p>Addressed by id or by the {@code (investigation, publication)} pair the unique key is built on.
 * The precedence rule that decides whether a given writer is allowed to overwrite what it finds lives
 * at the service layer; the DAO reads and writes rows without judging them.</p>
 */
public interface PublicationAssociationDao extends BaseDao<PublicationAssociation> {

    /**
     * The assertion held for this exact pair, or {@code null} if none. There is at most one — the
     * unique key sees to that, which is what makes an upsert well defined.
     */
    @Nullable
    PublicationAssociation findByInvestigationAndPublication( Investigation investigation,
            BibliographicReference publication );

    /**
     * Every assertion attached to the investigation, accepted before rejected and stable thereafter by
     * id.
     *
     * @param statusFilter optional status filter; {@code null} = all.
     */
    List<PublicationAssociation> findByInvestigation( Investigation investigation,
            @Nullable PublicationAssociationStatus statusFilter );

    /**
     * Assertions for the investigation whose publication is one of {@code publications}, in one query.
     * <p>
     * Exists so the read path can decorate a dataset's publication list without a query per
     * publication, and so a bulk writer can look up everything it is about to touch up front.
     */
    List<PublicationAssociation> findByInvestigationAndPublications( Investigation investigation,
            java.util.Collection<BibliographicReference> publications );

    /**
     * Every investigation-side rejection of this publication, across all datasets. Answers "who has
     * ruled this paper out, and why?" — the question a finder should ask before proposing it again.
     */
    List<PublicationAssociation> findRejectionsByPublication( BibliographicReference publication );

    /**
     * Move every assertion about {@code from} onto {@code to}, for the duplicate-reference merge.
     *
     * <p>Needed because the FK to the reference deliberately does not cascade: without this, deleting
     * a merged-away duplicate hits the constraint, and {@code MergeDuplicateBibRefsCli} — which
     * repoints the experiment links and then deletes the duplicate — would quietly stop deleting
     * anything while the assertions went on pointing at a reference nothing else uses.</p>
     *
     * <p>An investigation that already asserts something about {@code to} keeps that assertion and the
     * duplicate's row is dropped, since the unique key permits only one and the surviving reference's
     * own record is the one to trust.</p>
     *
     * @return the number of rows moved (rows dropped as redundant are not counted).
     */
    int rebindPublication( BibliographicReference from, BibliographicReference to );
}
