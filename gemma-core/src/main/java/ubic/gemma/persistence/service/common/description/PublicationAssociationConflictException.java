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

import ubic.gemma.model.common.description.PublicationAssociation;

/**
 * Thrown when a writer tries to accept a publication that a higher authority has already ruled out
 * for that experiment.
 *
 * <p>This is the refusal the whole design exists to produce, so it is a named type rather than a bare
 * {@link IllegalArgumentException}: the REST layer maps it to {@code 409 Conflict}, and an automated
 * writer such as the GEO refresh catches it and moves on instead of failing the import. The message
 * quotes the standing rejection's evidence, because a writer that is refused should be able to say
 * why without a second query.</p>
 */
public class PublicationAssociationConflictException extends IllegalArgumentException {

    private final transient PublicationAssociation standing;

    public PublicationAssociationConflictException( String message, PublicationAssociation standing ) {
        super( message );
        this.standing = standing;
    }

    /**
     * @return the rejection that blocked the write, so the caller can report its source, evidence and
     *         date rather than a bare "denied".
     */
    public PublicationAssociation getStanding() {
        return standing;
    }
}
