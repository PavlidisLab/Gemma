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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Side-channel the web-layer design mapper hands to {@link ExpressionExperimentService#commitCuration} so the
 * service can, <em>after</em> applying the proposed design, (1) resolve each new entity's {@code clientRef} to the
 * database id it was assigned and (2) wire sample assignments that target freshly-created factor values.
 * <p>
 * The proposed {@link ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject} carries new entities as
 * {@code id == null}; it cannot express which {@code clientRef} each corresponds to, nor can its
 * {@code bioMaterialAssignments} (which reference factor values by {@code Long} id) target a not-yet-created factor
 * value. This plan closes both gaps.
 * <p>
 * <strong>Correlation is order-based.</strong> The rebuilt design read after apply sorts factors and factor values by
 * ascending id, database ids are monotonic in creation order, and creation follows the proposed-items order — so the
 * k-th newly-created entity corresponds to the k-th recorded {@code clientRef}. The {@code preExisting*} id sets tell
 * the service which ids in the rebuilt design are "new" (absent from these sets).
 */
public class DesignCommitPlan {

    /** Factor ids present in the design BEFORE the commit — anything else in the rebuilt design is newly created. */
    private Set<Long> preExistingFactorIds;
    /** Factor-value ids present BEFORE the commit. */
    private Set<Long> preExistingFactorValueIds;

    /** clientRefs of new factors, in the order they appear (as {@code id == null}) in the proposed design. */
    private List<String> newFactorClientRefs = new ArrayList<>();

    /**
     * clientRefs of new factor values, keyed by their parent factor's stable key and ordered within that factor.
     * Parent key: {@code "F:" + factorId} for an existing factor, {@code "C:" + factorClientRef} for a new factor.
     */
    private Map<String, List<String>> newFactorValueClientRefsByParentKey = new LinkedHashMap<>();

    /** Sample assignments that target a new factor value; resolved to real ids and applied in the second pass. */
    private List<PendingAssignment> pendingAssignments = new ArrayList<>();

    public Set<Long> getPreExistingFactorIds() {
        return preExistingFactorIds;
    }

    public void setPreExistingFactorIds( Set<Long> preExistingFactorIds ) {
        this.preExistingFactorIds = preExistingFactorIds;
    }

    public Set<Long> getPreExistingFactorValueIds() {
        return preExistingFactorValueIds;
    }

    public void setPreExistingFactorValueIds( Set<Long> preExistingFactorValueIds ) {
        this.preExistingFactorValueIds = preExistingFactorValueIds;
    }

    public List<String> getNewFactorClientRefs() {
        return newFactorClientRefs;
    }

    public void setNewFactorClientRefs( List<String> newFactorClientRefs ) {
        this.newFactorClientRefs = newFactorClientRefs;
    }

    public Map<String, List<String>> getNewFactorValueClientRefsByParentKey() {
        return newFactorValueClientRefsByParentKey;
    }

    public void setNewFactorValueClientRefsByParentKey( Map<String, List<String>> newFactorValueClientRefsByParentKey ) {
        this.newFactorValueClientRefsByParentKey = newFactorValueClientRefsByParentKey;
    }

    public List<PendingAssignment> getPendingAssignments() {
        return pendingAssignments;
    }

    public void setPendingAssignments( List<PendingAssignment> pendingAssignments ) {
        this.pendingAssignments = pendingAssignments;
    }

    /** Parent-key helpers keep the "F:" / "C:" convention in one place. */
    public static String existingFactorKey( Long factorId ) {
        return "F:" + factorId;
    }

    public static String newFactorKey( String factorClientRef ) {
        return "C:" + factorClientRef;
    }

    /** A sample assignment whose target factor value does not exist yet (identified by its {@code clientRef}). */
    public static class PendingAssignment {
        private final String factorValueClientRef;
        private final Set<Long> bioMaterialIds;

        public PendingAssignment( String factorValueClientRef, Set<Long> bioMaterialIds ) {
            this.factorValueClientRef = factorValueClientRef;
            this.bioMaterialIds = bioMaterialIds;
        }

        public String getFactorValueClientRef() {
            return factorValueClientRef;
        }

        public Set<Long> getBioMaterialIds() {
            return bioMaterialIds;
        }
    }
}
