/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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

import org.hibernate.search.annotations.*;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.util.Objects;

/**
 * A "Probe set" (Affymetrix) or a "Probe" (other types of arrays). The sequence referred to is a "target sequence"
 * (Affymetrix), oligo (oligo arrays) or cDNA clone/EST (cDNA arrays)
 */
@Indexed
public class CompositeSequence extends AbstractDescribable {

    private BioSequence biologicalCharacteristic;
    private ArrayDesign arrayDesign;

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    @Field
    public String getName() {
        return super.getName();
    }

    @Override
    @Field(store = Store.YES)
    public String getDescription() {
        return super.getDescription();
    }

    public ArrayDesign getArrayDesign() {
        return this.arrayDesign;
    }

    public void setArrayDesign( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    /**
     * @return The sequence for this composite sequence.
     */
    @IndexedEmbedded
    public BioSequence getBiologicalCharacteristic() {
        return this.biologicalCharacteristic;
    }

    public void setBiologicalCharacteristic( BioSequence biologicalCharacteristic ) {
        this.biologicalCharacteristic = biologicalCharacteristic;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( !( obj instanceof CompositeSequence ) )
            return false;
        CompositeSequence other = ( CompositeSequence ) obj;
        if ( getId() != null && other.getId() != null ) {
            return getId().equals( other.getId() );
        }
        return Objects.equals( getName(), other.getName() );
    }

    public static final class Factory {

        public static CompositeSequence newInstance() {
            return new CompositeSequence();
        }

        public static CompositeSequence newInstance( String name ) {
            CompositeSequence cs = new CompositeSequence();
            cs.setName( name );
            return cs;
        }

        public static CompositeSequence newInstance( String name, ArrayDesign ad ) {
            CompositeSequence cs = new CompositeSequence();
            cs.setName( name );
            cs.setArrayDesign( ad );
            return cs;
        }

        public static CompositeSequence newInstance( String name, ArrayDesign ad, BioSequence bioSequence ) {
            CompositeSequence cs = newInstance( name, ad );
            cs.setBiologicalCharacteristic( bioSequence );
            return cs;
        }
    }

}