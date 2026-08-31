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
import ubic.gemma.model.annotations.WithheldFromApi;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;

/**
 * @author luke
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Data
// The statement labels are part of the identity because they are the only thing separating two
// annotations that share a subject under one category -- "wild type genotype" bare and
// "wild type genotype has background APP/PS1". Without them the two VOs compare equal whenever
// their ids are null, and a Set of annotations silently keeps one.
@EqualsAndHashCode(of = { "category", "categoryUri", "objectClass", "valueUri", "value",
        "predicate", "object", "secondPredicate", "secondObject" }, callSuper = true)
public class AnnotationValueObject extends IdentifiableValueObject<Characteristic> {

    /** Category URI. Same field as {@link Characteristic#getCategoryUri()}, under the same name. */
    private String categoryUri;
    /** Category label. Same field as {@link Characteristic#getCategory()}, under the same name. */
    private String category;
    /** URI of the annotated term. On a {@link Statement} row this is the subject's URI. */
    private String valueUri;
    /**
     * Label of the annotated term — the value as curated. On a {@link Statement} row this is the
     * subject's label, and the predicate / object labels stay in their own fields: this field never
     * carries a composed sentence. {@link ubic.gemma.model.expression.experiment.FactorValueUtils#getSummaryString}
     * is the separate sentence-builder for display.
     */
    private String value;
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "no constructor or caller ever populates it")
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
     * wire shape — {@link #valueUri} and {@link #value} carry the statement's subject URI and
     * label (Statement aliases subject → value internally).
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
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "ontology-tree render state, never populated; superseded by /annotations/term")
    private String parentName;
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "ontology-tree render state, never populated; superseded by /annotations/term")
    private String parentDescription;
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "ontology-tree render state, never populated; superseded by /annotations/term")
    private String parentLink;
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "ontology-tree render state, never populated; superseded by /annotations/term")
    private String parentOfParentName;
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "ontology-tree render state, never populated; superseded by /annotations/term")
    private String parentOfParentDescription;
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "ontology-tree render state, never populated; superseded by /annotations/term")
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

    public AnnotationValueObject( String categoryUri, String category, String valueUri, String value, Class<?> objectClass ) {
        this.categoryUri = categoryUri;
        this.category = category;
        this.valueUri = valueUri;
        this.value = value;
        this.objectClass = formatObjectClass( objectClass );
    }

    public AnnotationValueObject( Characteristic c ) {
        super( c );
        categoryUri = c.getCategoryUri();
        category = c.getCategory();
        // See CharacteristicUtils#canonicalUri: a read-time stand-in for the parked migration.
        valueUri = CharacteristicUtils.canonicalUri( c.getValueUri() );
        value = CharacteristicUtils.canonicalLabel( c.getValueUri(), c.getValue() );
        evidenceCode = c.getEvidenceCode() != null ? c.getEvidenceCode().name() : null;
        if ( c instanceof Statement ) {
            Statement s = ( Statement ) c;
            predicate = s.getPredicate();
            predicateUri = s.getPredicateUri();
            // A Statement has three annotatable value slots, and a term is as often in the
            // object as the subject -- canonicalizing only the subject would fix a third of it.
            objectUri = CharacteristicUtils.canonicalUri( s.getObjectUri() );
            object = CharacteristicUtils.canonicalLabel( s.getObjectUri(), s.getObject() );
            secondPredicate = s.getSecondPredicate();
            secondPredicateUri = s.getSecondPredicateUri();
            secondObjectUri = CharacteristicUtils.canonicalUri( s.getSecondObjectUri() );
            secondObject = CharacteristicUtils.canonicalLabel( s.getSecondObjectUri(), s.getSecondObject() );
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
                "categoryUri='" + categoryUri + '\'' +
                ", category='" + category + '\'' +
                ", valueUri='" + valueUri + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
