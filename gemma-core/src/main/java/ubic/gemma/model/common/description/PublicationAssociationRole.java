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
 * Which of the two publication slots on an {@link ubic.gemma.model.analysis.Investigation} an
 * {@link PublicationAssociationStatus#ACCEPTED accepted} {@link PublicationAssociation} occupies.
 * <p>
 * Kept separate from {@link PublicationAssociationStatus} rather than folded into a single
 * three-valued column, because the two answer different questions and only one of them applies to a
 * rejected row: "is this paper ours?" is always meaningful, "primary or merely relevant?" is not.
 * Recording it also means the legacy projection
 * ({@code INVESTIGATION.PRIMARY_PUBLICATION_FK} + {@code RELEVANT_PUBLICATIONS}) is reconstructable
 * from this table alone, which is what makes those two structures droppable at the Gemma 1.x cutover.
 */
public enum PublicationAssociationRole {

    /** The primary citable publication — the paper that reports this experiment. */
    PRIMARY,

    /** A publication that is relevant to the experiment without being the one that reports it. */
    OTHER_RELEVANT;

    /**
     * @return the lowercase external form, for use in JSON DTOs / API surfaces.
     */
    public String getDbValue() {
        return name().toLowerCase();
    }

    /**
     * Parse the lowercase external form back into the enum. Accepts either case.
     */
    public static PublicationAssociationRole fromDbValue( String v ) {
        return PublicationAssociationRole.valueOf( v.toUpperCase() );
    }
}
