/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.model.common.description;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.lang.Nullable;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;

/**
 * @author luke
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Data
@EqualsAndHashCode(of = { "className", "classUri", "objectClass", "termUri", "termName" }, callSuper = true)
public class AnnotationValueObject extends IdentifiableValueObject<Characteristic> {

    private String classUri;
    private String className;
    private String termUri;
    private String termName;
    @GemmaWebOnly
    private String description;
    @Schema(implementation = GOEvidenceCode.class)
    private String evidenceCode;
    private String objectClass;
    /**
     * Predicate label for {@link Statement}-backed annotations (e.g. {@code "has_dose"}).
     * Null when the underlying row is a plain {@link Characteristic}. The
     * {@code predicate*} / {@code object*} pair, together with the optional
     * {@code secondPredicate*} / {@code secondObject*} pair, exposes the Statement
     * relational shape to read-side consumers without breaking the Characteristic
     * wire shape — the existing {@link #termName} / {@link #termUri} fields still carry
     * the statement's subject (Statement aliases subject → value internally).
     */
    @Nullable
    @Schema(description = "Predicate label of a Statement-backed annotation (e.g. \"has_dose\"). Null on plain Characteristic rows.")
    private String predicate;
    @Nullable
    @Schema(description = "Predicate URI of a Statement-backed annotation. Null on plain Characteristic rows.")
    private String predicateUri;
    @Nullable
    @Schema(description = "Object label of a Statement-backed annotation (e.g. \"30%\"). Null on plain Characteristic rows.")
    private String object;
    @Nullable
    @Schema(description = "Object URI of a Statement-backed annotation. Null on plain Characteristic rows.")
    private String objectUri;
    @Nullable
    @Schema(description = "Second predicate label for compound Statement annotations (e.g. \"for\" in \"HFD for 12 weeks\").")
    private String secondPredicate;
    @Nullable
    @Schema(description = "Second predicate URI for compound Statement annotations.")
    private String secondPredicateUri;
    @Nullable
    @Schema(description = "Second object label for compound Statement annotations (e.g. \"12 weeks\").")
    private String secondObject;
    @Nullable
    @Schema(description = "Second object URI for compound Statement annotations.")
    private String secondObjectUri;
    @GemmaWebOnly
    private String parentName;
    @GemmaWebOnly
    private String parentDescription;
    @GemmaWebOnly
    private String parentLink;
    @GemmaWebOnly
    private String parentOfParentName;
    @GemmaWebOnly
    private String parentOfParentDescription;
    @GemmaWebOnly
    private String parentOfParentLink;
    /**
     * Verbatim provenance backing a curated tag — a JSON array of {@code {quote, source, location, ...}}
     * items the curation agents emitted (the agents-side {@code FindingEvidence} shape). Gemma stores and
     * serves it opaquely, so the agents repo owns the schema. Null when the tag has no recorded evidence
     * (plain/legacy tags, and ontology-term hits that were never accepted from a proposal).
     */
    @Nullable
    @Schema(description = "Verbatim provenance backing a curated tag — a JSON array of {quote, source, location} items the curation agents emitted. Null when the tag has no recorded evidence.")
    private JsonNode supportingEvidence;

    public AnnotationValueObject() {
        super();
    }

    public AnnotationValueObject( Long id ) {
        super( id );
    }

    public AnnotationValueObject( String classUri, String className, String termUri, String termName, Class<?> objectClass ) {
        this.classUri = classUri;
        this.className = className;
        this.termUri = termUri;
        this.termName = termName;
        this.objectClass = formatObjectClass( objectClass );
    }

    public AnnotationValueObject( Characteristic c ) {
        super( c );
        classUri = c.getCategoryUri();
        className = c.getCategory();
        termUri = c.getValueUri();
        termName = c.getValue();
        evidenceCode = c.getEvidenceCode() != null ? c.getEvidenceCode().name() : null;
        if ( c instanceof Statement ) {
            Statement s = ( Statement ) c;
            predicate = s.getPredicate();
            predicateUri = s.getPredicateUri();
            object = s.getObject();
            objectUri = s.getObjectUri();
            secondPredicate = s.getSecondPredicate();
            secondPredicateUri = s.getSecondPredicateUri();
            secondObject = s.getSecondObject();
            secondObjectUri = s.getSecondObjectUri();
        }
        supportingEvidence = CharacteristicUtils.parseSupportingEvidence( c.getSupportingEvidence() );
    }

    public AnnotationValueObject( Characteristic c, Class<?> objectClass ) {
        this( c );
        this.objectClass = formatObjectClass( objectClass );
    }

    private static String formatObjectClass( Class<?> objectClass ) {
        if ( ExpressionExperiment.class.isAssignableFrom( objectClass ) ) {
            return "ExperimentTag";
        } else {
            return objectClass.getSimpleName();
        }
    }

    @Override
    public String toString() {
        return "AnnotationValueObject{" +
                "classUri='" + classUri + '\'' +
                ", className='" + className + '\'' +
                ", termUri='" + termUri + '\'' +
                ", termName='" + termName + '\'' +
                '}';
    }
}
