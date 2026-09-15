/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.common.description;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.annotations.GemmaRestOnly;
import ubic.gemma.model.annotations.WithheldFromApi;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.*;

import static ubic.gemma.model.common.description.CharacteristicUtils.compareTerm;

/**
 * Value object representation of a {@link Characteristic}.
 * @see Characteristic
 * @author poirigui
 */
@Data
public class CharacteristicValueObject extends IdentifiableValueObject<Characteristic> implements Comparable<CharacteristicValueObject> {

    private static final Comparator<CharacteristicValueObject> COMPARATOR = Comparator
            .comparing( ( CharacteristicValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getCategory(), c1.getCategoryUri(), c2.getCategory(), c2.getCategoryUri() ) )
            .thenComparing( CharacteristicValueObject::getTaxon, Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) )
            .thenComparing( ( CharacteristicValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getValue(), c1.getValueUri(), c2.getValue(), c2.getValueUri() ) )
            .thenComparing( CharacteristicValueObject::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    private String category;
    private String categoryUri;
    private String value;
    private String valueUri;

    /**
     * The submitter's own wording, as it arrived — before curation replaced {@link #value} with an
     * ontology label, and before a compound field was split.
     * <p>
     * Once {@code value} has been grounded the original is not recoverable from anything else on the
     * wire: {@code organism part: "hypothalamus"} does not say the submitter wrote {@code tissue:
     * "Hypothalamus"}. That makes two questions unanswerable without it — "is this the right
     * resolution of what they actually wrote?" and "what literal should a curator revisit when no
     * term covers it?" (a mouse cohort aged {@code "2-3 months"} straddles two stages; the span is
     * real and only the literal carries it). Curators were preserving such strings by hand in
     * free-text tags because nothing surfaced this field.
     * <p>
     * <b>Null means "not recorded", NEVER "same as value".</b> It is populated at GEO import and
     * backfilled from the pre-edit value when a curator grounds a tag, so it is absent for rows
     * written through the curation API and for anything that never had a distinct original. A
     * consumer that reads null as "unchanged" turns an unknown into a confirmed no-op.
     * <p>
     * Serialized only when present. Was withheld from REST under the old {@code @GemmaWebOnly}
     * marker until the field's only consumer, Gemma Web, was retired.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String originalValue = null;

    /**
     * A unique ontology identifier (i.e. IRI) for this characteristic.
     */
    @GemmaRestOnly
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String valueId;

    /**
     * Verbatim provenance backing a curated characteristic — a JSON array of
     * {@code {quote, source, location, …}} items the curation agents emitted. Gemma stores and serves it
     * opaquely; the agents repo owns the schema.
     * <p>
     * This is the field that answers "where did this come from" for an {@link ExperimentalFactor}'s
     * <em>category</em>, which is a {@link Characteristic} like any other and therefore already has the
     * storage. Null means "nothing recorded", which is the expected reading for most rows — it is not an
     * error and must not be rendered as one.
     * <p>
     * Provenance rather than identity, so it is excluded from equals/hashCode for the same reason
     * {@link #supplementary} is: the same term reached with and without recorded evidence is the same term,
     * and including it here would break the de-duplication the search paths rely on.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @EqualsAndHashCode.Exclude
    @Schema(description = "Verbatim provenance backing a curated characteristic — a JSON array of {quote, source, location} items the curation agents emitted. Null when none is recorded.")
    private JsonNode supportingEvidence;

    // TODO: all the following fields are Phenocarta-specific and should be relocated FIXME it's not clear which fields are referred to by this comment. I've marked some candidates

    /**
     * id used by url on the client side
     */
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "never populated")
    private String urlId = "";
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "curator editor state; says nothing about the characteristic itself")
    private boolean alreadyPresentInDatabase = false;
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "Phenocarta editor state, never populated")
    private boolean alreadyPresentOnGene = false; // phenocarta?
    /**
     * True when this candidate came from a flat lexical catalogue (MGI names, Cellosaurus) rather
     * than a conventional ontology.
     * <p>
     * These sources exist to back-fill names the ontologies lack, and their index applies a large
     * exact-name boost whose scores are not comparable with a Jena index's — which is why
     * {@code OntologyServiceImpl.findTermsInexact} ranks them below every conventional hit rather
     * than merging them. Carrying the flag onto the value object lets the ranking layers keep that
     * distinction instead of re-deriving it from URI namespaces one pair at a time.
     * <p>
     * Provenance rather than identity, so it is excluded from equals/hashCode: the same term
     * reached through two sources is the same term.
     */
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private boolean supplementary = false;
    /**
     * child term from a root
     */
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "ontology-tree render state, never populated")
    private boolean child = false;
    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "usageCount that is made available provides the number of experiments that a term is used in. this instead counts the number of samples a term appears in which is less useful to the end user")
    private int numTimesUsed = 0;
    /**
     * what Ontology uses this term
     */
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "never populated")
    private String ontologyUsed = null;
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "never populated; a Phenocarta gene tally, not an ACL count")
    private long privateGeneCount = 0L; // phenocarta?
    /**
     * number of occurrences in all genes
     */
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "never populated; a Phenocarta gene tally, not an ACL count")
    private long publicGeneCount = 0L; // phenocarta?
    /**
     * root of a query
     */
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "ontology-tree render state, never populated")
    private boolean root = false;
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "flattened common name, never populated outside tests")
    private String taxon = "";
    /**
     * The definition of the value, if it is an ontology term, as supplied by the ontology. If the value is
     * free text, this will be empty
     */
    @WithheldFromApi(value = Reason.INTERNAL_ONLY,
            comment = "never populated")
    private String valueDefinition = "";

    /**
     * Required when using the class as a spring bean.
     */
    public CharacteristicValueObject() {
        super();
    }

    public CharacteristicValueObject( Long id ) {
        super( id );
    }

    public CharacteristicValueObject( Characteristic characteristic ) {
        super( characteristic );
        this.category = characteristic.getCategory();
        this.categoryUri = characteristic.getCategoryUri();
        // Report the canonical term, not always the stored one -- a read-time stand-in for the
        // parked migration (CharacteristicUtils#canonicalUri). The label moves with the URI:
        // the new URI beside the old label is a row that says one thing and means another.
        this.valueUri = CharacteristicUtils.canonicalUri( characteristic.getValueUri() );
        this.value = CharacteristicUtils.canonicalLabel( characteristic.getValueUri(), characteristic.getValue() );
        this.urlId = parseUrlId( this.valueUri );
        this.originalValue = characteristic.getOriginalValue();
        this.supportingEvidence = CharacteristicUtils.parseSupportingEvidence( characteristic.getSupportingEvidence() );
    }

    public CharacteristicValueObject( String value, @Nullable String valueUri ) {
        this.valueUri = valueUri;
        this.value = value;
        this.urlId = parseUrlId( valueUri );
    }

    public CharacteristicValueObject( String value, @Nullable String valueUri, String category, @Nullable String categoryUri ) {
        this( value, valueUri );
        this.category = category;
        this.categoryUri = categoryUri;
    }

    public static Collection<CharacteristicValueObject> characteristic2CharacteristicVO(
            Collection<? extends Characteristic> characteristics ) {

        Collection<CharacteristicValueObject> characteristicValueObjects;

        if ( characteristics instanceof List )
            characteristicValueObjects = new ArrayList<>();
        else
            characteristicValueObjects = new HashSet<>();

        for ( Characteristic characteristic : characteristics ) {
            CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject( characteristic );
            characteristicValueObjects.add( characteristicValueObject );
        }
        return characteristicValueObjects;
    }

    @Override
    public int hashCode() {
        return Objects.hash( StringUtils.lowerCase( categoryUri != null ? categoryUri : category ),
                StringUtils.lowerCase( valueUri != null ? valueUri : value ) );
    }

    @Override
    public boolean equals( Object object ) {
        if ( object == null )
            return false;
        if ( this == object )
            return true;
        if ( !( object instanceof CharacteristicValueObject ) )
            return false;
        CharacteristicValueObject that = ( CharacteristicValueObject ) object;
        if ( this.getId() != null && that.getId() != null )
            return super.equals( object );
        return CharacteristicUtils.equals( category, categoryUri, that.category, that.categoryUri )
                && CharacteristicUtils.equals( value, valueUri, that.value, that.valueUri );
    }

    @Override
    public int compareTo( @NonNull CharacteristicValueObject that ) {
        return COMPARATOR.compare( this, that );
    }

    @Override
    public String toString() {
        return String.format( "[Category=%s%s Value=%s%s]",
                category,
                categoryUri != null ? " (" + categoryUri + ")" : "",
                value,
                valueUri != null ? " (" + valueUri + ")" : "" );
    }

    public void incrementOccurrenceCount() {
        this.numTimesUsed++;
    }

    private static String parseUrlId( @Nullable String valueUri ) {
        if ( StringUtils.isBlank( valueUri ) )
            return "";
        if ( valueUri.indexOf( "#" ) > 0 ) {
            return valueUri.substring( valueUri.lastIndexOf( "#" ) + 1 );
        } else if ( valueUri.lastIndexOf( "/" ) > 0 ) {
            return valueUri.substring( valueUri.lastIndexOf( "/" ) + 1 );
        } else {
            return "";
        }
    }
}
