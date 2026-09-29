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
package ubic.gemma.persistence.service.expression.experiment;

/**
 * Thrown when a curation commit asks to delete a tag or sample characteristic that is not on the experiment.
 *
 * <p>A named type so the REST layer can answer {@code 400}, as it does for the same mistake in the design
 * section, instead of the {@code 409} a bare {@link IllegalArgumentException} maps to.</p>
 */
public class UnknownDeletedIdsException extends IllegalArgumentException {

    public UnknownDeletedIdsException( String message ) {
        super( message );
    }
}
