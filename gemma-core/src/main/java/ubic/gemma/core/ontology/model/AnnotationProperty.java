/*
 * The basecode project
 *
 * Copyright (c) 2007-2019 University of British Columbia
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
 *
 */
package ubic.gemma.core.ontology.model;

import javax.annotation.Nullable;

/**
 * @author pavlidis
 */
public interface AnnotationProperty extends OntologyResource {

    /**
     * A label for the property of this annotation.
     */
    String getProperty();

    /**
     * The value associated to this annotation as a string.
     * <p>
     * 🛑 When the value is a RESOURCE, this returns its {@code rdfs:label} — not its URI — and null
     * when the resource carries no label. That indirection is right for display and wrong for
     * identity: use {@link #getValueUri()} whenever the annotation names a term rather than
     * describes one.
     */
    @Nullable
    String getContents();

    /**
     * The URI of this annotation's value when the value is a URI resource, else {@code null}
     * (literal values, blank nodes).
     * <p>
     * Exists because {@link #getContents()} resolves a resource to its label, and a label is not an
     * identity. MONDO's {@code crossSpeciesExactMatch} is the case that proved it: the mapping
     * {@code MONDO:0700199 → MONDO:0005061} came back as the string {@code "lung adenocarcinoma"},
     * which names BOTH {@code MONDO:0005061} (a disease) and {@code HP:0030078} (a phenotype)
     * equally well. A consumer repairing an annotation from that string can silently land on the
     * phenotype — worse than not repairing, because it looks like it worked.
     */
    @Nullable
    default String getValueUri() {
        return null;
    }
}
