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

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

/**
 * @author anton
 * @version $Id$
 */
public class CompositeSequenceValueObject {

    private Long id;

    private String name;

    private String description;
    private ArrayDesignValueObject arrayDesign;

    // for java bean contract
    public CompositeSequenceValueObject() {

    }

    public CompositeSequenceValueObject( CompositeSequence cs ) {
        this.id = cs.getId();
        this.name = cs.getName();
        this.description = cs.getDescription();
        this.arrayDesign = new ArrayDesignValueObject();
        arrayDesign.setId( cs.getArrayDesign().getId() );
    }

    public CompositeSequenceValueObject( Describable describable, ArrayDesignValueObject arrayDesign ) {
        super();
        this.id = describable.getId();
        this.name = describable.getName();
        this.description = describable.getDescription();
        this.arrayDesign = arrayDesign;
    }

    public CompositeSequenceValueObject( Long id, String name, String description, ArrayDesignValueObject arrayDesign ) {
        super();
        this.id = id;
        this.name = name;
        this.description = description;
        this.arrayDesign = arrayDesign;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CompositeSequenceValueObject other = ( CompositeSequenceValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) ) return false;
        return true;
    }

    public ArrayDesignValueObject getArrayDesign() {
        return arrayDesign;
    }

    // all getters and setters required for java bean contract

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    public void setArrayDesign( ArrayDesignValueObject arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setName( String name ) {
        this.name = name;
    }

}
