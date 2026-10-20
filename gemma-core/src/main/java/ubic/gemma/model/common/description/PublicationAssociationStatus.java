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

/**
 * Whether a {@link PublicationAssociation} affirms or denies the link between an experiment and a
 * publication.
 * <p>
 * {@link #REJECTED} is the addition that matters. Before this table Gemma could say "the publication
 * for this experiment is X" but had no way to say "Y was considered and ruled out, on this evidence,
 * by this authority" — so every rejection had to live outside Gemma in a hand-maintained exclusion
 * file, and every re-run of a publication finder re-proposed the paper a curator had already thrown
 * out.
 */
public enum PublicationAssociationStatus {

    /**
     * This publication belongs to this experiment. Mirrored into the
     * {@link ubic.gemma.model.analysis.Investigation#getPrimaryPublication() primary} /
     * {@link ubic.gemma.model.analysis.Investigation#getOtherRelevantPublications() other-relevant}
     * link the rest of Gemma (and Gemma 1.x) reads, according to the row's
     * {@link PublicationAssociationRole role}.
     */
    ACCEPTED,

    /**
     * This publication was considered for this experiment and ruled out. There is deliberately no
     * corresponding link on the experiment: a rejected row is a record of the decision, not a weaker
     * kind of association, and it must stay invisible to anything that lists a dataset's publications.
     */
    REJECTED;

    /**
     * @return the lowercase external form, for use in JSON DTOs / API surfaces.
     */
    public String getDbValue() {
        return name().toLowerCase();
    }

    /**
     * Parse the lowercase external form back into the enum. Accepts either case.
     */
    public static PublicationAssociationStatus fromDbValue( String v ) {
        return PublicationAssociationStatus.valueOf( v.toUpperCase() );
    }
}
