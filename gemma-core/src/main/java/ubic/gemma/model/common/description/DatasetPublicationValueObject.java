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
package ubic.gemma.model.common.description;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;

/**
 * A publication as it appears in the context of one dataset: the reference, plus the evidenced claim
 * that ties it to (or rules it out for) that dataset.
 *
 * <p>Extends {@link BibliographicReferenceValueObject} rather than wrapping it so the addition is
 * strictly additive on the wire — {@code GET /datasets/{id}/publications} keeps emitting every field
 * it emitted before, with {@code association} alongside them. A client that has never heard of
 * evidence is unaffected.</p>
 *
 * <p>The association is nullable, and its being null means something: the link exists but nothing is
 * recorded about where it came from. That is the state of any link written by a path that predates
 * this record and has not been backfilled, and it is worth being able to see rather than smoothing
 * over.</p>
 */
@Schema(description = "A publication of a dataset, together with the evidenced claim that attaches it — who says so, on what basis, and whether the claim is an acceptance or a rejection.")
public class DatasetPublicationValueObject extends BibliographicReferenceValueObject {

    @Schema(description = "The evidenced claim tying this publication to the dataset. Null when the link carries no recorded provenance.")
    @Nullable
    private PublicationAssociationValueObject association;

    public DatasetPublicationValueObject() {
        super();
    }

    public DatasetPublicationValueObject( BibliographicReference ref, @Nullable PublicationAssociation association ) {
        super( ref );
        this.association = association != null ? new PublicationAssociationValueObject( association ) : null;
    }

    @Nullable
    public PublicationAssociationValueObject getAssociation() {
        return association;
    }

    public void setAssociation( @Nullable PublicationAssociationValueObject association ) {
        this.association = association;
    }
}
