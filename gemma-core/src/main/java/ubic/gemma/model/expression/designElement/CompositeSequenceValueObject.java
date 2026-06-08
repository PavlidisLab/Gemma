/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.model.expression.designElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;
import ubic.gemma.model.analysis.sequence.GeneMappingSummary;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

import java.util.Collection;

/**
 * @author anton
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
@Getter
@Setter
public class CompositeSequenceValueObject extends IdentifiableValueObject<CompositeSequence> {

    private static final long serialVersionUID = 4915680501039784666L;

    private String name;
    private String description;
    private ArrayDesignValueObject arrayDesign;
    @JsonIgnore
    private Collection<GeneMappingSummary> geneMappingSummaries;
    /**
     * Raw probe sequence from the associated {@code BioSequence.sequence}.
     * Populated only when the caller opts in via {@code ?withSequence=true}
     * on the platform-elements endpoint (otherwise null and elided from the
     * wire by {@code @JsonInclude(NON_NULL)}). Kept out of the default
     * response because sequences are 25-300bp per probe and would inflate
     * a 22k-element platform listing by ~1 MB.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String sequence;
    /**
     * Pre-computed length from {@code BioSequence.length}, exposed alongside
     * {@code sequence}. Independent so a caller can request length without
     * paying the full-string cost (future-proofing; for now both come together
     * with {@code ?withSequence=true}).
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long sequenceLength;

    /**
     * Required when using the class as a spring bean.
     */
    public CompositeSequenceValueObject() {
        super();
    }

    public CompositeSequenceValueObject( Long id ) {
        super( id );
    }

    public CompositeSequenceValueObject( CompositeSequence cs ) {
        // eagerly fetched in entity definition
        this( cs, new ArrayDesignValueObject( cs.getArrayDesign() ) );
    }

    /**
     * Constructor that reuses an existing {@link ArrayDesignValueObject}.
     */
    public CompositeSequenceValueObject( CompositeSequence cs, ArrayDesignValueObject arrayDesign ) {
        super( cs );
        this.name = cs.getName();
        this.description = cs.getDescription();
        this.arrayDesign = arrayDesign;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        CompositeSequenceValueObject other = ( CompositeSequenceValueObject ) obj;
        if ( id == null ) {
            return other.id == null;
        } else
            return id.equals( other.id );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }
}
