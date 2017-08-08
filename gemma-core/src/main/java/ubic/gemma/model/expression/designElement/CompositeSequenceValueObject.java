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

import ubic.gemma.core.analysis.sequence.GeneMappingSummary;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author anton
 */
public class CompositeSequenceValueObject extends IdentifiableValueObject<CompositeSequence> implements Serializable {

    private static final long serialVersionUID = 4915680501039784666L;

    private String name;
    private String description;
    private ArrayDesignValueObject arrayDesign;
    private Collection<GeneMappingSummary> geneMappingSummaries;

    /**
     * Required when using the class as a spring bean.
     */
    public CompositeSequenceValueObject() {
    }

    public CompositeSequenceValueObject(Long id) {
        super(id);
    }

    public CompositeSequenceValueObject( CompositeSequence cs ) {
        this(cs.getId(), cs.getName(), cs.getDescription(), new ArrayDesignValueObject(cs.getArrayDesign().getId()));
    }

    public CompositeSequenceValueObject( Describable describable, ArrayDesignValueObject arrayDesignVo ) {
        this(describable.getId(), describable.getName(), describable.getDescription(), arrayDesignVo);
    }

    public CompositeSequenceValueObject( Long id, String name, String description,
            ArrayDesignValueObject arrayDesign ) {
        super(id);
        this.name = name;
        this.description = description;
        this.arrayDesign = arrayDesign;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        CompositeSequenceValueObject other = ( CompositeSequenceValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null )
                return false;
        } else if ( !id.equals( other.id ) )
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    public ArrayDesignValueObject getArrayDesign() {
        return arrayDesign;
    }

    public void setArrayDesign( ArrayDesignValueObject arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public Collection<GeneMappingSummary> getGeneMappingSummaries() {
        return geneMappingSummaries;
    }

    public void setGeneMappingSummaries( Collection<GeneMappingSummary> geneMappingSummaries ) {
        this.geneMappingSummaries = geneMappingSummaries;
    }
}
