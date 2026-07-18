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
package ubic.gemma.rest.util;

import ubic.gemma.core.ontology.TermViolation;

import java.util.List;

/**
 * Thrown when one or more submitted ontology terms fail grounding validation on a write. Carries every
 * failing slot (across the whole request, not just the first) so a single 400 response tells the client
 * exactly what to fix; mapped to a structured body by {@code OntologyTermValidationExceptionMapper}.
 *
 * @author gemma
 */
public class OntologyTermValidationException extends RuntimeException {

    /**
     * A {@link TermViolation} paired with the request-body path it was found at (e.g.
     * {@code tags[clientRef=t7].predicate}), so the client can map the error back to the item it sent.
     */
    public static class Located {

        private final String location;
        private final TermViolation violation;

        public Located( String location, TermViolation violation ) {
            this.location = location;
            this.violation = violation;
        }

        public String getLocation() {
            return location;
        }

        public TermViolation getViolation() {
            return violation;
        }
    }

    private final List<Located> violations;

    public OntologyTermValidationException( List<Located> violations ) {
        super( violations.size() + " ontology term(s) failed grounding validation." );
        this.violations = violations;
    }

    public List<Located> getViolations() {
        return violations;
    }
}
