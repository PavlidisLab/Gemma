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
 *
 */
package ubic.gemma.model.expression.arrayDesign;

import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * Compact platform identity — enough to name a platform and link to it.
 * <p>
 * Exists so {@link ArrayDesignValueObject} can point at another platform (its merge target, or the
 * platforms merged into it) without nesting a full platform VO, which would recurse and carry
 * curation details, taxon, external references and event triples that a cross-reference never
 * renders.
 *
 * @author paul
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "id" })
public class ArrayDesignReferenceValueObject implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Gemma-internal platform id, suitable for {@code GET /platforms/{id}}.
     */
    private Long id;

    /**
     * Short name, e.g. {@code GPL96}. This is what a page displays.
     */
    @Nullable
    private String shortName;

    /**
     * Full name, e.g. {@code Affymetrix GeneChip Human Genome U133A 2.0 Array}.
     * <p>
     * The short name is an accession and the full name is the only part a curator reads as a
     * description of the hardware, so a reference that carries only the accession cannot in fact
     * name the platform it points at.
     */
    @Nullable
    private String name;

    /**
     * What kind of platform it is — {@code ONECOLOR}, {@code TWOCOLOR}, {@code SEQUENCING},
     * {@code GENELIST}, {@code OTHER}. Rendered as the enum's name.
     * <p>
     * Here because a client that has to say whether a dataset is a microarray or a sequencing run
     * was otherwise reduced to matching the platform's NAME against a pattern — which reads
     * correctly for {@code Affymetrix GeneChip …} by luck and returns nothing for a sequencing
     * platform that does not say so in its name (uib, 2026-08-28).
     */
    @Nullable
    private String technologyType;

    /**
     * The identity-only form, for the callers that select two columns.
     */
    public ArrayDesignReferenceValueObject( Long id, @Nullable String shortName ) {
        this( id, shortName, null, null );
    }

    /**
     * Identity plus name, for the callers that have no technology type in hand.
     */
    public ArrayDesignReferenceValueObject( Long id, @Nullable String shortName, @Nullable String name ) {
        this( id, shortName, name, null );
    }

    @Override
    public String toString() {
        return "ArrayDesignReferenceValueObject [id=" + id
                + ( shortName != null ? ", shortName=" + shortName : "" ) + "]";
    }

    /**
     * Project a full {@link ArrayDesignValueObject} down to this shape at serialization time.
     * <p>
     * For a field that holds a full platform VO but only ever needs to name it. Applied with
     * {@code @JsonSerialize(converter = …)} plus a matching
     * {@code @Schema(implementation = ArrayDesignReferenceValueObject.class)}, it changes the wire
     * shape without changing the Java type, which is what lets a payload shrink while in-JVM readers
     * of the same field keep the fields they consume.
     *
     * @see ubic.gemma.model.expression.bioAssay.BioAssayValueObject#getArrayDesign()
     */
    public static class FromArrayDesignValueObject extends StdConverter<ArrayDesignValueObject, ArrayDesignReferenceValueObject> {

        @Override
        public ArrayDesignReferenceValueObject convert( @Nullable ArrayDesignValueObject value ) {
            if ( value == null ) {
                return null;
            }
            return new ArrayDesignReferenceValueObject( value.getId(), value.getShortName(), value.getName(),
                    value.getTechnologyType() );
        }
    }
}
