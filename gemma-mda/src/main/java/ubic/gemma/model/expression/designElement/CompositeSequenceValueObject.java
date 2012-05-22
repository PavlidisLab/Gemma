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
    public CompositeSequenceValueObject( ) {
        
    }
    
    public CompositeSequenceValueObject( Long id, String name, String description, ArrayDesignValueObject arrayDesign ) {
        super();
        this.id = id;
        this.name = name;
        this.description = description;
        this.arrayDesign = arrayDesign;
    }

    
    public CompositeSequenceValueObject( Describable describable, ArrayDesignValueObject arrayDesign ) {
        super();
        this.id = describable.getId();
        this.name = describable.getName();
        this.description = describable.getDescription();
        this.arrayDesign = arrayDesign;
    }

    // all getters and setters required for java bean contract
    
    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public ArrayDesignValueObject getArrayDesign() {
        return arrayDesign;
    }

    public void setArrayDesign( ArrayDesignValueObject arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }
    
    
}
